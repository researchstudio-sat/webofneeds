import { generateIdString } from "../../app/utils.js";
import Immutable from "immutable";
import won from "../../app/won-es6.js";

function genSPlace({ value, identifier, contentUri }) {
  const randomLocationId = generateIdString(10);
  return {
    "@id":
      contentUri && identifier
        ? contentUri + "/" + identifier + "/" + randomLocationId
        : undefined,
    "@type": "s:Place",
    "s:geo": {
      "@id":
        contentUri && identifier
          ? contentUri +
            "/" +
            identifier +
            "/" +
            randomLocationId +
            "/locationgeo"
          : undefined,
      "@type": "s:GeoCoordinates",
      "s:latitude": value.lat.toFixed(6),
      "s:longitude": value.lng.toFixed(6),
      "won:geoSpatial": {
        "@type": "http://www.bigdata.com/rdf/geospatial/literals/v1#lat-lon",
        "@value": `${value.lat.toFixed(6)}#${value.lng.toFixed(6)}`,
      },
    },
    "s:name": value.name,
    "won:hasBoundingBox":
      !value.nwCorner || !value.seCorner
        ? undefined
        : {
            "@id":
              contentUri && identifier
                ? contentUri +
                  "/" +
                  identifier +
                  "/" +
                  randomLocationId +
                  "/Bounds"
                : undefined,
            "won:hasNorthWestCorner": {
              "@id":
                contentUri && identifier
                  ? contentUri +
                    "/" +
                    identifier +
                    "/" +
                    randomLocationId +
                    "/Bounds/NW"
                  : undefined,
              "@type": "s:GeoCoordinates",
              "s:latitude": value.nwCorner.lat.toFixed(6),
              "s:longitude": value.nwCorner.lng.toFixed(6),
            },
            "won:hasSouthEastCorner": {
              "@id":
                contentUri && identifier
                  ? contentUri +
                    "/" +
                    identifier +
                    "/" +
                    randomLocationId +
                    "/Bounds/SE"
                  : undefined,
              "@type": "s:GeoCoordinates",
              "s:latitude": value.seCorner.lat.toFixed(6),
              "s:longitude": value.seCorner.lng.toFixed(6),
            },
          },
  };
}
function parseSPlace(jsonldLocation) {
  // const jsonldLocation = jsonLDImm && jsonLDImm.get("won:hasLocation");
  if (!jsonldLocation) return undefined; // NO LOCATION PRESENT

  const jsonldLocationImm = Immutable.fromJS(jsonldLocation);

  let location = {
    address: undefined,
    lat: undefined,
    lng: undefined,
    nwCorner: {
      lat: undefined,
      lng: undefined,
    },
    seCorner: {
      lat: undefined,
      lng: undefined,
    },
  };

  location.address = won.parseFrom(jsonldLocationImm, ["s:name"], "xsd:string");

  const parseFloatFromLocation = path =>
    won.parseFrom(jsonldLocationImm, path, "xsd:float");

  location.lat = parseFloatFromLocation(["s:geo", "s:latitude"]);
  location.lng = parseFloatFromLocation(["s:geo", "s:longitude"]);
  location.nwCorner.lat = parseFloatFromLocation([
    "won:hasBoundingBox",
    "won:hasNorthWestCorner",
    "s:latitude",
  ]);
  location.nwCorner.lng = parseFloatFromLocation([
    "won:hasBoundingBox",
    "won:hasNorthWestCorner",
    "s:longitude",
  ]);
  location.seCorner.lat = parseFloatFromLocation([
    "won:hasBoundingBox",
    "won:hasSouthEastCorner",
    "s:latitude",
  ]);
  location.seCorner.lng = parseFloatFromLocation([
    "won:hasBoundingBox",
    "won:hasSouthEastCorner",
    "s:longitude",
  ]);

  if (
    location.address &&
    location.lat &&
    location.lng &&
    location.nwCorner.lat &&
    location.nwCorner.lng &&
    location.seCorner.lat &&
    location.seCorner.lng
  ) {
    return Immutable.fromJS(location);
  }

  console.error(
    "Cant parse location, data is an invalid location-object: ",
    jsonldLocationImm.toJS()
  );
  return undefined;
}
function sPlaceToHumanReadable({ value, includeLabel }) {
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
      return includeLabel
        ? this.label + ": " + humanReadable.trim()
        : humanReadable.trim();
    }
  }
  return undefined;
}

export const location = {
  identifier: "location",
  label: "Location",
  icon: "#ico36_detail_location",
  placeholder: "Search for location",
  component: "won-location-picker",
  viewerComponent: "won-location-viewer",
  messageEnabled: true,
  parseToRDF: function({ value, identifier, contentUri }) {
    if (!value) {
      return { "won:hasLocation": undefined };
    }

    return {
      "won:hasLocation": genSPlace({ value, identifier, contentUri }),
    };
  },
  parseFromRDF: function(jsonLDImm) {
    const jsonldLocation = jsonLDImm && jsonLDImm.get("won:hasLocation");
    return parseSPlace(jsonldLocation);
  },
  generateHumanReadable: function({ value, includeLabel }) {
    return sPlaceToHumanReadable({ value, includeLabel });
  },
};

