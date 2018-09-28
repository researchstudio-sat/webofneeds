/**
 * Created by fsuda on 18.09.2018.
 */
import { details, abstractDetails, emptyDraft } from "../detail-definitions.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";
import won from "../../app/won-es6.js";
import { isValidNumber, get, getInFromJsonLd } from "../../app/utils.js";

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
        is: { title: "Want to send something" },
      },
      isDetails: {
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
    },
    transportOffer: {
      identifier: "transportOffer",
      label: "Offer goods transport",
      icon: "#ico36_uc_transport_offer",
      doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
      draft: {
        ...emptyDraft,
        is: { title: "Transportation offer" },
        searchString: "transport", // TODO: replace this with a query
      },
      isDetails: {
        title: { ...details.title },
        location: { ...details.location },
      },
      seeksDetails: {
        tags: { ...details.tags },
        description: { ...details.description },
      },
      // generateQuery: (draft, resultName) => {
      //   const filterStrings = [];
      //   const prefixes = {
      //     s: won.defaultContext["s"],
      //     won: won.defaultContext["won"],
      //   };

      //   let queryTemplate =
      //     `
      //     ${prefixesString(prefixes)}
      //     SELECT DISTINCT ${resultName}
      //     WHERE {
      //     ${resultName}
      //       won:is ?is.
      //       ${filterStrings && filterStrings.join(" ")}
      //     }` + (location ? `ORDER BY ASC(?geoDistance)` : "");
      //   return new SparqlParser().parse(queryTemplate);
      // },
    },
  },
};
