import { get, getIn, is } from "../../app/utils.js";
import { select } from "../details/abstract.js";
import Immutable from "immutable";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";

import WonTitleViewer from "../../app/components/details/viewer/title-viewer.jsx";
import WonDescriptionViewer from "../../app/components/details/viewer/description-viewer.jsx";
import WonTagsViewer from "../../app/components/details/viewer/tags-viewer.jsx";
import WonSuggestAtomViewer from "../../app/components/details/viewer/suggest-atom-viewer.jsx";

import WonTitlePicker from "../../app/components/details/picker/title-picker.jsx";
import WonDescriptionPicker from "../../app/components/details/picker/description-picker.jsx";
import WonTagsPicker from "../../app/components/details/picker/tags-picker.jsx";
import WonSuggestAtomPicker from "../../app/components/details/picker/suggest-atom-picker.jsx";

export const title = {
  identifier: "title",
  label: "Title",
  icon: "#ico36_detail_title",
  placeholder: "What? (Short title shown in lists)",
  component: WonTitlePicker,
  viewerComponent: WonTitleViewer,
  parseToRDF: function({ value }) {
    const val = value ? value : undefined;
    return {
      "dc:title": val,
      "s:title": val,
    };
  },
  parseFromRDF: function(jsonLDImm) {
    const dcTitle = jsonLdUtils.parseFrom(
      jsonLDImm,
      ["dc:title"],
      "xsd:string"
    );
    return (
      dcTitle || jsonLdUtils.parseFrom(jsonLDImm, ["s:title"], "xsd:string")
    );
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
  component: WonTitlePicker,
  viewerComponent: WonTitleViewer,
  parseToRDF: function({ value }) {
    const val = value ? value : undefined;
    return {
      "s:name": val,
    };
  },
  parseFromRDF: function(jsonLDImm) {
    return jsonLdUtils.parseFrom(jsonLDImm, ["s:name"], "xsd:string");
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
  component: WonTitlePicker,
  viewerComponent: WonTitleViewer,
  parseToRDF: function({ value }) {
    const val = value ? value : undefined;
    return {
      "s:url": val,
    };
  },
  parseFromRDF: function(jsonLDImm) {
    return jsonLdUtils.parseFrom(jsonLDImm, ["s:url"], "xsd:string");
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
  component: WonTitlePicker,
  viewerComponent: WonTitleViewer,
  parseToRDF: function({ value }) {
    const val = value ? value : undefined;
    return {
      "match:searchString": val,
    };
  },
  parseFromRDF: function(jsonLDImm) {
    return jsonLdUtils.parseFrom(
      jsonLDImm,
      ["match:searchString"],
      "xsd:string"
    );
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
  component: WonDescriptionPicker,
  viewerComponent: WonDescriptionViewer,
  parseToRDF: function({ value }) {
    const val = value ? value : undefined;
    return {
      "dc:description": val,
      "s:description": val,
    };
  },
  parseFromRDF: function(jsonLDImm) {
    const dcDescription = jsonLdUtils.parseFrom(
      jsonLDImm,
      ["dc:description"],
      "xsd:string"
    );
    return (
      dcDescription ||
      jsonLdUtils.parseFrom(jsonLDImm, ["s:description"], "xsd:string")
    );
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      return includeLabel ? this.label + ": " + value : value;
    }
    return undefined;
  },
};

export const termsOfService = {
  identifier: "termsOfService",
  label: "Terms Of Service",
  icon: "#ico36_detail_description",
  placeholder: "Enter Description...",
  component: WonDescriptionPicker,
  viewerComponent: WonDescriptionViewer,
  parseToRDF: function({ value }) {
    const val = value ? value : undefined;
    return {
      "s:termsOfService": val,
    };
  },
  parseFromRDF: function(jsonLDImm) {
    return jsonLdUtils.parseFrom(jsonLDImm, ["s:termsOfService"], "xsd:string");
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
  component: WonTagsPicker,
  viewerComponent: WonTagsViewer,
  messageEnabled: true,
  parseToRDF: function({ value }) {
    if (!value) {
      return { "con:tag": undefined };
    }
    return { "con:tag": value };
  },
  parseFromRDF: function(jsonLDImm) {
    return jsonLdUtils.parseListFrom(jsonLDImm, ["con:tag"], "xsd:string");
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
  component: WonSuggestAtomPicker,
  viewerComponent: WonSuggestAtomViewer,
  parseToRDF: function({ value }) {
    const val = value ? value : undefined;

    if (val) {
      return {
        "con:suggestedAtom": { "@id": val },
      };
    } else {
      return { "con:suggestedAtom": undefined };
    }
  },
  parseFromRDF: function(jsonLDImm) {
    return getIn(jsonLDImm, ["con:suggestedAtom", "@id"]);
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
  component: WonSuggestAtomPicker,
  viewerComponent: WonSuggestAtomViewer,
  messageEnabled: false,
  parseToRDF: function({ value }) {
    const val = value ? value : undefined;

    if (val) {
      return {
        "con:inResponseTo": { "@id": val },
      };
    } else {
      return { "con:inResponseTo": undefined };
    }
  },
  parseFromRDF: function(jsonLDImm) {
    return getIn(jsonLDImm, ["con:inResponseTo", "@id"]);
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
    { value: "match:NoHintForMe", label: "Silent" },
    { value: "match:NoHintForCounterpart", label: "Invisible" },
    { value: "con:DirectResponse", label: "DirectResponse" },
    { value: "match:UsedForTesting", label: "UsedForTesting" },
  ],
  parseToRDF: function({ value }) {
    if (!value) {
      return { "match:flag": undefined };
    } else if (is("Array", value)) {
      const idFlags = value.map(item => {
        return { "@id": item };
      });
      return { "match:flag": idFlags };
    } else {
      return { "match:flag": [{ "@id": value }] };
    }
  },
  parseFromRDF: function(jsonLDImm) {
    return jsonLdUtils.parseListFrom(jsonLDImm, ["match:flag"], "xsd:ID");
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
  viewerComponent: undefined, //this is so we do not display this with a detail-viewer,
  component: undefined,
  multiSelect: true,
  options: [
    {
      value: { "#chatSocket": "chat:ChatSocket" },
      label: "ChatSocket",
    },
    {
      value: { "#groupSocket": "group:GroupSocket" },
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
      value: { "#reviewSocket": "review:ReviewSocket" },
      label: "ReviewSocket",
    },
  ],
  parseToRDF: function({ value }) {
    //TODO: PARSE TO RDF ONLY WHEN VALUE IS CONTAINING ONLY POSSIBLE ONES
    if (value) {
      let sockets = [];
      Immutable.fromJS(value).map((socket, key) => {
        sockets.push({ "@id": key, "won:socketDefinition": { "@id": socket } });
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
          sockets = sockets.set(
            get(socket, "@id"),
            getIn(socket, ["won:socketDefinition", "@id"])
          );
        });
        if (sockets.size > 0) {
          return sockets;
        }
      } else {
        return sockets.set(
          get(wonHasSockets, "@id"),
          getIn(wonHasSockets, ["won:socketDefinition", "@id"])
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
  viewerComponent: undefined, //this is so we do not display this with a detail-viewer,
  component: undefined,
  multiSelect: false,
  parseToRDF: function({ value }) {
    //TODO: PARSE TO RDF ONLY WHEN VALUE IS ONE OF THE POSSIBLE ONES
    if (value) {
      let sockets = [];
      Immutable.fromJS(value).map((socket, key) => {
        sockets.push({ "@id": key, "won:socketDefinition": { "@id": socket } });
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
            getIn(foundDefaultSocket, ["won:socketDefinition", "@id"])
          );
        } else if (get(wonHasSockets, "@id") === defaultSocketId) {
          return defaultSocket.set(
            defaultSocketId,
            getIn(wonHasSockets, ["won:socketDefinition", "@id"])
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
export const eventObjectAboutUris = {
  identifier: "eventObjectAboutUris",
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
            plns.push(jsonLdUtils.parseFrom(planObj, ["s:about"], "xsd:ID"));
          });
      } else {
        let pln = jsonLdUtils.parseFrom(planObjs, ["s:about"], "xsd:ID");
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
