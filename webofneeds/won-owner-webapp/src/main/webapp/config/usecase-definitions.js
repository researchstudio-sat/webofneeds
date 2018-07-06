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
    icon: "#ico36_plus",
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
    label: "Get breakfast together",
    icon: "#ico36_plus",
    draft: {
      ...emptyDraft,
      is: { tags: ["essen", "food"] },
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
    label: "Get lunch together",
    icon: "#ico36_plus",
    draft: {
      ...emptyDraft,
      is: { tags: ["essen", "food"] },
      seeks: { title: "lunch" },
      searchString: "lunch",
    },
    isDetails: {
      description: { ...details.description },
      foodAllergies: {
        ...details.description,
        identifier: "foodallergies",
        label: "Food Allergies",
      },
      location: { ...details.location },
      tags: { ...details.tags },
    },
    seeksDetails: undefined,
  },
  afterparty: {
    identifier: "afterparty",
    label: "Enjoy the evening together",
    icon: "#ico36_plus",
    draft: { ...emptyDraft },
    seeksDetails: {
      description: { ...details.description },
      location: { ...details.location },
    },
  },
  sightseeing: {
    identifier: "sightseeing",
    label: "Go sightseeing together",
    icon: "#ico36_plus",
    draft: { ...emptyDraft },
    seeksDetails: {
      description: { ...details.description },
      location: { ...details.location },
    },
  },
  // carsharing: {},
  // meetSomeone: {},
  // activity: {},
};

const professionalUseCases = {
  getToKnow: {
    identifier: "getToKnow",
    label: "Find people",
    icon: "#ico36_plus",
    draft: { ...emptyDraft },
    isDetails: {
      description: { ...details.description },
      location: { ...details.location },
      person: { ...details.person },
    },
  },
  phdIs: {
    identifier: "phdIs",
    label: "Offer a PhD position",
    icon: "#ico36_plus",
    draft: { ...emptyDraft },
    isDetails: {
      description: { ...details.description },
      location: { ...details.location },
    },
  },
  phdSeeks: {
    identifier: "phdSeeks",
    label: "Find a PhD position",
    icon: "#ico36_plus",
    draft: { ...emptyDraft },
    isDetails: {
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
    icon: "#ico36_plus",
    draft: { ...emptyDraft },
    isDetails: {
      description: { ...details.description },
      location: { ...details.location },
    },
  },
  postDocSeeks: {
    identifier: "postDocSeeks",
    label: "Find a PostDoc position",
    icon: "#ico36_plus",
    draft: { ...emptyDraft },
    isDetails: {
      person: { ...details.person },
    },
    seeksDetails: {
      description: { ...details.description },
      location: { ...details.location },
    },
  },
  consortiumIs: {
    identifier: "consortiumIs",
    label: "Announce slot in project consortium",
    icon: "#ico36_plus",
    draft: { ...emptyDraft },
    isDetails: {
      description: { ...details.description },
      location: { ...details.location },
    },
  },
  consortiumSeeks: {
    identifier: "consortiumSeeks",
    label: "Find a project consortium to join",
    icon: "#ico36_plus",
    draft: { ...emptyDraft },
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
    icon: "#ico36_plus",
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
    icon: "#ico36_plus",
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
    label: "Fun activities",
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
