import won from "../../app/won-es6.js";

export const title = {
  identifier: "title",
  label: "Title",
  icon: "#ico36_detail_title",
  placeholder: "What? (Short title shown in lists)",
  component: "won-title-picker",
  viewerComponent: "won-title-viewer",
  parseToRDF: function({ value }) {
    const val = value ? value : undefined;
    return {
      "dc:title": val,
      "s:title": val,
    };
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
};

export const description = {
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
      dcDescription || won.parseFrom(jsonLDImm, ["s:description"], "xsd:string")
    );
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      return includeLabel ? this.label + ": " + value : value;
    }
    return undefined;
  },
};

export const tags = {
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
        return includeLabel ? this.label + ": " + humanReadable : humanReadable;
      }
    }
    return undefined;
  },
};
