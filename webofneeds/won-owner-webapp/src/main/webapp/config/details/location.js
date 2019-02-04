import {
  get,
  getIn,
  generateIdString,
  getFromJsonLd,
} from "../../app/utils.js";
import Immutable from "immutable";
import won from "../../app/won-es6.js";

export const location = {
  identifier: "location",
  label: "Location",
  icon: "#ico36_detail_location",
  placeholder: "Search for location",
  component: "won-location-picker",
  viewerComponent: "won-location-viewer",
  messageEnabled: true,
  parseToRDF: function({ value, identifier, contentUri }) {
    return {
      "s:location": genSPlace({
        geoData: value,
        baseUri: genDetailBaseUri(contentUri, identifier),
      }),
    };
  },
  parseFromRDF: function(jsonLDImm) {
    const jsonldLocation =
      jsonLDImm &&
      (jsonLDImm.get("s:location") || jsonLDImm.get("won:hasLocation"));
    return parseSPlace(jsonldLocation);
  },
  generateHumanReadable: function({ value, includeLabel }) {
    return sPlaceToHumanReadable({
      value,
      label: includeLabel ? this.label : "",
    });
  },
};

export const jobLocation = {
  identifier: "jobLocation",
  label: "Job Location",
  placeholder: "Search for location",
  icon: "#ico36_detail_location",
  component: "won-location-picker",
  viewerComponent: "won-location-viewer",
  messageEnabled: true,
  parseToRDF: function({ value, identifier, contentUri }) {
    return {
      "s:jobLocation": genSPlace({
        geoData: value,
        baseUri: genDetailBaseUri(contentUri, identifier),
      }),
    };
  },
  parseFromRDF: function(jsonLDImm) {
    const jsonldLocation = jsonLDImm && jsonLDImm.get("s:jobLocation");
    return parseSPlace(jsonldLocation);
  },
  generateHumanReadable: function({ value, includeLabel }) {
    return sPlaceToHumanReadable({
      value,
      label: includeLabel ? this.label : "",
    });
  },
};

