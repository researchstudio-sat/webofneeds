/**
 * Created by quasarchimaere on 17.06.2019.
 */
import GeoPoint from "geopoint";
import Immutable from "immutable";
import { isValidNumber } from "../utils.js";

/**
 * Searches the nominatim address-lookup service and
 * returns a list with the search results.
 */
export function searchNominatim(searchStr) {
  const url =
    "https://nominatim.openstreetmap.org/search" +
    "?q=" +
    encodeURIComponent(searchStr) +
    "&format=json";
  return fetchJSON(url);
}

export function reverseSearchNominatim(lat, lon, zoom) {
  let url =
    "https://nominatim.openstreetmap.org/reverse" +
    "?lat=" +
    lat +
    "&lon=" +
    lon +
    "&format=json";

  if (isValidNumber(zoom)) {
    url += "&zoom=" + Math.max(0, Math.min(zoom, 18));
  }

  let json = fetchJSON(url).catch(function() {
    const distance = 0.2;
    const gp = new GeoPoint(lat, lon);
    const bBox = gp.boundingCoordinates(distance, true);
    return {
      display_name: "-",
      lat: lat,
      lon: lon,
      boundingbox: [
        bBox[0]._degLat,
        bBox[1]._degLat,
        bBox[0]._degLon,
        bBox[1]._degLon,
      ],
    };
  });
  return json;
}

export function scrubSearchResults(searchResults) {
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

/**
 * drop info not stored in rdf, thus info that we
 * couldn't restore for previously used locations
 */
export function nominatim2draftLocation(searchResult) {
  const b = searchResult.boundingbox;
  return {
    name: searchResult.display_name,
    lng: Number.parseFloat(searchResult.lon),
    lat: Number.parseFloat(searchResult.lat),
    //importance: searchResult.importance,
    nwCorner: {
      lat: Number.parseFloat(b[0]),
      lng: Number.parseFloat(b[2]),
    },
    seCorner: {
      lat: Number.parseFloat(b[1]),
      lng: Number.parseFloat(b[3]),
    },
    //bounds: [
    //    [ Number.parseFloat(b[0]), Number.parseFloat(b[2]) ], //north-western point
    //    [ Number.parseFloat(b[1]), Number.parseFloat(b[3]) ] //south-eastern point
    //],
  };
}

function fetchJSON(url) {
  return fetch(url, {
    method: "get",
    //credentials: "same-origin",
    headers: { Accept: "application/json" },
  }).then(resp => {
    /*
             * handle errors and read json-data
             */
    const errorMsg =
      "GET to " +
      url +
      " failed with (" +
      resp.status +
      "): " +
      resp.statusText +
      "\n" +
      resp;
    if (resp.status !== 200) {
      throw new Error(errorMsg);
    } else {
      try {
        return resp.json();
      } catch (jsonParseError) {
        // nominatim responded with an HTTP-200 with an error html-page m(
        const e = new Error(errorMsg);
        e.originalErr = jsonParseError;
        throw e;
      }
    }
  });
}
