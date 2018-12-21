/**
 * Created by fsuda on 18.09.2018.
 */
import { details, emptyDraft } from "../detail-definitions.js";
import { interestsDetail, skillsDetail } from "../details/person.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";

export const getToKnow = {
  identifier: "getToKnow",
  label: "Meet people",
  icon: "#ico36_uc_find_people",
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...emptyDraft,
    content: {
      title: "I'm up for meeting new people!",
      tags: ["meetup"],
      searchString: "meetup",
    },
  },
  details: {
    title: { ...details.title },
    description: { ...details.description },
    location: { ...details.location },
    person: { ...details.person },
    skills: { ...skillsDetail },
    interests: { ...interestsDetail },
  },
  seeksDetails: {
    description: { ...details.description },
    location: { ...details.location },
    skills: { ...skillsDetail },
    interests: { ...interestsDetail },
  },
};
