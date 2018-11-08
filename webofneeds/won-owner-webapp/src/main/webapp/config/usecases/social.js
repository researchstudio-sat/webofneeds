/**
 * Created by fsuda on 18.09.2018.
 */
import { details, emptyDraft } from "../detail-definitions.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";
import { interestsDetail } from "../details/person.js";

export const socialGroup = {
  identifier: "socialgroup",
  label: "Social Activities",
  icon: undefined,
  useCases: {
    breakfast: {
      identifier: "breakfast",
      label: "Get breakfast",
      icon: "#ico36_uc_breakfast",
      doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
      draft: {
        ...emptyDraft,
        content: {
          title: "I'm up for breakfast! Any plans?",
          tags: ["breakfast"],
          searchString: "breakfast",
        },
        seeks: { title: "breakfast" },
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
    },
    lunch: {
      identifier: "lunch",
      label: "Get lunch",
      icon: "#ico36_uc_meal-half",
      doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
      draft: {
        ...emptyDraft,
        content: {
          title: "I'm up for lunch! Any plans?",
          tags: ["lunch"],
          searchString: "lunch",
        },
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
    },
    afterparty: {
      identifier: "afterparty",
      label: "Go out",
      icon: "#ico36_uc_drinks",
      doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
      draft: {
        ...emptyDraft,
        content: {
          title: "I'm up for partying! Any plans?",
          tags: ["afterparty"],
          searchString: "afterparty",
        },
      },
      details: {
        title: { ...details.title },
        fromDatetime: { ...details.fromDatetime },
        throughDatetime: { ...details.throughDatetime },
        description: { ...details.description },
        location: { ...details.location },
        interests: { ...interestsDetail },
      },
    },
    sightseeing: {
      identifier: "sightseeing",
      label: "Go sightseeing",
      icon: "#ico36_uc_sightseeing",
      doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
      draft: {
        ...emptyDraft,
        content: { tags: ["sightseeing"], searchString: "sightseeing" },
      },
      details: {
        title: { ...details.title },
        description: { ...details.description },
        fromDatetime: { ...details.fromDatetime },
        throughDatetime: { ...details.throughDatetime },
        location: { ...details.location },
        interests: { ...interestsDetail },
      },
    },
  },
};