export const jobLocation = {
  ...location,
  identifier: "joblocation",
  placeholder: "Location: Jobs in Vicinity of...",
  parseToRDF: function({ value, identifier, contentUri }) {
    if (!value) {
      return { "s:jobLocation": undefined };
    }

    return {
      "s:jobLocation": genSPlace(value, identifier, contentUri),
    };
  },
  parseFromRDF: function(jsonLDImm) {
    const jsonldLocation = jsonLDImm && jsonLDImm.get("s:jobLocation");
    return parseSPlace(jsonldLocation);
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

    const randomTravelActionId = generateIdString(10);

    return {
      "won:travelAction": {
        "@id":
          contentUri && identifier
            ? contentUri + "/" + identifier + "/" + randomTravelActionId
            : undefined,
        "@type": "s:TravelAction",
        "s:fromLocation": !value.fromLocation
          ? undefined
          : {
              "@id":
                contentUri && identifier
                  ? contentUri +
                    "/" +
                    identifier +
                    "/" +
                    randomTravelActionId +
                    "/fromLocation"
                  : undefined,
              "@type": "s:Place",
              "s:geo": {
                "@id":
                  contentUri && identifier
                    ? contentUri +
                      "/" +
                      identifier +
                      "/" +
                      randomTravelActionId +
                      "/fromLocation/geocoords"
                    : undefined,
                "@type": "s:GeoCoordinates",
                "s:latitude": value.fromLocation.lat.toFixed(6),
                "s:longitude": value.fromLocation.lng.toFixed(6),
                "won:geoSpatial": {
                  "@type":
                    "http://www.bigdata.com/rdf/geospatial/literals/v1#lat-lon",
                  "@value": `${value.fromLocation.lat.toFixed(
                    6
                  )}#${value.fromLocation.lng.toFixed(6)}`,
                },
              },
              "s:name": value.fromLocation.name,
            },
        "s:toLocation": !value.toLocation
          ? undefined
          : {
              "@id":
                contentUri && identifier
                  ? contentUri +
                    "/" +
                    identifier +
                    "/" +
                    randomTravelActionId +
                    "/toLocation"
                  : undefined,
              "@type": "s:Place",
              "s:geo": {
                "@id":
                  contentUri && identifier
                    ? contentUri +
                      "/" +
                      identifier +
                      "/" +
                      randomTravelActionId +
                      "/toLocation/geocoords"
                    : undefined,
                "@type": "s:GeoCoordinates",
                "s:latitude": value.toLocation.lat.toFixed(6),
                "s:longitude": value.toLocation.lng.toFixed(6),
                "won:geoSpatial": {
                  "@type":
                    "http://www.bigdata.com/rdf/geospatial/literals/v1#lat-lon",
                  "@value": `${value.toLocation.lat.toFixed(
                    6
                  )}#${value.toLocation.lng.toFixed(6)}`,
                },
              },
              "s:name": value.toLocation.name,
            },
      },
    };
  },
  parseFromRDF: function(jsonLDImm) {
    const jsonTravelAction = jsonLDImm && jsonLDImm.get("won:travelAction");
    if (!jsonTravelAction) return undefined;

    const travelActionImm = Immutable.fromJS(jsonTravelAction);

    let travelAction = {
      fromAddress: undefined,
      fromLocation: {
        lat: undefined,
        lng: undefined,
      },
      toAddress: undefined,
      toLocation: {
        lat: undefined,
        lng: undefined,
      },
    };

    travelAction.fromAddress = won.parseFrom(
      travelActionImm,
      ["s:fromLocation", "s:name"],
      "xsd:string"
    );

    const parseFloatFromTravelAction = path =>
      won.parseFrom(travelActionImm, path, "xsd:float");

    travelAction.fromLocation.lat = parseFloatFromTravelAction([
      "s:fromLocation",
      "s:geo",
      "s:latitude",
    ]);
    travelAction.fromLocation.lng = parseFloatFromTravelAction([
      "s:fromLocation",
      "s:geo",
      "s:longitude",
    ]);

    travelAction.toAddress = won.parseFrom(
      travelActionImm,
      ["s:toLocation", "s:name"],
      "xsd:string"
    );

    travelAction.toLocation.lat = parseFloatFromTravelAction([
      "s:toLocation",
      "s:geo",
      "s:latitude",
    ]);
    travelAction.toLocation.lng = parseFloatFromTravelAction([
      "s:toLocation",
      "s:geo",
      "s:longitude",
    ]);

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
        travelActionImm.toJS()
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
