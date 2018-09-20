/**
 * Created by fsuda on 18.09.2018.
 */
import { details } from "detailDefinitions";
import won from "../../app/won-es6.js";

export const skillsDetail = {
  ...details.tags,
  identifier: "skills",
  label: "Skills",
  icon: "#ico36_detail_skill",
  placeholder: "e.g. RDF, project-management",
  messageEnabled: false,
  parseToRDF: function({ value }) {
    if (!value) {
      return { "s:knowsAbout": undefined };
    }
    return { "s:knowsAbout": value };
  },
  parseFromRDF: function(jsonLDImm) {
    return won.parseListFrom(jsonLDImm, ["s:knowsAbout"], "xsd:string");
  },
};

export const interestsDetail = {
  ...details.tags,
  identifier: "interests",
  label: "Interests",
  icon: "#ico36_detail_interests",
  placeholder: "e.g. food, cats",
  messageEnabled: false,
  parseToRDF: function({ value }) {
    if (!value) {
      return { "foaf:topic_interest": undefined };
    }
    return { "foaf:topic_interest": value };
  },
  parseFromRDF: function(jsonLDImm) {
    return won.parseListFrom(jsonLDImm, "foaf:topic_interest");
  },
};
