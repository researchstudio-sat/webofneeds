/**
 * Created by fsuda on 18.09.2018.
 */
import Immutable from "immutable";
import { details, abstractDetails } from "../detail-definitions.js";

import {
  is,
  isValidNumber,
  getFromJsonLd,
  getInFromJsonLd,
} from "../../app/utils.js";
import won from "../../app/won-es6.js";

function minMaxLabel(min, max) {
  const min_ = Number.parseFloat(min);
  const max_ = Number.parseFloat(max);
  const minIsNumber = isValidNumber(min_);
  const maxIsNumber = isValidNumber(max_);
  if (minIsNumber && maxIsNumber) {
    return min_ + "–" + max_;
  } else if (minIsNumber) {
    return "At least " + min_;
  } else if (maxIsNumber) {
    return "At most " + max_;
  } else {
    return "Unspecified number of ";
  }
}

export const realEstateFloorSizeDetail = {
  ...abstractDetails.number,
  identifier: "floorSize",
  label: "Floor size in square meters",
  icon: "#ico36_detail_floorsize",
  messageEnabled: false,
  parseToRDF: function({ value }) {
    if (!isValidNumber(value)) {
      return { "s:floorSize": undefined };
    } else {
      return {
        "s:floorSize": {
          "@type": "s:QuantitativeValue",
          "s:value": [{ "@value": value, "@type": "xsd:float" }],
          "s:unitCode": "MTK",
        },
      };
    }
  },
  parseFromRDF: function(jsonLDImm) {
    const fs = won.parseFrom(
      jsonLDImm,
      ["s:floorSize", "s:value"],
      "xsd:float"
    );
    const unit = getInFromJsonLd(
      jsonLDImm,
      ["s:floorSize", "s:unitCode"],
      won.defaultContext
    );
    if (!fs) {
      return undefined;
    } else {
      if (unit === "MTK") {
        return fs + "m²";
      } else if (unit === "FTK") {
        return fs + "sq ft";
      } else if (unit === "YDK") {
        return fs + "sq yd";
      } else if (!unit) {
        return fs + " (no unit specified)";
      }
      return fs + " " + unit;
    }
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (isValidNumber(value)) {
      return (includeLabel ? this.label + ": " + value : value) + "m²";
    }
    return undefined;
  },
};

export const realEstateNumberOfRoomsDetail = {
  ...abstractDetails.number,
  identifier: "numberOfRooms",
  label: "Number of Rooms",
  icon: "#ico36_detail_number-of-rooms",
  messageEnabled: false,
  parseToRDF: function({ value }) {
    if (!isValidNumber(value)) {
      return { "s:numberOfRooms": undefined };
    } else {
      return { "s:numberOfRooms": [{ "@value": value, "@type": "xsd:float" }] };
    }
  },
  parseFromRDF: function(jsonLDImm) {
    return won.parseFrom(jsonLDImm, ["s:numberOfRooms"], "xsd:float");
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (isValidNumber(value)) {
      return (includeLabel ? this.label + ": " + value : value) + " Rooms";
    } else {
      return undefined;
    }
  },
};

export const realEstateNumberOfRoomsRangeDetail = {
  ...abstractDetails.range,
  identifier: "numberOfRoomsRange",
  label: "Number of Rooms",
  minLabel: "From",
  maxLabel: "To",
  messageEnabled: false,
  icon: "#ico36_detail_number-of-rooms",
  parseToRDF: function({ value }) {
    if (!value) {
      return {};
    }
    return {
      "sh:property": {
        "sh:path": { "@id": "s:numberOfRooms" },
        "sh:minInclusive": value.min && [
          { "@value": value.min, "@type": "xsd:float" },
        ],
        "sh:maxInclusive": value.max && [
          { "@value": value.max, "@type": "xsd:float" },
        ],
      },
    };
  },
  parseFromRDF: function(jsonLDImm) {
    let properties = getFromJsonLd(
      jsonLDImm,
      "sh:property",
      won.defaultContext
    );
    if (!properties) return undefined;

    if (!Immutable.List.isList(properties))
      properties = Immutable.List.of(properties);

    const numberOfRooms = properties.find(
      property =>
        getInFromJsonLd(property, ["sh:path", "@id"], won.defaultContext) ===
        "s:numberOfRooms"
    );
    const minNumberOfRooms = getFromJsonLd(
      numberOfRooms,
      "sh:minInclusive",
      won.defaultContext
    );
    const maxNumberOfRooms = getFromJsonLd(
      numberOfRooms,
      "sh:maxInclusive",
      won.defaultContext
    );

    if (minNumberOfRooms || maxNumberOfRooms) {
      return Immutable.fromJS({
        min: minNumberOfRooms,
        max: maxNumberOfRooms,
      });
    } else {
      return undefined;
    }
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      return (
        (includeLabel ? `${this.label}: ` : "") +
        minMaxLabel(value.min, value.max) +
        " Room(s)"
      );
    }
    return undefined;
  },
};

