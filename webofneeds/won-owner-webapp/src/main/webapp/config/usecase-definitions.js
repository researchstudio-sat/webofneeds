import { details, emptyDraft } from "detailDefinitions";
import { realEstateUseCases } from "realEstateUseCases";
import { transportUseCases } from "transportUseCases";
import { complainUseCases } from "complainUseCases";
import { socialUseCases } from "socialUseCases";
import { professionalUseCases } from "professionalUseCases";
import { mobilityUseCases } from "mobilityUseCases";
import { musicianUseCases } from "musicianUseCases";

import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../app/won-utils.js";

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
    doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
    draft: { ...emptyDraft },
    isDetails: details,
    seeksDetails: details,
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
