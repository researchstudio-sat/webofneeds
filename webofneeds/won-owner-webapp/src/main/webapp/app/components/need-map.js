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

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
        <div class="need-map__mapmount"
             in-view="$inview && self.mapInView($inviewInfo)">
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      //TODO: debug; deleteme
      window.map4dbg = this;

      this.map = initLeaflet(this.mapMount());

      /* TODO: This does not work. 
        I suppose this is because I also check this.locationRoute, which is probably not set yet.
        -> ask fabian if watch is there in the first place because digest cycles suck and the location isn't loaded otherwise
        -> see if there's any way to reliably use this.locationRoute, as it's kind of important to know here

        intention: if a location is shown, do everything as before
        if a route is show, use the updateRouteMap method, that shows routes

        also note: using schema definitions instead of won definitions means that it's a lot of fun to get info
        -> look at post-info to see details.
       */
      this.$scope.$watch("self.locations", newLocations => {
        if (newLocations) {
          this.updateMap(newLocations);
          this._mapHasBeenAutoCentered = true;
        }
      });

      // const selectFromState = state => {
      //   const post = this.uri && state.getIn(["needs", this.uri]);
      //   const isSeeksPart = post && post.get(this.isSeeks);

      //   const location = isSeeksPart && isSeeksPart.get("location");
      //   const travelAction = isSeeksPart && isSeeksPart.get("travelAction");

      //   return {
      //     location: location,
      //     travelAction: travelAction,
      //   };
      // };
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

    updateMap(locations) {
      let markedLocations = [];
      let boundCoords = [];

      for (let location of locations) {
        if (!location || !location.get("lat") || !location.get("lng")) {
          console.log("no marker set for location: ", location);
          continue;
        }

        markedLocations.push(location);
        boundCoords.push(
          new L.LatLng(location.get("lat"), location.get("lng"))
        );

        if (location.get("nwCorner"))
          boundCoords.push(
            new L.latLng(
              location.getIn(["nwCorner", "lat"]),
              location.getIn(["nwCorner", "lng"])
            )
          );

        if (location.get("seCorner"))
          boundCoords.push(
            new L.latLng(
              location.getIn(["seCorner", "lat"]),
              location.getIn(["seCorner", "lng"])
            )
          );
      }

      if (markedLocations.length === 0) {
        console.log("no markers set for locations: ", locations.toJS());
        return;
      }
      this.placeMarkers(markedLocations);

      if (boundCoords.length === 0) {
        console.log(
          "no map coordinates found for locations: ",
          locations.toJS()
        );
        return;
      }
      this.map.fitBounds(boundCoords, { maxZoom: 14 });

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

      for (let m of this.markers) {
        this.map.addLayer(m);
      }
    }

    mapMountNg() {
      return this.domCache.ng(".need-map__mapmount");
    }
    mapMount() {
      return this.domCache.dom(".need-map__mapmount");
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
      //uri: "=",
      //isSeeks: "=",
    },
  };
}

export default angular
  .module("won.owner.components.needMapModule", [inviewModule.name])
  .directive("wonNeedMap", genComponentConf).name;