export const travelAction = {
  identifier: "travelAction",
  label: "Route (From - To)",
  icon: "#ico36_detail_travelaction",
  placeholder: {
    departure: "Start location",
    destination: "Destination",
  },
  component: "won-travel-action-picker",
  viewerComponent: "won-travel-action-viewer",
  messageEnabled: true,
  parseToRDF: function({ value, identifier, contentUri }) {
    if (!value) {
      return { "won:travelAction": undefined };
    }

    const baseUri = genDetailBaseUri(contentUri, identifier);
    return {
      "won:travelAction": {
        "@id": baseUri,
        "@type": "s:TravelAction",
        "s:fromLocation": genSPlace({
          geoData: {
            lat: getIn(value, ["fromLocation", "lat"]),
            lng: getIn(value, ["fromLocation", "lng"]),
            name: getIn(value, ["fromLocation", "name"]),
            // excluding nwCorner and seCorner as travelaction calculates its bounding box differently
          },
          baseUri: baseUri && baseUri + "/fromLocation",
        }),

        "s:toLocation": genSPlace({
          geoData: {
            lat: getIn(value, ["toLocation", "lat"]),
            lng: getIn(value, ["toLocation", "lng"]),
            name: getIn(value, ["toLocation", "name"]),
            // excluding nwCorner and seCorner as travelaction calculates its bounding box differently
          },
          baseUri: baseUri && baseUri + "/toLocation",
        }),
      },
    };
  },
  parseFromRDF: function(jsonLDImm) {
    const jsonLdTravelAction = jsonLDImm && jsonLDImm.get("won:travelAction");
    if (!jsonLdTravelAction) return undefined;

    const jsonLdTravelActionImm = Immutable.fromJS(jsonLdTravelAction);

    const fromLocation = parsePlaceLeniently(
      getFromJsonLd(jsonLdTravelActionImm, "s:fromLocation", won.defaultContext)
    );
    const toLocation = parsePlaceLeniently(
      getFromJsonLd(jsonLdTravelActionImm, "s:toLocation", won.defaultContext)
    );

    const travelAction = {
      fromAddress: get(fromLocation, "address"),
      fromLocation: {
        lat: get(fromLocation, "lat"),
        lng: get(fromLocation, "lng"),
      },
      toAddress: get(toLocation, "address"),
      toLocation: {
        lat: get(toLocation, "lat"),
        lng: get(toLocation, "lng"),
      },
    };

    if (
      (travelAction.fromAddress &&
        travelAction.fromLocation.lat &&
        travelAction.fromLocation.lng) ||
      (travelAction.toAddress &&
        travelAction.toLocation.lat &&
        travelAction.toLocation.lng)
    ) {
      return Immutable.fromJS(travelAction);
    } else {
      console.error(
        "Cant parse travelAction, data is an invalid travelAction-object: ",
        jsonLdTravelActionImm.toJS()
      );
      return undefined;
    }
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value && (value.fromLocation || value.toLocation)) {
      const fromLocation = value.fromLocation;
      const toLocation = value.toLocation;

      let fromLocationName;
      let toLocationName;

      if (fromLocation && fromLocation.name) {
        fromLocationName = fromLocation.name;
      } else if (value && value.fromAddress) {
        fromLocationName = value.fromAddress;
      }

      if (toLocation && toLocation.name) {
        toLocationName = toLocation.name;
      } else if (value && value.toAddress) {
        toLocationName = value.toAddress;
      }

      let humanReadable;

      if (fromLocationName) {
        humanReadable = "from: " + fromLocationName + " ";
      } else {
        const fromLocationLat =
          fromLocation && fromLocation.lat && fromLocation.lat.toFixed(6);
        const fromLocationLng =
          fromLocation && fromLocation.lng && fromLocation.lng.toFixed(6);
        if (fromLocationLat && fromLocationLng) {
          humanReadable =
            "from: @(" + fromLocationLat + " , " + fromLocationLng + ") ";
        }
      }

      if (toLocationName) {
        if (humanReadable) {
          humanReadable += "to: " + toLocationName + " ";
        } else {
          humanReadable = "to: " + toLocationName + " ";
        }
      } else {
        const toLocationLat =
          toLocation && toLocation.lat && toLocation.lat.toFixed(6);
        const toLocationLng =
          toLocation && toLocation.lng && toLocation.lng.toFixed(6);
        if (toLocationLat && toLocationLng) {
          if (humanReadable) {
            humanReadable +=
              "to: @(" + toLocationLat + " , " + toLocationLng + ") ";
          } else {
            humanReadable +=
              "to: @(" + toLocationLat + " , " + toLocationLng + ") ";
          }
        }
      }

      if (humanReadable) {
        return includeLabel
          ? this.label + ": " + humanReadable.trim()
          : humanReadable.trim();
      }
    }
    return undefined;
  },
};

function genDetailBaseUri(baseUri, detailIdentifier) {
  if (!baseUri || !detailIdentifier) {
    return undefined;
  }
  const randomId = generateIdString(10);
  return baseUri + "/" + detailIdentifier + "/" + randomId;
}

function genSPlace({ geoData, baseUri }) {
  if (!geoData) {
    return undefined;
  }
  if (!geoData.lat || !geoData.lng || !geoData.name) {
    return undefined;
  }

  return {
    "@id": baseUri,
    "@type": "s:Place",
    "s:name": geoData.name,
    "s:geo": genGeo({ lat: geoData.lat, lng: geoData.lng, baseUri }),
    "won:hasBoundingBox": genBoundingBox({
      nwCorner: geoData.nwCorner,
      seCorner: geoData.seCorner,
      baseUri,
    }),
  };
}

function genGeo({ lat, lng, baseUri }) {
  if (isNaN(lat) || isNaN(lng)) {
    return undefined;
  }
  return {
    "@id": baseUri ? baseUri + "/geo" : undefined,
    "@type": "s:GeoCoordinates",
    "s:latitude": lat.toFixed(6),
    "s:longitude": lng.toFixed(6),
    "won:geoSpatial": {
      "@type": "http://www.bigdata.com/rdf/geospatial/literals/v1#lat-lon",
      "@value": `${lat.toFixed(6)}#${lng.toFixed(6)}`,
    },
  };
}

