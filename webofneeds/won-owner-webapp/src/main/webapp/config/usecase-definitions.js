import won from "../app/service/won.js";

import { details, emptyDraft } from "./detail-definitions.js";

import { realEstateGroup } from "./usecases/real-estate.js";
import { transportGroup } from "./usecases/transport.js";
import { complainGroup } from "./usecases/complain.js";
import { socialGroup } from "./usecases/social.js";
import { professionalGroup } from "./usecases/professional/professional.js";
import { mobilityGroup } from "./usecases/mobility.js";
import { musicianGroup } from "./usecases/musician.js";
import { classifiedsGroup } from "./usecases/classifieds.js";

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
 *    details: {
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
    details: details,
    seeksDetails: details,
  },
};

const groupChatUsecase = {
  groupChat: {
    identifier: "groupChat",
    label: "New Groupchat Post",
    draft: {
      ...emptyDraft,
      facet: { "@id": "#groupFacet", "@type": won.WON.GroupFacet },
    },
    details: details,
    seeksDetails: details,
  },
};

export const useCaseGroups = {
  complain: complainGroup,
  transport: transportGroup,
  mobility: mobilityGroup,
  realEstate: realEstateGroup,
  musician: musicianGroup,
  social: socialGroup,
  professional: professionalGroup,
  classifieds: classifiedsGroup,
  other: {
    identifier: "othergroup",
    label: "Something Else",
    icon: undefined,
    useCases: { ...allDetailsUseCase, ...groupChatUsecase },
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
