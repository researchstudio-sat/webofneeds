import WonTitlePicker from "../../app/components/details/picker/title-picker";
import WonTitleViewer from "../../app/components/details/viewer/title-viewer";
import * as jsonLdUtils from "../../app/service/jsonld-utils";

export const isbn = {
  identifier: "isbn",
  label: "ISBN",
  icon: "#ico36_detail_title",
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
  icon: "#ico36_detail_title",
  placeholder: "Author Name",
  component: WonTitlePicker,
  viewerComponent: WonTitleViewer,
  parseToRDF: function({ value }) {
    const val = value ? value : undefined;
    return {
      "s:author": {
        "@type": "s:Person",
        "s:name": val,
      },
    };
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
