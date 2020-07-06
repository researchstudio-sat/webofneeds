import WonTitlePicker from "../../app/components/details/picker/title-picker";
import WonTitleViewer from "../../app/components/details/viewer/title-viewer";
import WikiDataPicker from "../../app/components/details/picker/wikidata-picker";
import WikiDataViewer from "../../app/components/details/viewer/wikidata-viewer";
import * as jsonLdUtils from "../../app/service/jsonld-utils";
import ico36_detail_title from "../../images/won-icons/ico36_detail_title.svg";
import ico36_detail_person from "../../images/won-icons/ico36_detail_person.svg";
import { is } from "~/app/utils";

export const isbn = {
  identifier: "isbn",
  label: "ISBN",
  icon: ico36_detail_title,
  placeholder: "ISBN Number",
  component: WonTitlePicker,
  viewerComponent: WonTitleViewer,
  parseToRDF: function({ value }) {
    const val = value ? value : undefined;
    return {
      "s:isbn": val,
    };
  },
  parseFromRDF: function(jsonLDImm) {
    return jsonLdUtils.parseFrom(jsonLDImm, ["s:isbn"], "xsd:string");
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      return includeLabel ? this.label + ": " + value : value;
    }
    return undefined;
  },
};

export const author = {
  identifier: "author",
  label: "Author",
  icon: ico36_detail_person,
  placeholder: "Author Name",
  component: WonTitlePicker,
  viewerComponent: WonTitleViewer,
  parseToRDF: function({ value }) {
    if (value) {
      return {
        "s:author": {
          "@type": "s:Person",
          "s:name": value,
        },
      };
    }
    return undefined;
  },
  parseFromRDF: function(jsonLDImm) {
    return jsonLdUtils.parseFrom(
      jsonLDImm,
      ["s:author", "s:name"],
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

export const classifiedAs = {
  identifier: "classifiedAs",
  label: "Classified As", //TODO: Better Label
  icon: ico36_detail_title, //TODO: Better Icon
  messageDetail: false,
  placeholder: "Type to find what you look for...", //TODO: Better Placeholder
  component: WikiDataPicker,
  viewerComponent: WikiDataViewer,
  parseToRDF: function({ value }) {
    if (!value) {
      return;
    } else if (is("Array", value)) {
      const classifiedAsObjects = value.map(item => {
        return {
          "@type": "xsd:ID",
          "@value": item,
        };
      });
      return { "vf:classifiedAs": classifiedAsObjects };
    } else {
      return {
        "vf:classifiedAs": {
          "@type": "xsd:ID",
          "@value": value,
        },
      };
    }
  },
  parseFromRDF: function(jsonLDImm) {
    return jsonLdUtils.parseListFrom(jsonLDImm, ["vf:classifiedAs"], "xsd:ID");
  },
  generateHumanReadable: function({ value, includeLabel }) {
    //TODO: Implement
    if (value) {
      return includeLabel ? this.label + ": " + value : value;
    }
    return undefined;
  },
};
