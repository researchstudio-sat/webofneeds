// TODO: import detail-picker components here, not in create-post
// TODO: each detail picker should know it's own rdf template
// --> both for parsing to and from rdf
// --> templates are used in need-builder (toRDF) and in parse-need (from RDF)
import { getIn } from "../app/utils.js";

export const details = {
  description: {
    identifier: "description",
    label: "Description",
    icon: "#ico36_description_circle",
    component: "won-description-picker",
    parseToRDF: function({ value }) {
      if (!value) {
        return { "dc:description": undefined };
      }
      return { "dc:description": value };
    },
  },
  location: {
    identifier: "location",
    label: "Location",
    icon: "#ico36_location_circle",
    component: "won-location-picker",
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
  },
  person: {
    identifier: "person",
    label: "Person",
    icon: "#ico36_person_single_circle",
    component: "won-person-picker",
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
        "s:knowsAbout": getIn(value, ["skills"])
          ? getIn(value, ["skills"]).toJS()
          : undefined,
      };
    },
  },
  // TODO: rename to travelAction
  route: {
    identifier: "travelAction",
    label: "Route (From - To)",
    icon: "#ico36_location_circle",
    component: "won-route-picker",
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
  },
  tags: {
    identifier: "tags",
    label: "Tags",
    icon: "#ico36_tags_circle",
    component: "won-tags-picker",
    parseToRDF: function({ value }) {
      if (!value) {
        return { "won:hasTag": undefined };
      }
      return { "won:hasTag": value };
    },
  },
  ttl: {
    identifier: "ttl",
    label: "Turtle (TTL)",
    icon: "#ico36_rdf_logo_circle",
    component: "won-ttl-picker",
    parseToRDF: function({ value }) {
      if (!value) {
        return undefined;
      }
      // TODO: return value
    },
  },
};