function genBoundingBox({ nwCorner, seCorner, baseUri }) {
  return !nwCorner || !seCorner
    ? undefined
    : {
        "@id": baseUri ? baseUri + "/bounds" : undefined,
        "won:hasNorthWestCorner": {
          "@id": baseUri ? baseUri + "/bounds/nw" : undefined,
          "@type": "s:GeoCoordinates",
          "s:latitude": nwCorner.lat.toFixed(6),
          "s:longitude": nwCorner.lng.toFixed(6),
        },
        "won:hasSouthEastCorner": {
          "@id": baseUri ? baseUri + "/bounds/se" : undefined,
          "@type": "s:GeoCoordinates",
          "s:latitude": seCorner.lat.toFixed(6),
          "s:longitude": seCorner.lng.toFixed(6),
        },
      };
}

function parseSPlace(jsonldLocation) {
  if (!jsonldLocation) return undefined; // NO LOCATION PRESENT

  const location = parsePlaceLeniently(jsonldLocation);

  if (
    location &&
    location.address &&
    location.lat &&
    location.lng &&
    location.nwCorner.lat &&
    location.nwCorner.lng &&
    location.seCorner.lat &&
    location.seCorner.lng
  ) {
    return Immutable.fromJS(location);
  } else {
    console.error(
      "Cant parse location, data is an invalid location-object: ",
      jsonldLocation
    );
    return undefined;
  }
}

/**
 * Parses json-ld of an `s:Place` in a best-effort kinda style.
 * Any data missing in the RDF will be missing in the result object.
 * @param {*} jsonldLocation
 */
function parsePlaceLeniently(jsonldLocation) {
  if (!jsonldLocation) return undefined; // NO LOCATION PRESENT

  const jsonldLocationImm = Immutable.fromJS(jsonldLocation);

  const parseFloatFromLocation = path =>
    won.parseFrom(jsonldLocationImm, path, "xsd:float");

  const place = {
    address: won.parseFrom(jsonldLocationImm, ["s:name"], "xsd:string"),
    lat: parseFloatFromLocation(["s:geo", "s:latitude"]),
    lng: parseFloatFromLocation(["s:geo", "s:longitude"]),
    // nwCorner if present (see below)
    // seCorner if present (see below)
  };

  const nwCornerLat = parseFloatFromLocation([
    "won:hasBoundingBox",
    "won:hasNorthWestCorner",
    "s:latitude",
  ]);
  const nwCornerLng = parseFloatFromLocation([
    "won:hasBoundingBox",
    "won:hasNorthWestCorner",
    "s:longitude",
  ]);
  const seCornerLat = parseFloatFromLocation([
    "won:hasBoundingBox",
    "won:hasSouthEastCorner",
    "s:latitude",
  ]);
  const seCornerLng = parseFloatFromLocation([
    "won:hasBoundingBox",
    "won:hasSouthEastCorner",
    "s:longitude",
  ]);

  if (nwCornerLat || nwCornerLng) {
    place.nwCorner = {
      lat: nwCornerLat,
      lng: nwCornerLng,
    };
  }
  if (seCornerLat || seCornerLng) {
    place.seCorner = {
      lat: seCornerLat,
      lng: seCornerLng,
    };
  }
  return place;
}

function sPlaceToHumanReadable({ value, label }) {
  if (value) {
    let humanReadable;
    if (value.name) {
      humanReadable = value.name;
    } else if (value.address) {
      humanReadable = value.address;
    } else {
      const locationLat = value.lat && value.lat.toFixed(6);
      const locationLng = value.lng && value.lng.toFixed(6);
      if (locationLat && locationLng) {
        humanReadable = "@(" + locationLat + " , " + locationLng + ")";
      }
    }
    if (humanReadable) {
      return (label ? `${label}: ` : "") + humanReadable.trim();
    }
  }
  return undefined;
}
