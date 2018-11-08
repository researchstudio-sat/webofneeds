/**
 * Created by fsuda on 18.09.2018.
 */
import { details, abstractDetails, emptyDraft } from "../detail-definitions.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";
import won from "../../app/won-es6.js";
import { isValidNumber, get, getIn, getInFromJsonLd } from "../../app/utils.js";
import {
  filterInVicinity,
  concatenateFilters,
  sparqlQuery,
} from "../../app/sparql-builder-utils.js";

export const transportGroup = {
  identifier: "transportgroup",
  label: "Transport and Delivery",
  icon: undefined,
  useCases: {
    transportDemand: {
      identifier: "transportDemand",
      label: "Send something",
      icon: "#ico36_uc_transport_demand",
      doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
      draft: {
        ...emptyDraft,
        content: {
          title: "Want to send something",
          type: "http://dbpedia.org/resource/Cargo",
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
            const content = won.parseFrom(jsonLDImm, ["s:name"], "xsd:string");
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
          icon: "#ico36_detail_weight",
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
            const w = won.parseFrom(
              jsonLDImm,
              ["s:weight", "s:value"],
              "xsd:float"
            );
            const unit = getInFromJsonLd(
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
          icon: "#ico36_detail_measurement",
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
            const l = won.parseFrom(
              jsonLDImm,
              ["s:length", "s:value"],
              "xsd:float"
            );
            const unit = getInFromJsonLd(
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
          icon: "#ico36_detail_measurement",
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
            const w = won.parseFrom(
              jsonLDImm,
              ["s:width", "s:value"],
              "xsd:float"
            );
            const unit = getInFromJsonLd(
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
          icon: "#ico36_detail_measurement",
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
            const h = won.parseFrom(
              jsonLDImm,
              ["s:height", "s:value"],
              "xsd:float"
            );
            const unit = getInFromJsonLd(
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
        const toLocation = getIn(draft, [
          "seeks",
          "travelAction",
          "toLocation",
        ]);

        const baseFilter = {
          prefixes: {
            won: won.defaultContext["won"],
          },
          operations: [
            `${resultName} a won:Need.`,
            `${resultName} won:isInState won:Active. ?is a <http://dbpedia.org/resource/Transport>. `,
            `${resultName} won:is ?is.`,
          ],
        };

        const locationFilter = filterInVicinity(
          "?location",
          fromLocation,
          /*radius=*/ 100
        );
        const fromLocationFilter = filterInVicinity(
          "?fromLocation",
          fromLocation,
          /*radius=*/ 5
        );
        const toLocationFilter = filterInVicinity(
          "?toLocation",
          toLocation,
          /*radius=*/ 5
        );

        const union = operations => {
          if (!operations || operations.length === 0) {
            return "";
          } else {
            return "{" + operations.join("} UNION {") + "}";
          }
        };
        const filterAndJoin = (arrayOfStrings, seperator) =>
          arrayOfStrings.filter(str => str).join(seperator);

        const locationFilters = {
          prefixes: locationFilter.prefixes,
          operations: union([
            filterAndJoin(
              [
                fromLocation &&
                  `?is a <http://dbpedia.org/resource/Transport>. ?is won:travelAction/s:fromLocation ?fromLocation. `,
                fromLocation && fromLocationFilter.operations.join(" "),
                toLocation && "?is won:travelAction/s:toLocation ?toLocation.",
                toLocation && toLocationFilter.operations.join(" "),
              ],
              " "
            ),
            filterAndJoin(
              [
                location &&
                  `?is a <http://dbpedia.org/resource/Transport> . ?is won:hasLocation ?location .`,
                location && locationFilter.operations.join(" "),
              ],
              " "
            ),
          ]),
        };

        const concatenatedFilter = concatenateFilters([
          baseFilter,
          locationFilters,
        ]);

        return sparqlQuery({
          prefixes: concatenatedFilter.prefixes,
          distinct: true,
          variables: [resultName],
          where: concatenatedFilter.operations,
          orderBy: [
            {
              order: "ASC",
              variable: "?location_geoDistance",
            },
          ],
        });
      },
    },
    transportOffer: {
      identifier: "transportOffer",
      label: "Offer goods transport",
      icon: "#ico36_uc_transport_offer",
      doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
      draft: {
        ...emptyDraft,
        content: {
          title: "Transportation offer",
          type: "http://dbpedia.org/resource/Transport",
        },
      },
      details: {
        title: { ...details.title },
        location: { ...details.location },
      },
      seeksDetails: {
        tags: { ...details.tags },
        description: { ...details.description },
      },
      generateQuery: (draft, resultName) => {
        const location = getIn(draft, ["content", "location"]);
        const filters = [
          {
            // to select seeks-branch
            prefixes: {
              won: won.defaultContext["won"],
            },
            operations: [
              `${resultName} a won:Need.`,
              `${resultName} won:seeks ?seeks.`,
              `${resultName} won:is ?is.`,
              `?is a <http://dbpedia.org/resource/Cargo>.`,
              location && "?seeks won:travelAction/s:fromLocation ?location.",
            ],
          },

          filterInVicinity("?location", location, /*radius=*/ 100),
        ];

        const concatenatedFilter = concatenateFilters(filters);

        return sparqlQuery({
          prefixes: concatenatedFilter.prefixes,
          distinct: true,
          variables: [resultName],
          where: concatenatedFilter.operations,
          orderBy: [
            {
              order: "ASC",
              variable: "?location_geoDistance",
            },
          ],
        });
      },
    },
  },
};
