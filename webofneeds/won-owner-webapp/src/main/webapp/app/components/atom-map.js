/**
 * Created by fsuda on 21.08.2017.
 */
import angular from "angular";
import inviewModule from "angular-inview";

import { attach } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import L from "../leaflet-bundleable.js";
import { initLeaflet } from "../leaflet-bundleable.js";
import { connect2Redux } from "../configRedux.js";

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

      this.$scope.$watchGroup(
        ["self.locations", "self.currentLocation"],
        newValues => {
          this.updateMap(newValues[0]);
          this._mapHasBeenAutoCentered = true;
        }
      );

      const selectFromState = () => {
        return {};
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.locations", "self.currentLocation"],
        this
      );
    }

    mapInView(inviewInfo) {
      if (inviewInfo.changed) {
        this.map.invalidateSize();
      }
    }

    updateMap(locations) {
      let markedLocations = [];

      for (let location of locations) {
        if (!location || !location.get("lat") || !location.get("lng")) {
          console.warn("no marker set for location: ", location);
          continue;
        }

        markedLocations.push(location);
      }

      if (markedLocations.length === 0 && !this.currentLocation) {
        console.warn("no markers set for locations: ", locations);
        return;
      }
      this.placeMarkers(markedLocations);

      if (this.markers.length === 0) {
        console.warn("no map coordinates found for locations: ", locations);
        return;
      }

      this.map.fitBounds(
        L.featureGroup(this.markers)
          .getBounds()
          .pad(0.5),
        this.currentLocation
          ? markedLocations.length === 0
            ? { maxZoom: 10, animate: false }
            : { animate: false }
          : { maxZoom: 14, animate: false }
      );

      this.mapAlreadyInitialized = true;
    }

    placeMarkers(locations) {
      if (this.markers) {
        //remove previously placed markers
        for (let m of this.markers) {
          this.map.removeLayer(m);
        }
      }

      this.markers = locations.map(
        location => L.marker([location.get("lat"), location.get("lng")]) //.bindPopup(location.name)
      );

      if (
        this.currentLocation &&
        this.currentLocation.get("lat") &&
        this.currentLocation.get("lng")
      ) {
        const currentLocationMarkerIcon = L.divIcon({
          className: "wonCurrentLocationMarkerIcon",
        });

        this.markers.push(
          L.marker(
            [this.currentLocation.get("lat"), this.currentLocation.get("lng")],
            { icon: currentLocationMarkerIcon }
          )
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
      currentLocation: "=",
    },
  };
}

export default angular
  .module("won.owner.components.atomMapModule", [inviewModule.name])
  .directive("wonAtomMap", genComponentConf).name;
