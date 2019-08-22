/**
 * Created by fsuda on 21.08.2017.
 */

import React from "react";
import { Map, Marker, TileLayer } from "react-leaflet";
import L from "leaflet";
import VisibilitySensor from "react-visibility-sensor";
import PropTypes from "prop-types";

import "leaflet/dist/leaflet.css";

const currentLocationIcon = L.divIcon({
  className: "wonCurrentLocationMarkerIcon",
});

export default class WonAtomMap extends React.Component {
  render() {
    // TODO Impl Center and zoom to all markers (work with bounds: <Map ref="map" bounds={bounds} className="atom-map__mapmount" boundsOptions={{padding: [50, 50]}} zoom={zoom}>)
    // TODO Impl MarkerClusters see -> https://yuzhva.github.io/react-leaflet-markercluster/

    const locations = this.props && this.props.locations;
    const currentLocation = this.props && this.props.currentLocation;
    const disableControls = !!(this.props && this.props.disableControls);
    const zoom = 13;
    if (locations && locations.length > 0) {
      const currentLocationTupel = currentLocation && [
        currentLocation.get("lat"),
        currentLocation.get("lng"),
      ];
      const firstLocationTupel = locations[0] && [
        locations[0].get("lat"),
        locations[0].get("lng"),
      ];

      const locationMarkers = locations.map((location, index) => {
        const lat = location && location.get("lat");
        const lng = location && location.get("lng");
        if (lat != undefined && lng != undefined) {
          const locationTupel = [lat, lng];
          //We should use key here, but i do not know if we can find a unique key from the provided properties :-/
          return (
            <Marker
              key={locationTupel.toString() + "/" + index}
              position={locationTupel}
            />
          );
        }
      });

      const currentLocationMarker = currentLocationTupel ? (
        <Marker position={currentLocationTupel} icon={currentLocationIcon} />
      ) : (
        undefined
      );

      return (
        <VisibilitySensor
          partialVisibility={true}
          offset={{ top: -300, bottom: -300 }}
        >
          {({ isVisible }) => {
            if (isVisible) {
              return (
                <Map
                  center={
                    currentLocationTupel
                      ? currentLocationTupel
                      : firstLocationTupel
                  }
                  className="atom-map__mapmount"
                  zoom={zoom}
                  zoomControl={!disableControls}
                >
                  <TileLayer
                    attribution="&amp;copy <a href=&quot;http://osm.org/copyright&quot;>OpenStreetMap</a> contributors"
                    url="https://www.matchat.org/tile/{z}/{x}/{y}.png"
                  />
                  {locationMarkers}
                  {currentLocationMarker}
                </Map>
              );
            } else {
              return (
                <div className="atom-map__mapmount atom-map__mapmount--loading">
                  <svg className="won-atom-map__spinner hspinner">
                    <use
                      xlinkHref="#ico_loading_anim"
                      href="#ico_loading_anim"
                    />
                  </svg>
                </div>
              );
            }
          }}
        </VisibilitySensor>
      );
    }
    console.debug("render with no location(s)");
    return <div />;
  }
}
WonAtomMap.propTypes = {
  locations: PropTypes.arrayOf(PropTypes.object).isRequired,
  currentLocation: PropTypes.object,
  disableControls: PropTypes.bool,
};
