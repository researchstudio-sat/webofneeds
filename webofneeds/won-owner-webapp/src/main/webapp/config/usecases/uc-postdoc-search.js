/**
 * Created by fsuda on 18.09.2018.
 */
import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import { interestsDetail, skillsDetail } from "../details/person.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";

export const postdocSearch = {
  identifier: "postdocSearch",
  label: "Find a PostDoc position",
  icon: "#ico36_uc_postdoc",
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["won:PostDocSearch"],
        title: "I'm looking for a PostDoc position!",
        tags: ["search-postdoc"],
        searchString: "offer-postdoc",
      },
      seeks: {
        type: ["won:PostDocPosition"],
      },
    }),
  },
  reactionUseCases: ["postdocOffer"],
  details: {
    title: { ...details.title },
    description: { ...details.description },
    person: { ...details.person },
    skills: { ...skillsDetail, placeholder: "" }, // TODO: find good placeholders
    interests: { ...interestsDetail, placeholder: "" },
  },
  seeksDetails: {
    description: { ...details.description },
    location: { ...details.location },
  },
};
