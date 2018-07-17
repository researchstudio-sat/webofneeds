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
  },
  tags: {
    identifier: "tags",
    label: "Tags",
    icon: "#ico36_tags_circle",
    placeholder: "e.g. #couch #free",
    component: "won-tags-picker",
    viewerComponent: "won-tags-viewer",
    parseToRDF: function({ value }) {
      if (!value) {
        return { "won:hasTag": undefined };
      }
      return { "won:hasTag": value };
    },
    parseFromRDF: function(jsonLDImm) {
      const tags = jsonLDImm && jsonLDImm.get("won:hasTag");

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
  },
  gender: {
    ...abstractDetails.dropdown,
    identifier: "gender",
    label: "Gender",
    icon: "#ico36_tags_circle",
    placeholder: "Select Gender",
    options: [
      { value: "Agender", label: "Agender" },
      { value: "Androgyne", label: "Androgyne" },
      { value: "Androgynous", label: "Androgynous" },
      { value: "Bigender", label: "Bigender" },
      { value: "Cis", label: "Cis" },
      { value: "Cisgender", label: "Cisgender" },
      { value: "Cis Female", label: "Cis Female" },
      { value: "Cis Male", label: "Cis Male" },
      { value: "Cis Man", label: "Cis Man" },
      { value: "Cis Woman", label: "Cis Woman" },
      { value: "Cisgender Female", label: "Cisgender Female" },
      { value: "Cisgender Male", label: "Cisgender Male" },
      { value: "Cisgender Man", label: "Cisgender Man" },
      { value: "Cisgender Woman", label: "Cisgender Woman" },
      { value: "Female to Male", label: "Female to Male" },
      { value: "FTM", label: "FTM" },
      { value: "Gender Fluid", label: "Gender Fluid" },
      { value: "Gender Nonconforming", label: "Gender Nonconforming" },
      { value: "Gender Questioning", label: "Gender Questioning" },
      { value: "Gender Variant", label: "Gender Variant" },
      { value: "Genderqueer", label: "Genderqueer" },
      { value: "Intersex", label: "Intersex" },
      { value: "Male to Female", label: "Male to Female" },
      { value: "MTF", label: "MTF" },
      { value: "Neither", label: "Neither" },
      { value: "Neutrois", label: "Neutrois" },
      { value: "Non-binary", label: "Non-binary" },
      { value: "Other", label: "Other" },
      { value: "Pangender", label: "Pangender" },
      { value: "Trans", label: "Trans" },
      { value: "Trans*", label: "Trans*" },
      { value: "Trans Female", label: "Trans Female" },
      { value: "Trans* Female", label: "Trans* Female" },
      { value: "Trans Male", label: "Trans Male" },
      { value: "Trans* Male", label: "Trans* Male" },
      { value: "Trans Man", label: "Trans Man" },
      { value: "Trans* Man", label: "Trans* Man" },
      { value: "Trans Person", label: "Trans Person" },
      { value: "Trans* Person", label: "Trans* Person" },
      { value: "Trans Woman", label: "Trans Woman" },
      { value: "Trans* Woman", label: "Trans* Woman" },
      { value: "Transfeminine", label: "Transfeminine" },
      { value: "Transgender", label: "Transgender" },
      { value: "Transgender Female", label: "Transgender Female" },
      { value: "Transgender Male", label: "Transgender Male" },
      { value: "Transgender Man", label: "Transgender Man" },
      { value: "Transgender Person", label: "Transgender Person" },
      { value: "Transgender Woman", label: "Transgender Woman" },
      { value: "Transmasculine", label: "Transmasculine" },
      { value: "Transsexual", label: "Transsexual" },
      { value: "Transsexual Female", label: "Transsexual Female" },
      { value: "Transsexual Male", label: "Transsexual Male" },
      { value: "Transsexual Man", label: "Transsexual Man" },
      { value: "Transsexual Person", label: "Transsexual Person" },
      { value: "Transsexual Woman", label: "Transsexual Woman" },
      { value: "Two-Spirit", label: "Two-Spirit" },
    ],
    parseToRDF: function({ value }) {
      if (!value) {
        return { "s:gender": undefined };
      }
      return { "s:gender": value };
    },
    parseFromRDF: function(jsonLDImm) {
      return jsonLDImm && jsonLDImm.get("s:gender");
    },
  },
};
