import { generateIdString } from "../../app/utils.js";
//import Immutable from "immutable";
import won from "../../app/won-es6.js";
import { get } from "../../app/utils.js";

export const paypalPayment = {
  identifier: "paypalPayment",
  label: "PayPal Payment",
  icon: "#ico36_detail_price", //TODO: ADAPT
  amountLabel: "Price:",
  amountPlaceholder: "Enter Amount...",
  receiverLabel: "Recipient:",
  receiverPlaceholder: "PayPal Account ID (Email)...",
  secretLabel: "Secret:",
  secretPlaceholder: "Enter Secret...",
  customerLabel: "Who pays?",
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
      !value.customerUri
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
          "@type": "s:PaymentMethod",
          "@id": "gr:PayPal",
        },
        "s:paymentStatus": { "@id": "s:PaymentDue" },
        "s:totalPaymentDue": {
          "@id": idString ? idString + "#totalPaymentDue" : undefined,
          "@type": "s:CompoundPriceSpecification",
          "s:price": [{ "@value": value.amount, "@type": "xsd:float" }],
          "s:priceCurrency": value.currency,
        },
        "s:paymentMethodId": value.secret, //TODO not sure if this would be the correct predicate for our Secret
        "s:accountId": value.receiver,
        "s:customer": {
          "@type": "won:Need",
          "@id": value.customerUri,
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
    const invoice = get(jsonLDImm, "s:invoice");
    if (!invoice) return undefined;

    //Only Parse PayPal Payments
    const paymentMethod = get(invoice, "s:paymentMethod");
    if (
      !paymentMethod ||
      paymentMethod.get("@type") !== "s:PaymentMethod" ||
      paymentMethod.get("@id") !== "gr:PayPal"
    ) {
      return undefined;
    }

    const customer = get(invoice, "s:customer");
    if (!customer || customer.get("@type") !== won.WON.NeedCompacted) {
      return undefined;
    }

    const customerUri = get(customer, "@id");
    const amount = won.parseFrom(
      invoice,
      ["s:totalPaymentDue", "s:price"],
      "xsd:float"
    );
    const currency = won.parseFrom(
      invoice,
      ["s:totalPaymentDue", "s:priceCurrency"],
      "xsd:string"
    );
    const secret = get(invoice, "s:paymentMethodId");
    const receiver = get(invoice, "s:accountId");

    if (!amount || !currency || !secret || !receiver || !customerUri) {
      return undefined;
    }

    return {
      amount: amount,
      currency: currency,
      secret: secret,
      receiver: receiver,
      customerUri: customerUri,
    };
  },
  generateHumanReadable: function({ value, includeLabel }) {
    //TODO: IMPL
    if (value) {
      const amount = value.amount;
      const secret = value.secret;
      const receiver = value.receiver;
      const customerUri = value.customerUri;

      let currencyLabel = undefined;

      this.currency &&
        this.currency.forEach(curr => {
          if (curr.value === value.currency) {
            currencyLabel = curr.label;
          }
        });
      currencyLabel = currencyLabel || value.currency;

      const amountString = "Amount: " + amount + currencyLabel;
      const receiverString = "Recipient: " + receiver;
      const customerString = "Customer: <" + customerUri + ">";
      const secretString = "Secret: " + secret;

      const fullHumanReadable =
        amountString +
        " " +
        receiverString +
        " " +
        customerString +
        " " +
        secretString;

      return includeLabel
        ? this.label + ": " + fullHumanReadable
        : fullHumanReadable;
    }
    return undefined;
  },
};
