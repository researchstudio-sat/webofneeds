/**
 * Created by fsuda on 18.09.2018.
 */
import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";
import { interestsDetail } from "../details/person.js";

export const getLunch = {
  identifier: "getLunch",
  label: "Get lunch",
  icon: "#ico36_uc_meal-half",
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        title: "I'm up for lunch! Any plans?",
        tags: ["lunch"],
        searchString: "lunch",
      },
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
