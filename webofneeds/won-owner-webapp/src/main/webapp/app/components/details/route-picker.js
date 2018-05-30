import angular from "angular";
import Immutable from "immutable"; // also exports itself as (window).L
import L from "../../leaflet-bundleable.js";
import {
  attach,
  searchNominatim,
  reverseSearchNominatim,
  nominatim2draftLocation,
  leafletBounds,
  delay,
  getIn,
} from "../../utils.js";
import { doneTypingBufferNg, DomCache } from "../../cstm-ng-utils.js";

import { initLeaflet } from "../../won-utils.js";

const serviceDependencies = ["$scope", "$element", "$sce"];
function genComponentConf() {
  const prevLocationBlock = (selectLocationFnctName, prevLocation) => `
  <!-- PREVIOUS LOCATION -->
  <li class="rp__searchresult" 
      ng-if="self.showPrevLocationResult()">
      <svg class="rp__searchresult__icon" style="--local-primary:var(--won-subtitle-gray);">
          <!-- TODO: create and use a more appropriate icon here -->
          <use xlink:href="#ico16_indicator_location" href="#ico16_indicator_location"></use>
      </svg>
      <a class="rp__searchresult__text" href=""
          ng-click="${selectLocationFnctName}(${prevLocation})"
          ng-bind-html="self.highlight(${prevLocation}.name, self.lastSearchedFor)">
      </a>
      (previous)
  </li>`;

  const searchResultsBlock = selectLocationFnctName => `<!-- SEARCH RESULTS -->
  <li class="rp__searchresult" 
      ng-repeat="result in self.searchResults">
      <svg class="rp__searchresult__icon" style="--local-primary:var(--won-subtitle-gray);">
          <use xlink:href="#ico16_indicator_location" href="#ico16_indicator_location"></use>
      </svg>
      <a class="rp__searchresult__text" href=""
          ng-click="${selectLocationFnctName}(result)"
          ng-bind-html="self.highlight(result.name, self.lastSearchedFor)">
      </a>
  </li>`;

  const template = `
        <!-- FROM LOCATION SEARCH BOX -->
        <div class="rp__searchbox">
            <input
                type="text"
                id="rp__from-searchbox__inner"
                class="rp__searchbox__inner"
                placeholder="Start Location"
                ng-class="{'rp__searchbox__inner--withreset' : self.showFromResetButton}"/>
            <svg class="rp__searchbox__icon clickable" 
                 style="--local-primary:var(--won-primary-color);"
                 ng-if="self.showFromResetButton"
                 ng-click="self.resetFromLocationAndSearch()">
                    <use xlink:href="#ico36_close" href="#ico36_close"></use>
            </svg>
        </div>
        <!-- LIST OF SUGGESTED LOCATIONS -->
        <ul class="rp__searchresults" ng-class="{ 
            'rp__searchresults--filled': self.showFromResultDropDown(), 
            'rp__searchresults--empty': !self.showFromResultDropDown() 
        }">
            <!-- CURRENT GEOLOCATION -->
            <li class="rp__searchresult" 
                ng-if="self.showCurrentLocationResult()">
                <svg class="rp__searchresult__icon" style="--local-primary:var(--won-subtitle-gray);">
                    <use xlink:href="#ico16_indicator_location" href="#ico36_location_current"></use>
                </svg>
                <a class="rp__searchresult__text" href=""
                    ng-click="self.selectedFromLocation(self.currentLocation)"
                    ng-bind-html="self.highlight(self.currentLocation.name, self.lastSearchedFor)">
                </a>
            </li>
            ${prevLocationBlock(
              "self.selectedFromLocation",
              "self.previousFromLocation"
            )}
            ${searchResultsBlock("self.selectedFromLocation")}
        </ul>

        <!-- TO LOCATION SEARCH BOX -->
        <div class="rp__searchbox">
            <input
                type="text"
                id="rp__to-searchbox__inner"
                class="rp__searchbox__inner"
                placeholder="Destination"
                ng-class="{'rp__searchbox__inner--withreset' : self.showToResetButton}"/>
            <svg class="rp__searchbox__icon clickable" 
                 style="--local-primary:var(--won-primary-color);"
                 ng-if="self.showToResetButton"
                 ng-click="self.resetToLocationAndSearch()">
                    <use xlink:href="#ico36_close" href="#ico36_close"></use>
            </svg>
        </div>
        <!-- LIST OF SUGGESTED LOCATIONS -->
        <ul class="rp__searchresults" ng-class="{ 
            'rp__searchresults--filled': self.showToResultDropDown(), 
            'rp__searchresults--empty': !self.showToResultDropDown() 
        }">
            ${prevLocationBlock(
              "self.selectedToLocation",
              "self.previousToLocation"
            )}
            ${searchResultsBlock("self.selectedToLocation")}
        </ul>

        <div class="rp__mapmount" id="rp__mapmount"></div>
            `;

  // TODO: add attribute if not valid -> use attribute to disable publish
  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      this.map = initLeaflet(this.mapMount());
      this.map.on("click", e => onMapClick(e, this));

      // debug output
      window.rp4dbg = this;

      // TODO: do I need this?
      //   this.locationIsSaved = !!this.initialLocation;

      this.addedFromLocation = this.initialFromLocation;
      this.addedToLocation = this.initialToLocation;

      this.previousFromLocation = undefined;
      this.previousToLocation = undefined;

      this.showFromResetButton = false;
      this.showToResetButton = false;

      // needs to happen after constructor finishes, otherwise
      // the component's callbacks won't be registered.
      delay(0).then(() => this.showInitialLocations());

      // only works if we have access to the current location
      // TODO: if we do have access, set this as the default fromLocation?
      // Issue: if form checking is implemented checking whether both/no fields are filled,
      // putting down geoLocation as fromLocation by default requires user to make the form valid again
      // just closing the picker would result in an error message!
      this.determineCurrentLocation();

      doneTypingBufferNg(e => this.doneTyping(e), this.textfieldNg(), 300);
    }

    showInitialLocations() {
      // TODO: set invalid attribute element.validity.invalid; if invalid
      // TODO: zoom/center to show one/both markers?
      this.addedFromLocation = this.initialFromLocation;
      this.addedToLocation = this.initialToLocation;

      let markedLocations = [];

      if (this.initialFromLocation) {
        markedLocations.push(this.initialFromLocation);
        this.showFromResetButton = true;
        this.fromTextfield().value = this.initialFromLocation.name;
      }

      if (this.initialToLocation) {
        markedLocations.push(this.initialToLocation);
        this.showToResetButton = true;
        this.toTextfield().value = this.initialToLocation.name;
      }

      this.placeMarkers(markedLocations);

      this.$scope.$apply();
    }

    placeMarkers(locations) {
      if (this.markers) {
        //remove previously placed markers
        for (let m of this.markers) {
          this.map.removeLayer(m);
        }
      }

      this.markers = locations.map(location =>
        L.marker([location.lat, location.lng]).bindPopup(location.name)
      );

      for (let m of this.markers) {
        this.map.addLayer(m);
      }
    }

    resetLocations() {
      this.resetFromLocation();
      this.resetToLocation();
    }

    resetFromLocation() {
      this.previousFromLocation = this.addedFromLocation;
      this.addedFromLocation = undefined;

      let markers = this.addedToLocation || [];
      this.placeMarkers(markers);

      this.showFromResetButton = false;
      this.fromTextfield().value = "";

      this.onRouteUpdated({
        fromLocation: undefined,
        toLocation: this.addedToLocation,
      });
    }

    resetToLocation() {
      this.previousToLocation = this.addedToLocation;
      this.addedToLocation = undefined;

      let markers = this.addedFromLocation || [];
      this.placeMarkers(markers);

      this.showToResetButton = false;
      this.toTextfield().value = "";

      this.onRouteUpdated({
        fromLocation: this.addedFromLocation,
        toLocation: undefined,
      });
    }

    // TODO:
    resetSearchResults() {
      this.searchResults = undefined;
      this.lastSearchedFor = undefined;
      this.placeMarkers([]);
    }

    // TODO:
    resetLocationAndSearch() {
      this.resetLocation();
      this.textfield().value = "";
    }

    // TODO: split into from/to (or add another parameter)
    selectedLocation(location) {
      // callback to update location in isseeks
      this.onLocationPicked({ location: location });
      this.locationIsSaved = true;
      this.pickedLocation = location;

      this.resetSearchResults(); // picked one, can hide the rest if they were there
      this.textfield().value = location.name;
      this.showResetButton = true;

      this.placeMarkers([location]);
      this.map.fitBounds(leafletBounds(location), { animate: true });
      this.markers[0].openPopup();
    }

    // TODO: needs to work for both textfields
    doneTyping() {
      const text = this.textfield().value;

      this.showResetButton = false;
      this.$scope.$apply(() => {
        this.resetLocation();
      });

      if (!text) {
        this.$scope.$apply(() => {
          this.resetSearchResults();
        });
      } else {
        // TODO: sort results by distance/relevance/???
        // TODO: limit amount of shown results
        searchNominatim(text).then(searchResults => {
          const parsedResults = scrubSearchResults(searchResults, text);
          this.$scope.$apply(() => {
            this.searchResults = parsedResults;
            //this.lastSearchedFor = { name: text };
            this.lastSearchedFor = text;
          });
          this.placeMarkers(Object.values(parsedResults));
        });
      }
    }

    /**
     * Taken from <http://stackoverflow.com/questions/15519713/highlighting-a-filtered-result-in-angularjs>
     * @param text
     * @param search
     * @return {*}
     */
    highlight(text, search) {
      if (!text) {
        text = "";
      }
      if (!search) {
        return this.$sce.trustAsHtml(text);
      }
      return this.$sce.trustAsHtml(
        text.replace(
          new RegExp(search, "gi"),
          '<span class="highlightedText">$&</span>'
        )
      );
    }

    determineCurrentLocation() {
      // check if there's any saved location to display instead
      //   if (this.initialFromLocation) {
      //     // constructor may not be done in time, so set values here again.
      //     this.locationIsSaved = true;
      //     this.pickedLocation = this.initialLocation;

      //     const initialLat = this.pickedLocation.lat;
      //     const initialLng = this.pickedLocation.lng;
      //     const initialZoom = 13; // arbitrary zoom level as there's none available

      //     // center map around current location
      //     this.map.setZoom(initialZoom);
      //     this.map.panTo([initialLat, initialLng]);

      //     this.textfield().value = this.pickedLocation.name;
      //     this.showResetButton = true;
      //     this.placeMarkers([this.pickedLocation]);
      //     this.markers[0].openPopup();
      //   }

      // check for current geolocation
      if ("geolocation" in navigator) {
        navigator.geolocation.getCurrentPosition(
          currentLocation => {
            const geoLat = currentLocation.coords.latitude;
            const geoLng = currentLocation.coords.longitude;
            const geoZoom = 13; // TODO: use `currentLocation.coords.accuracy` to control coarseness of query / zoom-level

            // center map around geolocation only if there's no initial location
            if (!this.initialFromLocation) {
              this.map.setZoom(geoZoom);
              this.map.panTo([geoLat, geoLng]);
            }

            reverseSearchNominatim(geoLat, geoLng, geoZoom).then(
              searchResult => {
                const location = nominatim2draftLocation(searchResult);
                this.$scope.$apply(() => {
                  this.currentLocation = location;
                });
              }
            );
          },
          err => {
            //error handler
            if (err.code === 2) {
              alert("Position is unavailable!"); //TODO toaster
            }
          },
          {
            //options
            enableHighAccuracy: true,
            timeout: 5000,
            maximumAge: 0,
          }
        );
      }

      //this.$scope.$apply();
    }

    showFromResultDropdown() {
      let showGeo = !this.addedFromLocation && this.currentLocation;
      let showPrev =
        !this.addedFromLocation &&
        this.previousFromLocation &&
        getIn(this, ["previousFromLocation", "name"]) !==
          getIn(this, ["currentLocation", "name"]);

      return (
        (this.searchResults && this.searchResults.length > 0) ||
        showGeo ||
        showPrev
      );
    }

    showToResultDropDown() {
      let showPrev = !this.addedToLocation && this.previousToLocation;
      return (this.searchResults && this.searchResults.length > 0) || showPrev;
    }

    fromTextfieldNg() {
      return this.domCache.ng("#rp__from-searchbox__inner");
    }

    fromTextfield() {
      return this.domCache.dom("#rp__from-searchbox__inner");
    }

    toTextfieldNg() {
      return this.domCache.ng("#rp__from-searchbox__inner");
    }

    toTextfield() {
      return this.domCache.dom("#rp__from-searchbox__inner");
    }

    mapMountNg() {
      return this.domCache.ng(".rp__mapmount");
    }

    mapMount() {
      return this.domCache.dom(".rp__mapmount");
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      onRouteUpdated: "&",
      initialFromLocation: "=",
      initialToLocation: "=",
    },
    template: template,
  };
}

