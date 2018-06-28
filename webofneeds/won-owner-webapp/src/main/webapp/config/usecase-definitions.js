import details from "detail-definitions.js";

export const useCases = {
  allDetailsUseCase,
  ...socialUseCases,
  ...professionalUseCases,
  ...otherUseCases,
};

// identifier have to be unique across all use cases!
export const useCaseList = useCases.map(useCase => useCase.identifier);

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
    icon: "#ico36_plus_circle",
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
  breakfast: {},
  lunch: {
    identifier: "lunch",
    label: "Get Lunch Together",
    icon: "#ico36_plus_circle",
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

const otherUseCases = {
  // taxi: {},
  // transport: {},
  // realEstate: {},
  // job: {},
};
