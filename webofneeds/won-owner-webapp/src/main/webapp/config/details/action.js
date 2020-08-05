import { get } from "../../app/utils.js";
import * as wonUtils from "../../app/won-utils.js";
import Immutable from "immutable";
import { isValidNumber } from "../../app/utils.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import WonDescriptionViewer from "../../app/components/details/viewer/description-viewer.jsx";
import WonDescriptionPicker from "../../app/components/details/picker/description-picker.jsx";
import WonAmountViewer from "../../app/components/details/viewer/amount-viewer.jsx";
import WonAmountPicker from "../../app/components/details/picker/amount-picker.jsx";
import ico36_detail_description from "../../images/won-icons/ico36_detail_description.svg";

export const raiseAction = {
  identifier: "raiseAction",
  label: "Raise Action",
  icon: ico36_detail_description,
  component: WonAmountPicker,
  viewerComponent: WonAmountViewer,
  placeholder: "0",
  unit: [
    { value: "om2:kilogram", label: "kg", default: true },
    { value: "om2:litre", label: "l", default: true },
    { value: "om2:one", label: "piece(s)", default: true },
  ],
  parseToRDF: function({ value, contentUri }) {
    if (
      !value ||
      value.amount === undefined ||
      value.amount < 0 ||
      !value.unit ||
      !isValidNumber(value.amount)
    ) {
      return undefined;
    }
    const payload = {
      "vf:accountingQuantity": {
        "om2:hasUnit": { "@id": value.unit },
        "om2:hasNumericalValue": value.amount,
      },
    };
    return generateEconomicEvent({
      baseUri: wonUtils.genDetailBaseUri(contentUri),
      actionType: "vf:raise",
      payload: payload,
    });
  },
  parseFromRDF: function(jsonLDImm) {
    const amount = jsonLdUtils.parseFrom(
      jsonLDImm,
      ["vf:EconomicEvent", "vf:accountingQuantity", "om2:hasNumericalValue"],
      "xsd:float"
    );
    const unit = jsonLdUtils.parseFrom(
      jsonLDImm,
      ["vf:EconomicEvent", "vf:accountingQuantity", "om2:hasUnit"],
      "xsd:id"
    );

    if (amount === undefined || !unit) {
      return undefined;
    } else {
      return {
        amount: amount,
        unit: unit,
      };
    }
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (
      !value ||
      value.amount === undefined ||
      value.amount < 0 ||
      !value.unit ||
      !isValidNumber(value.amount)
    ) {
      return undefined;
    }
    const amount = value.amount;
    let unitLabel = undefined;

    this.unit &&
      this.unit.forEach(unit => {
        if (unit.value === value.unit) {
          unitLabel = unit.label;
        }
      });
    unitLabel = unitLabel || value.unit;

    return (includeLabel ? this.label + ": " + amount : amount) + unitLabel;
  },
};

export const lowerAction = {
  identifier: "lowerAction",
  label: "Lower Action",
  icon: ico36_detail_description,
  component: WonAmountPicker,
  viewerComponent: WonAmountViewer,
  placeholder: "0",
  unit: [
    { value: "om2:kilogram", label: "kg", default: true },
    { value: "om2:litre", label: "l", default: true },
    { value: "om2:one", label: "piece(s)", default: true },
  ],
  parseToRDF: function({ value, contentUri }) {
    if (
      !value ||
      value.amount === undefined ||
      value.amount < 0 ||
      !value.unit ||
      !isValidNumber(value.amount)
    ) {
      return undefined;
    }
    const payload = {
      "vf:accountingQuantity": {
        "om2:hasUnit": { "@id": value.unit },
        "om2:hasNumericalValue": value.amount,
      },
    };
    return generateEconomicEvent({
      baseUri: wonUtils.genDetailBaseUri(contentUri),
      actionType: "vf:lower",
      payload: payload,
    });
  },
  parseFromRDF: function(jsonLDImm) {
    const amount = jsonLdUtils.parseFrom(
      jsonLDImm,
      ["vf:EconomicEvent", "vf:accountingQuantity", "om2:hasNumericalValue"],
      "xsd:float"
    );
    const unit = jsonLdUtils.parseFrom(
      jsonLDImm,
      ["vf:EconomicEvent", "vf:accountingQuantity", "om2:hasUnit"],
      "xsd:id"
    );

    if (amount === undefined || !unit) {
      return undefined;
    } else {
      return {
        amount: amount,
        unit: unit,
      };
    }
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (
      !value ||
      value.amount === undefined ||
      value.amount < 0 ||
      !value.unit ||
      !isValidNumber(value.amount)
    ) {
      return undefined;
    }
    const amount = value.amount;
    let unitLabel = undefined;

    this.unit &&
      this.unit.forEach(unit => {
        if (unit.value === value.unit) {
          unitLabel = unit.label;
        }
      });
    unitLabel = unitLabel || value.unit;

    return (includeLabel ? this.label + ": " + amount : amount) + unitLabel;
  },
};

export const customAction = {
  identifier: "customAction",
  label: "Custom Action",
  icon: ico36_detail_description,
  placeholder: "Enter Action Description...",
  component: WonDescriptionPicker,
  viewerComponent: WonDescriptionViewer,
  parseToRDF: function({ value, contentUri }) {
    const payload = {
      "s:description": value,
    };
    return value
      ? generateEconomicEvent({
          baseUri: wonUtils.genDetailBaseUri(contentUri),
          actionType: "vf:noEffect",
          payload: payload,
        })
      : undefined;
  },
  parseFromRDF: function(jsonLDImm) {
    const description = get(jsonLDImm, "vf:EconomicEvent");
    if (Immutable.List.isList(description)) {
      return get(
        description.filter(d => get(d, "@language") === "en").first(),
        "@value"
      );
    } else {
      return jsonLdUtils.parseFrom(
        jsonLDImm,
        ["vf:EconomicEvent", "s:description"],
        "xsd:string"
      );
    }
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      return includeLabel ? this.label + ": " + value : value;
    }
    return undefined;
  },
};

function generateEconomicEvent({ baseUri, actionType, payload }) {
  if (!payload) {
    return undefined;
  }

  return {
    "vf:EconomicEvent": {
      "@id": baseUri ? baseUri + "-economicEvent" : undefined,
      "@type": "vf:EconomicEvent",
      "vf:action": { "@id": actionType },
      ...payload,
    },
  };
}