function scrubSearchResults(searchResults) {
  return (
    Immutable.fromJS(searchResults.map(nominatim2draftLocation))
      /*
                   * filter "duplicate" results (e.g. "Wien"
                   *  -> 1x waterway, 1x boundary, 1x place)
                   */
      .groupBy(r => r.get("name"))
      .map(sameNamedResults => sameNamedResults.first())
      .toList()
      .toJS()
  );
}

// TODO: disable this? pick from first and to only if from is selected?
function onMapClick(e, ctrl) {
  //`this` is the mapcontainer here as leaflet
  // apparently binds itself to the function.
  // This code was moved out of the controller
  // here to avoid confusion resulting from
  // this binding.
  reverseSearchNominatim(
    e.latlng.lat,
    e.latlng.lng,
    ctrl.map.getZoom() // - 1
  ).then(searchResult => {
    const location = nominatim2draftLocation(searchResult);

    //use coords of original click though (to allow more detailed control)
    location.lat = e.latlng.lat;
    location.lng = e.latlng.lng;
    ctrl.$scope.$apply(() => {
      ctrl.selectedLocation(location);
    });
  });
}

export default angular
  .module("won.owner.components.routePicker", [])
  .directive("wonRoutePicker", genComponentConf).name;

window.searchNominatim4dbg = searchNominatim;
window.reverseSearchNominatim4dbg = reverseSearchNominatim;
window.nominatim2wonLocation4dbg = nominatim2draftLocation;
