/**
 * Created by fsuda on 18.09.2018.
 */
import { details, emptyDraft } from "../../detail-definitions.js";
import { interestsDetail, skillsDetail } from "../../details/person.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../../app/won-utils.js";
import { jobSearch } from "../uc-job-search";
import { jobOffer } from "../uc-job-offer";

export const professionalGroup = {
  identifier: "professionalgroup",
  label: "Professional Networking",
  icon: undefined,
  useCases: {
    jobSearch: jobSearch,
    jobOffer: jobOffer,
    getToKnow: {
      identifier: "getToKnow",
      label: "Find people",
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
    },
    phdIs: {
      identifier: "phdIs",
      label: "Offer a PhD position",
      icon: "#ico36_uc_phd",
      doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
      draft: {
        ...emptyDraft,
        content: {
          title: "I'm offering a PhD position!",
          tags: ["offer-phd"],
          searchString: "search-phd",
        },
      },
      details: {
        title: { ...details.title },
        description: { ...details.description },
        location: { ...details.location },
      },
      seeksDetails: {
        skills: { ...skillsDetail, placeholder: "" }, // TODO: find good placeholders
        interests: { ...interestsDetail, placeholder: "" },
      },
    },
    phdSeeks: {
      identifier: "phdSeeks",
      label: "Find a PhD position",
      icon: "#ico36_uc_phd",
      doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
      draft: {
        ...emptyDraft,
        content: {
          title: "I'm looking for a PhD position!",
          tags: ["search-phd"],
          searchString: "offer-phd",
        },
      },
      details: {
        title: { ...details.title },
        description: { ...details.description },
        person: { ...details.person },
        skills: { ...skillsDetail, placeholder: "" }, // TODO: find good placeholders
        interests: { ...interestsDetail, placeholder: "" },
      },
      seeksDetails: {
        description: { ...details.description },
        location: { ...details.location },
      },
    },
    postDocIs: {
      identifier: "postDocIs",
      label: "Offer a PostDoc position",
      icon: "#ico36_uc_postdoc",
      doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
      draft: {
        ...emptyDraft,
        content: {
          title: "I'm offering a PostDoc position!",
          tags: ["offer-postdoc"],
          searchString: "search-postdoc",
        },
      },
      details: {
        title: { ...details.title },
        description: { ...details.description },
        location: { ...details.location },
      },
      seeksDetails: {
        skills: { ...skillsDetail, placeholder: "" }, // TODO: find good placeholders
        interests: { ...interestsDetail, placeholder: "" },
      },
    },
    postDocSeeks: {
      identifier: "postDocSeeks",
      label: "Find a PostDoc position",
      icon: "#ico36_uc_postdoc",
      doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
      draft: {
        ...emptyDraft,
        content: {
          title: "I'm looking for a PostDoc position!",
          tags: ["search-postdoc"],
          searchString: "offer-postdoc",
        },
      },
      details: {
        title: { ...details.title },
        description: { ...details.description },
        person: { ...details.person },
        skills: { ...skillsDetail, placeholder: "" }, // TODO: find good placeholders
        interests: { ...interestsDetail, placeholder: "" },
      },
      seeksDetails: {
        description: { ...details.description },
        location: { ...details.location },
      },
    },
    consortiumIs: {
      identifier: "consortiumIs",
      label: "Offer slot in a project consortium",
      icon: "#ico36_uc_consortium-offer",
      doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
      draft: {
        ...emptyDraft,
        content: {
          title: "Offering a slot in a project consortium",
          tags: ["offer-consortium"],
          searchString: "search-consortium",
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
    },
    consortiumSeeks: {
      identifier: "consortiumSeeks",
      label: "Find a project consortium to join",
      icon: "#ico36_uc_consortium-search",
      doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
      draft: {
        ...emptyDraft,
        content: {
          title: "Looking for a slot in a project consortium",
          tags: ["search-consortium"],
          searchString: "offer-consortium",
        },
      },
      details: {
        title: { ...details.title },
        description: { ...details.description },
        location: { ...details.location },
        skills: { ...skillsDetail, placeholder: "" }, // TODO: find good placeholders
        interests: { ...interestsDetail, placeholder: "" },
      },
      seeksDetails: {
        description: { ...details.description },
        location: { ...details.location },
      },
    },
  },
};
