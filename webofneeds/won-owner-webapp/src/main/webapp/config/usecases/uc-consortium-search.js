/**
 * Created by fsuda on 18.09.2018.
 */
import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import { interestsDetail, skillsDetail } from "../details/person.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";

export const consortiumSearch = {
  identifier: "consortiumSearch",
  label: "Find a project consortium to join",
  icon: "#ico36_uc_consortium-search",
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["won:ConsortiumSearch"],
        title: "Looking for a slot in a project consortium",
        tags: ["search-consortium"],
        searchString: "offer-consortium",
      },
    }),
  },
  details: {
    title: { ...details.title },
    description: { ...details.description },
    location: { ...details.location },
    skills: { ...skillsDetail, placeholder: "" }, // TODO: find good placeholders
    interests: { ...interestsDetail, placeholder: "" },
  },
  seeksDetails: {
    description: { ...details.description },
    location: { ...details.location },
  },
};
