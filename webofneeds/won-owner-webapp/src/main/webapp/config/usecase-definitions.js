import { is, get, getFromJsonLd, getInFromJsonLd } from "../app/utils.js";
import Immutable from "immutable";
import { details, abstractDetails } from "detailDefinitions";
import { Parser as SparqlParser } from "sparqljs";
import { findLatestIntervallEndInJsonLd } from "../app/won-utils.js";
import { reduceObjectByKeys } from "../app/utils";
import won from "../app/won-es6.js";

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
    if (!value) {
      return { "s:floorSize": undefined };
    }
    return {
      "s:floorSize": {
        "@type": "s:QuantitativeValue",
        "s:value": [{ "@value": value, "@type": "s:Float" }],
        "s:unitCode": "MTK",
      },
    };
  },
  parseFromRDF: function(jsonLDImm) {
    const fs = won.parseFrom(jsonLDImm, ["s:floorSize", "s:value"], "s:Float");
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
    if (value) {
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
    if (!value) {
      return { "s:numberOfRooms": undefined };
    }
    return { "s:numberOfRooms": [{ "@value": value, "@type": "s:Float" }] };
  },
  parseFromRDF: function(jsonLDImm) {
    return won.parseFrom(jsonLDImm, ["s:numberOfRooms"], "s:Float");
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      return (includeLabel ? this.label + ": " + value : value) + " Rooms";
    }
    return undefined;
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
  const minIsNumber = !isNaN(min_);
  const maxIsNumber = !isNaN(max_);
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
  ...abstractDetails.number,
  identifier: "rent",
  label: "Rent in EUR/month",
  icon: "#ico36_detail_rent",
  parseToRDF: function({ value }) {
    if (!value) {
      return { "s:priceSpecification": undefined };
    }
    return {
      "s:priceSpecification": {
        "@type": "s:CompoundPriceSpecification",
        "s:price": [{ "@value": value, "@type": "s:Float" }],
        "s:priceCurrency": "EUR",
        "s:description": "total rent per month",
        // "s:priceComponent": {
        //   "@type": "s:UnitPriceSpecification",
        //   "s:price": 0,
        //   "s:priceCurrency": "EUR",
        //   "s:description": "",
        // }
      },
    };
  },
  parseFromRDF: function(jsonLDImm) {
    const rent = won.parseFrom(
      jsonLDImm,
      ["s:priceSpecification", "s:price"],
      "s:Float"
    );

    if (!rent) {
      return undefined;
    } else {
      return rent + " EUR/month";
    }
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      return (includeLabel ? this.label + ": " + value : value) + " EUR/month";
    }
    return undefined;
  },
};

