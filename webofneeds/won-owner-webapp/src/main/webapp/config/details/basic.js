import won from "../../app/won-es6.js";
import { is } from "../../app/utils.js";

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

export const searchString = {
  identifier: "searchString",
  label: "Searching for",
  icon: "#ico36_search",
  placeholder: "What do you look for?",
  component: "won-title-picker",
  viewerComponent: "won-title-viewer",
  parseToRDF: function({ value }) {
    const val = value ? value : undefined;
    return {
      "won:hasSearchString": val,
    };
  },
  parseFromRDF: function(jsonLDImm) {
    return won.parseFrom(jsonLDImm, ["won:hasSearchString"], "xsd:string");
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
    const val = value ? value : undefined;
    return {
      "dc:description": val,
      "s:description": val,
    };
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
    if (value && is("Array", value) && value.length > 0) {
      const prefix = includeLabel ? this.label + ": " : "";
      return prefix + value.join(", ");
    } else {
      return undefined;
    }
  },
};

export const suggestPost = {
  identifier: "suggestPost",
  label: "Suggest Post",
  icon: "#ico36_detail_title", //TODO: CORRECT ICON
  placeholder: "Insert PostUri and Accept",
  component: "won-suggestpost-picker",
  viewerComponent: "won-suggestpost-viewer",
  parseToRDF: function({ value }) {
    const val = value ? value : undefined;

    if (val) {
      return {
        "won:suggestPostUri": { "@id": val },
      };
    } else {
      return { "won:suggestPostUri": undefined };
    }
  },
  parseFromRDF: function(jsonLDImm) {
    const suggestUriJsonLDImm =
      jsonLDImm && jsonLDImm.get("won:suggestPostUri");
    return suggestUriJsonLDImm && suggestUriJsonLDImm.get("@id");
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      return includeLabel ? this.label + ": " + value : value;
    }
    return undefined;
  },
};

export const responseToUri = {
  identifier: "responseToUri",
  label: "Response To Post",
  icon: "#ico36_detail_title", //TODO: CORRECT ICON
  placeholder: "Insert PostUri and Accept",
  component: "won-suggestpost-picker",
  viewerComponent: "won-suggestpost-viewer",
  messageEnabled: false,
  parseToRDF: function({ value }) {
    const val = value ? value : undefined;

    if (val) {
      return {
        "won:responseToUri": { "@id": val },
      };
    } else {
      return { "won:responseToUri": undefined };
    }
  },
  parseFromRDF: function(jsonLDImm) {
    const responseToUriJsonLDImm =
      jsonLDImm && jsonLDImm.get("won:responseToUri");
    return responseToUriJsonLDImm && responseToUriJsonLDImm.get("@id");
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      return includeLabel ? this.label + ": " + value : value;
    }
    return undefined;
  },
};
