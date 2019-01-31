import won from "../../app/won-es6.js";
import { is, get } from "../../app/utils.js";
import { select } from "../details/abstract.js";
import Immutable from "immutable";

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

export const personaName = {
  identifier: "personaName",
  label: "Name",
  icon: "#ico36_detail_title",
  placeholder: "Your Name",
  component: "won-title-picker",
  viewerComponent: "won-title-viewer",
  parseToRDF: function({ value }) {
    const val = value ? value : undefined;
    return {
      "s:name": val,
    };
  },
  parseFromRDF: function(jsonLDImm) {
    return won.parseFrom(jsonLDImm, ["s:name"], "xsd:string");
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      return includeLabel ? this.label + ": " + value : value;
    }
    return undefined;
  },
};

export const website = {
  //FIXME: Implement URL Picker once the persona-creator also generates url type urls, instead of strings
  identifier: "website",
  label: "Website",
  icon: "#ico36_detail_title",
  placeholder: "Website",
  component: "won-title-picker",
  viewerComponent: "won-title-viewer",
  parseToRDF: function({ value }) {
    const val = value ? value : undefined;
    return {
      "s:url": val,
    };
  },
  parseFromRDF: function(jsonLDImm) {
    return won.parseFrom(jsonLDImm, ["s:url"], "xsd:string");
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

export const flags = {
  ...select,
  identifier: "flags",
  label: "Flags",
  icon: "#ico36_detail_title", //TODO: CORRECT ICON
  viewerComponent: undefined, //this is so we do not display this with a detail-viewer,
  component: undefined, //this is so we do not display the component as a detail-picker, but are still able to use the parseToRDF, parseFromRDF functions
  multiSelect: true,
  options: [
    { value: "won:WhatsNew", label: "WhatsNew" },
    { value: "won:WhatsAround", label: "WhatsAround" },
    { value: "won:NoHintForMe", label: "NoHintForMe" },
    { value: "won:NoHintForCounterpart", label: "NoHintForCounterpart" },
    { value: "won:DirectResponse", label: "DirectResponse" },
    { value: "won:UsedForTesting", label: "UsedForTesting" },
  ],
  parseToRDF: function({ value }) {
    if (!value) {
      return { "won:hasFlag": undefined };
    } else if (is("Array", value)) {
      const idFlags = value.map(item => {
        return { "@id": item };
      });
      return { "won:hasFlag": idFlags };
    } else {
      return { "won:hasFlag": [{ "@id": value }] };
    }
  },
  parseFromRDF: function(jsonLDImm) {
    return won.parseListFrom(jsonLDImm, ["won:hasFlag"], "xsd:ID");
  },
  generateHumanReadable: function({ value, includeLabel }) {
    //TODO: Implement this so that the label shows instead of the value
    if (value && is("Array", value) && value.length > 0) {
      const prefix = includeLabel ? this.label + ": " : "";
      return prefix + value.join(", ");
    } else {
      return undefined;
    }
  },
};

export const type = {
  ...select,
  identifier: "type",
  label: "Types",
  icon: "#ico36_detail_title", //TODO: CORRECT ICON
  viewerComponent: undefined, //this is so we do not display this with a detail-viewer,
  component: undefined, //this is so we do not display the component as a detail-picker, but are still able to use the parseToRDF, parseFromRDF functions
  multiSelect: true,
  options: [
    //TODO: Implement options
  ],
  parseToRDF: function({ value }) {
    if (!value) {
      return;
    } else if (is("Array", value)) {
      const idFlags = value.map(item => {
        return item;
      });
      return { "@type": idFlags };
    } else {
      return { "@type": value };
    }
  },
  parseFromRDF: function(jsonLDImm) {
    return won.parseListFrom(jsonLDImm, ["@type"]);
  },
  generateHumanReadable: function({ value, includeLabel }) {
    //TODO: Implement this so that the label shows instead of the value
    if (value && is("Array", value) && value.length > 0) {
      const prefix = includeLabel ? this.label + ": " : "";
      return prefix + value.join(", ");
    } else {
      return undefined;
    }
  },
};

export const facets = {
  ...select,
  identifier: "facets",
  label: "Facets",
  icon: "#ico36_detail_title", //TODO: CORRECT ICON
  viewerComponent: undefined,
  component: undefined,
  multiSelect: true,
  options: [
    {
      value: { "#chatFacet": "won:ChatFacet" },
      label: "ChatFacet",
    },
    {
      value: { "#groupFacet": "won:GroupFacet" },
      label: "GroupFacet",
    },
    {
      value: { "#holderFacet": "won:HolderFacet" },
      label: "HolderFacet",
    },
    {
      value: { "#holdableFacet": "won:HoldableFacet" },
      label: "HoldableFacet",
    },
    {
      value: { "#reviewFacet": "won:ReviewFacet" },
      label: "ReviewFacet",
    },
  ],
  parseToRDF: function({ value }) {
    //TODO: PARSE TO RDF ONLY WHEN VALUE IS CONTAINING ONLY POSSIBLE ONES
    if (value) {
      let facets = [];
      Immutable.fromJS(value).map((facet, key) => {
        facets.push({ "@id": key, "@type": facet });
      });

      if (facets.length > 0) {
        return {
          "won:hasFacet": facets,
        };
      }
    }

    return undefined;
  },
  parseFromRDF: function(jsonLDImm) {
    const wonHasFacets = get(jsonLDImm, "won:hasFacet");
    let facets = Immutable.Map();

    if (wonHasFacets) {
      if (Immutable.List.isList(wonHasFacets)) {
        wonHasFacets.map(facet => {
          facets = facets.set(get(facet, "@id"), get(facet, "@type"));
        });
        if (facets.size > 0) {
          return facets;
        }
      } else {
        return facets.set(get(wonHasFacets, "@id"), get(wonHasFacets, "@type"));
      }
    }
    return undefined;
  },
  generateHumanReadable: function({ value, includeLabel }) {
    //TODO: Implement this so that the label shows instead of the value
    if (value && is("Array", value) && value.length > 0) {
      const prefix = includeLabel ? this.label + ": " : "";
      return prefix + value.join(", ");
    } else {
      return undefined;
    }
  },
};

export const defaultFacet = {
  ...facets,
  identifier: "defaultFacet",
  label: "Default Facet",
  icon: "#ico36_detail_title", //TODO: CORRECT ICON
  viewerComponent: undefined,
  component: undefined,
  multiSelect: false,
  parseToRDF: function({ value }) {
    //TODO: PARSE TO RDF ONLY WHEN VALUE IS ONE OF THE POSSIBLE ONES
    if (value) {
      let facets = [];
      Immutable.fromJS(value).map((facet, key) => {
        facets.push({ "@id": key, "@type": facet });
      });

      if (facets.length == 1) {
        return {
          "won:hasDefaultFacet": facets,
        };
      }
    }

    return undefined;
  },
  parseFromRDF: function(jsonLDImm) {
    const wonHasDefaultFacet = get(jsonLDImm, "won:hasDefaultFacet");

    if (wonHasDefaultFacet && !Immutable.List.isList(wonHasDefaultFacet)) {
      return wonHasDefaultFacet;
    }
    return undefined;
  },
  generateHumanReadable: function({ value, includeLabel }) {
    //TODO: Implement this so that the label shows instead of the value
    if (value) {
      return includeLabel ? this.label + ": " + value : value;
    }
  },
};

export const sPlanAction = {
  identifier: "sPlanAction",
  label: "Plan",
  icon: "#ico36_detail_title",
  placeholder: "What? (Short title shown in lists)",
  parseToRDF: function({ value }) {
    const val = value ? value : undefined;
    if (!val) {
      return;
    }
    return {
      "@type": "s:PlanAction",
      "s:object": {
        "@type": "s:Event",
        "s:about": value,
      },
    };
  },
  parseFromRDF: function(jsonLDImm) {
    const types = jsonLDImm.getIn(["@type"]);
    if (
      (Immutable.List.isList(types) && types.includes("s:PlanAction")) ||
      types === "s:PlanAction"
    ) {
      const planObjs = jsonLDImm.getIn(jsonLDImm, ["s:object"]);
      let plns = [];

      if (Immutable.List.isList(planObjs)) {
        planObjs &&
          planObjs.forEach(planObj => {
            plns.push(won.parseFrom(planObj, ["s:about"], "xsd:ID"));
          });
      } else {
        let pln = won.parseFrom(planObjs, ["s:about"], "xsd:ID");
        if (pln) {
          return Immutable.fromJS([pln]);
        }
      }
      if (plns.length > 0) {
        return Immutable.fromJS(plns);
      }
      return undefined;
    }
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      return includeLabel ? this.label + ": " + value : value;
    }
    return undefined;
  },
};
