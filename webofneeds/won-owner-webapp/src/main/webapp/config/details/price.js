import { generateIdString } from "../../app/utils.js";
import Immutable from "immutable";
import won from "../../app/won-es6.js";

export const pricerange = {
  identifier: "pricerange",
  label: "Price range",
  minLabel: "Min",
  maxLabel: "Max",
  minPlaceholder: "Min Price",
  maxPlaceholder: "Max Price",
  icon: "#ico36_detail_price",
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
  component: "won-price-range-picker",
  viewerComponent: "won-price-viewer",
  messageEnabled: true,
  parseToRDF: function({ value, identifier, contentUri }) {
    if (!value || !(value.min || value.max) || !value.currency) {
      return { "s:priceSpecification": undefined };
    }
    return {
      "s:priceSpecification": {
        "@id":
          contentUri && identifier
            ? contentUri + "/" + identifier + "/" + generateIdString(10)
            : undefined,
        "@type": "s:CompoundPriceSpecification",
        "s:minPrice": value.min && [
          { "@value": value.min, "@type": "xsd:float" },
        ],
        "s:maxPrice": value.max && [
          { "@value": value.max, "@type": "xsd:float" },
        ],
        "s:priceCurrency": value.currency,
        "s:unitCode": value.unitCode,
        "s:description": "total rent per month in between min/max",
      },
    };
  },
  parseFromRDF: function(jsonLDImm) {
    const minRent = won.parseFrom(
      jsonLDImm,
      ["s:priceSpecification", "s:minPrice"],
      "xsd:float"
    );
    const maxRent = won.parseFrom(
      jsonLDImm,
      ["s:priceSpecification", "s:maxPrice"],
      "xsd:float"
    );
    const currency = won.parseFrom(
      jsonLDImm,
      ["s:priceSpecification", "s:priceCurrency"],
      "xsd:string"
    );
    const unitCode = won.parseFrom(
      jsonLDImm,
      ["s:priceSpecification", "s:unitCode"],
      "xsd:string"
    );

    if (!minRent && !maxRent) {
      return undefined;
    } else if (!currency) {
      return undefined;
    } else {
      // if there's anything, use it
      return Immutable.fromJS({
        min: minRent,
        max: maxRent,
        currency: currency,
        unitCode: unitCode,
      });
    }
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      const min = value.min;
      const max = value.max;

      let amount;
      if (min && max) {
        amount = min + " - " + max;
      } else if (min) {
        amount = "at least " + min;
      } else if (max) {
        amount = "at most " + max;
      } else {
        return undefined;
      }

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

export const price = {
  identifier: "price",
  label: "Price",
  icon: "#ico36_detail_price",
  placeholder: "Enter Price...",
  component: "won-price-picker",
  viewerComponent: "won-price-viewer",
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
    if (!value || !value.amount || !value.currency) {
      return { "s:priceSpecification": undefined };
    }

    return {
      "s:priceSpecification": {
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
      ["s:priceSpecification", "s:price"],
      "xsd:float"
    );

    const currency = won.parseFrom(
      jsonLDImm,
      ["s:priceSpecification", "s:priceCurrency"],
      "xsd:string"
    );

    const unitCode = won.parseFrom(
      jsonLDImm,
      ["s:priceSpecification", "s:unitCode"],
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
