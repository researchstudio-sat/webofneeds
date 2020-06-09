import {
  details,
  abstractDetails,
  mergeInEmptyDraft,
} from "../detail-definitions.js";
import { sparqlQuery } from "../../app/sparql-builder-utils.js";
import won from "../../app/service/won.js";
import vocab from "../../app/service/vocab.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";

import { isValidNumber, get, getIn } from "../../app/utils.js";
import ico36_detail_measurement from "../../images/won-icons/ico36_detail_measurement.svg";
import ico36_uc_transport_demand from "../../images/won-icons/ico36_uc_transport_demand.svg";
import ico36_detail_weight from "../../images/won-icons/ico36_detail_weight.svg";

export const goodsTransportSearch = {
  identifier: "goodsTransportSearch",
  label: "Send something",
  icon: ico36_uc_transport_demand,
  doNotMatchAfter: jsonLdUtils.findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        title: "Want to send something",
        type: ["http://dbpedia.org/resource/Cargo"],
      },
    }),
  },
  reactions: {
    [vocab.CHAT.ChatSocketCompacted]: {
      [vocab.CHAT.ChatSocketCompacted]: ["goodsTransportOffer"],
    },
  },
  details: {
    title: { ...details.title },
    content: {
      ...details.description,
      identifier: "content",
      label: "Content",
      placeholder: "Provide information about what should be transported",
      parseToRDF: function({ value }) {
        if (!value) {
          return { "s:name": undefined };
        } else {
          return { "@type": "s:Product", "s:name": value };
        }
      },
      parseFromRDF: function(jsonLDImm) {
        const content = jsonLdUtils.parseFrom(
          jsonLDImm,
          ["s:name"],
          "xsd:string"
        );
        const type = get(jsonLDImm, "@type");
        if (content && type === "s:Product") {
          return content;
        } else {
          return undefined;
        }
      },
    },
    weight: {
      ...abstractDetails.number,
      identifier: "weight",
      label: "Weight in kg",
      icon: ico36_detail_weight,
      parseToRDF: function({ value }) {
        if (!isValidNumber(value)) {
          return { "s:weight": undefined };
        } else {
          return {
            "@type": "s:Product",
            "s:weight": {
              "@type": "s:QuantitativeValue",
              "s:value": [{ "@value": value, "@type": "xsd:float" }],
              "s:unitCode": "KGM",
            },
          };
        }
      },
      parseFromRDF: function(jsonLDImm) {
        const w = jsonLdUtils.parseFrom(
          jsonLDImm,
          ["s:weight", "s:value"],
          "xsd:float"
        );
        const unit = jsonLdUtils.getInFromJsonLd(
          jsonLDImm,
          ["s:weight", "s:unitCode"],
          won.defaultContext
        );

        if (!w) {
          return undefined;
        } else {
          if (unit === "KGM") {
            return w + "kg";
          } else if (unit === "GRM") {
            return w + "g";
          } else if (!unit) {
            return w + " (no unit specified)";
          }
          return w + " " + unit;
        }
      },
      generateHumanReadable: function({ value, includeLabel }) {
        if (isValidNumber(value)) {
          return (includeLabel ? this.label + ": " + value : value) + "kg";
        }
        return undefined;
      },
    },
    length: {
      ...abstractDetails.number,
      identifier: "length",
      label: "Length in cm",
      icon: ico36_detail_measurement,
      parseToRDF: function({ value }) {
        if (!isValidNumber(value)) {
          return { "s:length": undefined };
        } else {
          return {
            "@type": "s:Product",
            "s:length": {
              "@type": "s:QuantitativeValue",
              "s:value": [{ "@value": value, "@type": "xsd:float" }],
              "s:unitCode": "CMT",
            },
          };
        }
      },
      parseFromRDF: function(jsonLDImm) {
        const l = jsonLdUtils.parseFrom(
          jsonLDImm,
          ["s:length", "s:value"],
          "xsd:float"
        );
        const unit = jsonLdUtils.getInFromJsonLd(
          jsonLDImm,
          ["s:length", "s:unitCode"],
          won.defaultContext
        );

        if (!l) {
          return undefined;
        } else {
          if (unit === "CMT") {
            return l + "cm";
          } else if (unit === "MTR") {
            return l + "m";
          } else if (!unit) {
            return l + " (no unit specified)";
          }
          return l + " " + unit;
        }
      },
      generateHumanReadable: function({ value, includeLabel }) {
        if (isValidNumber(value)) {
          return (includeLabel ? this.label + ": " + value : value) + "cm";
        }
        return undefined;
      },
    },
    width: {
      ...abstractDetails.number,
      identifier: "width",
      label: "Width in cm",
      icon: ico36_detail_measurement,
      parseToRDF: function({ value }) {
        if (!isValidNumber(value)) {
          return { "s:width": undefined };
        } else {
          return {
            "@type": "s:Product",
            "s:width": {
              "@type": "s:QuantitativeValue",
              "s:value": [{ "@value": value, "@type": "xsd:float" }],
              "s:unitCode": "CMT",
            },
          };
        }
      },
      parseFromRDF: function(jsonLDImm) {
        const w = jsonLdUtils.parseFrom(
          jsonLDImm,
          ["s:width", "s:value"],
          "xsd:float"
        );
        const unit = jsonLdUtils.getInFromJsonLd(
          jsonLDImm,
          ["s:width", "s:unitCode"],
          won.defaultContext
        );

        if (!w) {
          return undefined;
        } else {
          if (unit === "CMT") {
            return w + "cm";
          } else if (unit === "MTR") {
            return w + "m";
          } else if (!unit) {
            return w + " (no unit specified)";
          }
          return w + " " + unit;
        }
      },
      generateHumanReadable: function({ value, includeLabel }) {
        if (isValidNumber(value)) {
          return (includeLabel ? this.label + ": " + value : value) + "cm";
        }
        return undefined;
      },
    },
    height: {
      ...abstractDetails.number,
      identifier: "height",
      label: "Height in cm",
      icon: ico36_detail_measurement,
      parseToRDF: function({ value }) {
        if (!isValidNumber(value)) {
          return { "s:height": undefined };
        } else {
          return {
            "@type": "s:Product",
            "s:height": {
              "@type": "s:QuantitativeValue",
              "s:value": [{ "@value": value, "@type": "xsd:float" }],
              "s:unitCode": "CMT",
            },
          };
        }
      },
      parseFromRDF: function(jsonLDImm) {
        const h = jsonLdUtils.parseFrom(
          jsonLDImm,
          ["s:height", "s:value"],
          "xsd:float"
        );
        const unit = jsonLdUtils.getInFromJsonLd(
          jsonLDImm,
          ["s:height", "s:unitCode"],
          won.defaultContext
        );

        if (!h) {
          return undefined;
        } else {
          if (unit === "CMT") {
            return h + "cm";
          } else if (unit === "MTR") {
            return h + "m";
          } else if (!unit) {
            return h + " (no unit specified)";
          }
          return h + " " + unit;
        }
      },
      generateHumanReadable: function({ value, includeLabel }) {
        if (isValidNumber(value)) {
          return (includeLabel ? this.label + ": " + value : value) + "cm";
        }
        return undefined;
      },
    },
    tags: { ...details.tags },
    images: { ...details.images },
  },
  seeksDetails: {
    travelAction: { ...details.travelAction },
  },
  generateQuery: (draft, resultName) => {
    const fromLocation = getIn(draft, [
      "seeks",
      "travelAction",
      "fromLocation",
    ]);
    const toLocation = getIn(draft, ["seeks", "travelAction", "toLocation"]);

    let filter;
    if (
      fromLocation &&
      fromLocation.lat &&
      fromLocation.lng &&
      toLocation &&
      toLocation.lat &&
      toLocation.lng
    ) {
      filter = {
        prefixes: {
          won: won.defaultContext["won"],
          s: won.defaultContext["s"],
          geo: "http://www.bigdata.com/rdf/geospatial#",
          xsd: "http://www.w3.org/2001/XMLSchema#",
        },
        operations: [
          `${resultName} a won:Atom.`,
          `${resultName} a <http://dbpedia.org/resource/Transport>. `,
          `${resultName} (won:location|s:location) ?location.`,
          "?location s:geo ?location_geo.",
          "?location_geo s:latitude ?location_lat;",
          "s:longitude ?location_lon;",
          `bind (abs(xsd:decimal(?location_lat) - ${
            fromLocation.lat
          }) as ?fromLatDiffRaw)`,
          `bind (abs(xsd:decimal(?location_lon) - ${
            fromLocation.lng
          }) as ?fromLonDiff)`,
          "bind (if ( ?fromLatDiffRaw > 180, 360 - ?fromLatDiffRaw, ?fromLatDiffRaw ) as ?fromLatDiff)",
          "bind ( ?fromLatDiff * ?fromLatDiff + ?fromLonDiff * ?fromLonDiff as ?fromLocation_geoDistanceScore)",
          `bind (abs(xsd:decimal(?location_lat) - ${
            toLocation.lat
          }) as ?latDiffRaw)`,
          `bind (abs(xsd:decimal(?location_lon) - ${
            toLocation.lng
          }) as ?toLonDiff)`,
          "bind (if ( ?toLatDiffRaw > 180, 360 - ?toLatDiffRaw, ?toLatDiffRaw ) as ?toLatDiff)",
          "bind ( ?toLatDiff * ?toLatDiff + ?toLonDiff * ?toLonDiff as ?toLocation_geoDistanceScore)",
          "bind (?fromLocation_geoDistanceScore + ?toLocation_geoDistanceScore as ?distScore)",
        ],
      };
    } else if (fromLocation && fromLocation.lat && fromLocation.lng) {
      filter = {
        prefixes: {
          won: won.defaultContext["won"],
          s: won.defaultContext["s"],
          geo: "http://www.bigdata.com/rdf/geospatial#",
          xsd: "http://www.w3.org/2001/XMLSchema#",
        },
        operations: [
          `${resultName} a won:Atom.`,
          `${resultName} a <http://dbpedia.org/resource/Transport>.`,
          `${resultName} (won:location|s:location) ?location.`,
          "?location s:geo ?location_geo.",
          "?location_geo s:latitude ?location_lat;",
          "s:longitude ?location_lon;",
          `bind (abs(xsd:decimal(?location_lat) - ${
            fromLocation.lat
          }) as ?latDiffRaw)`,
          `bind (abs(xsd:decimal(?location_lon) - ${
            fromLocation.lng
          }) as ?lonDiff)`,
          "bind (if ( ?latDiffRaw > 180, 360 - ?latDiffRaw, ?latDiffRaw ) as ?latDiff)",
          "bind ( ?latDiff * ?latDiff + ?lonDiff * ?lonDiff as ?location_geoDistanceScore)",
          "bind (?location_geoDistanceScore as ?distScore)",
        ],
      };
    } else if (toLocation && toLocation.lat && toLocation.lng) {
      filter = {
        prefixes: {
          won: won.defaultContext["won"],
          s: won.defaultContext["s"],
          geo: "http://www.bigdata.com/rdf/geospatial#",
          xsd: "http://www.w3.org/2001/XMLSchema#",
        },
        operations: [
          `${resultName} a won:Atom.`,
          `${resultName} a <http://dbpedia.org/resource/Transport>.`,
          `${resultName} (won:location|s:location) ?location.`,
          "?location s:geo ?location_geo.",
          "?location_geo s:latitude ?location_lat;",
          "s:longitude ?location_lon;",
          `bind (abs(xsd:decimal(?location_lat) - ${
            toLocation.lat
          }) as ?latDiffRaw)`,
          `bind (abs(xsd:decimal(?location_lon) - ${
            toLocation.lng
          }) as ?lonDiff)`,
          "bind (if ( ?latDiffRaw > 180, 360 - ?latDiffRaw, ?latDiffRaw ) as ?latDiff)",
          "bind ( ?latDiff * ?latDiff + ?lonDiff * ?lonDiff as ?location_geoDistanceScore)",
          "bind (?location_geoDistanceScore as ?distScore)",
        ],
      };
    } else {
      filter = {
        prefixes: {
          won: won.defaultContext["won"],
        },
        operations: [
          `${resultName} a won:Atom.`,
          `${resultName} a <http://dbpedia.org/resource/Transport>. `,
        ],
      };
    }

    const generatedQuery = sparqlQuery({
      prefixes: filter.prefixes,
      distinct: true,
      variables: [resultName],
      where: filter.operations,
      orderBy: [
        {
          order: "ASC",
          variable: "?distScore",
        },
      ],
    });

    return generatedQuery;
  },
};
