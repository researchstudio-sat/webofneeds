/**
 * Created by ksinger on 15.07.2016.
 */

import angular from "angular";
import Immutable from "immutable"; // also exports itself as (window).L
import L from "../../../leaflet-bundleable.js";
import {
  attach,
  searchNominatim,
  reverseSearchNominatim,
  nominatim2draftLocation,
  delay,
  getIn,
} from "../../../utils.js";
import { doneTypingBufferNg, DomCache } from "../../../cstm-ng-utils.js";

import { initLeaflet, leafletBounds } from "../../../won-utils.js";

import "style/_locationpicker.scss";

import "leaflet/dist/leaflet.css";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
        <!-- LOCATION SEARCH BOX -->
        <div class="lp__searchbox">
            <input
                type="text"
                class="lp__searchbox__inner"
                placeholder="{{self.detail.placeholder}}"
                ng-class="{'lp__searchbox__inner--withreset' : self.showResetButton}"/>
            <svg class="lp__searchbox__icon clickable" 
                 style="--local-primary:var(--won-primary-color);"
                 ng-if="self.showResetButton"
                 ng-click="self.resetLocationAndSearch()">
                    <use xlink:href="#ico36_close" href="#ico36_close"></use>
            </svg>
        </div>

        <!-- LIST OF SUGGESTED LOCATIONS -->
        <ul class="lp__searchresults" ng-class="{ 
            'lp__searchresults--filled': self.showResultDropDown(), 
            'lp__searchresults--empty': !self.showResultDropDown() 
        }">
            <!-- CURRENT GEOLOCATION -->
            <li class="lp__searchresult" 
                ng-if="self.showCurrentLocationResult()">
                <svg class="lp__searchresult__icon" style="--local-primary:var(--won-subtitle-gray);">
                    <use xlink:href="#ico16_indicator_location" href="#ico36_location_current"></use>
                </svg>
                <a class="lp__searchresult__text" href=""
                    ng-click="self.selectedLocation(self.currentLocation)">
                    {{ self.currentLocation.name }}
                </a>
            </li>
            <!-- PREVIOUS LOCATION -->
            <li class="lp__searchresult" 
                ng-if="self.showPrevLocationResult()">
                <svg class="lp__searchresult__icon" style="--local-primary:var(--won-subtitle-gray);">
                    <!-- TODO: create and use a more appropriate icon here -->
                    <use xlink:href="#ico16_indicator_location" href="#ico16_indicator_location"></use>
                </svg>
                <a class="lp__searchresult__text" href=""
                    ng-click="self.selectedLocation(self.previousLocation)">
                    {{ self.previousLocation.name }}
                </a>
                (previous)
            </li>
            <!-- SEARCH RESULTS -->
            <li class="lp__searchresult" 
                ng-repeat="result in self.searchResults">
                <svg class="lp__searchresult__icon" style="--local-primary:var(--won-subtitle-gray);">
                    <use xlink:href="#ico16_indicator_location" href="#ico16_indicator_location"></use>
                </svg>
                <a class="lp__searchresult__text" href=""
                    ng-click="self.selectedLocation(result)">
                    {{ result.name }}
                </a>
            </li>
        </ul>
        <div class="lp__mapmount" in-view="$inview && self.mapInView($inviewInfo)"></div>
            `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      this.map = initLeaflet(this.mapMount());
      this.map.on("click", e => onMapClick(e, this));

      this.locationIsSaved = !!this.initialValue;
      this.pickedLocation = this.initialValue;
      this.previousLocation = undefined;
      this.showResetButton = false;

      window.lp4dbg = this;

      // needs to happen after constructor finishes, otherwise
      // the component's callbacks won't be registered.
      delay(0).then(() => this.determineCurrentLocation());

      doneTypingBufferNg(e => this.doneTyping(e), this.textfieldNg(), 300);
    }

    /**
     * Checks validity and uses callback method
     */
    update(location) {
      if (location) {
        // check validity?
        this.onUpdate({ value: location });
      } else {
        this.onUpdate({ value: undefined });
      }
    }

    mapInView(inviewInfo) {
      if (inviewInfo.changed) {
        this.map.invalidateSize();
      }
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

    resetSearchResults() {
      this.searchResults = undefined;
      this.lastSearchedFor = undefined;
      this.placeMarkers([]);
    }

    resetLocationAndSearch() {
      this.resetLocation();
      this.textfield().value = "";
    }

    resetLocation() {
      this.previousLocation = this.pickedLocation;

      this.locationIsSaved = false;
      this.pickedLocation = undefined;
      this.placeMarkers([]);
      this.showResetButton = false;

      this.update(undefined);
    }

    selectedLocation(location) {
      // callback to update location in isseeks
      this.update(location);
      this.locationIsSaved = true;
      this.pickedLocation = location;

      this.resetSearchResults(); // picked one, can hide the rest if they were there
      this.textfield().value = location.name;
      this.showResetButton = true;

      this.placeMarkers([location]);
      this.map.fitBounds(leafletBounds(location), { animate: true });
      this.markers[0].openPopup();
    }

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

    determineCurrentLocation() {
      // check if there's any saved location to display instead
      if (this.initialValue) {
        // constructor may not be done in time, so set values here again.
        this.locationIsSaved = true;
        this.pickedLocation = this.initialValue;

        const initialLat = this.pickedLocation.lat;
        const initialLng = this.pickedLocation.lng;
        const initialZoom = 13; // arbitrary zoom level as there's none available

        // center map around current location
        this.map.setView(new L.LatLng(initialLat, initialLng), initialZoom);
        this.map.invalidateSize();

        this.textfield().value = this.pickedLocation.name;
        this.showResetButton = true;
        this.placeMarkers([this.pickedLocation]);
        this.markers[0].openPopup();
      }

      // check for current geolocation
      if ("geolocation" in navigator) {
        navigator.geolocation.getCurrentPosition(
          currentLocation => {
            const geoLat = currentLocation.coords.latitude;
            const geoLng = currentLocation.coords.longitude;
            const geoZoom = 13; // TODO: use `currentLocation.coords.accuracy` to control coarseness of query / zoom-level

            // center map around geolocation only if there's no initial location
            if (!this.initialValue) {
              this.map.setView(new L.LatLng(geoLat, geoLng), geoZoom);
              this.map.invalidateSize();
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

      this.$scope.$apply();
    }

    showCurrentLocationResult() {
      return !this.locationIsSaved && this.currentLocation;
    }

    showPrevLocationResult() {
      return (
        !this.locationIsSaved &&
        this.previousLocation &&
        getIn(this, ["previousLocation", "name"]) !==
          getIn(this, ["currentLocation", "name"])
      );
    }

    showResultDropDown() {
      return (
        (this.searchResults && this.searchResults.length > 0) ||
        this.showPrevLocationResult() ||
        this.showCurrentLocationResult()
      );
    }

    textfieldNg() {
      return this.domCache.ng(".lp__searchbox__inner");
    }

    textfield() {
      return this.domCache.dom(".lp__searchbox__inner");
    }

    mapMountNg() {
      return this.domCache.ng(".lp__mapmount");
    }

    mapMount() {
      return this.domCache.dom(".lp__mapmount");
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      onUpdate: "&",
      initialValue: "=",
      detail: "=",
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
  .module("won.owner.components.locationPicker", [])
  .directive("wonLocationPicker", genComponentConf).name;

window.searchNominatim4dbg = searchNominatim;
window.reverseSearchNominatim4dbg = reverseSearchNominatim;
window.nominatim2wonLocation4dbg = nominatim2draftLocation;