const realEstateRentRangeDetail = {
  ...abstractDetails.range,
  identifier: "rentRange",
  label: "Rent in EUR/month",
  minLabel: "From",
  maxLabel: "To",
  icon: "#ico36_detail_rent",
  parseToRDF: function({ value }) {
    if (!value || !(value.min || value.max)) {
      return { "s:priceSpecification": undefined };
    }
    return {
      "s:priceSpecification": {
        "@type": "s:CompoundPriceSpecification",
        "s:minPrice": value.min && [
          { "@value": value.min, "@type": "s:Float" },
        ],
        "s:maxPrice": value.max && [
          { "@value": value.max, "@type": "s:Float" },
        ],
        "s:priceCurrency": "EUR",
        "s:description": "total rent per month in between min/max",
      },
    };
  },
  parseFromRDF: function(jsonLDImm) {
    const minRent = won.parseFrom(
      jsonLDImm,
      ["s:priceSpecification", "s:minPrice"],
      "s:Float"
    );
    const maxRent = won.parseFrom(
      jsonLDImm,
      ["s:priceSpecification", "s:maxPrice"],
      "s:Float"
    );
    if (!minRent && !maxRent) {
      return undefined;
    } else {
      // if there's anything, use it
      return Immutable.fromJS({
        min: minRent && minRent + " EUR/month",
        max: maxRent && maxRent + " EUR/month",
      });
    }
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      let humanReadable;
      if (value.min && value.max) {
        humanReadable =
          "between " + value.min + " and " + value.max + " EUR/month";
      } else if (value.min) {
        humanReadable = "at least " + value.min + "EUR/month";
      } else if (value.max) {
        humanReadable = "at most " + value.max + "EUR/month";
      }
      if (humanReadable) {
        return includeLabel ? this.label + ": " + humanReadable : humanReadable;
      }
    }
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

      let filterStrings = [];

      if (rentRange) {
        if (rentRange.min || rentRange.max) {
          filterStrings.push("FILTER (?currency = 'EUR') ");
        }
        if (rentRange.min) {
          filterStrings.push(
            "FILTER (?price >= " + draft.seeks.rentRange.min + " )"
          );
        }
        if (rentRange.max) {
          filterStrings.push(
            "FILTER (?price <= " + draft.seeks.rentRange.max + " )"
          );
        }
      }

      if (floorSizeRange) {
        if (floorSizeRange.min) {
          filterStrings.push(
            "FILTER (?floorSize >= " + draft.seeks.floorSizeRange.min + " )"
          );
        }
        if (floorSizeRange.max) {
          filterStrings.push(
            "FILTER (?floorSize <= " + draft.seeks.floorSizeRange.max + " )"
          );
        }
      }

      if (numberOfRoomsRange) {
        if (numberOfRoomsRange.min) {
          filterStrings.push(
            "FILTER (?numberOfRooms >= " +
              draft.seeks.numberOfRoomsRange.min +
              " )"
          );
        }
        if (numberOfRoomsRange.max) {
          filterStrings.push(
            "FILTER (?numberOfRooms <= " +
              draft.seeks.numberOfRoomsRange.max +
              " )"
          );
        }
      }

      if (location) {
        filterStrings.push(
          `?result won:is/won:hasLocation/s:geo ?geo
          SERVICE geo:search {
            ?geo geo:search "inCircle" .
            ?geo geo:searchDatatype geoliteral:lat-lon .
            ?geo geo:predicate won:geoSpatial .
            ?geo geo:spatialCircleCenter "${location.lat}#${location.lng}" .
            ?geo geo:spatialCircleRadius "10" .
            ?geo geo:distanceValue ?geoDistance .
          }`
        );
      }

      const prefixes = `
        prefix s:     <http://schema.org/>
        prefix won:   <http://purl.org/webofneeds/model#>
        prefix dc:    <http://purl.org/dc/elements/1.1/>
        prefix geo: <http://www.bigdata.com/rdf/geospatial#>
        prefix geoliteral: <http://www.bigdata.com/rdf/geospatial/literals/v1#>
      `;
      let queryTemplate =
        `
        ${prefixes}
        SELECT DISTINCT ${resultName}
        WHERE {
        ${resultName}
          won:is ?is.
          ?is s:priceSpecification ?pricespec.
          ?pricespec s:price ?price.
          ?pricespec s:priceCurrency ?currency.
          ?is s:floorSize/s:value ?floorSize.
          ?is s:numberOfRooms ?numberOfRooms.
          ${filterStrings && filterStrings.join(" ")}
        }` + (location ? `ORDER BY ASC(?geoDistance)` : "");

      return new SparqlParser().parse(queryTemplate);
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
          if (!value) {
            return { "s:weight": undefined };
          } else {
            return {
              "@type": "s:Product",
              "s:weight": {
                "@type": "s:QuantitativeValue",
                "s:value": [{ "@value": value, "@type": "s:Float" }],
                "s:unitCode": "KGM",
              },
            };
          }
        },
        parseFromRDF: function(jsonLDImm) {
          const w = won.parseFrom(
            jsonLDImm,
            ["s:weight", "s:value"],
            "s:Float"
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
          if (value) {
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
          if (!value) {
            return { "s:length": undefined };
          } else {
            return {
              "@type": "s:Product",
              "s:length": {
                "@type": "s:QuantitativeValue",
                "s:value": [{ "@value": value, "@type": "s:Float" }],
                "s:unitCode": "CMT",
              },
            };
          }
        },
        parseFromRDF: function(jsonLDImm) {
          const l = won.parseFrom(
            jsonLDImm,
            ["s:length", "s:value"],
            "s:Float"
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
          if (value) {
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
          if (!value) {
            return { "s:width": undefined };
          } else {
            return {
              "@type": "s:Product",
              "s:width": {
                "@type": "s:QuantitativeValue",
                "s:value": [{ "@value": value, "@type": "s:Float" }],
                "s:unitCode": "CMT",
              },
            };
          }
        },
        parseFromRDF: function(jsonLDImm) {
          const w = won.parseFrom(jsonLDImm, ["s:width", "s:value"], "s:Float");
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
          if (value) {
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
          if (!value) {
            return { "s:height": undefined };
          } else {
            return {
              "@type": "s:Product",
              "s:height": {
                "@type": "s:QuantitativeValue",
                "s:value": [{ "@value": value, "@type": "s:Float" }],
                "s:unitCode": "CMT",
              },
            };
          }
        },
        parseFromRDF: function(jsonLDImm) {
          const h = won.parseFrom(
            jsonLDImm,
            ["s:height", "s:value"],
            "s:Float"
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
          if (value) {
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
  },
  taxiOffer: {
    identifier: "taxiOffer",
    label: "Offer Taxi Service",
    icon: "#ico36_uc_taxi_offer",
    doNotMatchAfter: findLatestIntervallEndInJsonLd,
    draft: {
      ...emptyDraft,
      is: { title: "Taxi", tags: "offer-lift" },
      searchString: "search-lift",
    },
    isDetails: {
      title: { ...details.title },
      description: { ...details.description },
      location: { ...details.location },
    },
  },
  rideShareOffer: {
    identifier: "rideShareOffer",
    label: "Offer to Share a Ride",
    icon: "#ico36_uc_taxi_offer",
    doNotMatchAfter: findLatestIntervallEndInJsonLd,
    draft: {
      ...emptyDraft,
      is: { title: "Share a Ride", tags: "offer-lift" },
      searchString: "search-lift",
    },
    isDetails: {
      title: { ...details.title },
      description: { ...details.description },
      fromDatetime: { ...details.fromDatetime },
      throughDatetime: { ...details.throughDatetime },
      travelAction: { ...details.travelAction },
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
      ...reduceObjectByKeys(realEstateUseCases.searchRent.seeksDetails, [
        "numberOfRoomsRange",
      ]),
      features: {
        ...realEstateFeaturesDetail,
        placeholder: "e.g. PA, Drumkit",
      },
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
      ...reduceObjectByKeys(realEstateUseCases.offerRent.isDetails, [
        "numberOfRooms",
      ]),
      features: {
        ...realEstateFeaturesDetail,
        placeholder: "e.g. PA, Drumkit",
      },
      fromDatetime: { ...details.fromDatetime },
      throughDatetime: { ...details.throughDatetime },
    },

    seeksDetails: undefined,
  },
};

export const useCases = {
  ...complainUseCases,
  ...socialUseCases,
  ...professionalUseCases,
  ...realEstateUseCases,
  ...transportUseCases,
  ...mobilityUseCases,
  ...allDetailsUseCase,
  ...musicianUseCases,
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
    label: "Transport",
    icon: undefined,
    useCases: { ...transportUseCases },
  },
  mobility: {
    identifier: "mobilitygroup",
    label: "Mobility",
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
    label: "Musician",
    icon: undefined,
    useCases: { ...musicianUseCases },
  },
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
  other: {
    identifier: "othergroup",
    label: "Something else",
    icon: undefined,
    useCases: { ...allDetailsUseCase },
  },
};
