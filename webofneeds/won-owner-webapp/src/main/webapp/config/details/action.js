import { get } from "../../app/utils.js";
import * as wonUtils from "../../app/won-utils.js";
import Immutable from "immutable";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import WonDescriptionViewer from "../../app/components/details/viewer/description-viewer.jsx";
import WonDescriptionPicker from "../../app/components/details/picker/description-picker.jsx";
import ico36_detail_description from "../../images/won-icons/ico36_detail_description.svg";

export const customAction = {
  identifier: "customAction",
  label: "Custom Action",
  icon: ico36_detail_description,
  placeholder: "Enter Action Description...",
  component: WonDescriptionPicker,
  viewerComponent: WonDescriptionViewer,
  parseToRDF: function({ value, contentUri }) {
    const val = value ? value : undefined;
    return {
      "vf:EconomicEvent": genEconomicEvent({
        eventData: val,
        baseUri: wonUtils.genDetailBaseUri(contentUri),
      }),
    };
  },
  parseFromRDF: function(jsonLDImm) {
    const description = get(jsonLDImm, "vf:EconomicEvent");
    if (Immutable.List.isList(description)) {
      return get(
        description.filter(d => get(d, "@language") === "en").first(),
        "@value"
      );
    } else {
      return jsonLdUtils.parseFrom(
        jsonLDImm,
        ["vf:EconomicEvent", "s:description"],
        "xsd:string"
      );
    }
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      return includeLabel ? this.label + ": " + value : value;
    }
    return undefined;
  },
};

function genEconomicEvent({ eventData, baseUri }) {
  if (!eventData) {
    return undefined;
  }

  return {
    "@id": baseUri ? baseUri + "-economicEvent" : undefined,
    "@type": "vf:EconomicEvent",
    "vf:action": { "@id": "vf:noEffect" },
    "s:description": eventData,
  };
}
