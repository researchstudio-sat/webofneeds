/**
 * Created by fsuda on 18.09.2018.
 */
import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import { interestsDetail, skillsDetail } from "../details/person.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";

export const phdSearch = {
  identifier: "phdSearch",
  label: "Find a PhD position",
  icon: "#ico36_uc_phd",
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["won:PhdPositionSearch"],
        title: "I'm looking for a PhD position!",
        tags: ["search-phd"],
        searchString: "offer-phd",
      },
      seeks: {
        type: ["won:PhdPosition"],
      },
    }),
  },
  reactionUseCases: ["phdOffer"],
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
