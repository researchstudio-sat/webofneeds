/**
 * Created by fsuda on 18.09.2018.
 */
import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";
import { interestsDetail } from "../details/person.js";

export const afterparty = {
  identifier: "afterparty",
  label: "Go out",
  icon: "#ico36_uc_drinks",
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["demo:AfterParty"],
        title: "I'm up for partying! Any plans?",
        tags: ["afterparty"],
        searchString: "afterparty",
      },
    }),
  },
  details: {
    title: { ...details.title },
    fromDatetime: { ...details.fromDatetime },
    throughDatetime: { ...details.throughDatetime },
    description: { ...details.description },
    location: { ...details.location },
    interests: { ...interestsDetail },
  },
};
