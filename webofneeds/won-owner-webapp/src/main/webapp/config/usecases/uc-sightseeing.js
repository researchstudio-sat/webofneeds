/**
 * Created by fsuda on 18.09.2018.
 */
import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import { interestsDetail } from "../details/person.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import ico36_uc_sightseeing from "../../images/won-icons/ico36_uc_sightseeing.svg";

export const sightseeing = {
  identifier: "sightseeing",
  label: "Go sightseeing",
  icon: ico36_uc_sightseeing,
  doNotMatchAfter: jsonLdUtils.findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["demo:SightSeeing"],
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
