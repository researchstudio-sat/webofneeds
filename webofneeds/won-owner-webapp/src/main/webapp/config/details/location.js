import { get, getIn } from "../../app/utils.js";
import * as wonUtils from "../../app/won-utils.js";
import Immutable from "immutable";
import won from "../../app/won-es6.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";

export const location = {
  identifier: "location",
  label: "Location",
  icon: "#ico36_detail_location",
  placeholder: "Search for location",
  component: "won-location-picker",
  viewerComponent: "won-location-viewer",
  messageEnabled: true,
  overrideAddressDetail: {
    placeholder:
      "Alternative Address name (e.g. if doornumber should be included)",
  },
  parseToRDF: function({ value, identifier, contentUri }) {
    return {
      "s:location": genSPlace({
        geoData: value,
        baseUri: wonUtils.genDetailBaseUri(contentUri, identifier),
      }),
    };
  },
  parseFromRDF: function(jsonLDImm) {
    const jsonldLocation =
      jsonLDImm &&
      (jsonLDImm.get("s:location") || jsonLDImm.get("won:location"));
    return jsonLdUtils.parseSPlace(jsonldLocation);
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
        baseUri: wonUtils.genDetailBaseUri(contentUri, identifier),
      }),
    };
  },
  parseFromRDF: function(jsonLDImm) {
    const jsonldLocation = jsonLDImm && jsonLDImm.get("s:jobLocation");
    return jsonLdUtils.parseSPlace(jsonldLocation);
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
      return { "con:travelAction": undefined };
    }

    const baseUri = wonUtils.genDetailBaseUri(contentUri, identifier);
    return {
      "con:travelAction": {
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
    const jsonLdTravelAction = jsonLDImm && jsonLDImm.get("con:travelAction");
    if (!jsonLdTravelAction) return undefined;

    const jsonLdTravelActionImm = Immutable.fromJS(jsonLdTravelAction);

    const fromLocation = jsonLdUtils.parsePlaceLeniently(
      jsonLdUtils.getFromJsonLd(
        jsonLdTravelActionImm,
        "s:fromLocation",
        won.defaultContext
      )
    );
    const toLocation = jsonLdUtils.parsePlaceLeniently(
      jsonLdUtils.getFromJsonLd(
        jsonLdTravelActionImm,
        "s:toLocation",
        won.defaultContext
      )
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

/**
 * Generates rdf from given geoData (e.g. location from a location picker)
 * @param geoData
 * @param baseUri
 * @returns {*}
 */
function genSPlace({ geoData, baseUri }) {
  if (!geoData) {
    return undefined;
  }
  if (!geoData.lat || !geoData.lng || !geoData.name) {
    return undefined;
  }

  const genGeo = ({ lat, lng, baseUri }) => {
    if (isNaN(lat) || isNaN(lng)) {
      return undefined;
    }
    return {
      "@id": baseUri ? baseUri + "/geo" : undefined,
      "@type": "s:GeoCoordinates",
      "s:latitude": lat.toFixed(6),
      "s:longitude": lng.toFixed(6),
      "con:geoSpatial": {
        "@type": "http://www.bigdata.com/rdf/geospatial/literals/v1#lat-lon",
        "@value": `${lat.toFixed(6)}#${lng.toFixed(6)}`,
      },
    };
  };
  const genBoundingBox = ({ nwCorner, seCorner, baseUri }) => {
    return !nwCorner || !seCorner
      ? undefined
      : {
          "@id": baseUri ? baseUri + "/bounds" : undefined,
          "con:northWestCorner": {
            "@id": baseUri ? baseUri + "/bounds/nw" : undefined,
            "@type": "s:GeoCoordinates",
            "s:latitude": nwCorner.lat.toFixed(6),
            "s:longitude": nwCorner.lng.toFixed(6),
          },
          "con:southEastCorner": {
            "@id": baseUri ? baseUri + "/bounds/se" : undefined,
            "@type": "s:GeoCoordinates",
            "s:latitude": seCorner.lat.toFixed(6),
            "s:longitude": seCorner.lng.toFixed(6),
          },
        };
  };

  return {
    "@id": baseUri,
    "@type": "s:Place",
    "s:name": geoData.name,
    "s:geo": genGeo({ lat: geoData.lat, lng: geoData.lng, baseUri }),
    "con:boundingBox": genBoundingBox({
      nwCorner: geoData.nwCorner,
      seCorner: geoData.seCorner,
      baseUri,
    }),
  };
}
