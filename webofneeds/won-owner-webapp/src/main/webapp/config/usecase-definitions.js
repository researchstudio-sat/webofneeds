import { is } from "../app/utils.js";
import Immutable from "immutable";
import { details } from "detailDefinitions";

export const emptyDraft = {
  is: {},
  seeks: {},
  matchingContext: undefined,
};

/**
 * USE CASE REQUIREMENTS
 * detail identifiers in is and seeks have to be unique
 * detail identifiers must not be "search"
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

const skillsDetail = {
  ...details.tags,
  identifier: "skills",
  label: "Skills",
  icon: "#ico36_skill_circle",
  parseToRDF: function({ value }) {
    if (!value) {
      return { "s:knowsAbout": undefined };
    }
    return { "s:knowsAbout": value };
  },
  parseFromRDF: function(jsonLDImm) {
    const skills = jsonLDImm && jsonLDImm.get("s:knowsAbout");
    if (!skills) {
      return undefined;
    } else if (is("String", skills)) {
      return Immutable.fromJS([skills]);
    } else if (is("Array", skills)) {
      return Immutable.fromJS(skills);
    } else if (Immutable.List.isList(skills)) {
      return skills; // id; it is already in the format we want
    } else {
      console.error(
        "Found unexpected format of skills (should be Array, " +
          "Immutable.List, or a single tag as string): " +
          JSON.stringify(skills)
      );
      return undefined;
    }
  },
};

const interestsDetail = {
  ...details.tags,
  identifier: "interests",
  label: "Interests",
  icon: "#ico36_heart_circle",
  parseToRDF: function({ value }) {
    if (!value) {
      return { "foaf:topic_interest": undefined };
    }
    return { "foaf:topic_interest": value };
  },
  parseFromRDF: function(jsonLDImm) {
    const interests = jsonLDImm && jsonLDImm.get("foaf:topic_interest");
    if (!interests) {
      return undefined;
    } else if (is("String", interests)) {
      return Immutable.fromJS([interests]);
    } else if (is("Array", interests)) {
      return Immutable.fromJS(interests);
    } else if (Immutable.List.isList(interests)) {
      return interests; // id; it is already in the format we want
    } else {
      console.error(
        "Found unexpected format of interests (should be Array, " +
          "Immutable.List, or a single tag as string): " +
          JSON.stringify(interests)
      );
      return undefined;
    }
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
      title: { ...details.title },
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
      interests: { ...interestsDetail },
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
      title: { ...details.title },
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
      interests: { ...interestsDetail },
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
      title: { ...details.title },
      description: { ...details.description },
      location: { ...details.location },
      interests: { ...interestsDetail },
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
      title: { ...details.title },
      description: { ...details.description },
      location: { ...details.location },
      interests: { ...interestsDetail },
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
    draft: {
      ...emptyDraft,
      is: {
        tags: ["offer-phd"],
        title: "I'm offering a PhD position!",
      },
      searchString: "search-phd",
    },
    isDetails: {
      title: { ...details.title },
      description: { ...details.description },
      location: { ...details.location },
    },
    seeksDetails: {
      skills: { ...skillsDetail },
      interests: { ...interestsDetail },
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
      title: { ...details.title },
      description: { ...details.description },
      person: { ...details.person },
      skills: { ...skillsDetail },
      interests: { ...interestsDetail },
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
      title: { ...details.title },
      description: { ...details.description },
      location: { ...details.location },
    },
    seeksDetails: {
      skills: { ...skillsDetail },
      interests: { ...interestsDetail },
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
      title: { ...details.title },
      description: { ...details.description },
      person: { ...details.person },
      skills: { ...skillsDetail },
      interests: { ...interestsDetail },
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
      title: { ...details.title },
      description: { ...details.description },
      location: { ...details.location },
    },
    seeksDetails: {
      description: { ...details.description },
      location: { ...details.location },
      skills: { ...skillsDetail },
      interests: { ...interestsDetail },
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
      title: { ...details.title },
      description: { ...details.description },
      location: { ...details.location },
      skills: { ...skillsDetail },
      interests: { ...interestsDetail },
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
      title: { ...details.title },
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
      title: { ...details.title },
      description: { ...details.description },
      location: { ...details.location },
      tags: { ...details.tags },
    },
    seeksDetails: undefined,
  },
};

const realEstateUseCases = {
  searchRent: {
    identifier: "searchRent",
    label: "Find a place to rent",
    icon: "#ico36_uc_custom", // TODO: replace this icon
    draft: {
      ...emptyDraft,
      seeks: { title: "Looking for a place to rent" },
      searchString: "for-rent",
    },
    isDetails: undefined,
    seeksDetails: {
      location: { ...details.location },
      floorSize: {
        ...details.title,
        identifier: "floorSize",
        label: "Floor size in square meters",
        icon: "#ico36_plus_circle", // TODO: better icon
        parseToRDF: function({ value }) {
          if (!value) {
            return { "s:floorSize": undefined };
          }
          return { "s:floorSize": value };
        },
        parseFromRDF: function(jsonLDImm) {
          const floorSize = jsonLDImm && jsonLDImm.get("s:floorSize");
          if (!floorSize) {
            return undefined;
          } else {
            return floorSize + "m2";
          }
        },
      },
      numberOfRooms: {
        ...details.title,
        identifier: "numberOfRooms",
        label: "Number of Rooms",
        icon: "#ico36_plus_circle", // TODO: better icon
        parseToRDF: function({ value }) {
          if (!value) {
            return { "s:numberOfRooms": undefined };
          }
          return { "s:numberOfRooms": value };
        },
        parseFromRDF: function(jsonLDImm) {
          const numberOfRooms = jsonLDImm && jsonLDImm.get("s:numberOfRooms");
          if (!numberOfRooms) {
            return undefined;
          } else {
            return numberOfRooms;
          }
        },
      },
      features: {
        ...details.tags,
        identifier: "features",
        label: "Features",
        icon: "#ico36_plus_circle", //TODO: better icon
        parseToRDF: function({ value }) {
          if (!value) {
            return { "s:amenityFeature": undefined };
          } else {
            return {
              "s:amenityFeature": {
                "@type": "s:LocationFeatureSpecification",
                "s:name": value,
              },
            };
          }
        },
        parseFromRDF: function(jsonLDImm) {
          const amenityFeature = jsonLDImm && jsonLDImm.get("s:amenityFeature");
          const features = amenityFeature && amenityFeature.get("s:name");

          if (!features) {
            return undefined;
          } else if (is("String", features)) {
            return Immutable.fromJS([features]);
          } else if (is("Array", features)) {
            return Immutable.fromJS(features);
          } else if (Immutable.List.isList(features)) {
            return features;
          } else {
            console.error(
              "Found unexpected format of features (should be Array, " +
                "Immutable.List, or a single tag as string): " +
                JSON.stringify(features)
            );
            return undefined;
          }
        },
      },
      rent: {
        ...details.title,
        identifier: "rent",
        label: "Rent per month",
        parseToRDF: function({ value }) {
          if (!value) {
            return { "won:rent": undefined }; // FIXME: won:rent does not exist
          }
          return { "won:rent": value };
        },
        parseFromRDF: function(jsonLDImm) {
          return jsonLDImm && jsonLDImm.get("won:rent");
        },
      },
    },
  },
  offerRent: {
    identifier: "offerRent",
    label: "Rent a place out",
    icon: "#ico36_uc_custom", // TODO: replace this icon
    draft: {
      ...emptyDraft,
      is: {
        title: "For Rent",
        tags: "for-rent",
      },
    },
    isDetails: {
      title: { ...details.title },
      description: { ...details.description },
      location: { ...details.location },
      floorSize: {
        ...details.title,
        identifier: "floorSize",
        label: "Floor size in square meters",
        icon: "#ico36_plus_circle", // TODO: better icon
        parseToRDF: function({ value }) {
          if (!value) {
            return { "s:floorSize": undefined };
          }
          return { "s:floorSize": value };
        },
        parseFromRDF: function(jsonLDImm) {
          const floorSize = jsonLDImm && jsonLDImm.get("s:floorSize");
          if (!floorSize) {
            return undefined;
          } else {
            return floorSize + "m2";
          }
        },
      },
      numberOfRooms: {
        ...details.title,
        identifier: "numberOfRooms",
        label: "Number of Rooms",
        icon: "#ico36_plus_circle", // TODO: better icon
        parseToRDF: function({ value }) {
          if (!value) {
            return { "s:numberOfRooms": undefined };
          }
          return { "s:numberOfRooms": value };
        },
        parseFromRDF: function(jsonLDImm) {
          const numberOfRooms = jsonLDImm && jsonLDImm.get("s:numberOfRooms");
          if (!numberOfRooms) {
            return undefined;
          } else {
            return numberOfRooms;
          }
        },
      },
      features: {
        ...details.tags,
        identifier: "features",
        label: "Features",
        icon: "#ico36_plus_circle", //TODO: better icon
        parseToRDF: function({ value }) {
          if (!value) {
            return { "s:amenityFeature": undefined };
          } else {
            return {
              "s:amenityFeature": {
                "@type": "s:LocationFeatureSpecification",
                "s:name": value,
              },
            };
          }
        },
        parseFromRDF: function(jsonLDImm) {
          const amenityFeature = jsonLDImm && jsonLDImm.get("s:amenityFeature");
          const features = amenityFeature && amenityFeature.get("s:name");

          if (!features) {
            return undefined;
          } else if (is("String", features)) {
            return Immutable.fromJS([features]);
          } else if (is("Array", features)) {
            return Immutable.fromJS(features);
          } else if (Immutable.List.isList(features)) {
            return features;
          } else {
            console.error(
              "Found unexpected format of features (should be Array, " +
                "Immutable.List, or a single tag as string): " +
                JSON.stringify(features)
            );
            return undefined;
          }
        },
      },
      rent: {
        ...details.title,
        identifier: "rent",
        label: "Rent per month",
        parseToRDF: function({ value }) {
          if (!value) {
            return { "won:rent": undefined }; // FIXME: won:rent does not exist
          }
          return { "won:rent": value };
        },
        parseFromRDF: function(jsonLDImm) {
          return jsonLDImm && jsonLDImm.get("won:rent");
        },
      },
    },
    seeksDetails: undefined,
  },
  // searchBuy: {},
  // offerBuy: {},
};

/*const otherUseCases = {
  // taxi: {},
  // transport: {},
  // job: {},
};*/

export const useCases = {
  ...socialUseCases,
  ...professionalUseCases,
  ...infoUseCases,
  ...realEstateUseCases,
  ...allDetailsUseCase,
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
  realEstate: {
    identifier: "realestategroup",
    label: "Real Estate",
    icon: undefined,
    useCases: { ...realEstateUseCases },
  },
  other: {
    identifier: "othergroup",
    label: "Something else",
    icon: undefined,
    useCases: { ...allDetailsUseCase },
  },
};
