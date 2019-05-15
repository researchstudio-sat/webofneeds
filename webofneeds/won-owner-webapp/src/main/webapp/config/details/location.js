import { get, getIn, getFromJsonLd } from "../../app/utils.js";
import {
  genSPlace,
  genDetailBaseUri,
  parseSPlace,
  parsePlaceLeniently,
} from "../../app/won-utils.js";
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
  overrideAddressDetail: {
    placeholder:
      "Alternative Address name (e.g. if doornumber should be included)",
  },
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
      (jsonLDImm.get("s:location") || jsonLDImm.get("won:location"));
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
