/**
 * Created by fsuda on 18.09.2018.
 */
import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import { interestsDetail } from "../details/person.js";
import ico36_uc_breakfast from "../../images/won-icons/ico36_uc_breakfast.svg";

export const getBreakfast = {
  identifier: "getBreakfast",
  label: "Get breakfast",
  icon: ico36_uc_breakfast,
  doNotMatchAfter: jsonLdUtils.findLatestIntervallEndInJsonLdOrNowAndAddMillis,
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
  reactions: {
    ...defaultReactions,
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
