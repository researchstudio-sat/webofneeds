/**
 * Created by fsuda on 18.09.2018.
 */
import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import { interestsDetail, skillsDetail } from "../details/person.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";

export const getToKnow = {
  identifier: "getToKnow",
  label: "Meet people",
  icon: "#ico36_uc_find_people",
  doNotMatchAfter: jsonLdUtils.findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["demo:Meetup"],
        title: "I'm up for meeting new people!",
        tags: ["meetup"],
        searchString: "meetup",
      },
    }),
  },
  details: {
    title: { ...details.title },
    description: { ...details.description },
    location: { ...details.location },
    person: { ...details.person },
    skills: { ...skillsDetail },
    interests: { ...interestsDetail },
    images: { ...details.images },
  },
  seeksDetails: {
    description: { ...details.description },
    location: { ...details.location },
    skills: { ...skillsDetail },
    interests: { ...interestsDetail },
  },
};
