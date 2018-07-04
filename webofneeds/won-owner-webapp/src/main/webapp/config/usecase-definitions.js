import { details } from "detailDefinitions";

// don't put detail placeholders in the draft, this only makes it harder to handle.
// if we want to set a initial value (can be changed/deleted unless hidden), the draft
// needs to be adjusted anyway, see "lunch"
export const emptyDraft = {
  is: {
    // title: undefined,
    // description: undefined,
    // tags: undefined,
    // person: undefined,
    // location: undefined,
    // travelAction: undefined,
  },
  seeks: {
    // title: undefined,
    // description: undefined,
    // tags: undefined,
    // person: undefined,
    // location: undefined,
    // travelAction: undefined,
  },
  matchingContext: undefined,
};

const allDetailsUseCase = {
  allDetails: {
    identifier: "allDetails",
    label: "All Details",
    icon: "#ico36_plus",
    draft: { ...emptyDraft },
    isDetails: details,
    seeksDetails: details,
  },
};

const pureSearchUseCase = {
  pureSearch: {
    identifier: "pureSearch",
    label: "Search Posts",
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
  lunch: {
    identifier: "lunch",
    label: "Get Lunch Together",
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
    label: "Afterparty",
    icon: "#ico36_plus",
    draft: { ...emptyDraft },
    seeksDetails: {
      description: { ...details.description },
      location: { ...details.location },
    },
  },
  sightseeing: {
    identifier: "sightseeing",
    label: "Sight Seeing",
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
  // getToKnow: {},
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
    label: "Search for a PhD position",
    icon: "#ico36_plus",
    draft: { ...emptyDraft },
    isDetails: {
      description: { ...details.description },
      location: { ...details.location },
    },
  },
  // postdoc: {},
  // consortium: {},
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
  ...allDetailsUseCase,
  ...pureSearchUseCase,
};

export const useCaseGroups = {
  social: {
    identifier: "socialgroup",
    label: "Do something social",
    icon: undefined,
    useCases: { ...socialUseCases },
  },
  professional: {
    identifier: "professionalgroup",
    label: "Do something professional",
    icon: "#ico36_plus",
    useCases: { ...professionalUseCases },
  },
  other: {
    identifier: "othergroup",
    label: "And now for something completely different",
    icon: "#ico36_plus",
    useCases: { ...allDetailsUseCase, ...pureSearchUseCase },
  },
};
