import {
  is,
  isValidNumber,
  get,
  getFromJsonLd,
  getInFromJsonLd,
  getIn,
  isValidDate,
} from "../app/utils.js";
import Immutable from "immutable";
import { details, abstractDetails } from "detailDefinitions";
import { findLatestIntervallEndInJsonLd } from "../app/won-utils.js";
import won from "../app/won-es6.js";
import {
  filterInVicinity,
  filterFloorSizeRange,
  filterNumOfRoomsRange,
  filterRentRange,
  concatenateFilters,
  filterAboutTime,
  sparqlQuery,
} from "../app/sparql-builder-utils.js";

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
 *
 * SUPPLYING A QUERY
 * If it is necessary to fine-tune the matching behaviour of a usecase, a custom SPARQL query can be added to the definition.
 * Exmaple:
 * useCase: {
 *    ...,
 *    generateQuery: (draft, resultName) => {
 *        new SparqlParser.parse(`
 *            PREFIX won: <http://purl.org/webofneeds/model#>
 *
 *            SELECT ${resultName} WHERE {
 *                ${resultName} a won:Need .
 *            }
 *        `)
 *    }
 * }
 *
 * A `generateQuery` is a function that takes the current need draft and the name of the result variable and returns a sparqljs json representation of the query. This can be created either programmatically or by using the Parser class from the sparqljs library.
 *
 * The query needs to be a SELECT query and select only the resultName variable.
 * This will be automatically enforced by the need builder.
 */

const allDetailsUseCase = {
  allDetails: {
    identifier: "allDetails",
    label: "New custom post",
    icon: "#ico36_uc_custom",
    doNotMatchAfter: findLatestIntervallEndInJsonLd,
    draft: { ...emptyDraft },
    isDetails: details,
    seeksDetails: details,
  },
};

const skillsDetail = {
  ...details.tags,
  identifier: "skills",
  label: "Skills",
  icon: "#ico36_detail_skill",
  placeholder: "e.g. RDF, project-management",
  parseToRDF: function({ value }) {
    if (!value) {
      return { "s:knowsAbout": undefined };
    }
    return { "s:knowsAbout": value };
  },
  parseFromRDF: function(jsonLDImm) {
    return won.parseListFrom(jsonLDImm, ["s:knowsAbout"], "xsd:string");
  },
};

