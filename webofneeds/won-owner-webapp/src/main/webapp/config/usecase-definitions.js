import details from "detail-definitions.js";

export const useCases = {
  allDetailsUseCase,
  ...socialUseCases,
  ...professionalUseCases,
  ...otherUseCases,
};

// identifier have to be unique across all use cases!
export const useCaseList = useCases.map(useCase => useCase.identifier);

const emptyDraft = {
  is: {
    title: undefined,
    description: undefined,
    tags: undefined,
    person: undefined,
    location: undefined,
    travelAction: undefined,
  },
  seeks: {
    title: undefined,
    description: undefined,
    tags: undefined,
    person: undefined,
    location: undefined,
    travelAction: undefined,
  },
  matchingContext: undefined,
};

const allDetailsUseCase = {
  allDetails: {
    identifier: "allDetails",
    label: "All Details",
    icon: "#ico36_plus_circle",
    draft: undefined,
    isDetails: details,
    seeksDetails: details,
  },
};

// TODO: roles?
const socialUseCases = {
  breakfast: {},
  lunch: {
    identifier: "lunch",
    label: "Get Lunch Together",
    icon: "#ico36_plus_circle",
    draft: { ...emptyDraft, usecase: "lunch", seeks: { title: "lunch" } },
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
  afterparty: {},
  sightseeing: {},
  carsharing: {},
  meetSomeone: {},
  activity: {},
};

const professionalUseCases = {
  getToKnow: {},
  phd: {},
  postdoc: {},
  consortium: {},
};

const otherUseCases = {
  taxi: {},
  transport: {},
  realEstate: {},
  job: {},
};