export const realEstateFloorSizeRangeDetail = {
  ...abstractDetails.range,
  identifier: "floorSizeRange",
  label: "Floor size in square meters",
  minLabel: "From",
  maxLabel: "To",
  icon: "#ico36_detail_floorsize",
  messageEnabled: false,
  parseToRDF: function({ value }) {
    if (!value) {
      return {};
    }
    return {
      "sh:property": {
        "sh:path": { "@id": "s:floorSize" },
        "sh:minInclusive": value.min && [
          { "@value": value.min, "@type": "xsd:float" },
        ],
        "sh:maxInclusive": value.max && [
          { "@value": value.max, "@type": "xsd:float" },
        ],
      },
    };
  },
  parseFromRDF: function(jsonLDImm) {
    let properties = getFromJsonLd(
      jsonLDImm,
      "sh:property",
      won.defaultContext
    );
    if (!properties) return undefined;

    if (!Immutable.List.isList(properties))
      properties = Immutable.List.of(properties);

    const floorSize = properties.find(
      property =>
        getInFromJsonLd(property, ["sh:path", "@id"], won.defaultContext) ===
        "s:floorSize"
    );

    const minFloorSize = getFromJsonLd(
      floorSize,
      "sh:minInclusive",
      won.defaultContext
    );
    const maxFloorSize = getFromJsonLd(
      floorSize,
      "sh:maxInclusive",
      won.defaultContext
    );

    if (minFloorSize || maxFloorSize) {
      return Immutable.fromJS({
        min: minFloorSize && minFloorSize + "m²",
        max: maxFloorSize && maxFloorSize + "m²",
      });
    } else {
      return undefined;
    }
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      return (
        (includeLabel ? `${this.label}: ` : "") +
        minMaxLabel(value.min, value.max) +
        "m²"
      );
    }
    return undefined;
  },
};

export const realEstateFeaturesDetail = {
  ...details.tags,
  identifier: "features",
  label: "Features",
  icon: "#ico36_detail_feature",
  placeholder: "e.g. balcony, bathtub",
  messageEnabled: false,
  parseToRDF: function({ value }) {
    if (!value || !is("Array", value) || value.length === 0) {
      return { "s:amenityFeature": undefined };
    } else {
      const features = value.map(feature => ({
        "@type": "s:LocationFeatureSpecification",
        "s:value": { "@value": feature, "@type": "s:Text" },
      }));
      return {
        "s:amenityFeature": features,
      };
    }
  },
  parseFromRDF: function(jsonLDImm) {
    return won.parseListFrom(
      jsonLDImm,
      ["s:amenityFeature"], //, "s:value"],
      "s:Text"
    );
  },
};

export const realEstateRentDetail = {
  ...details.price,
  identifier: "rent",
  label: "Rent",
  icon: "#ico36_detail_rent",
  currency: [{ value: "EUR", label: "€", default: true }],
  unitCode: [{ value: "MON", label: "per month", default: true }],
  messageEnabled: false,
  parseFromRDF: function() {
    //That way we can make sure that parsing fromRDF is made only by the price detail itself
    return undefined;
  },
};

export const realEstateRentRangeDetail = {
  ...details.pricerange,
  identifier: "rentRange",
  label: "Rent in EUR/month",
  minLabel: "From",
  maxLabel: "To",
  currency: [{ value: "EUR", label: "€", default: true }],
  unitCode: [{ value: "MON", label: "per month", default: true }],
  icon: "#ico36_detail_rent",
  messageEnabled: false,
  parseFromRDF: function() {
    return undefined;
  },
};
