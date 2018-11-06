import { generateIdString } from "../../app/utils.js";
//import Immutable from "immutable";
import won from "../../app/won-es6.js";

export const paypalPayment = {
  identifier: "paypalPayment",
  label: "PayPal Payment",
  icon: "#ico36_detail_price", //TODO: ADAPT
  amountLabel: "Price:",
  amountPlaceholder: "Enter Amount...",
  receiverLabel: "PayPal Account that should receive Payment:",
  receiverPlaceholder: "PayPal Account ID...",
  secretLabel: "Secret:",
  secretPlaceholder: "Enter Secret...",
  costumerLabel: "Who pays?",
  component: "won-paypal-payment-picker",
  viewerComponent: "won-paypal-payment-viewer",
  currency: [
    { value: "EUR", label: "€", default: true },
    { value: "USD", label: "$" },
    { value: "GBP", label: "£" },
    //TODO: ADAPT
  ],
  messageEnabled: true,
  parseToRDF: function({ value, identifier, contentUri }) {
    //TODO: IMPL
    if (
      !value ||
      !value.amount ||
      !value.currency ||
      !value.secret ||
      !value.receiver ||
      !value.costumerUri
    ) {
      return { "s:invoice": undefined };
    }

    const idString =
      contentUri && identifier
        ? contentUri + "/" + identifier + "/" + generateIdString(10)
        : undefined;
    return {
      "s:invoice": {
        "@id": idString,
        "@type": "s:Invoice",
        "s:paymentMethod": {
          "@type": "PaymentMethod",
          "@id": "gr:PayPal",
        },
        "s:paymentStatus": { "@id": "s:PaymentDue" },
        "s:totalPaymentDue": {
          "@id": idString ? idString + "#totalPaymentDue" : undefined,
          "@type": "s:CompoundPriceSpecification",
          "s:price": [{ "@value": value.amount, "@type": "xsd:float" }],
          "s:priceCurrency": value.currency,
        },
        "s:paymentMethodId": value.secret, //TODO not sure if this would be the correctSecret
        "s:accountId": value.receiver,
        "s:customer": {
          "@type": "won:Need",
          "@id": value.costumerUri,
        },
        //"pay:hasFeePayer": "feePayer", //TODO Adapt and include Optional
        //"pay:hasTax": "hasTax", //TODO Adapt and include Optional
        //"pay:hasInvoiceId": "invoiceId", //TODO Adapt and include Optional
        //"pay:hasExpirationTime": "expTime", //TODO Adapt and include Optional
        //"pay:hasInvoiceDetails": "invoiceDetails", //TODO Adapt and include Optional
      },
    };
  },
  parseFromRDF: function(jsonLDImm) {
    //TODO: IMPL
    const amount = won.parseFrom(
      jsonLDImm,
      ["s:invoice", "s:price"],
      "xsd:float"
    );

    const currency = won.parseFrom(
      jsonLDImm,
      ["s:invoice", "s:priceCurrency"],
      "xsd:string"
    );

    if (!amount || !currency) {
      return undefined;
    } else {
      return { amount: amount, currency: currency };
    }
  },
  generateHumanReadable: function({ value, includeLabel }) {
    //TODO: IMPL
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
