// TODO: import detail-picker components here, not in create-post
// TODO: each detail picker should know it's own rdf template
// --> both for parsing to and from rdf
// --> templates are used in need-builder (toRDF) and in parse-need (from RDF)
import { getIn, is } from "../app/utils.js";
import Immutable from "immutable";

/**
 * Defines a set of details that will only be visible within a specific 'implementation'
 * you will need to alter the identifier, label, icon, parseToRDF, and parseFromRDF if
 * you want to use it.
 */
export const abstractDetails = {
  range: {
    identifier: function() {
      throw "abstract Detail does not override necessary identifier";
    },
    label: function() {
      throw "abstract Detail does not override necessary label";
    },
    minLabel: function() {
      throw "abstract Detail does not override necessary minLabel";
    },
    maxLabel: function() {
      throw "abstract Detail does not override necessary maxLabel";
    },
    minPlaceholder: undefined,
    maxPlaceholder: undefined,
    icon: undefined,
    component: "won-range-picker",
    viewerComponent: "won-range-viewer",
    parseToRDF: function() {
      throw "abstract Detail does not override necessary function";
    },
    parseFromRDF: function() {
      throw "abstract Detail does not override necessary function";
    },
    generateHumanReadable: function() {
      throw "abstract Detail does not override necessary function";
    },
  },
  number: {
    identifier: function() {
      throw "abstract Detail does not override necessary identifier";
    },
    label: function() {
      throw "abstract Detail does not override necessary label";
    },
    icon: undefined,
    component: "won-number-picker",
    viewerComponent: "won-number-viewer",
    parseToRDF: function() {
      throw "abstract Detail does not override necessary function";
    },
    parseFromRDF: function() {
      throw "abstract Detail does not override necessary function";
    },
    generateHumanReadable: function() {
      throw "abstract Detail does not override necessary function";
    },
  },
  select: {
    identifier: function() {
      throw "abstract Detail does not override necessary identifier";
    },
    label: function() {
      throw "abstract Detail does not override necessary label";
    },
    icon: undefined,
    component: "won-select-picker",
    viewerComponent: "won-select-viewer",
    multiSelect: false,
    options: function() {
      throw 'abstract Detail does not override necessary options array(structure: [{value: val, label: "labeltext"}...]';
      /**
       * e.g. number of rooms ....
       [
        {value: "1", label: "one"},
        {value: "2", label: "two"},
        {value: "3", label: "three"},
        {value: "4", label: "four"},
        {value: "5+", label: "more"},
       ]
       */
    },
    parseToRDF: function() {
      throw "abstract Detail does not override necessary function";
    },
    parseFromRDF: function() {
      throw "abstract Detail does not override necessary function";
    },
    generateHumanReadable: function() {
      throw "abstract Detail does not override necessary function";
    },
  },
  dropdown: {
    identifier: function() {
      throw "abstract Detail does not override necessary identifier";
    },
    label: function() {
      throw "abstract Detail does not override necessary label";
    },
    icon: undefined,
    component: "won-dropdown-picker",
    viewerComponent: "won-dropdown-viewer",
    options: function() {
      throw 'abstract Detail does not override necessary options array(structure: [{value: val, label: "labeltext"}...]';
      /**
       * e.g. relationship status....
        [
         {value: "single", label: "single"},
         {value: "married", label: "married"},
         {value: "complicated", label: "it's complicated"},
         {value: "divorced", label: "divorced"},
         {value: "free", label: "free for all"},
        ]
       */
    },
    parseToRDF: function() {
      throw "abstract Detail does not override necessary function";
    },
    parseFromRDF: function() {
      throw "abstract Detail does not override necessary function";
    },
    generateHumanReadable: function() {
      throw "abstract Detail does not override necessary function";
    },
  },
};

