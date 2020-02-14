/**
 * Created by fsuda on 18.09.2018.
 */
import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import { interestsDetail, skillsDetail } from "../details/person.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import won from "../../app/service/won.js";

export const consortiumOffer = {
  identifier: "consortiumOffer",
  label: "Offer slot in a project consortium",
  icon: "#ico36_uc_consortium-offer",
  doNotMatchAfter: jsonLdUtils.findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["demo:ConsortiumOffer"],
        title: "Offering a slot in a project consortium",
        tags: ["offer-consortium"],
        searchString: "search-consortium",
      },
    }),
  },
  reactionUseCases: [
    {
      identifier: "consortiumSearch",
      senderSocketType: won.CHAT.ChatSocketCompacted,
      targetSocketType: won.CHAT.ChatSocketCompacted,
    },
  ],
  details: {
    title: { ...details.title },
    description: { ...details.description },
    location: { ...details.location },
  },
  seeksDetails: {
    description: { ...details.description },
    location: { ...details.location },
    skills: { ...skillsDetail, placeholder: "" }, // TODO: find good placeholders
    interests: { ...interestsDetail, placeholder: "" },
  },
};
