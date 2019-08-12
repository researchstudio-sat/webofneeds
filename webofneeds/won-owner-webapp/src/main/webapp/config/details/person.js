/**
 * Created by fsuda on 18.09.2018.
 */
import * as basicDetails from "./basic.js";
import { generateIdString, getIn } from "../../app/utils.js";
import Immutable from "immutable";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import WonPersonViewer from "../../app/components/details/viewer/person-viewer.jsx";

export const skillsDetail = {
  ...basicDetails.tags,
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
    return jsonLdUtils.parseListFrom(jsonLDImm, ["s:knowsAbout"], "xsd:string");
  },
};

export const interestsDetail = {
  ...basicDetails.tags,
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
    return jsonLdUtils.parseListFrom(jsonLDImm, "foaf:topic_interest");
  },
};

export const person = {
  identifier: "person",
  label: "Person",
  icon: "#ico36_detail_person",
  placeholder: undefined,
  component: "won-person-picker",
  viewerComponent: WonPersonViewer,
  messageEnabled: true,
  parseToRDF: function({ value, identifier, contentUri }) {
    if (!value) {
      return { "foaf:person": undefined };
    }
    return {
      "foaf:title": getIn(value, ["title"])
        ? getIn(value, ["title"])
        : undefined,
      "foaf:name": getIn(value, ["name"]) ? getIn(value, ["name"]) : undefined,
      "s:worksFor": getIn(value, ["company"])
        ? {
            "@id":
              contentUri && identifier
                ? contentUri +
                  "/" +
                  identifier +
                  "/" +
                  generateIdString(10) +
                  "/organization"
                : undefined,
            "@type": "s:Organization",
            "s:name": getIn(value, ["company"]),
          }
        : undefined,
      "s:jobTitle": getIn(value, ["position"])
        ? getIn(value, ["position"])
        : undefined,
    };
  },
  parseFromRDF: function(jsonLDImm) {
    if (!jsonLDImm) return undefined;

    let person = {
      name: undefined,
      title: undefined,
      company: undefined,
      position: undefined,
      // bio: undefined,
    };

    person.name = jsonLdUtils.parseFrom(jsonLDImm, ["foaf:name"], "xsd:string");
    person.title = jsonLdUtils.parseFrom(
      jsonLDImm,
      ["foaf:title"],
      "xsd:string"
    );
    person.company = jsonLdUtils.parseFrom(
      jsonLDImm,
      ["s:worksFor", "s:name"],
      "xsd:string"
    );
    person.position = jsonLdUtils.parseFrom(
      jsonLDImm,
      ["s:jobTitle"],
      "xsd:string"
    );

    // if there's anything, use it
    if (person.name || person.title || person.company || person.position) {
      return Immutable.fromJS(person);
    }
    return undefined;
  },
  generateHumanReadable: function({ value, includeLabel }) {
    const title = getIn(value, ["title"]);
    const name = getIn(value, ["name"]);
    const company = getIn(value, ["company"]);
    const position = getIn(value, ["position"]);

    let humanReadable;
    if (title) {
      humanReadable = title + " ";
    }
    if (name) {
      if (humanReadable) {
        humanReadable += name + " ";
      } else {
        humanReadable = name + " ";
      }
    }
    if (company) {
      if (humanReadable) {
        humanReadable += "works at " + company + " ";
      } else {
        humanReadable = company + " ";
      }
    }
    if (position) {
      if (humanReadable) {
        if (company) {
          humanReadable += "as a " + position;
        } else {
          humanReadable += "is a " + position;
        }
      } else {
        humanReadable = position;
      }
    }

    if (humanReadable) {
      return includeLabel
        ? this.label + ": " + humanReadable.trim()
        : humanReadable.trim();
    }
    return undefined;
  },
};
