/**
 * Created by fsuda on 18.09.2018.
 */
import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";
import { interestsDetail } from "../details/person.js";

export const getBreakfast = {
  identifier: "getBreakfast",
  label: "Get breakfast",
  icon: "#ico36_uc_breakfast",
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["demo:Breakfast"],
        title: "I'm up for breakfast! Any plans?",
        tags: ["breakfast"],
        searchString: "breakfast",
      },
      seeks: { title: "breakfast" },
    }),
  },
  details: {
    title: { ...details.title },
    description: { ...details.description },
    fromDatetime: { ...details.fromDatetime },
    throughDatetime: { ...details.throughDatetime },
    location: { ...details.location },
    interests: { ...interestsDetail },
  },
  seeksDetails: undefined,
};