const interestsDetail = {
  ...details.tags,
  identifier: "interests",
  label: "Interests",
  icon: "#ico36_detail_interests",
  placeholder: "e.g. food, cats",
  parseToRDF: function({ value }) {
    if (!value) {
      return { "foaf:topic_interest": undefined };
    }
    return { "foaf:topic_interest": value };
  },
  parseFromRDF: function(jsonLDImm) {
    return won.parseListFrom(jsonLDImm, "foaf:topic_interest");
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
    doNotMatchAfter: findLatestIntervallEndInJsonLd,
    draft: {
      ...emptyDraft,
      is: {
        title: "I'm up for breakfast! Any plans?",
        tags: ["breakfast"],
      },
      seeks: { title: "breakfast" },
      searchString: "breakfast",
    },
    isDetails: {
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
    doNotMatchAfter: findLatestIntervallEndInJsonLd,
    draft: {
      ...emptyDraft,
      is: {
        title: "I'm up for lunch! Any plans?",
        tags: ["lunch"],
      },
      searchString: "lunch",
    },
    isDetails: {
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
    doNotMatchAfter: findLatestIntervallEndInJsonLd,
    draft: {
      ...emptyDraft,
      is: {
        title: "I'm up for partying! Any plans?",
        tags: ["afterparty"],
      },
      searchString: "afterparty",
    },
    isDetails: {
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
    doNotMatchAfter: findLatestIntervallEndInJsonLd,
    draft: {
      ...emptyDraft,
      is: { tags: ["sightseeing"] },
      searchString: "sightseeing",
    },
    isDetails: {
      title: { ...details.title },
      description: { ...details.description },
      fromDatetime: { ...details.fromDatetime },
      throughDatetime: { ...details.throughDatetime },
      location: { ...details.location },
      interests: { ...interestsDetail },
    },
  },
};

const complainUseCases = {
  complain: {
    identifier: "complain",
    label: "Complain about something",
    icon: "#ico36_uc_wtf",
    doNotMatchAfter: findLatestIntervallEndInJsonLd,
    draft: {
      ...emptyDraft,
      is: {
        title: "WTF?",
        tags: ["wtf"],
      },
      seeks: {},
      searchString: "wtf",
    },
    isDetails: {
      title: { ...details.title },
      description: { ...details.description },
      location: { ...details.location },
      tags: { ...details.tags },
    },
    seeksDetails: undefined,
  },
  handleComplaints: {
    identifier: "handleComplaints",
    label: "Handle complaints",
    icon: "#ico36_uc_wtf_interest",
    doNotMatchAfter: findLatestIntervallEndInJsonLd,
    draft: {
      ...emptyDraft,
      is: {
        title: "I'll discuss complaints",
      },
      seeks: {},
      searchString: "wtf",
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

const professionalUseCases = {
  getToKnow: {
    identifier: "getToKnow",
    label: "Find people",
    icon: "#ico36_uc_find_people",
    doNotMatchAfter: findLatestIntervallEndInJsonLd,
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
    doNotMatchAfter: findLatestIntervallEndInJsonLd,
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
    doNotMatchAfter: findLatestIntervallEndInJsonLd,
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
    doNotMatchAfter: findLatestIntervallEndInJsonLd,
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
    doNotMatchAfter: findLatestIntervallEndInJsonLd,
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
    doNotMatchAfter: findLatestIntervallEndInJsonLd,
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
    doNotMatchAfter: findLatestIntervallEndInJsonLd,
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
};

const realEstateFloorSizeDetail = {
  ...abstractDetails.number,
  identifier: "floorSize",
  label: "Floor size in square meters",
  icon: "#ico36_detail_floorsize",
  parseToRDF: function({ value }) {
    if (!isValidNumber(value)) {
      return { "s:floorSize": undefined };
    } else {
      return {
        "s:floorSize": {
          "@type": "s:QuantitativeValue",
          "s:value": [{ "@value": value, "@type": "xsd:float" }],
          "s:unitCode": "MTK",
        },
      };
    }
  },
  parseFromRDF: function(jsonLDImm) {
    const fs = won.parseFrom(
      jsonLDImm,
      ["s:floorSize", "s:value"],
      "xsd:float"
    );
    const unit = getInFromJsonLd(
      jsonLDImm,
      ["s:floorSize", "s:unitCode"],
      won.defaultContext
    );
    if (!fs) {
      return undefined;
    } else {
      if (unit === "MTK") {
        return fs + "m²";
      } else if (unit === "FTK") {
        return fs + "sq ft";
      } else if (unit === "YDK") {
        return fs + "sq yd";
      } else if (!unit) {
        return fs + " (no unit specified)";
      }
      return fs + " " + unit;
    }
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (isValidNumber(value)) {
      return (includeLabel ? this.label + ": " + value : value) + "m²";
    }
    return undefined;
  },
};

const realEstateNumberOfRoomsDetail = {
  ...abstractDetails.number,
  identifier: "numberOfRooms",
  label: "Number of Rooms",
  icon: "#ico36_detail_number-of-rooms",
  parseToRDF: function({ value }) {
    if (!isValidNumber(value)) {
      return { "s:numberOfRooms": undefined };
    } else {
      return { "s:numberOfRooms": [{ "@value": value, "@type": "xsd:float" }] };
    }
  },
  parseFromRDF: function(jsonLDImm) {
    return won.parseFrom(jsonLDImm, ["s:numberOfRooms"], "xsd:float");
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (isValidNumber(value)) {
      return (includeLabel ? this.label + ": " + value : value) + " Rooms";
    } else {
      return undefined;
    }
  },
};

const realEstateNumberOfRoomsRangeDetail = {
  ...abstractDetails.range,
  identifier: "numberOfRoomsRange",
  label: "Number of Rooms",
  minLabel: "From",
  maxLabel: "To",
  icon: "#ico36_detail_number-of-rooms",
  parseToRDF: function({ value }) {
    if (!value) {
      return {};
    }
    return {
      "sh:property": {
        "sh:path": "s:numberOfRooms",
        "sh:minInclusive": value.min && [
          { "@value": value.min, "@type": "xsd:float" },
        ],
        "sh:maxInclusive": value.max && [
          { "@value": value.max, "@type": "xsd:float" },
        ],
      },
    };
  },
  parseFromRDF: function(jsonLDImm) {
    let properties = getFromJsonLd(
      jsonLDImm,
      "sh:property",
      won.defaultContext
    );
    if (!properties) return undefined;

    if (!Immutable.List.isList(properties))
      properties = Immutable.List.of(properties);

    const numberOfRooms = properties.find(
      property =>
        getFromJsonLd(property, "sh:path", won.defaultContext) ===
        "s:numberOfRooms"
    );
    const minNumberOfRooms = getFromJsonLd(
      numberOfRooms,
      "sh:minInclusive",
      won.defaultContext
    );
    const maxNumberOfRooms = getFromJsonLd(
      numberOfRooms,
      "sh:maxInclusive",
      won.defaultContext
    );

    if (minNumberOfRooms || maxNumberOfRooms) {
      return Immutable.fromJS({
        min: minNumberOfRooms,
        max: maxNumberOfRooms,
      });
    } else {
      return undefined;
    }
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      return (
        (includeLabel ? `${this.label}: ` : "") +
        minMaxLabel(value.min, value.max) +
        " Room(s)"
      );
    }
    return undefined;
  },
};

function minMaxLabel(min, max) {
  const min_ = Number.parseFloat(min);
  const max_ = Number.parseFloat(max);
  const minIsNumber = isValidNumber(min_);
  const maxIsNumber = isValidNumber(max_);
  if (minIsNumber && maxIsNumber) {
    return min_ + "–" + max_;
  } else if (minIsNumber) {
    return "At least " + min_;
  } else if (maxIsNumber) {
    return "At most " + max_;
  } else {
    return "Unspecified number of ";
  }
}

const realEstateFloorSizeRangeDetail = {
  ...abstractDetails.range,
  identifier: "floorSizeRange",
  label: "Floor size in square meters",
  minLabel: "From",
  maxLabel: "To",
  icon: "#ico36_detail_floorsize",
  parseToRDF: function({ value }) {
    if (!value) {
      return {};
    }
    return {
      "sh:property": {
        "sh:path": "s:floorSize",
        "sh:minInclusive": value.min && [
          { "@value": value.min, "@type": "xsd:float" },
        ],
        "sh:maxInclusive": value.max && [
          { "@value": value.max, "@type": "xsd:float" },
        ],
      },
    };
  },
  parseFromRDF: function(jsonLDImm) {
    let properties = getFromJsonLd(
      jsonLDImm,
      "sh:property",
      won.defaultContext
    );
    if (!properties) return undefined;

    if (!Immutable.List.isList(properties))
      properties = Immutable.List.of(properties);

    const floorSize = properties.find(
      property =>
        getFromJsonLd(property, "sh:path", won.defaultContext) === "s:floorSize"
    );

    const minFloorSize = getFromJsonLd(
      floorSize,
      "sh:minInclusive",
      won.defaultContext
    );
    const maxFloorSize = getFromJsonLd(
      floorSize,
      "sh:maxInclusive",
      won.defaultContext
    );

    if (minFloorSize || maxFloorSize) {
      return Immutable.fromJS({
        min: minFloorSize && minFloorSize + "m²",
        max: maxFloorSize && maxFloorSize + "m²",
      });
    } else {
      return undefined;
    }
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      return (
        (includeLabel ? `${this.label}: ` : "") +
        minMaxLabel(value.min, value.max) +
        "m²"
      );
    }
    return undefined;
  },
};

const realEstateFeaturesDetail = {
  ...details.tags,
  identifier: "features",
  label: "Features",
  icon: "#ico36_detail_feature",
  placeholder: "e.g. balcony, bathtub",
  parseToRDF: function({ value }) {
    if (!value || !is("Array", value) || value.length === 0) {
      return { "s:amenityFeature": undefined };
    } else {
      const features = value.map(feature => ({
        "@type": "s:LocationFeatureSpecification",
        "s:value": { "@value": feature, "@type": "s:Text" },
      }));
      return {
        "s:amenityFeature": features,
      };
    }
  },
  parseFromRDF: function(jsonLDImm) {
    return won.parseListFrom(
      jsonLDImm,
      ["s:amenityFeature"], //, "s:value"],
      "s:Text"
    );
  },
};

const realEstateRentDetail = {
  ...details.price,
  identifier: "rent",
  label: "Rent",
  icon: "#ico36_detail_rent",
  currency: [{ value: "EUR", label: "€", default: true }],
  unitCode: [{ value: "MON", label: "per month", default: true }],
  parseFromRDF: function() {
    //That way we can make sure that parsing fromRDF is made only by the price detail itself
    return undefined;
  },
};

const realEstateRentRangeDetail = {
  ...details.pricerange,
  identifier: "rentRange",
  label: "Rent in EUR/month",
  minLabel: "From",
  maxLabel: "To",
  currency: [{ value: "EUR", label: "€", default: true }],
  unitCode: [{ value: "MON", label: "per month", default: true }],
  icon: "#ico36_detail_rent",
  parseFromRDF: function() {
    return undefined;
  },
};

const realEstateUseCases = {
  searchRent: {
    identifier: "searchRent",
    label: "Find a place to rent",
    icon: "#ico36_uc_realestate",
    doNotMatchAfter: findLatestIntervallEndInJsonLd,
    draft: {
      ...emptyDraft,
      seeks: {
        type: "won:RealEstateRentOffer",
        tags: ["RentOutRealEstate"],
      },
      is: {
        type: "won:RealEstateRentDemand",
        tags: ["SearchRealEstateToRent"],
      },
    },
    isDetails: undefined,
    seeksDetails: {
      location: { ...details.location },
      floorSizeRange: { ...realEstateFloorSizeRangeDetail },
      numberOfRoomsRange: { ...realEstateNumberOfRoomsRangeDetail },
      features: { ...realEstateFeaturesDetail },
      rentRange: { ...realEstateRentRangeDetail },
    },
    generateQuery: (draft, resultName) => {
      const seeksBranch = draft && draft.seeks;
      const rentRange = seeksBranch && seeksBranch.rentRange;
      const floorSizeRange = seeksBranch && seeksBranch.floorSizeRange;
      const numberOfRoomsRange = seeksBranch && seeksBranch.numberOfRoomsRange;
      const location = seeksBranch && seeksBranch.location;

      const filters = [
        {
          // to select is-branch
          prefixes: {
            won: won.defaultContext["won"],
          },
          operations: [
            `${resultName} a won:Need.`,
            `${resultName} won:is ?is.`,
            location && "?is won:hasLocation ?location.",
          ],
        },
        rentRange &&
          filterRentRange(
            "?is",
            rentRange.min,
            rentRange.max,
            rentRange.currency
          ),

        floorSizeRange &&
          filterFloorSizeRange("?is", floorSizeRange.min, floorSizeRange.max),

        numberOfRoomsRange &&
          filterNumOfRoomsRange(
            "?is",
            numberOfRoomsRange.min,
            numberOfRoomsRange.max
          ),

        filterInVicinity("?location", location),
      ];

      const concatenatedFilter = concatenateFilters(filters);

      return sparqlQuery({
        prefixes: concatenatedFilter.prefixes,
        selectDistinct: resultName,
        where: concatenatedFilter.operations,
        orderBy: [
          {
            order: "ASC",
            variable: "?location_geoDistance",
          },
        ],
      });
    },
  },
  offerRent: {
    identifier: "offerRent",
    label: "Rent a place out",
    icon: "#ico36_uc_realestate",
    doNotMatchAfter: findLatestIntervallEndInJsonLd,
    draft: {
      ...emptyDraft,
      is: {
        type: "won:RealEstateRentOffer",
        title: "For Rent",
        tags: ["RentOutRealEstate"],
      },
      seeks: {
        type: "won:RealEstateRentDemand",
        tags: ["SearchRealEstateToRent"],
      },
    },
    isDetails: {
      title: { ...details.title },
      description: { ...details.description },
      location: {
        ...details.location,
        mandatory: true,
      },
      floorSize: {
        ...realEstateFloorSizeDetail,
        mandatory: true,
      },
      numberOfRooms: {
        ...realEstateNumberOfRoomsDetail,
        mandatory: true,
      },
      features: { ...realEstateFeaturesDetail },
      rent: {
        ...realEstateRentDetail,
        mandatory: true,
      },
    },
    seeksDetails: undefined,
  },
  // searchBuy: {},
  // offerBuy: {},
};

const transportUseCases = {
  transportDemand: {
    identifier: "transportDemand",
    label: "Send something",
    icon: "#ico36_uc_transport_demand",
    doNotMatchAfter: findLatestIntervallEndInJsonLd,
    draft: {
      ...emptyDraft,
      is: { title: "Want to send something" },
    },
    isDetails: {
      title: { ...details.title },
      content: {
        ...details.description,
        identifier: "content",
        label: "Content",
        placeholder: "Provide information about what should be transported",
        parseToRDF: function({ value }) {
          if (!value) {
            return { "s:name": undefined };
          } else {
            return { "@type": "s:Product", "s:name": value };
          }
        },
        parseFromRDF: function(jsonLDImm) {
          const content = won.parseFrom(jsonLDImm, ["s:name"], "xsd:string");
          const type = get(jsonLDImm, "@type");
          if (content && type === "s:Product") {
            return content;
          } else {
            return undefined;
          }
        },
      },
      weight: {
        ...abstractDetails.number,
        identifier: "weight",
        label: "Weight in kg",
        icon: "#ico36_detail_weight",
        parseToRDF: function({ value }) {
          if (!isValidNumber(value)) {
            return { "s:weight": undefined };
          } else {
            return {
              "@type": "s:Product",
              "s:weight": {
                "@type": "s:QuantitativeValue",
                "s:value": [{ "@value": value, "@type": "xsd:float" }],
                "s:unitCode": "KGM",
              },
            };
          }
        },
        parseFromRDF: function(jsonLDImm) {
          const w = won.parseFrom(
            jsonLDImm,
            ["s:weight", "s:value"],
            "xsd:float"
          );
          const unit = getInFromJsonLd(
            jsonLDImm,
            ["s:weight", "s:unitCode"],
            won.defaultContext
          );

          if (!w) {
            return undefined;
          } else {
            if (unit === "KGM") {
              return w + "kg";
            } else if (unit === "GRM") {
              return w + "g";
            } else if (!unit) {
              return w + " (no unit specified)";
            }
            return w + " " + unit;
          }
        },
        generateHumanReadable: function({ value, includeLabel }) {
          if (isValidNumber(value)) {
            return (includeLabel ? this.label + ": " + value : value) + "kg";
          }
          return undefined;
        },
      },
      length: {
        ...abstractDetails.number,
        identifier: "length",
        label: "Length in cm",
        icon: "#ico36_detail_measurement",
        parseToRDF: function({ value }) {
          if (!isValidNumber(value)) {
            return { "s:length": undefined };
          } else {
            return {
              "@type": "s:Product",
              "s:length": {
                "@type": "s:QuantitativeValue",
                "s:value": [{ "@value": value, "@type": "xsd:float" }],
                "s:unitCode": "CMT",
              },
            };
          }
        },
        parseFromRDF: function(jsonLDImm) {
          const l = won.parseFrom(
            jsonLDImm,
            ["s:length", "s:value"],
            "xsd:float"
          );
          const unit = getInFromJsonLd(
            jsonLDImm,
            ["s:length", "s:unitCode"],
            won.defaultContext
          );

          if (!l) {
            return undefined;
          } else {
            if (unit === "CMT") {
              return l + "cm";
            } else if (unit === "MTR") {
              return l + "m";
            } else if (!unit) {
              return l + " (no unit specified)";
            }
            return l + " " + unit;
          }
        },
        generateHumanReadable: function({ value, includeLabel }) {
          if (isValidNumber(value)) {
            return (includeLabel ? this.label + ": " + value : value) + "cm";
          }
          return undefined;
        },
      },
      width: {
        ...abstractDetails.number,
        identifier: "width",
        label: "Width in cm",
        icon: "#ico36_detail_measurement",
        parseToRDF: function({ value }) {
          if (!isValidNumber(value)) {
            return { "s:width": undefined };
          } else {
            return {
              "@type": "s:Product",
              "s:width": {
                "@type": "s:QuantitativeValue",
                "s:value": [{ "@value": value, "@type": "xsd:float" }],
                "s:unitCode": "CMT",
              },
            };
          }
        },
        parseFromRDF: function(jsonLDImm) {
          const w = won.parseFrom(
            jsonLDImm,
            ["s:width", "s:value"],
            "xsd:float"
          );
          const unit = getInFromJsonLd(
            jsonLDImm,
            ["s:width", "s:unitCode"],
            won.defaultContext
          );

          if (!w) {
            return undefined;
          } else {
            if (unit === "CMT") {
              return w + "cm";
            } else if (unit === "MTR") {
              return w + "m";
            } else if (!unit) {
              return w + " (no unit specified)";
            }
            return w + " " + unit;
          }
        },
        generateHumanReadable: function({ value, includeLabel }) {
          if (isValidNumber(value)) {
            return (includeLabel ? this.label + ": " + value : value) + "cm";
          }
          return undefined;
        },
      },
      height: {
        ...abstractDetails.number,
        identifier: "height",
        label: "Height in cm",
        icon: "#ico36_detail_measurement",
        parseToRDF: function({ value }) {
          if (!isValidNumber(value)) {
            return { "s:height": undefined };
          } else {
            return {
              "@type": "s:Product",
              "s:height": {
                "@type": "s:QuantitativeValue",
                "s:value": [{ "@value": value, "@type": "xsd:float" }],
                "s:unitCode": "CMT",
              },
            };
          }
        },
        parseFromRDF: function(jsonLDImm) {
          const h = won.parseFrom(
            jsonLDImm,
            ["s:height", "s:value"],
            "xsd:float"
          );
          const unit = getInFromJsonLd(
            jsonLDImm,
            ["s:height", "s:unitCode"],
            won.defaultContext
          );

          if (!h) {
            return undefined;
          } else {
            if (unit === "CMT") {
              return h + "cm";
            } else if (unit === "MTR") {
              return h + "m";
            } else if (!unit) {
              return h + " (no unit specified)";
            }
            return h + " " + unit;
          }
        },
        generateHumanReadable: function({ value, includeLabel }) {
          if (isValidNumber(value)) {
            return (includeLabel ? this.label + ": " + value : value) + "cm";
          }
          return undefined;
        },
      },
      tags: { ...details.tags },
    },
    seeksDetails: {
      travelAction: { ...details.travelAction },
    },
  },
  transportOffer: {
    identifier: "transportOffer",
    label: "Offer goods transport",
    icon: "#ico36_uc_transport_offer",
    doNotMatchAfter: findLatestIntervallEndInJsonLd,
    draft: {
      ...emptyDraft,
      is: { title: "Transportation offer" },
      searchString: "transport", // TODO: replace this with a query
    },
    isDetails: {
      title: { ...details.title },
      location: { ...details.location },
    },
    seeksDetails: {
      tags: { ...details.tags },
      description: { ...details.description },
    },
    // generateQuery: (draft, resultName) => {
    //   const filterStrings = [];
    //   const prefixes = {
    //     s: won.defaultContext["s"],
    //     won: won.defaultContext["won"],
    //   };

    //   let queryTemplate =
    //     `
    //     ${prefixesString(prefixes)}
    //     SELECT DISTINCT ${resultName}
    //     WHERE {
    //     ${resultName}
    //       won:is ?is.
    //       ${filterStrings && filterStrings.join(" ")}
    //     }` + (location ? `ORDER BY ASC(?geoDistance)` : "");
    //   return new SparqlParser().parse(queryTemplate);
    // },
  },
};

const mobilityUseCases = {
  liftDemand: {
    identifier: "liftDemand",
    label: "Need a Lift",
    icon: "#ico36_uc_route_demand",
    doNotMatchAfter: findLatestIntervallEndInJsonLd,
    draft: {
      ...emptyDraft,
      is: { title: "Need a lift", tags: "search-lift" },
      searchString: "offer-lift",
    },
    // TODO: amount of people? other details?
    isDetails: {
      title: { ...details.title },
      description: { ...details.description },
    },
    seeksDetails: {
      fromDatetime: { ...details.fromDatetime },
      travelAction: { ...details.travelAction },
    },
    generateQuery: (draft, resultName) => {
      const fromLocation = getIn(draft, [
        "seeks",
        "travelAction",
        "fromLocation",
      ]);
      const toLocation = getIn(draft, ["seeks", "travelAction", "toLocation"]);

      const baseFilter = {
        prefixes: {
          won: won.defaultContext["won"],
        },
        operations: [`${resultName} a won:Need.`, `${resultName} won:is ?is.`],
      };

      const locationFilter = filterInVicinity(
        "?location",
        fromLocation,
        /*radius=*/ 100
      );
      const fromLocationFilter = filterInVicinity(
        "?fromLocation",
        fromLocation,
        /*radius=*/ 100
      );
      const toLocationFilter = filterInVicinity(
        "?toLocation",
        toLocation,
        /*radius=*/ 100
      );

      const union = operations => {
        if (!operations || operations.length === 0) {
          return "";
        } else {
          return "{" + operations.join("} UNION {") + "}";
        }
      };
      const filterAndJoin = (arrayOfStrings, seperator) =>
        arrayOfStrings.filter(str => str).join(seperator);

      const locationFilters = {
        prefixes: locationFilter.prefixes,
        operations: union([
          filterAndJoin(
            [
              fromLocation &&
                `${resultName} a <http://dbpedia.org/page/Ridesharing>. ?is won:travelAction/s:fromLocation ?fromLocation. `,
              fromLocation && fromLocationFilter.operations.join(" "),
              toLocation && "?is won:travelAction/s:toLocation ?toLocation.",
              toLocation && toLocationFilter.operations.join(" "),
            ],
            " "
          ),
          filterAndJoin(
            [
              location &&
                `${resultName} a s:TaxiService . ?is won:hasLocation ?location .`,
              location && locationFilter.operations.join(" "),
            ],
            " "
          ),
        ]),
      };

      const concatenatedFilter = concatenateFilters([
        baseFilter,
        locationFilters,
      ]);

      return sparqlQuery({
        prefixes: concatenatedFilter.prefixes,
        selectDistinct: resultName,
        where: concatenatedFilter.operations,
        orderBy: [
          {
            order: "ASC",
            variable: "?location_geoDistance",
          },
        ],
      });
    },
  },
  taxiOffer: {
    identifier: "taxiOffer",
    label: "Offer Taxi Service",
    icon: "#ico36_uc_taxi_offer",
    doNotMatchAfter: findLatestIntervallEndInJsonLd,
    draft: {
      ...emptyDraft,
      is: { title: "Taxi", type: "s:TaxiService" },
    },
    isDetails: {
      title: { ...details.title },
      description: { ...details.description },
      location: { ...details.location },
    },
    generateQuery: (draft, resultName) => {
      const location = getIn(draft, ["is", "location"]);
      const filters = [
        {
          // to select seeks-branch
          prefixes: {
            won: won.defaultContext["won"],
          },
          operations: [
            `${resultName} a won:Need.`,
            `${resultName} won:seeks ?seeks.`,
            location && "?seeks won:travelAction/s:fromLocation ?location.",
          ],
        },

        filterInVicinity("?location", location, /*radius=*/ 100),
      ];

      const concatenatedFilter = concatenateFilters(filters);

      return sparqlQuery({
        prefixes: concatenatedFilter.prefixes,
        selectDistinct: resultName,
        where: concatenatedFilter.operations,
        orderBy: [
          {
            order: "ASC",
            variable: "?location_geoDistance",
          },
        ],
      });
    },
  },
  rideShareOffer: {
    identifier: "rideShareOffer",
    label: "Offer to Share a Ride",
    icon: "#ico36_uc_taxi_offer",
    doNotMatchAfter: findLatestIntervallEndInJsonLd,
    draft: {
      ...emptyDraft,
      is: {
        title: "Share a Ride",
        type: "http://dbpedia.org/page/Ridesharing",
      },
    },
    isDetails: {
      title: { ...details.title },
      description: { ...details.description },
      fromDatetime: { ...details.fromDatetime },
      throughDatetime: { ...details.throughDatetime },
      travelAction: { ...details.travelAction },
    },
    generateQuery: (draft, resultName) => {
      const toLocation = getIn(draft, ["is", "travelAction", "toLocation"]);
      const fromLocation = getIn(draft, ["is", "travelAction", "fromLocation"]);

      const fromTime = getIn(draft, ["is", "fromDatetime"]);
      const filters = [
        {
          // to select seeks-branch
          prefixes: {
            won: won.defaultContext["won"],
          },
          operations: [
            `${resultName} a won:Need.`,
            `${resultName} won:seeks ?seeks.`,
            fromLocation &&
              "?seeks won:travelAction/s:fromLocation ?fromLocation.",
            toLocation && "?seeks won:travelAction/s:toLocation ?toLocation.",
            isValidDate(fromTime) && "?seeks s:validFrom ?starttime",
          ],
        },

        filterInVicinity("?fromLocation", fromLocation, /*radius=*/ 100),

        filterInVicinity("?toLocation", toLocation, /*radius=*/ 100),

        filterAboutTime("?starttime", fromTime, 12 /* hours before and after*/),
      ];

      const concatenatedFilter = concatenateFilters(filters);

      return sparqlQuery({
        prefixes: concatenatedFilter.prefixes,
        selectDistinct: resultName,
        where: concatenatedFilter.operations,
        orderBy: [
          {
            order: "ASC",
            variable: "?fromLocation_geoDistance",
          },
        ],
      });
    },
  },
};

/**
 * band musician use cases
 */
const instrumentsDetail = {
  ...details.tags,
  identifier: "instruments",
  label: "Instruments",
  icon: "#ico36_detail_instrument",
  placeholder: "e.g. Guitar, Vocals",
  parseToRDF: function({ value }) {
    if (!value) {
      return { "won:instruments": undefined };
    }
    return { "won:instruments": value };
  },
  parseFromRDF: function(jsonLDImm) {
    const instruments = jsonLDImm && jsonLDImm.get("won:instruments");
    if (!instruments) {
      return undefined;
    } else if (is("String", instruments)) {
      return Immutable.fromJS([instruments]);
    } else if (is("Array", instruments)) {
      return Immutable.fromJS(instruments);
    } else if (Immutable.List.isList(instruments)) {
      return instruments; // id; it is already in the format we want
    } else {
      console.error(
        "Found unexpected format of instruments (should be Array, " +
          "Immutable.List, or a single tag as string): " +
          JSON.stringify(instruments)
      );
      return undefined;
    }
  },
};

const genresDetail = {
  ...details.tags,
  identifier: "genres",
  label: "Genres",
  icon: "#ico36_detail_genre",
  placeholder: "e.g. Rock, Pop",
  parseToRDF: function({ value }) {
    if (!value) {
      return { "won:genres": undefined };
    }
    return { "won:genres": value };
  },
  parseFromRDF: function(jsonLDImm) {
    const genres = jsonLDImm && jsonLDImm.get("won:genres");
    if (!genres) {
      return undefined;
    } else if (is("String", genres)) {
      return Immutable.fromJS([genres]);
    } else if (is("Array", genres)) {
      return Immutable.fromJS(genres);
    } else if (Immutable.List.isList(genres)) {
      return genres; // id; it is already in the format we want
    } else {
      console.error(
        "Found unexpected format of genres (should be Array, " +
          "Immutable.List, or a single tag as string): " +
          JSON.stringify(genres)
      );
      return undefined;
    }
  },
};

const musicianUseCases = {
  findBand: {
    identifier: "findBand",
    label: "Find Band",
    icon: "#ico36_uc_find_band",
    doNotMatchAfter: findLatestIntervallEndInJsonLd,
    draft: {
      ...emptyDraft,
      is: {
        title: "I'm looking for a band!",
        //tags: ["musician", "band"],
      },
      searchString: "band",
    },
    isDetails: {
      title: { ...details.title },
      description: { ...details.description },
      instruments: {
        ...instrumentsDetail,
        //mandatory: true,
      },
    },
    seeksDetails: {
      description: { ...details.description },
      genres: { ...genresDetail },
      location: { ...details.location },
    },
  },
  findMusician: {
    identifier: "findMusician",
    label: "Find Musician",
    icon: "#ico36_uc_find_musician",
    doNotMatchAfter: findLatestIntervallEndInJsonLd,
    draft: {
      ...emptyDraft,
      is: {
        title: "Looking for a Musician!",
        //tags: ["band", "musician"],
      },
      searchString: "musician",
    },
    isDetails: {
      title: { ...details.title },
      description: { ...details.description },
      genres: { ...genresDetail },
      location: { ...details.location },
    },
    seeksDetails: {
      description: { ...details.description },
      instruments: {
        ...instrumentsDetail,
        //mandatory: true,
      },
    },
  },
  findRehearsalRoom: {
    identifier: "findRehearsalRoom",
    label: "Find Rehearsal Room",
    icon: "#ico36_uc_realestate",
    doNotMatchAfter: findLatestIntervallEndInJsonLd,
    draft: {
      ...emptyDraft,
      is: {
        title: "Looking for Rehearsal Room!",
        tags: ["SearchRehearsal"],
      },
      seeks: {
        tags: ["OfferRehearsal"],
      },
      searchString: "Rehearsal Room",
    },
    isDetails: undefined,
    seeksDetails: {
      location: { ...details.location },
      floorSizeRange: { ...realEstateFloorSizeRangeDetail },
      features: {
        ...realEstateFeaturesDetail,
        placeholder: "e.g. PA, Drumkit",
      },
      rentRange: { ...realEstateRentRangeDetail },
      fromDatetime: { ...details.fromDatetime },
      throughDatetime: { ...details.throughDatetime },
    },
  },
  offerRehearsalRoom: {
    identifier: "OfferRehearsalRoom",
    label: "Offer Rehearsal Room",
    icon: "#ico36_uc_realestate",
    doNotMatchAfter: findLatestIntervallEndInJsonLd,
    draft: {
      ...emptyDraft,
      is: {
        title: "Offer Rehearsal Room!",
        tags: ["OfferRehearsal"],
      },
      seeks: {
        tags: ["SearchRehearsal"],
      },
    },
    isDetails: {
      title: { ...details.title },
      description: { ...details.description },
      location: {
        ...details.location,
        mandatory: true,
      },
      floorSize: {
        ...realEstateFloorSizeDetail,
        mandatory: true,
      },
      numberOfRooms: {
        ...realEstateNumberOfRoomsDetail,
        mandatory: true,
      },
      features: {
        ...realEstateFeaturesDetail,
        placeholder: "e.g. PA, Drumkit",
      },
      rent: {
        ...realEstateRentDetail,
        mandatory: true,
      },
      fromDatetime: { ...details.fromDatetime },
      throughDatetime: { ...details.throughDatetime },
    },

    seeksDetails: undefined,
  },
};

export const useCaseGroups = {
  complain: {
    identifier: "complaingroup",
    label: "Complaints",
    icon: undefined,
    useCases: { ...complainUseCases },
  },
  transport: {
    identifier: "transportgroup",
    label: "Transport and Delivery",
    icon: undefined,
    useCases: { ...transportUseCases },
  },
  mobility: {
    identifier: "mobilitygroup",
    label: "Personal Mobility",
    icon: undefined,
    useCases: { ...mobilityUseCases },
  },
  realEstate: {
    identifier: "realestategroup",
    label: "Real Estate",
    icon: undefined,
    useCases: { ...realEstateUseCases },
  },
  musician: {
    identifier: "musiciangroup",
    label: "Artists and Bands",
    icon: undefined,
    useCases: { ...musicianUseCases },
  },
  social: {
    identifier: "socialgroup",
    label: "Social Activities",
    icon: undefined,
    useCases: { ...socialUseCases },
  },
  professional: {
    identifier: "professionalgroup",
    label: "Professional Networking",
    icon: undefined,
    useCases: { ...professionalUseCases },
  },
  other: {
    identifier: "othergroup",
    label: "Something Else",
    icon: undefined,
    useCases: { ...allDetailsUseCase },
  },
};

// generate a list of usecases from all use case groups
// TODO: find a good way to handle potential ungrouped use cases
let tempUseCases = {};
for (let key in useCaseGroups) {
  const useCases = useCaseGroups[key].useCases;
  for (let identifier in useCases) {
    tempUseCases[identifier] = useCases[identifier];
  }
}

export const useCases = tempUseCases;
// export const useCases = {
//   ...complainUseCases,
//   ...socialUseCases,
//   ...professionalUseCases,
//   ...realEstateUseCases,
//   ...transportUseCases,
//   ...mobilityUseCases,
//   ...musicianUseCases,
//   ...allDetailsUseCase,
// };
