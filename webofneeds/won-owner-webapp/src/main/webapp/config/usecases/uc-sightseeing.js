/**
 * Created by fsuda on 18.09.2018.
 */
import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";
import { interestsDetail } from "../details/person.js";

export const sightseeing = {
  identifier: "sightseeing",
  label: "Go sightseeing",
  icon: "#ico36_uc_sightseeing",
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["won:SightSeeing"],
        tags: ["sightseeing"],
        searchString: "sightseeing",
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
};
