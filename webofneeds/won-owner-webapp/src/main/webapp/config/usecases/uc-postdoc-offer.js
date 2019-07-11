/**
 * Created by fsuda on 18.09.2018.
 */
import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import { interestsDetail, skillsDetail } from "../details/person.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";

export const postdocOffer = {
  identifier: "postdocOffer",
  label: "Offer a PostDoc position",
  icon: "#ico36_uc_postdoc",
  doNotMatchAfter: jsonLdUtils.findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["demo:PostDocPosition"],
        title: "I'm offering a PostDoc position!",
        tags: ["offer-postdoc"],
        searchString: "search-postdoc",
      },
    }),
  },
  reactionUseCases: ["postdocSearch"],
  details: {
    title: { ...details.title },
    description: { ...details.description },
    location: { ...details.location },
  },
  seeksDetails: {
    skills: { ...skillsDetail, placeholder: "" }, // TODO: find good placeholders
    interests: { ...interestsDetail, placeholder: "" },
  },
};
