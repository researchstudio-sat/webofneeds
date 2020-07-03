import { get, getIn, is } from "../../app/utils.js";
import { select } from "../details/abstract.js";
import Immutable from "immutable";
import { schema } from "@tpluscode/rdf-ns-builders";
import { namedNode } from "@rdfjs/data-model";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";

import WonTitleViewer from "../../app/components/details/viewer/title-viewer.jsx";
import WonDescriptionViewer from "../../app/components/details/viewer/description-viewer.jsx";
import WonTagsViewer from "../../app/components/details/viewer/tags-viewer.jsx";
import WonSuggestAtomViewer from "../../app/components/details/viewer/suggest-atom-viewer.jsx";
import WonImageUrlViewer from "../../app/components/details/viewer/imageurl-viewer.jsx";

import WonTitlePicker from "../../app/components/details/picker/title-picker.jsx";
import WonDescriptionPicker from "../../app/components/details/picker/description-picker.jsx";
import WonTagsPicker from "../../app/components/details/picker/tags-picker.jsx";
import WonSuggestAtomPicker from "../../app/components/details/picker/suggest-atom-picker.jsx";
import vocab from "../../app/service/vocab";

import ico36_detail_title from "../../images/won-icons/ico36_detail_title.svg";
import ico36_search from "../../images/won-icons/ico36_search.svg";
import ico36_detail_description from "../../images/won-icons/ico36_detail_description.svg";
import ico36_detail_tags from "../../images/won-icons/ico36_detail_tags.svg";
import ico36_detail_media from "../../images/won-icons/ico36_detail_media.svg";
import WikiDataViewer from "~/app/components/details/viewer/wikidata-viewer";
import WikiDataPicker from "~/app/components/details/picker/wikidata-picker";

export const title = {
  identifier: "title",
  label: "Title",
  icon: ico36_detail_title,
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
  parseFromCF: function(cfDataset) {
    if (cfDataset) {
      //TODO: Fix for language fetching once CF PR is closed and dep is updated https://github.com/zazuko/clownface/pull/41
      const cfTitles = cfDataset.out(schema.title);
      if (cfTitles && cfTitles.values && cfTitles.values.length > 0) {
        return cfTitles.values[0];
      }
    }
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
  icon: ico36_detail_title,
  placeholder: "Your Name",
  component: WonTitlePicker,
  viewerComponent: WonTitleViewer,
  parseToRDF: function({ value }) {
    const val = value ? value : undefined;
    return {
      "s:name": val,
    };
  },
  parseFromCF: function(cfDataset) {
    if (cfDataset) {
      //TODO: Fix for language fetching once CF PR is closed and dep is updated https://github.com/zazuko/clownface/pull/41
      const cfNames = cfDataset.out(schema.name);
      if (cfNames && cfNames.values && cfNames.values.length > 0) {
        return cfNames.values[0];
      }
    }
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
  icon: ico36_detail_title,
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
  icon: ico36_search,
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

export const imageUrl = {
  identifier: "imageUrl",
  label: "Image",
  icon: ico36_detail_media,
  placeholder: "Add image url...",
  component: WonTitlePicker,
  viewerComponent: WonImageUrlViewer,
  parseFromCF: function(cfDataset) {
    if (cfDataset) {
      const cfImageUrl = cfDataset.out(
        namedNode("http://www.wikidata.org/prop/direct/P18")
      );
      return cfImageUrl && cfImageUrl.value;
    }
  },
  parseToRDF: function({ value }) {
    const val = value ? value : undefined;
    // Image
    if (value) {
      return {
        "http://www.wikidata.org/prop/direct/P18": { "@id": val },
      };
    }
  },
  parseFromRDF: function(jsonLDImm) {
    return jsonLdUtils.parseFrom(
      jsonLDImm,
      ["http://www.wikidata.org/prop/direct/P18"],
      "xsd:ID"
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
  icon: ico36_detail_description,
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
  parseFromCF: function(cfDataset) {
    if (cfDataset) {
      //TODO: Fix for language fetching once CF PR is closed and dep is updated https://github.com/zazuko/clownface/pull/41
      const cfDescription = cfDataset.out(schema.description);
      if (
        cfDescription &&
        cfDescription.values &&
        cfDescription.values.length > 0
      ) {
        return cfDescription.values[0];
      }
    }
  },
  parseFromRDF: function(jsonLDImm) {
    const description = get(jsonLDImm, "s:description");
    if (Immutable.List.isList(description)) {
      return get(
        description.filter(d => get(d, "@language") === "en").first(),
        "@value"
      );
    } else {
      return jsonLdUtils.parseFrom(jsonLDImm, ["s:description"], "xsd:string");
    }
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
  icon: ico36_detail_description,
  placeholder: "Enter Description...",
  component: WonDescriptionPicker,
  viewerComponent: WonDescriptionViewer,
  parseFromCF: function(cfDataset) {
    if (cfDataset) {
      //TODO: Fix for language fetching once CF PR is closed and dep is updated https://github.com/zazuko/clownface/pull/41
      const cfToS = cfDataset.out(
        namedNode("http://schema.org/termsOfService")
      );
      if (cfToS && cfToS.values && cfToS.values.length > 0) {
        return cfToS.values[0];
      }
    }
  },
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
  icon: ico36_detail_tags,
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
  icon: ico36_detail_title, //TODO: CORRECT ICON
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
  icon: ico36_detail_title, //TODO: CORRECT ICON
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
  icon: ico36_detail_title, //TODO: CORRECT ICON
  viewerComponent: undefined, //this is so we do not display this with a detail-viewer,
  component: undefined, //this is so we do not display the component as a detail-picker, but are still able to use the parseToRDF, parseFromRDF functions
  multiSelect: true,
  options: [
    { value: "match:NoHintForMe", label: "Silent" },
    { value: "match:NoHintForCounterpart", label: "Invisible" },
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
  icon: ico36_detail_title, //TODO: CORRECT ICON
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
  icon: ico36_detail_title, //TODO: CORRECT ICON
  viewerComponent: undefined, //this is so we do not display this with a detail-viewer,
  component: undefined,
  multiSelect: true,
  options: [
    {
      value: { "#chatSocket": vocab.CHAT.ChatSocketCompacted },
      label: "ChatSocket",
    },
    {
      value: { "#groupSocket": vocab.GROUP.GroupSocketCompacted },
      label: "GroupSocket",
    },
    {
      value: { "#holderSocket": vocab.HOLD.HolderSocketCompacted },
      label: "HolderSocket",
    },
    {
      value: { "#holdableSocket": vocab.HOLD.HoldableSocketCompacted },
      label: "HoldableSocket",
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

/*
  Use this detail with the s:PlanAction type -> if you use this detail make sure you add the s:PlanAction type to the corresponding branch (content or seeks)
*/
export const eventObjectAboutUris = {
  identifier: "eventObjectAboutUris",
  label: "Event",
  icon: ico36_detail_title,
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
  component: WikiDataPicker,
  viewerComponent: WikiDataViewer,
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
