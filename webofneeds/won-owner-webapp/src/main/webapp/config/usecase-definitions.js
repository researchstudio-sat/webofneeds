import { details } from "detailDefinitions";

export const emptyDraft = {
  is: {},
  seeks: {},
  matchingContext: undefined,
};

/**
 * USE CASE REQUIREMENTS
 * detail identifiers in is and seeks have to be unique
 * if two details use the same predicate on the same level,
 * the latter detail will overwrite the former.
 * Example:
 * useCase: {
 *    identifier: "useCase",
 *    isDetails: {
 *        detailA: {...details.description, identifier: "detailA"},
 *        detailB: {...details.description, identifier: "detailB"},
 *    }
 * }
 *
 * In this case, the value of detailB will overwrite the value of detailA, because
 * both use the predicate "dc:description".
 * To avoid this, redefine the parseToRDF() and parseFromRDF() methods for either
 * detail to use a different predicate.
 */

const allDetailsUseCase = {
  allDetails: {
    identifier: "allDetails",
    label: "New custom post",
    icon: "#ico36_uc_custom",
    draft: { ...emptyDraft },
    isDetails: details,
    seeksDetails: details,
  },
};

const pureSearchUseCase = {
  pureSearch: {
    identifier: "pureSearch",
    label: "Search posts",
    icon: "#ico36_search",
    showInList: true /*This parameter defines if the useCase should be shown outside of the usecase picker as well*/,
    draft: { ...emptyDraft },
    isDetails: undefined,
    seeksDetails: {},
  },
};

// TODO: roles?
// note: if no details are to be added for is or seeks,
// there won't be an is or seeks part unless defined in the draft
// details predefined in the draft can only be changed IF included in the correct detail list
const socialUseCases = {
  breakfast: {
    identifier: "breakfast",
    label: "Get breakfast",
    icon: "#ico36_uc_breakfast",
    draft: {
      ...emptyDraft,
      is: {
        tags: ["breakfast"],
        title: "I'm up for breakfast! Any plans?",
      },
      seeks: { title: "breakfast" },
      searchString: "breakfast",
    },
    isDetails: {
      description: { ...details.description },
      foodAllergies: {
        ...details.description,
        identifier: "foodallergies",
        label: "Food Allergies",
        parseToRDF: function({ value }) {
          if (!value) {
            return { "won:foodAllergies": undefined }; // FIXME: won:foodAllergies does not exist
          }
          return { "won:foodAllergies": value };
        },
        parseFromRDF: function(jsonLDImm) {
          return jsonLDImm && jsonLDImm.get("won:foodAllergies");
        },
      },
      location: { ...details.location },
      tags: { ...details.tags },
    },
    seeksDetails: undefined,
  },
  lunch: {
    identifier: "lunch",
    label: "Get lunch",
    icon: "#ico36_uc_meal-half",
    draft: {
      ...emptyDraft,
      is: {
        tags: ["lunch"],
        title: "I'm up for lunch! Any plans?",
      },
      seeks: { title: "lunch" },
      searchString: "lunch",
    },
    isDetails: {
      description: { ...details.description },
      foodAllergies: {
        ...details.description,
        identifier: "foodallergies",
        label: "Food Allergies",
        parseToRDF: function({ value }) {
          if (!value) {
            return { "won:foodAllergies": undefined }; // FIXME: won:foodAllergies does not exist
          }
          return { "won:foodAllergies": value };
        },
        parseFromRDF: function(jsonLDImm) {
          return jsonLDImm && jsonLDImm.get("won:foodAllergies");
        },
      },
      location: { ...details.location },
      tags: { ...details.tags },
    },
    seeksDetails: undefined,
  },
  afterparty: {
    identifier: "afterparty",
    label: "Go out",
    icon: "#ico36_uc_drinks",
    draft: {
      ...emptyDraft,
      is: {
        tags: ["afterparty"],
        title: "I'm up for partying! Any plans?",
      },
      seeks: { title: "afterparty" },
      searchString: "afterparty",
    },
    isDetails: {
      description: { ...details.description },
      location: { ...details.location },
    },
  },
  sightseeing: {
    identifier: "sightseeing",
    label: "Go sightseeing",
    icon: "#ico36_uc_sightseeing",
    draft: {
      ...emptyDraft,
      is: { tags: ["sightseeing"] },
      seeks: { title: "sightseeing" },
      searchString: "sightseeing",
    },
    isDetails: {
      description: { ...details.description },
      location: { ...details.location },
    },
  },
};

