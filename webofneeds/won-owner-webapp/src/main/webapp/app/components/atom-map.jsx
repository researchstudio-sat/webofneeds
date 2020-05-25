/**
 * Created by fsuda on 21.08.2017.
 */

import React from "react";
import { Map, Marker, TileLayer } from "react-leaflet";
import L from "leaflet";
import VisibilitySensor from "react-visibility-sensor";
import { get } from "../utils.js";
import PropTypes from "prop-types";

import "leaflet/dist/leaflet.css";
import ico_loading_anim from "~/images/won-icons/ico_loading_anim.svg";

const currentLocationIcon = L.divIcon({
  className: "wonCurrentLocationMarkerIcon",
  html: "<div class='marker'></div>",
});

const locationIcon = L.divIcon({
  className: "wonLocationMarkerIcon",
  html:
    "<svg class='marker__icon'><use xlink:href='#ico36_detail_location' href='#ico36_detail_location' /></svg>",
});

export default function WonAtomMap({
  locations,
  className,
  currentLocation,
  disableControls,
}) {
  // TODO Impl Center and zoom to all markers (work with bounds: <Map ref="map" bounds={bounds} className="atom-map__mapmount" boundsOptions={{padding: [50, 50]}} zoom={zoom}>)
  // TODO Impl MarkerClusters see -> https://yuzhva.github.io/react-leaflet-markercluster/
  const zoom = 13;
  if (locations && locations.length > 0) {
    const currentLocationTupel = currentLocation && [
      get(currentLocation, "lat"),
      get(currentLocation, "lng"),
    ];
    const firstLocationTupel = locations[0] && [
      get(locations[0], "lat"),
      get(locations[0], "lng"),
    ];

    const locationMarkers = locations.map((location, index) => {
      const lat = get(location, "lat");
      const lng = get(location, "lng");
      if (lat != undefined && lng != undefined) {
        const locationTupel = [lat, lng];
        //We should use key here, but i do not know if we can find a unique key from the provided properties :-/
        return (
          <Marker
            key={locationTupel.toString() + "/" + index}
            position={locationTupel}
            icon={locationIcon}
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
      <won-atom-map class={className ? className : ""}>
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
                    <use xlinkHref={ico_loading_anim} href={ico_loading_anim} />
                  </svg>
                </div>
              );
            }
          }}
        </VisibilitySensor>
      </won-atom-map>
    );
  }
  console.debug("render with no location(s)");
  return <div />;
}
WonAtomMap.propTypes = {
  className: PropTypes.string,
  locations: PropTypes.arrayOf(PropTypes.object).isRequired,
  currentLocation: PropTypes.object,
  disableControls: PropTypes.bool,
};
