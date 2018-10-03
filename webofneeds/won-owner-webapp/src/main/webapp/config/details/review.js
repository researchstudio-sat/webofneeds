/**
 * Created by fsuda on 03.10.2018.
 */
import { generateIdString } from "../../app/utils.js";
import won from "../../app/won-es6.js";

export const review = {
  identifier: "review",
  label: "Review",
  icon: "#ico36_detail_title" /*TODO: REVIEW/RATING ICON*/,
  component: "won-review-picker",
  viewerComponent: "won-review-viewer",
  currency: [
    { value: "EUR", label: "€", default: true },
    { value: "USD", label: "$" },
    { value: "GBP", label: "£" },
  ],
  unitCode: [
    { value: "MON", label: "per month" },
    { value: "WEE", label: "per week" },
    { value: "DAY", label: "per day" },
    { value: "HUR", label: "per hour" },
    { value: "", label: "total", default: true },
  ],
  messageEnabled: true,
  parseToRDF: function({ value, identifier, contentUri }) {
    //TODO IMPL
    if (!value || !value.amount || !value.currency) {
      return { "s:review": undefined };
    }

    return {
      "s:review": {
        "@id":
          contentUri && identifier
            ? contentUri + "/" + identifier + "/" + generateIdString(10)
            : undefined,
        "@type": "s:CompoundPriceSpecification",
        "s:price": [{ "@value": value.amount, "@type": "xsd:float" }],
        "s:priceCurrency": value.currency,
        "s:unitCode": value.unitCode,
      },
    };
  },
  parseFromRDF: function(jsonLDImm) {
    const amount = won.parseFrom(
      jsonLDImm,
      ["s:review", "s:price"],
      "xsd:float"
    );

    const currency = won.parseFrom(
      jsonLDImm,
      ["s:review", "s:priceCurrency"],
      "xsd:string"
    );

    const unitCode = won.parseFrom(
      jsonLDImm,
      ["s:review", "s:unitCode"],
      "xsd:string"
    );

    if (!amount || !currency) {
      return undefined;
    } else {
      return { amount: amount, currency: currency, unitCode: unitCode };
    }
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      const amount = value.amount;

      let currencyLabel = undefined;
      let unitCodeLabel = undefined;

      this.currency &&
        this.currency.forEach(curr => {
          if (curr.value === value.currency) {
            currencyLabel = curr.label;
          }
        });
      currencyLabel = currencyLabel || value.currency;

      this.unitCode &&
        this.unitCode.forEach(uc => {
          if (uc.value === value.unitCode) {
            unitCodeLabel = uc.label;
          }
        });
      unitCodeLabel = unitCodeLabel || value.unitCode;

      if (unitCodeLabel) {
        return (
          (includeLabel ? this.label + ": " + amount : amount) +
          currencyLabel +
          " " +
          unitCodeLabel
        );
      } else {
        return (
          (includeLabel ? this.label + ": " + amount : amount) + currencyLabel
        );
      }
    }
    return undefined;
  },
};
