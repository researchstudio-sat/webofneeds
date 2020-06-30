/**
 * Created by ms on 29.06.2020.
 */
import { details } from "../detail-definitions.js";
import { isValidNumber } from "../../app/utils.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import WonAmountViewer from "../../app/components/details/viewer/amount-viewer.jsx";
import WonAmountPicker from "../../app/components/details/picker/amount-picker.jsx";
import ico36_detail_floorsize from "../../images/won-icons/ico36_detail_floorsize.svg";

export const name = {
  ...details.title,
  identifier: "name",
  label: "Name",
  parseToRDF: function({ value }) {
    return value ? { "vf:name": value } : undefined;
  },
  parseFromRDF: function(jsonLDImm) {
    const dcTitle = jsonLdUtils.parseFrom(jsonLDImm, ["vf:name"], "xsd:string");
    return (
      dcTitle || jsonLdUtils.parseFrom(jsonLDImm, ["vf:name"], "xsd:string")
    );
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      return includeLabel ? this.label + ": " + value : value;
    }
    return undefined;
  },
};

export const accountingQuantity = {
  identifier: "accountingQuantity",
  icon: ico36_detail_floorsize,
  label: "Accounting quantity",
  placeholder: "1",
  messageEnabled: false,
  component: WonAmountPicker,
  viewerComponent: WonAmountViewer,
  unit: [
    { value: "om:kilogram", label: "kg", default: true },
    { value: "om:litre", label: "L", default: true },
  ],

  parseToRDF: function({ value }) {
    if (
      !value ||
      value.amount === undefined ||
      value.amount < 0 ||
      !value.unit ||
      !isValidNumber(value.amount)
    ) {
      return { "vf:accountingQuantity": undefined };
    } else {
      return {
        "vf:accountingQuantity": {
          "om2:hasUnit": { "@id": value.unit },
          "om2:hasNumericalValue": value.amount,
        },
      };
    }
  },
  parseFromRDF: function(jsonLDImm) {
    const amount = jsonLdUtils.parseFrom(
      jsonLDImm,
      ["vf:accountingQuantity", "om2:hasNumericalValue"],
      "xsd:float"
    );
    const unit = jsonLdUtils.parseFrom(
      jsonLDImm,
      ["vf:accountingQuantity", "om2:hasUnit"],
      "xsd:id"
    );

    if (amount === undefined && !unit) {
      return undefined;
    } else {
      return {
        amount: amount,
        unit: unit,
      };
    }
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
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
    }
    return undefined;
  },
};

export const onhandQuantity = {
  identifier: "onhandQuantity",
  icon: ico36_detail_floorsize,
  label: "Onhand quantity",
  placeholder: "1",
  messageEnabled: false,
  component: WonAmountPicker,
  viewerComponent: WonAmountViewer,
  unit: [
    { value: "om:kilogram", label: "kg", default: true },
    { value: "om:litre", label: "L", default: true },
  ],

  parseToRDF: function({ value }) {
    if (
      !value ||
      value.amount === undefined ||
      value.amount < 0 ||
      !value.unit ||
      !isValidNumber(value.amount)
    ) {
      return { "vf:onhandQuantity": undefined };
    } else {
      return {
        "vf:onhandQuantity": {
          "om2:hasUnit": { "@id": value.unit },
          "om2:hasNumericalValue": value.amount,
        },
      };
    }
  },
  parseFromRDF: function(jsonLDImm) {
    const amount = jsonLdUtils.parseFrom(
      jsonLDImm,
      ["vf:onhandQuantity", "om2:hasNumericalValue"],
      "xsd:float"
    );
    const unit = jsonLdUtils.parseFrom(
      jsonLDImm,
      ["vf:onhandQuantity", "om2:hasUnit"],
      "xsd:id"
    );

    if (amount === undefined && !unit) {
      return undefined;
    } else {
      return {
        amount: amount,
        unit: unit,
      };
    }
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
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
    }
    return undefined;
  },
};

/**
 * WIP
 */
export const currentLocation = {
  ...details.location,
  identifier: "currentLocation",
  parseToRDF: function({ value, contentUri }) {
    return {
      "vf:currentLocation": details.location.parseToRDF(value, contentUri),
    };
  },
  parseFromRDF: function(jsonLDImm) {
    const location = jsonLdUtils.parseFrom(jsonLDImm, ["vf:currentLocation"]);
    const jsonldLocation =
      jsonLDImm && (location.get("s:location") || location.get("won:location"));
    return jsonLdUtils.parseSPlace(jsonldLocation);
  },
};
