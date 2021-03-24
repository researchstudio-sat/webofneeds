import { classifiedsGroup } from "./usecases/group-classifieds";
import { socialGroup } from "./usecases/group-social";
import { academicGroup } from "./usecases/group-academic";
import { artistGroup } from "./usecases/group-artists";
import { realEstateGroup } from "./usecases/group-real-estate";
import { transportGroup } from "./usecases/group-transport";
import { otherGroup } from "./usecases/group-other";
import { personalMobilityGroup } from "./usecases/group-personal-mobility";
import { interestsGroup, plansGroup } from "./usecases/group-interests";
import { corporateGroup } from "./usecases/group-corporate";
//import { workGroup } from "./usecases/group-work";
// import { customUseCase } from "./usecases/uc-custom.js";

import { persona } from "./usecases/uc-persona.js";
import { genericExpertise } from "./usecases/uc-expertise.js";
import { serviceAtom } from "./usecases/uc-serviceatom.js";
import { review } from "./usecases/uc-review.js";
// TODO: INCLUDE ROLES AGAIN ONCE THE ACL'S ARE EDITABLE
// import { role } from "./usecases/uc-role.js";

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
 *            PREFIX won: <https://w3id.org/won/core#>
 *
 *            SELECT ${resultName} WHERE {
 *                ${resultName} a won:Atom .
 *            }
 *        `)
 *    }
 * }
 *
 * A `generateQuery` is a function that takes the current atom draft and the name of the result variable and returns a sparqljs json representation of the query. This can be created either programmatically or by using the Parser class from the sparqljs library.
 *
 * The query needs to be a SELECT query and select only the resultName variable.
 * This will be automatically enforced by the atom builder.
 */

const useCaseGroups = {
  interestsGroup: interestsGroup,
  plansGroup: plansGroup,
  socialGroup: socialGroup,
  classifiedsGroup: classifiedsGroup,
  corporateGroup: corporateGroup,
  //workGroup: workGroup, //TODO: Now fully included in corporate
  academicGroup: academicGroup,
  artistGroup: artistGroup,
  realEstateGroup: realEstateGroup,
  transportGroup: transportGroup,
  personalMobilityGroup: personalMobilityGroup,
  otherGroup: otherGroup,
};

// add useCases here that should not be visible in the grouped useCases (will be hidden and only accessible through reactions
export const hiddenUseCases = {
  persona: persona,
  serviceAtom: serviceAtom,
  review: review,
  genericExpertise: genericExpertise,
  // TODO: INCLUDE ROLES AGAIN ONCE THE ACL'S ARE EDITABLE
  // role: role,
};

// generate a list of usecases from all use case groups
let tempUseCases = {};
for (let key in useCaseGroups) {
  const elements = useCaseGroups[key].subItems;
  addUseCasesToTemp(elements);
}
for (let identifier in hiddenUseCases) {
  hiddenUseCases[identifier].hidden = true;
  tempUseCases[identifier] = hiddenUseCases[identifier];
}

function addUseCasesToTemp(elements) {
  for (let identifier in elements) {
    if (elements[identifier].subItems) {
      addUseCasesToTemp(elements[identifier].subItems);
    } else {
      tempUseCases[identifier] = elements[identifier];
    }
  }
}

const useCases = tempUseCases;
window.useCases4dbg = useCases;

function addUseCaseGroupToTemp(tempGroups, groups) {
  if (hasSubElements(groups)) {
    for (let identifier in groups) {
      if (groups[identifier] && groups[identifier].subItems) {
        tempGroups[identifier] = groups[identifier];
        addUseCaseGroupToTemp(tempGroups, groups[identifier].subItems);
      }
    }
  }
}

let tempUseCaseGroups = {};
addUseCaseGroupToTemp(tempUseCaseGroups, useCaseGroups);
const allUseCaseGroups = tempUseCaseGroups;

export function getAllUseCases() {
  return useCases;
}

export function getAllUseCaseGroups() {
  return allUseCaseGroups;
}

function hasSubElements(obj) {
  return obj && obj !== {} && Object.keys(obj).length > 0;
}
