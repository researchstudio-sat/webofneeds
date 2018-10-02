/**
 * Created by fsuda on 18.09.2018.
 */
import { details, emptyDraft } from "../detail-definitions.js";
import { interestsDetail, skillsDetail } from "../details/person.js";
import { jobLocation } from "../details/location.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";

export const professionalGroup = {
  identifier: "professionalgroup",
  label: "Professional Networking",
  icon: undefined,
  useCases: {
    jobSearch: {
      identifier: "getToKnow",
      label: "Search a Job",
      // icon: "#ico36_uc_find_people", TODO
      doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
      draft: {
        ...emptyDraft,
        is: {
          "@type": "s:Person",
          tags: ["search-job"],
        },
        seeks: {
          "@type": "s:JobPosting",
          /* example to match hokify-offers:
          "s:employmentType": "Full-time", // full-time/vollzeit, part-time,... - use a dropdown
    "s:industry": ["Computer Software", "Design"], // match by checking for intersection + text-similarity and translations

    //"s:baseSalary": {...}, // free-form-text in hokify json :|

    "s:hiringOrganization": { // for ppl who only want offers from a specific organization (rather niche tho)
      "@type": "s:Organization",
      "s:name": "", // JSON - company
    },

    "s:jobLocation": [
      {
        "@type": "s:Place",
        "s:geo": {
          "@id": "https://satvm05.researchstudio.at/won/resource/need/rsiiungn085u/location/6l7plycz2g", // unique id; i assume it's necessary for the geo-service
          "@type": "s:GeoCoordinates",
          "s:latitude": "48.216931",
          "s:longitude": "16.361197"
        },
        "s:address": {
          "@type": "s:PostalAddress",
          "s:addressCountry": "AT",
          "s:addressLocality": "Vienna"
        }
      },
      {
        "@type": "s:Place",
        //...
        "s:address": {
          //...
          "s:addressLocality": "Graz"
        }
      }
    ],
    */
        },
        searchString: ["offer-job", "job"],
      },
      isDetails: {
        title: { ...details.title },
        description: { ...details.description },
        location: { ...details.location },
        person: { ...details.person },
        skills: { ...skillsDetail },
        interests: { ...interestsDetail },
      },
      seeksDetails: {
        description: { ...details.description },
        jobLocation: { ...jobLocation },
        skills: { ...skillsDetail },
        interests: { ...interestsDetail },
      },
    },
    getToKnow: {
      identifier: "getToKnow",
      label: "Find people",
      icon: "#ico36_uc_find_people",
      doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
      draft: {
        ...emptyDraft,
        is: {
          title: "I'm up for meeting new people!",
          tags: ["meetup"],
        },
        searchString: "meetup",
      },
      isDetails: {
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
        is: {
          title: "I'm offering a PhD position!",
          tags: ["offer-phd"],
        },
        searchString: "search-phd",
      },
      isDetails: {
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
        is: {
          title: "I'm looking for a PhD position!",
          tags: ["search-phd"],
        },
        searchString: "offer-phd",
      },
      isDetails: {
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
        is: {
          title: "I'm offering a PostDoc position!",
          tags: ["offer-postdoc"],
        },
        searchString: "search-postdoc",
      },
      isDetails: {
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
        is: {
          title: "I'm looking for a PostDoc position!",
          tags: ["search-postdoc"],
        },
        searchString: "offer-postdoc",
      },
      isDetails: {
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
        is: {
          title: "Offering a slot in a project consortium",
          tags: ["offer-consortium"],
        },
        searchString: "search-consortium",
      },
      isDetails: {
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
        is: {
          title: "Looking for a slot in a project consortium",
          tags: ["search-consortium"],
        },
        searchString: "offer-consortium",
      },
      isDetails: {
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
