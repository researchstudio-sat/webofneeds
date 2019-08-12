import { generateIdString, get } from "../../app/utils.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import WonPaypalPaymentViewer from "../../app/components/details/viewer/paypal-payment-viewer.jsx";

// TODO: don't demand customer info
export const paypalPayment = {
  identifier: "paypalPayment",
  label: "PayPal Payment",
  icon: "#ico36_detail_price", //TODO: create and use better icon
  amountLabel: "Price:",
  amountPlaceholder: "Enter Amount...",
  receiverLabel: "Recipient:",
  receiverPlaceholder: "PayPal Account ID (Email)...",
  // secretLabel: "Secret:",
  // secretPlaceholder: "Enter Secret...",
  //customerLabel: "Invoice will be sent to...",
  component: "won-paypal-payment-picker",
  viewerComponent: WonPaypalPaymentViewer,
  currency: [
    { value: "EUR", label: "€", default: true },
    { value: "USD", label: "$" },
    { value: "GBP", label: "£" },
    //TODO: ADAPT
  ],
  messageEnabled: true,
  parseToRDF: function({ value, identifier, contentUri }) {
    if (
      !value ||
      !value.amount ||
      !value.currency ||
      // !value.secret ||
      !value.receiver //||
      // !value.customerUri
    ) {
      return { "s:invoice": undefined };
    }

    // TODO: error handling/sanity checking
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
        // "s:identifier": value.secret,
        "s:accountId": value.receiver,
        // "s:customer": {
        //   "@type": "won:Atom",
        //   "@id": value.customerUri,
        // },
        // TODO: handle optional information
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

    //Only Parse PayPal Payments - other payments should be handled by other details
    const paymentMethod = get(invoice, "s:paymentMethod");
    if (
      !paymentMethod ||
      paymentMethod.get("@type") !== "s:PaymentMethod" ||
      paymentMethod.get("@id") !== "gr:PayPal"
    ) {
      return undefined;
    }

    // TODO: this should work with personas too?
    // const customer = get(invoice, "s:customer");
    // if (!customer || customer.get("@type") !== won.WON.AtomCompacted) {
    //   return undefined;
    // }

    // const customerUri = get(customer, "@id");
    const amount = jsonLdUtils.parseFrom(
      invoice,
      ["s:totalPaymentDue", "s:price"],
      "xsd:float"
    );
    const currency = jsonLdUtils.parseFrom(
      invoice,
      ["s:totalPaymentDue", "s:priceCurrency"],
      "xsd:string"
    );
    // const secret = get(invoice, "s:identifier");
    const receiver = get(invoice, "s:accountId");

    if (!amount || !currency || !receiver) {
      return undefined;
    }

    // TODO: handle optional information

    return {
      amount: amount,
      currency: currency,
      // secret: secret,
      receiver: receiver,
      // customerUri: customerUri,
    };
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      const amount = value.amount;
      // const secret = value.secret;
      const receiver = value.receiver;
      //const customerUri = value.customerUri;

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
      // TODO: use title or persona name if available
      //const customerString = "Customer: <" + customerUri + ">";
      // const secretString = "Secret: " + secret;

      const fullHumanReadable = amountString + " " + receiverString;

      return includeLabel
        ? this.label + ": " + fullHumanReadable
        : fullHumanReadable;
    }
    return undefined;
  },
};
