import won from "../../app/won-es6.js";
import { is, get, getIn } from "../../app/utils.js";
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
      return { "won:tag": undefined };
    }
    return { "won:tag": value };
  },
  parseFromRDF: function(jsonLDImm) {
    return won.parseListFrom(jsonLDImm, ["won:tag"], "xsd:string");
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
    return getIn(jsonLDImm, ["won:suggestPostUri", "@id"]);
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
    return getIn(jsonLDImm, ["won:responseToUri", "@id"]);
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
    { value: "won:NoHintForMe", label: "Silent" },
    { value: "won:NoHintForCounterpart", label: "Invisible" },
    { value: "won:DirectResponse", label: "DirectResponse" },
    { value: "won:UsedForTesting", label: "UsedForTesting" },
  ],
  parseToRDF: function({ value }) {
    if (!value) {
      return { "won:flag": undefined };
    } else if (is("Array", value)) {
      const idFlags = value.map(item => {
        return { "@id": item };
      });
      return { "won:flag": idFlags };
    } else {
      return { "won:flag": [{ "@id": value }] };
    }
  },
  parseFromRDF: function(jsonLDImm) {
    return won.parseListFrom(jsonLDImm, ["won:flag"], "xsd:ID");
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
    const types = (rawTypes => {
      if (Immutable.List.isList(rawTypes)) {
        return Immutable.Set(rawTypes);
      } else if (rawTypes) {
        return Immutable.Set([rawTypes]);
      } else {
        return undefined;
      }
    })(jsonLDImm.get("@type"));
    return types;
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

export const sockets = {
  ...select,
  identifier: "sockets",
  label: "Sockets",
  icon: "#ico36_detail_title", //TODO: CORRECT ICON
  viewerComponent: undefined,
  component: undefined,
  multiSelect: true,
  options: [
    {
      value: { "#chatSocket": "won:ChatSocket" },
      label: "ChatSocket",
    },
    {
      value: { "#groupSocket": "won:GroupSocket" },
      label: "GroupSocket",
    },
    {
      value: { "#holderSocket": "hold:HolderSocket" },
      label: "HolderSocket",
    },
    {
      value: { "#holdableSocket": "hold:HoldableSocket" },
      label: "HoldableSocket",
    },
    {
      value: { "#reviewSocket": "won:ReviewSocket" },
      label: "ReviewSocket",
    },
  ],
  parseToRDF: function({ value }) {
    //TODO: PARSE TO RDF ONLY WHEN VALUE IS CONTAINING ONLY POSSIBLE ONES
    if (value) {
      let sockets = [];
      Immutable.fromJS(value).map((socket, key) => {
        sockets.push({ "@id": key, "won:socketDefinition": socket });
      });

      if (sockets.length > 0) {
        return {
          "won:socket": sockets,
        };
      }
    }

    return undefined;
  },
  parseFromRDF: function(jsonLDImm) {
    const wonHasSockets = get(jsonLDImm, "won:socket");
    let sockets = Immutable.Map();

    if (wonHasSockets) {
      if (Immutable.List.isList(wonHasSockets)) {
        wonHasSockets.map(socket => {
          sockets = sockets.set(get(socket, "@id"), get(socket, "won:socketDefinition"));
        });
        if (sockets.size > 0) {
          return sockets;
        }
      } else {
        return sockets.set(
          get(wonHasSockets, "@id"),
          get(wonHasSockets, "won:socketDefinition")
        );
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

export const defaultSocket = {
  ...sockets,
  identifier: "defaultSocket",
  label: "Default Socket",
  icon: "#ico36_detail_title", //TODO: CORRECT ICON
  viewerComponent: undefined,
  component: undefined,
  multiSelect: false,
  parseToRDF: function({ value }) {
    //TODO: PARSE TO RDF ONLY WHEN VALUE IS ONE OF THE POSSIBLE ONES
    if (value) {
      let sockets = [];
      Immutable.fromJS(value).map((socket, key) => {
        sockets.push({ "@id": key, "won:socketDefinition": socket });
      });

      if (sockets.length == 1) {
        return {
          "won:defaultSocket": sockets,
        };
      }
    }

    return undefined;
  },
  parseFromRDF: function(jsonLDImm) {
    const wonHasDefaultSocket = get(jsonLDImm, "won:defaultSocket");
    let defaultSocket = Immutable.Map();

    if (wonHasDefaultSocket && !Immutable.List.isList(wonHasDefaultSocket)) {
      const defaultSocketId = get(wonHasDefaultSocket, "@id");

      const wonHasSockets = get(jsonLDImm, "won:socket");

      if (wonHasSockets) {
        if (Immutable.List.isList(wonHasSockets)) {
          const foundDefaultSocket = wonHasSockets.find(
            socket => get(socket, "@id") === defaultSocketId
          );
          return defaultSocket.set(
            defaultSocketId,
            get(foundDefaultSocket, "won:socketDefinition")
          );
        } else if (get(wonHasSockets, "@id") === defaultSocketId) {
          return defaultSocket.set(
            defaultSocketId,
            get(wonHasSockets, "won:socketDefinition")
          );
        }
      }
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

/*
  Use this detail with the s:PlanAction type -> if you use this detail make sure you add the s:PlanAction type to the corresponding branch (content or seeks)
*/
export const eventObject = {
  identifier: "eventObject",
  label: "Event",
  icon: "#ico36_detail_title",
  placeholder: "What? (Short title shown in lists)",
  parseToRDF: function({ value }) {
    if (!value) {
      return;
    } else if (is("Array", value)) {
      const eventObjects = value.map(item => {
        return {
          "@type": "s:Event",
          "s:about": { "@id": item },
        };
      });
      return { "s:object": eventObjects };
    } else {
      return {
        "s:object": {
          "@type": "s:Event",
          "s:about": { "@id": value },
        },
      };
    }
  },
  parseFromRDF: function(jsonLDImm) {
    const types = get(jsonLDImm, "@type");
    if (
      (Immutable.List.isList(types) && types.includes("s:PlanAction")) ||
      types === "s:PlanAction"
    ) {
      const planObjs = get(jsonLDImm, "s:object");
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
