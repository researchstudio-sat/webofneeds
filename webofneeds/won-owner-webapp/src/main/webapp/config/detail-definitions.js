import personDetails from "./details/person.js";
import locationDetails from "./details/location.js";
import timeDetails from "./details/datetime.js";
import fileDetails from "./details/files.js";
import priceDetails from "./details/price.js";
import won from "../app/won-es6.js";

export const emptyDraft = {
  is: {},
  seeks: {},
  matchingContext: undefined,
};

/**
 * Defines a set of details that will only be visible within a specific 'implementation'
 * you will need to alter the identifier, label, icon, parseToRDF, and parseFromRDF if
 * you want to use it.
 */
export const abstractDetails = {
  range: {
    identifier: function() {
      throw "abstract Detail does not override necessary identifier";
    },
    label: function() {
      throw "abstract Detail does not override necessary label";
    },
    minLabel: function() {
      throw "abstract Detail does not override necessary minLabel";
    },
    maxLabel: function() {
      throw "abstract Detail does not override necessary maxLabel";
    },
    minPlaceholder: undefined,
    maxPlaceholder: undefined,
    icon: undefined,
    component: "won-range-picker",
    viewerComponent: "won-range-viewer",
    parseToRDF: function() {
      throw "abstract Detail does not override necessary function";
    },
    parseFromRDF: function() {
      throw "abstract Detail does not override necessary function";
    },
    generateHumanReadable: function() {
      throw "abstract Detail does not override necessary function";
    },
  },
  number: {
    identifier: function() {
      throw "abstract Detail does not override necessary identifier";
    },
    label: function() {
      throw "abstract Detail does not override necessary label";
    },
    icon: undefined,
    component: "won-number-picker",
    viewerComponent: "won-number-viewer",
    parseToRDF: function() {
      throw "abstract Detail does not override necessary function";
    },
    parseFromRDF: function() {
      throw "abstract Detail does not override necessary function";
    },
    generateHumanReadable: function() {
      throw "abstract Detail does not override necessary function";
    },
  },
  select: {
    identifier: function() {
      throw "abstract Detail does not override necessary identifier";
    },
    label: function() {
      throw "abstract Detail does not override necessary label";
    },
    icon: undefined,
    component: "won-select-picker",
    viewerComponent: "won-select-viewer",
    multiSelect: false,
    options: function() {
      throw 'abstract Detail does not override necessary options array(structure: [{value: val, label: "labeltext"}...]';
      /**
       * e.g. number of rooms ....
       [
        {value: "1", label: "one"},
        {value: "2", label: "two"},
        {value: "3", label: "three"},
        {value: "4", label: "four"},
        {value: "5+", label: "more"},
       ]
       */
    },
    parseToRDF: function() {
      throw "abstract Detail does not override necessary function";
    },
    parseFromRDF: function() {
      throw "abstract Detail does not override necessary function";
    },
    generateHumanReadable: function() {
      throw "abstract Detail does not override necessary function";
    },
  },
  dropdown: {
    identifier: function() {
      throw "abstract Detail does not override necessary identifier";
    },
    label: function() {
      throw "abstract Detail does not override necessary label";
    },
    icon: undefined,
    component: "won-dropdown-picker",
    viewerComponent: "won-dropdown-viewer",
    options: function() {
      throw 'abstract Detail does not override necessary options array(structure: [{value: val, label: "labeltext"}...]';
      /**
       * e.g. relationship status....
        [
         {value: "single", label: "single"},
         {value: "married", label: "married"},
         {value: "complicated", label: "it's complicated"},
         {value: "divorced", label: "divorced"},
         {value: "free", label: "free for all"},
        ]
       */
    },
    parseToRDF: function() {
      throw "abstract Detail does not override necessary function";
    },
    parseFromRDF: function() {
      throw "abstract Detail does not override necessary function";
    },
    generateHumanReadable: function() {
      throw "abstract Detail does not override necessary function";
    },
  },
};

const basicDetails = {
  title: {
    identifier: "title",
    label: "Title",
    icon: "#ico36_detail_title",
    placeholder: "What? (Short title shown in lists)",
    component: "won-title-picker",
    viewerComponent: "won-title-viewer",
    parseToRDF: function({ value }) {
      if (!value) {
        return { "dc:title": undefined };
      }
      return { "dc:title": value };
    },
    parseFromRDF: function(jsonLDImm) {
      const dcTitle = won.parseFrom(jsonLDImm, ["dc:title"], "xsd:string");
      return dcTitle || won.parseFrom(jsonLDImm, ["s:title"], "xsd:string");
    },
    generateHumanReadable: function({ value, includeLabel }) {
      if (value) {
        return includeLabel ? this.label + ": " + value : value;
      }
      return undefined;
    },
  },
  description: {
    identifier: "description",
    label: "Description",
    icon: "#ico36_detail_description",
    placeholder: "Enter Description...",
    component: "won-description-picker",
    viewerComponent: "won-description-viewer",
    parseToRDF: function({ value }) {
      if (!value) {
        return { "dc:description": undefined };
      }
      return { "dc:description": value };
    },
    parseFromRDF: function(jsonLDImm) {
      const dcDescription = won.parseFrom(
        jsonLDImm,
        ["dc:description"],
        "xsd:string"
      );
      return (
        dcDescription ||
        won.parseFrom(jsonLDImm, ["s:description"], "xsd:string")
      );
    },
    generateHumanReadable: function({ value, includeLabel }) {
      if (value) {
        return includeLabel ? this.label + ": " + value : value;
      }
      return undefined;
    },
  },
  tags: {
    identifier: "tags",
    label: "Tags",
    icon: "#ico36_detail_tags",
    placeholder: "e.g. couch, free",
    component: "won-tags-picker",
    viewerComponent: "won-tags-viewer",
    messageEnabled: true,
    parseToRDF: function({ value }) {
      if (!value) {
        return { "won:hasTag": undefined };
      }
      return { "won:hasTag": value };
    },
    parseFromRDF: function(jsonLDImm) {
      return won.parseListFrom(jsonLDImm, ["won:hasTag"], "xsd:string");
    },
    generateHumanReadable: function({ value, includeLabel }) {
      if (value) {
        let humanReadable = "";

        for (const key in value) {
          humanReadable += value[key] + ", ";
        }
        humanReadable = humanReadable.trim();

        if (humanReadable.length > 0) {
          humanReadable = humanReadable.substr(0, humanReadable.length - 1);
          return includeLabel
            ? this.label + ": " + humanReadable
            : humanReadable;
        }
      }
      return undefined;
    },
  },
};

export const details = {
  title: basicDetails.title,
  description: basicDetails.description,
  tags: basicDetails.tags,

  fromDatetime: timeDetails.fromDatetime,
  throughDatetime: timeDetails.throughDatetime,
  datetimeRange: timeDetails.datetimeRange,

  location: locationDetails.location,
  travelAction: locationDetails.travelAction,

  person: personDetails.person,

  files: fileDetails.files,
  images: fileDetails.images,
  bpmnWorkflow: fileDetails.bpmnWorkflow,
  petrinetWorkflow: fileDetails.petrinetWorkflow,

  pricerange: priceDetails.pricerange,
  price: priceDetails.price,
};
