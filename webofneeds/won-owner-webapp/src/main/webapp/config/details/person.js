/**
 * Created by fsuda on 18.09.2018.
 */
import * as basicDetails from "./basic.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import ico36_detail_skill from "../../images/won-icons/ico36_detail_skill.svg";
import ico36_detail_interests from "../../images/won-icons/ico36_detail_interests.svg";

export const skillsDetail = {
  ...basicDetails.tags,
  identifier: "skills",
  label: "Skills",
  icon: ico36_detail_skill,
  placeholder: "e.g. RDF, project-management",
  messageEnabled: false,
  parseToRDF: function({ value }) {
    if (!value) {
      return { "s:knowsAbout": undefined };
    }
    return { "s:knowsAbout": value };
  },
  parseFromRDF: function(jsonLDImm) {
    return jsonLdUtils.parseListFrom(jsonLDImm, ["s:knowsAbout"], "xsd:string");
  },
};

export const interestsDetail = {
  ...basicDetails.tags,
  identifier: "interests",
  label: "Interests",
  icon: ico36_detail_interests,
  placeholder: "e.g. food, cats",
  messageEnabled: false,
  parseToRDF: function({ value }) {
    if (!value) {
      return { "foaf:topic_interest": undefined };
    }
    return { "foaf:topic_interest": value };
  },
  parseFromRDF: function(jsonLDImm) {
    return jsonLdUtils.parseListFrom(jsonLDImm, "foaf:topic_interest");
  },
};
