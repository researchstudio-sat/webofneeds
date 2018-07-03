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
    draft: { ...emptyDraft, usecase: "allDetails" },
    isDetails: details,
    seeksDetails: details,
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
      usecase: "lunch",
      seeks: { title: "lunch" }, // TODO: this should not be title.
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
  // afterparty: {},
  // sightseeing: {},
  // carsharing: {},
  // meetSomeone: {},
  // activity: {},
};

const professionalUseCases = {
  // getToKnow: {},
  // phd: {},
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
    useCases: { ...allDetailsUseCase },
  },
};