const professionalUseCases = {
  getToKnow: {
    identifier: "getToKnow",
    label: "Find people",
    icon: "#ico36_uc_find_people",
    draft: {
      ...emptyDraft,
      is: {
        tags: ["meetup"],
        title: "I'm up for meeting new people!",
      },
      seeks: { title: "meetup" },
      searchString: "meetup",
    },
    isDetails: {
      description: { ...details.description },
      location: { ...details.location },
      person: { ...details.person },
    },
    seeksDetails: {
      description: { ...details.description },
      location: { ...details.location },
      person: { ...details.person },
    },
  },
  phdIs: {
    identifier: "phdIs",
    label: "Offer a PhD position",
    icon: "#ico36_uc_phd",
    draft: {
      ...emptyDraft,
      is: {
        tags: ["offer-phd"],
        title: "I'm offering a PhD position!",
      },
      searchString: "search-phd",
    },
    isDetails: {
      description: { ...details.description },
      location: { ...details.location },
    },
  },
  phdSeeks: {
    identifier: "phdSeeks",
    label: "Find a PhD position",
    icon: "#ico36_uc_phd",
    draft: {
      ...emptyDraft,
      is: {
        tags: ["search-phd"],
        title: "I'm looking for a PhD position!",
      },
      searchString: "offer-phd",
    },
    isDetails: {
      description: { ...details.description },
      person: { ...details.person },
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
    draft: {
      ...emptyDraft,
      is: {
        tags: ["offer-postdoc"],
        title: "I'm offering a PostDoc position!",
      },
      searchString: "search-postdoc",
    },
    isDetails: {
      description: { ...details.description },
      location: { ...details.location },
    },
  },
  postDocSeeks: {
    identifier: "postDocSeeks",
    label: "Find a PostDoc position",
    icon: "#ico36_uc_postdoc",
    draft: {
      ...emptyDraft,
      is: {
        tags: ["search-postdoc"],
        title: "I'm looking for a PostDoc position!",
      },
      searchString: "offer-postdoc",
    },
    isDetails: {
      description: { ...details.description },
      person: { ...details.person },
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
    draft: {
      ...emptyDraft,
      is: {
        tags: ["offer-consortium"],
        title: "Offering a slot in a project consortium",
      },
      searchString: "search-consortium",
    },
    isDetails: {
      description: { ...details.description },
      location: { ...details.location },
    },
    seeksDetails: {
      description: { ...details.description },
      location: { ...details.location },
    },
  },
  consortiumSeeks: {
    identifier: "consortiumSeeks",
    label: "Find a project consortium to join",
    icon: "#ico36_uc_consortium-search",
    draft: {
      ...emptyDraft,
      is: {
        tags: ["search-consortium"],
        title: "Looking for a slot in a project consortium",
      },
      searchString: "offer-consortium",
    },
    isDetails: {
      description: { ...details.description },
      location: { ...details.location },
    },
    seeksDetails: {
      description: { ...details.description },
      location: { ...details.location },
    },
  },
};

const infoUseCases = {
  question: {
    identifier: "question",
    label: "Ask a question",
    icon: "#ico36_uc_question",
    draft: {
      ...emptyDraft,
      is: { tags: ["question"] },
      seeks: { tags: "answer" },
    },
    isDetails: {
      description: { ...details.description },
      location: { ...details.location },
      tags: { ...details.tags },
    },
    seeksDetails: undefined,
  },
  answer: {
    //answer should have 'no hint for counterpart'
    identifier: "answer",
    label: "Answer questions",
    icon: "#ico36_uc_answer",
    draft: {
      ...emptyDraft,
      is: {
        title: "Answer questions",
        tags: ["answer"],
      },
      seeks: { tags: "question" },
    },
    isDetails: {
      description: { ...details.description },
      location: { ...details.location },
      tags: { ...details.tags },
    },
    seeksDetails: undefined,
  },
};

/*const otherUseCases = {
  // taxi: {},
  // transport: {},
  // realEstate: {},
  // job: {},
};*/

export const useCases = {
  ...socialUseCases,
  ...professionalUseCases,
  ...infoUseCases,
  ...allDetailsUseCase,
  ...pureSearchUseCase,
};

export const useCaseGroups = {
  social: {
    identifier: "socialgroup",
    label: "Fun activities to do together",
    icon: undefined,
    useCases: { ...socialUseCases },
  },
  professional: {
    identifier: "professionalgroup",
    label: "Professional networking",
    icon: undefined,
    useCases: { ...professionalUseCases },
  },
  info: {
    identifier: "infogroup",
    label: "Questions and Answers",
    icon: undefined,
    useCases: { ...infoUseCases },
  },
  other: {
    identifier: "othergroup",
    label: "Something else",
    icon: undefined,
    useCases: { ...allDetailsUseCase, ...pureSearchUseCase },
  },
};
