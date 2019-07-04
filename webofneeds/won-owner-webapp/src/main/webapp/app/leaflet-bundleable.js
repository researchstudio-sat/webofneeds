/**
 * Created by ksinger on 11.08.2016.
 */

import "leaflet";

import icon from "leaflet/dist/images/marker-icon.png";
import icon2x from "leaflet/dist/images/marker-icon-2x.png";
import shadow from "leaflet/dist/images/marker-shadow.png";

/* global L */
L.Icon.Default.prototype._getIconUrl = name => {
  if (L.Browser.retina && name === "icon") {
    name += "-2x";
  }
  if (name == "icon") {
    if (L.Browser.retina) {
      return icon2x;
    } else {
      return icon;
    }
  } else {
    return shadow;
  }
};

export default L;

export function initLeaflet(mapMount, overrideOptions) {
  if (!L) {
    throw new Error(
      "Tried to initialize a leaflet widget while leaflet wasn't loaded."
    );
  }
  Error;

  const baseMaps = initLeafletBaseMaps();

  const map = L.map(
    mapMount,
    Object.assign(
      {
        center: [37.44, -42.89], //centered on north-west africa
        zoom: 1, //world-map
        layers: [baseMaps["Detailed default map"]], //initially visible layers
      },
      overrideOptions
    )
  ); //.setView([51.505, -0.09], 13);

  //map.fitWorld() // shows every continent twice :|
  map.fitBounds([[-80, -190], [80, 190]]); // fitWorld without repetition

  // Force it to adapt to actual size
  // for some reason this doesn't happen by default
  // when the map is within a tag.
  // this.map.invalidateSize();
  // ^ doesn't work (needs to be done manually atm);

  return map;
}

function initLeafletBaseMaps() {
  if (!L) {
    throw new Error(
      "Tried to initialize leaflet map-sources while leaflet wasn't loaded."
    );
  }

  //const secureOsmSource = "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"; // secure osm.org
  const secureOsmSource = "https://www.matchat.org/tile/{z}/{x}/{y}.png"; // TODO: use own tile server instead of proxy
  const secureOsm = L.tileLayer(secureOsmSource, {
    attribution:
      '&copy; <a href="http://osm.org/copyright" target="_blank">OpenStreetMap</a> contributors',
  });

  const baseMaps = {
    "Detailed default map": secureOsm,
  };

  return baseMaps;
}

export function leafletBounds(location) {
  if (location && location.nwCorner && location.seCorner) {
    return new L.latLngBounds(
      new L.LatLng(location.nwCorner.lat, location.nwCorner.lng),
      new L.latLng(location.seCorner.lat, location.seCorner.lng)
    );
  }
}
