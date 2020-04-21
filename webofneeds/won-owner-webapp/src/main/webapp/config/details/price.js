import { generateIdString } from "../../app/utils.js";
import Immutable from "immutable";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import WonPriceViewer from "../../app/components/details/viewer/price-viewer.jsx";
import WonPricePicker from "../../app/components/details/picker/price-picker.jsx";
import WonPriceRangePicker from "../../app/components/details/picker/price-range-picker.jsx";
import ico36_detail_price from "../../images/won-icons/ico36_detail_price.svg";

export const pricerange = {
  identifier: "pricerange",
  label: "Price range",
  minLabel: "Min",
  maxLabel: "Max",
  minPlaceholder: "Min Price",
  maxPlaceholder: "Max Price",
  icon: ico36_detail_price,
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
  component: WonPriceRangePicker,
  viewerComponent: WonPriceViewer,
  messageEnabled: true,
  parseToRDF: function({ value, identifier, contentUri }) {
    if (
      value === undefined ||
      (value.min === undefined && value.max === undefined) ||
      !value.currency
    ) {
      return { "s:priceSpecification": undefined };
    }
    return {
      "s:priceSpecification": {
        "@id":
          contentUri && identifier
            ? contentUri + "#" + identifier + "-" + generateIdString(10)
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
        "s:description": "price in between min/max",
      },
    };
  },
  parseFromRDF: function(jsonLDImm) {
    const minRent = jsonLdUtils.parseFrom(
      jsonLDImm,
      ["s:priceSpecification", "s:minPrice"],
      "xsd:float"
    );
    const maxRent = jsonLdUtils.parseFrom(
      jsonLDImm,
      ["s:priceSpecification", "s:maxPrice"],
      "xsd:float"
    );
    const currency = jsonLdUtils.parseFrom(
      jsonLDImm,
      ["s:priceSpecification", "s:priceCurrency"],
      "xsd:string"
    );
    const unitCode = jsonLdUtils.parseFrom(
      jsonLDImm,
      ["s:priceSpecification", "s:unitCode"],
      "xsd:string"
    );

    if (minRent === undefined && maxRent === undefined) {
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
      // if min or max are 0, they equal to false -> fine here
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
  icon: ico36_detail_price,
  placeholder: "Enter Price...",
  component: WonPricePicker,
  viewerComponent: WonPriceViewer,
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
    if (
      !value ||
      value.amount === undefined ||
      value.amount < 0 ||
      !value.currency
    ) {
      return { "s:priceSpecification": undefined };
    }

    return {
      "s:priceSpecification": {
        "@id":
          contentUri && identifier
            ? contentUri + "#" + identifier + "-" + generateIdString(10)
            : undefined,
        "@type": "s:CompoundPriceSpecification",
        "s:price": [{ "@value": value.amount, "@type": "xsd:float" }],
        "s:priceCurrency": value.currency,
        "s:unitCode": value.unitCode,
      },
    };
  },
  parseFromRDF: function(jsonLDImm) {
    const amount = jsonLdUtils.parseFrom(
      jsonLDImm,
      ["s:priceSpecification", "s:price"],
      "xsd:float"
    );

    const currency = jsonLdUtils.parseFrom(
      jsonLDImm,
      ["s:priceSpecification", "s:priceCurrency"],
      "xsd:string"
    );

    const unitCode = jsonLdUtils.parseFrom(
      jsonLDImm,
      ["s:priceSpecification", "s:unitCode"],
      "xsd:string"
    );

    if (amount === undefined || !currency) {
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
