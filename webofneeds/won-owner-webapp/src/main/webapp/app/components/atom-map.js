/**
 * Created by fsuda on 21.08.2017.
 */
import angular from "angular";
import inviewModule from "angular-inview";

import { attach } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import L from "../leaflet-bundleable.js";
import { initLeaflet, connect2Redux } from "../won-utils.js";

import { DomCache } from "../cstm-ng-utils.js";

import "leaflet/dist/leaflet.css";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
        <div class="atom-map__mapmount"
             in-view="$inview && self.mapInView($inviewInfo)">
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      //TODO: debug; deleteme
      window.map4dbg = this;

      const overrideOptions = this.$element[0].hasAttribute("disable-controls")
        ? {
            dragging: false,
            //attributionControl: false,
            zoomControl: false,
            doubleClickZoom: false,
            boxZoom: false,
            scrollWheelZoom: false,
            touchZoom: false,
          }
        : {};

      this.map = initLeaflet(this.mapMount(), overrideOptions);
      this.addCurrentLocation = this.$element[0].hasAttribute(
        "add-current-location"
      );

      this.$scope.$watch("self.locations", newLocations => {
        if (newLocations) {
          if (this.addCurrentLocation && "geolocation" in navigator) {
            navigator.geolocation.getCurrentPosition(
              currentLocation => {
                const lat = currentLocation.coords.latitude;
                const lng = currentLocation.coords.longitude;

                this.updateMap(newLocations, [lat, lng]);
                this._mapHasBeenAutoCentered = true;
              },
              error => {
                //error handler
                console.error(
                  "Could not retrieve geolocation due to error: ",
                  error.code,
                  ", continuing map initialization without currentLocation. fullerror:",
                  error
                );
                this.updateMap(newLocations);
                this._mapHasBeenAutoCentered = true;
              },
              {
                //options
                enableHighAccuracy: true,
                maximumAge: 30 * 60 * 1000, //use if cache is not older than 30min
              }
            );
          } else {
            this.updateMap(newLocations);
            this._mapHasBeenAutoCentered = true;
          }
        }
      });

      const selectFromState = () => {
        return {};
      };

      connect2Redux(selectFromState, actionCreators, ["self.locations"], this);
    }

    mapInView(inviewInfo) {
      if (inviewInfo.changed) {
        this.map.invalidateSize();
      }
    }

    updateMap(locations, currentLatLng) {
      let markedLocations = [];

      for (let location of locations) {
        if (!location || !location.get("lat") || !location.get("lng")) {
          console.warn("no marker set for location: ", location);
          continue;
        }

        markedLocations.push(location);
      }

      if (markedLocations.length === 0 && !this.addCurrentLocation) {
        console.warn("no markers set for locations: ", locations);
        return;
      }
      this.placeMarkers(markedLocations, currentLatLng);

      if (this.markers.length === 0) {
        console.warn("no map coordinates found for locations: ", locations);
        return;
      }

      this.map.fitBounds(
        L.featureGroup(this.markers)
          .getBounds()
          .pad(0.5),
        this.addCurrentLocation ? {} : { maxZoom: 14 }
      );

      this.mapAlreadyInitialized = true;
    }

    placeMarkers(locations, currentLatLng) {
      if (this.markers) {
        //remove previously placed markers
        for (let m of this.markers) {
          this.map.removeLayer(m);
        }
      }

      this.markers = locations.map(
        location => L.marker([location.get("lat"), location.get("lng")]) //.bindPopup(location.name)
      );

      if (currentLatLng) {
        const currentLocationMarkerIcon = L.divIcon({
          className: "wonCurrentLocationMarkerIcon",
        });

        this.markers.push(
          L.marker(currentLatLng, { icon: currentLocationMarkerIcon })
        );
      }

      for (let m of this.markers) {
        this.map.addLayer(m);
      }
    }

    mapMountNg() {
      return this.domCache.ng(".atom-map__mapmount");
    }
    mapMount() {
      return this.domCache.dom(".atom-map__mapmount");
    }
  }
  Controller.$inject = serviceDependencies;
  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    template: template,
    scope: {
      locations: "=",
    },
  };
}

export default angular
  .module("won.owner.components.atomMapModule", [inviewModule.name])
  .directive("wonAtomMap", genComponentConf).name;