export const details = {
  title: {
    identifier: "title",
    label: "Title",
    icon: "#ico36_title_circle",
    placeholder: "What? (Short title shown in lists)",
    component: "won-title-picker",
    viewerComponent: "won-title-viewer",
    parseToRDF: function({ value }) {
      if (!value) {
        return { "dc:title": undefined };
      }
      return { "dc:title": value };
    },
    parseFromRDF: function(jsonLDImm) {
      return jsonLDImm && jsonLDImm.get("dc:title");
    },
    generateHumanReadable: function({ value, includeLabel }) {
      if (value) {
        return includeLabel ? this.label + ": " + value : value;
      }
      return undefined;
    },
  },
  description: {
    identifier: "description",
    label: "Description",
    icon: "#ico36_description_circle",
    placeholder: "Enter Description...",
    component: "won-description-picker",
    viewerComponent: "won-description-viewer",
    parseToRDF: function({ value }) {
      if (!value) {
        return { "dc:description": undefined };
      }
      return { "dc:description": value };
    },
    parseFromRDF: function(jsonLDImm) {
      return jsonLDImm && jsonLDImm.get("dc:description");
    },
    generateHumanReadable: function({ value, includeLabel }) {
      if (value) {
        return includeLabel ? this.label + ": " + value : value;
      }
      return undefined;
    },
  },
  date: {
    identifier: "date",
    label: "Date",
    icon: "#ico36_time_circle",
    placeholder: "Enter Date...",
    component: "won-date-picker",
    viewerComponent: "won-date-viewer",
    parseToRDF: function({ value }) {
      //TODO: Correct parseToRDF
      if (!value) {
        return { "dc:date": undefined };
      }
      return { "dc:date": value };
    },
    parseFromRDF: function(jsonLDImm) {
      //TODO: Correct parseFromRDF
      return jsonLDImm && jsonLDImm.get("dc:date");
    },
    generateHumanReadable: function({ value, includeLabel }) {
      if (value) {
        return includeLabel ? this.label + ": " + value : value;
      }
      return undefined;
    },
  },
  datetime: {
    identifier: "datetime",
    label: "Date & Time",
    icon: "#ico36_time_circle",
    placeholder: "Enter Date and Time...",
    component: "won-datetime-picker",
    viewerComponent: "won-datetime-viewer",
    parseToRDF: function({ value }) {
      //TODO: Correct parseToRDF
      if (!value) {
        return { "dc:datetime": undefined };
      }
      return { "dc:datetime": value };
    },
    parseFromRDF: function(jsonLDImm) {
      //TODO: Correct parseFromRDF
      return jsonLDImm && jsonLDImm.get("dc:datetime");
    },
    generateHumanReadable: function({ value, includeLabel }) {
      if (value) {
        return includeLabel ? this.label + ": " + value : value;
      }
      return undefined;
    },
  },
  time: {
    identifier: "time",
    label: "Time",
    icon: "#ico36_time_circle",
    placeholder: "Enter Time...",
    component: "won-time-picker",
    viewerComponent: "won-time-viewer",
    parseToRDF: function({ value }) {
      //TODO: Correct parseToRDF
      if (!value) {
        return { "dc:time": undefined };
      }
      return { "dc:time": value };
    },
    parseFromRDF: function(jsonLDImm) {
      //TODO: Correct parseFromRDF
      return jsonLDImm && jsonLDImm.get("dc:time");
    },
    generateHumanReadable: function({ value, includeLabel }) {
      if (value) {
        return includeLabel ? this.label + ": " + value : value;
      }
      return undefined;
    },
  },
  month: {
    identifier: "month",
    label: "Month",
    icon: "#ico36_time_circle",
    placeholder: "Enter Month...",
    component: "won-month-picker",
    viewerComponent: "won-month-viewer",
    parseToRDF: function({ value }) {
      //TODO: Correct parseToRDF
      if (!value) {
        return { "dc:month": undefined };
      }
      return { "dc:month": value };
    },
    parseFromRDF: function(jsonLDImm) {
      //TODO: Correct parseFromRDF
      return jsonLDImm && jsonLDImm.get("dc:month");
    },
    generateHumanReadable: function({ value, includeLabel }) {
      if (value) {
        return includeLabel ? this.label + ": " + value : value;
      }
      return undefined;
    },
  },
  location: {
    identifier: "location",
    label: "Location",
    icon: "#ico36_location_circle",
    placeholder: "Search for location",
    component: "won-location-picker",
    viewerComponent: "won-location-viewer",
    parseToRDF: function({ value, identifier }) {
      if (!value) {
        // TODO: this should happen in need-builder
        return { "won:hasLocation": undefined };
      }
      return {
        "won:hasLocation": {
          "@type": "s:Place",
          "s:geo": {
            "@id": "_:" + identifier + "-location",
            "@type": "s:GeoCoordinates",
            "s:latitude": value.lat.toFixed(6),
            "s:longitude": value.lng.toFixed(6),
            "won:geoSpatial": {
              "@type":
                "http://www.bigdata.com/rdf/geospatial/literals/v1#lat-lon",
              "@value": `${value.lat.toFixed(6)}#${value.lng.toFixed(6)}`,
            },
          },
          "s:name": value.name,
          "won:hasBoundingBox":
            !value.nwCorner || !value.seCorner
              ? undefined
              : {
                  "won:hasNorthWestCorner": {
                    "@id": "_:" + identifier + "BoundsNW",
                    "@type": "s:GeoCoordinates",
                    "s:latitude": value.nwCorner.lat.toFixed(6),
                    "s:longitude": value.nwCorner.lng.toFixed(6),
                  },
                  "won:hasSouthEastCorner": {
                    "@id": "_:" + identifier + "BoundsSE",
                    "@type": "s:GeoCoordinates",
                    "s:latitude": value.seCorner.lat.toFixed(6),
                    "s:longitude": value.seCorner.lng.toFixed(6),
                  },
                },
        },
      };
    },
    parseFromRDF: function(jsonLDImm) {
      const jsonldLocation = jsonLDImm && jsonLDImm.get("won:hasLocation");
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

      location.address =
        jsonldLocationImm.get("s:name") ||
        jsonldLocationImm.get("http://schema.org/name");

      location.lat = Number.parseFloat(
        jsonldLocationImm.getIn(["s:geo", "s:latitude"]) ||
          jsonldLocationImm.getIn([
            "http://schema.org/geo",
            "http://schema.org/latitude",
          ])
      );
      location.lng = Number.parseFloat(
        jsonldLocationImm.getIn(["s:geo", "s:longitude"]) ||
          jsonldLocationImm.getIn([
            "http://schema.org/geo",
            "http://schema.org/longitude",
          ])
      );

      location.nwCorner.lat = Number.parseFloat(
        jsonldLocationImm.getIn([
          "won:hasBoundingBox",
          "won:hasNorthWestCorner",
          "s:latitude",
        ]) ||
          jsonldLocationImm.getIn([
            "won:hasBoundingBox",
            "won:hasNorthWestCorner",
            "http://schema.org/latitude",
          ])
      );
      location.nwCorner.lng = Number.parseFloat(
        jsonldLocationImm.getIn([
          "won:hasBoundingBox",
          "won:hasNorthWestCorner",
          "s:longitude",
        ]) ||
          jsonldLocationImm.getIn([
            "won:hasBoundingBox",
            "won:hasNorthWestCorner",
            "http://schema.org/longitude",
          ])
      );
      location.seCorner.lat = Number.parseFloat(
        jsonldLocationImm.getIn([
          "won:hasBoundingBox",
          "won:hasSouthEastCorner",
          "s:latitude",
        ]) ||
          jsonldLocationImm.getIn([
            "won:hasBoundingBox",
            "won:hasSouthEastCorner",
            "http://schema.org/latitude",
          ])
      );
      location.seCorner.lng = Number.parseFloat(
        jsonldLocationImm.getIn([
          "won:hasBoundingBox",
          "won:hasSouthEastCorner",
          "s:longitude",
        ]) ||
          jsonldLocationImm.getIn([
            "won:hasBoundingBox",
            "won:hasSouthEastCorner",
            "http://schema.org/longitude",
          ])
      );

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
    },
    generateHumanReadable: function({ value, includeLabel }) {
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
    },
  },
  person: {
    identifier: "person",
    label: "Person",
    icon: "#ico36_person_single_circle",
    placeholder: undefined,
    component: "won-person-picker",
    viewerComponent: "won-person-viewer",
    parseToRDF: function({ value }) {
      if (!value) {
        return { "foaf:person": undefined };
      }
      return {
        "foaf:title": getIn(value, ["title"])
          ? getIn(value, ["title"])
          : undefined,
        "foaf:name": getIn(value, ["name"])
          ? getIn(value, ["name"])
          : undefined,
        "s:worksFor": getIn(value, ["company"])
          ? {
              "@type": "s:Organization",
              "s:name": getIn(value, ["company"]),
            }
          : undefined,
        "s:jobTitle": getIn(value, ["position"])
          ? getIn(value, ["position"])
          : undefined,
      };
    },
    parseFromRDF: function(jsonLDImm) {
      if (!jsonLDImm) return undefined;

      let person = {
        name: undefined,
        title: undefined,
        company: undefined,
        position: undefined,
        // bio: undefined,
      };

      person.name = jsonLDImm.get("foaf:name");
      person.title = jsonLDImm.get("foaf:title");
      person.company = jsonLDImm.getIn(["s:worksFor", "s:name"]);
      person.position = jsonLDImm.get("s:jobTitle");
      //person.bio = isOrSeeksImm.get("dc:description");

      // if there's anything, use it
      if (person.name || person.title || person.company || person.position) {
        return Immutable.fromJS(person);
      }
      return undefined;
    },
    generateHumanReadable: function({ value, includeLabel }) {
      const title = getIn(value, ["title"]);
      const name = getIn(value, ["name"]);
      const company = getIn(value, ["company"]);
      const position = getIn(value, ["position"]);

      let humanReadable;
      if (title) {
        humanReadable = title + " ";
      }
      if (name) {
        if (humanReadable) {
          humanReadable += name + " ";
        } else {
          humanReadable = name + " ";
        }
      }
      if (company) {
        if (humanReadable) {
          humanReadable += "works at " + company + " ";
        } else {
          humanReadable = company + " ";
        }
      }
      if (position) {
        if (humanReadable) {
          if (company) {
            humanReadable += "as a " + position;
          } else {
            humanReadable += "is a " + position;
          }
        } else {
          humanReadable = position;
        }
      }

      if (humanReadable) {
        return includeLabel
          ? this.label + ": " + humanReadable.trim()
          : humanReadable.trim();
      }
      return undefined;
    },
  },
  travelAction: {
    identifier: "travelAction",
    label: "Route (From - To)",
    icon: "#ico36_location_circle",
    placeholder: {
      departure: "Start location",
      destination: "Destination",
    },
    component: "won-travel-action-picker",
    viewerComponent: "won-travel-action-viewer",
    parseToRDF: function({ value }) {
      if (!value) {
        return { "won:travelAction": undefined };
      }
      return {
        "won:travelAction": {
          "@type": "s:TravelAction",
          "s:fromLocation": !value.fromLocation
            ? undefined
            : {
                "@type": "s:Place",
                "s:geo": {
                  "@type": "s:GeoCoordinates",
                  "s:latitude": value.fromLocation.lat.toFixed(6),
                  "s:longitude": value.fromLocation.lng.toFixed(6),
                },
                "s:name": value.fromLocation.name,
              },
          "s:toLocation": !value.toLocation
            ? undefined
            : {
                "@type": "s:Place",
                "s:geo": {
                  "@type": "s:GeoCoordinates",
                  "s:latitude": value.toLocation.lat.toFixed(6),
                  "s:longitude": value.toLocation.lng.toFixed(6),
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

      travelAction.fromAddress =
        travelActionImm.getIn(["s:fromLocation", "s:name"]) ||
        travelActionImm.getIn([
          "http://schema.org/fromLocation",
          "http://schema.org/name",
        ]);

      travelAction.fromLocation.lat =
        travelActionImm.getIn(["s:fromLocation", "s:geo", "s:latitude"]) ||
        travelActionImm.getIn([
          "http://schema.org/fromLocation",
          "http://schema.org/geo",
          "http://schema.org/latitude",
        ]);

      travelAction.fromLocation.lng =
        travelActionImm.getIn(["s:fromLocation", "s:geo", "s:longitude"]) ||
        travelActionImm.getIn([
          "http://schema.org/fromLocation",
          "http://schema.org/geo",
          "http://schema.org/longitude",
        ]);

      travelAction.toAddress =
        travelActionImm.getIn(["s:toLocation", "s:name"]) ||
        travelActionImm.getIn([
          "http://schema.org/toLocation",
          "http://schema.org/name",
        ]);

      travelAction.toLocation.lat =
        travelActionImm.getIn(["s:toLocation", "s:geo", "s:latitude"]) ||
        travelActionImm.getIn([
          "http://schema.org/toLocation",
          "http://schema.org/geo",
          "http://schema.org/latitude",
        ]);

      travelAction.toLocation.lng =
        travelActionImm.getIn(["s:toLocation", "s:geo", "s:longitude"]) ||
        travelActionImm.getIn([
          "http://schema.org/toLocation",
          "http://schema.org/geo",
          "http://schema.org/longitude",
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
      }

      console.error(
        "Cant parse travelAction, data is an invalid travelAction-object: ",
        travelActionImm.toJS()
      );
      return undefined;
    },
    generateHumanReadable: function({ value, includeLabel }) {
      if (value && (value.fromLocation || value.toLocation)) {
        const fromLocation = value.fromLocation;
        const toLocation = value.toLocation;

        let fromLocationName;
        let toLocationName;

        if (fromLocation.name) {
          fromLocationName = fromLocation.name;
        } else if (value && value.fromAddress) {
          fromLocationName = value.fromAddress;
        }

        if (toLocation.name) {
          toLocationName = toLocation.name;
        } else if (value && value.toAddress) {
          toLocationName = value.toAddress;
        }

        let humanReadable;

        if (fromLocationName) {
          humanReadable = "from: " + fromLocationName + " ";
        } else {
          const fromLocationLat =
            fromLocation.lat && fromLocation.lat.toFixed(6);
          const fromLocationLng =
            fromLocation.lng && fromLocation.lng.toFixed(6);
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
          const toLocationLat = toLocation.lat && toLocation.lat.toFixed(6);
          const toLocationLng = toLocation.lng && toLocation.lng.toFixed(6);
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
  },
  tags: {
    identifier: "tags",
    label: "Tags",
    icon: "#ico36_tags_circle",
    placeholder: "e.g. couch, free",
    component: "won-tags-picker",
    viewerComponent: "won-tags-viewer",
    parseToRDF: function({ value }) {
      if (!value) {
        return { "won:hasTag": undefined };
      }
      return { "won:hasTag": value };
    },
    parseFromRDF: function(jsonLDImm) {
      const tags =
        jsonLDImm &&
        (jsonLDImm.get("won:hasTag") ||
          jsonLDImm.get("http://purl.org/webofneeds/model#hasTag"));

      if (!tags) {
        return undefined;
      } else if (is("String", tags)) {
        return Immutable.fromJS([tags]);
      } else if (is("Array", tags)) {
        return Immutable.fromJS(tags);
      } else if (Immutable.List.isList(tags)) {
        return tags; // id; it is already in the format we want
      } else {
        console.error(
          "Found unexpected format of tags (should be Array, " +
            "Immutable.List, or a single tag as string): " +
            JSON.stringify(tags)
        );
        return undefined;
      }
    },
    generateHumanReadable: function({ value, includeLabel }) {
      if (value) {
        let humanReadable = "";

        for (const key in value) {
          humanReadable += value[key] + ", ";
        }
        humanReadable = humanReadable.trim();

        if (humanReadable.length > 0) {
          humanReadable = humanReadable.substr(0, humanReadable.length - 1);
          return includeLabel
            ? this.label + ": " + humanReadable
            : humanReadable;
        }
      }
      return undefined;
    },
  },
  files: {
    identifier: "files",
    label: "Files",
    icon: "#ico36_media_circle",
    placeholder: "",
    accepts: "*/*",
    component: "won-file-picker",
    viewerComponent: "won-file-viewer",
    parseToRDF: function({ value }) {
      //TODO: IMPL THIS METHOD
      if (!value) {
        return { "won:hasFile": undefined };
      }
      return { "won:hasFile": value };
    },
    parseFromRDF: function(jsonLDImm) {
      //TODO: IMPL THIS METHOD
      const files =
        jsonLDImm &&
        (jsonLDImm.get("won:hasFile") ||
          jsonLDImm.get("http://purl.org/webofneeds/model#hasFile"));
      console.log("files", files);
      return undefined;

      /*if (!tags) {
        return undefined;
      } else if (is("String", tags)) {
        return Immutable.fromJS([tags]);
      } else if (is("Array", tags)) {
        return Immutable.fromJS(tags);
      } else if (Immutable.List.isList(tags)) {
        return tags; // id; it is already in the format we want
      } else {
        console.error(
          "Found unexpected format of tags (should be Array, " +
            "Immutable.List, or a single tag as string): " +
            JSON.stringify(tags)
        );
        return undefined;
      }*/
    },
    generateHumanReadable: function({ value, includeLabel }) {
      if (value) {
        let humanReadable = "";
        if (value.length > 1) {
          humanReadable = value.length + " Files";
        } else {
          humanReadable = value[0].name;
        }

        return includeLabel ? this.label + ": " + humanReadable : humanReadable;
      }
      return undefined;
    },
  },
  images: {
    identifier: "images",
    label: "Images",
    icon: "#ico36_media_circle",
    placeholder: "",
    accepts: "image/*",
    component: "won-file-picker",
    viewerComponent: "won-file-viewer",
    parseToRDF: function({ value }) {
      //TODO: IMPL THIS METHOD
      if (!value) {
        return { "won:hasImage": undefined };
      }
      return { "won:hasImage": value };
    },
    parseFromRDF: function(jsonLDImm) {
      //TODO: IMPL THIS METHOD
      const images =
        jsonLDImm &&
        (jsonLDImm.get("won:hasImage") ||
          jsonLDImm.get("http://purl.org/webofneeds/model#hasImage"));
      console.log("images", images);
      return undefined;

      /*if (!tags) {
        return undefined;
      } else if (is("String", tags)) {
        return Immutable.fromJS([tags]);
      } else if (is("Array", tags)) {
        return Immutable.fromJS(tags);
      } else if (Immutable.List.isList(tags)) {
        return tags; // id; it is already in the format we want
      } else {
        console.error(
          "Found unexpected format of tags (should be Array, " +
            "Immutable.List, or a single tag as string): " +
            JSON.stringify(tags)
        );
        return undefined;
      }*/
    },
    generateHumanReadable: function({ value, includeLabel }) {
      if (value) {
        let humanReadable = "";
        if (value.length > 1) {
          humanReadable = value.length + " Images";
        } else {
          humanReadable = value[0].name;
        }

        return includeLabel ? this.label + ": " + humanReadable : humanReadable;
      }
      return undefined;
    },
  },
  ttl: {
    identifier: "ttl",
    label: "Turtle (TTL)",
    icon: "#ico36_rdf_logo_circle",
    placeholder: "Enter TTL...",
    component: "won-ttl-picker",
    viewerComponent: undefined,
    parseToRDF: function({ value }) {
      if (!value) {
        return undefined;
      }
      return undefined;
      // TODO: return value
    },
    parseFromRDF: function(jsonLDImm) {
      //console.error("IMPLEMENT ME", jsonLDImm);
      if (!jsonLDImm) {
        return undefined;
      }
      return undefined;
      // TODO: return value
    },
    generateHumanReadable: function({ value, includeLabel }) {
      //TODO: implement generateHumanReadable
      if (!value || includeLabel) {
        return undefined;
      }
      return undefined;
    },
  },
};
