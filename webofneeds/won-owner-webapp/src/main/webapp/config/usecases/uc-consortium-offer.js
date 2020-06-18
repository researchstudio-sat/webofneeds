/**
 * Created by fsuda on 18.09.2018.
 */
import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import { interestsDetail, skillsDetail } from "../details/person.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import vocab from "../../app/service/vocab.js";
import ico36_uc_consortium_offer from "../../images/won-icons/ico36_uc_consortium-offer.svg";

export const consortiumOffer = {
  identifier: "consortiumOffer",
  label: "Offer slot in a project consortium",
  icon: ico36_uc_consortium_offer,
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
  reactions: {
    [vocab.CHAT.ChatSocketCompacted]: {
      [vocab.CHAT.ChatSocketCompacted]: {
        useCaseIdentifiers: ["consortiumSearch", "persona"],
        refuseOwned: true,
      },
    },
  },
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
