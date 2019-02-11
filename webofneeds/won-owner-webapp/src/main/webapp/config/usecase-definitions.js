import { classifiedsGroup } from "./usecases/group-classifieds";
import { socialGroup } from "./usecases/group-social";
import { workGroup } from "./usecases/group-work";
import { academicGroup } from "./usecases/group-academic";
import { artistGroup } from "./usecases/group-artists";
import { realEstateGroup } from "./usecases/group-real-estate";
import { transportGroup } from "./usecases/group-transport";
import { otherGroup } from "./usecases/group-other";
import { personalMobilityGroup } from "./usecases/group-personal-mobility";
import { activitiesGroup } from "./usecases/group-activities";
// import { customUseCase } from "./usecases/uc-custom.js";

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

export const useCaseGroups = {
  activities: activitiesGroup,
  social: socialGroup,
  classifieds: classifiedsGroup,
  work: workGroup,
  academic: academicGroup,
  artists: artistGroup,
  realEstate: realEstateGroup,
  transport: transportGroup,
  personalMobility: personalMobilityGroup,
  other: otherGroup,
};

// generate a list of usecases from all use case groups
// TODO: find a good way to handle potential ungrouped use cases
let tempUseCases = {};
for (let key in useCaseGroups) {
  const useCases = useCaseGroups[key].useCases;
  for (let identifier in useCases) {
    if (useCases[identifier].useCases) {
      //ADD ONE MORE CASCADE (FIXME: CURRENTLY DOESNT WORK WITH SUBSUB GROUPS)
      for (let subIdentifier in useCases[identifier].useCases) {
        tempUseCases[subIdentifier] =
          useCases[identifier]["useCases"][subIdentifier];
      }
    } else {
      tempUseCases[identifier] = useCases[identifier];
    }
  }
}

const useCases = tempUseCases;

export function getUseCaseGroups() {
  return useCaseGroups;
}

export function getUseCases() {
  return useCases;
}

/**
 * returns an object containing all use cases that were
 * not found in a useCaseGroup or in a group that's too
 * small to be displayed as a group
 * @param threshold => defines size that is deemed too small to display group
 * @returns {*}
 */
export function getUnGroupedUseCases(threshold = 0) {
  const useCaseGroups = getUseCaseGroups();
  let ungroupedUseCases = JSON.parse(JSON.stringify(getUseCases()));

  for (const identifier in useCaseGroups) {
    const group = useCaseGroups[identifier];
    // show use cases from groups that can't be displayed
    // show use cases from groups that have no more than threshold use cases
    if (
      !isDisplayableUseCaseGroup(group) ||
      countDisplayableUseCasesInGroup(group) <= threshold
    ) {
      continue;
    }
    // don't show usecases in groups as sinle use cases
    for (const useCase in group.useCases) {
      if (group.useCases[useCase].useCases) {
        for (const subUseCase in group.useCases[useCase].useCases) {
          //FIXME: SUBSUB GROUPS ARE NOT SUPPORTED YET
          delete ungroupedUseCases[subUseCase];
        }
      } else {
        delete ungroupedUseCases[useCase];
      }
    }
  }
  return ungroupedUseCases;
}

export function getUseCase(useCaseString) {
  if (useCaseString) {
    const useCases = getUseCases();

    for (const useCaseName in useCases) {
      if (useCaseString === useCases[useCaseName]["identifier"]) {
        return useCases[useCaseName];
      }
    }
  }
  return undefined;
}

/**
 * This fucntion returns all UseCases that are defined with "showInList = true", this is so we can define useCases
 * that show up within our connection-overview "quick item"-buttons, e.g. if we want to define a usecase that is
 * always available from this list
 * @returns {{}}
 */
export function getListUseCases() {
  let listUseCases = {};
  const useCases = getUseCases();

  for (const useCaseKey in useCases) {
    if (useCases[useCaseKey]["showInList"]) {
      listUseCases[useCaseKey] = useCases[useCaseKey];
    }
  }
  return listUseCases;
}

export function getUseCaseGroupByIdentifier(groupIdentifier) {
  if (groupIdentifier) {
    for (const groupName in useCaseGroups) {
      const element = useCaseGroups[groupName];

      if (groupIdentifier === element["identifier"]) {
        return element;
      } else if (isUseCaseGroup(element)) {
        for (const subGroupName in element.useCases) {
          // FIXME: DOES NOT WORK FOR SUBSUB GROUPS
          if (
            groupIdentifier === element.useCases[subGroupName]["identifier"]
          ) {
            return element.useCases[subGroupName];
          }
        }
      }
    }
  }
  return undefined;
}

/**
 * return if the given useCase is displayable or not
 * @param useCase
 * @returns {*}
 */
export function isDisplayableUseCase(useCase) {
  return useCase && useCase.identifier && (useCase.label || useCase.icon);
}

/**
 * return if the given useCaseGroup is displayable or not
 * needs to have at least one displayable UseCase
 * @param useCase
 * @returns {*}
 */
export function isDisplayableUseCaseGroup(useCaseGroup) {
  const useCaseGroupValid =
    useCaseGroup &&
    (useCaseGroup.label || useCaseGroup.icon) &&
    useCaseGroup.useCases;

  if (useCaseGroupValid) {
    for (const key in useCaseGroup.useCases) {
      if (isDisplayableUseCase(useCaseGroup.useCases[key])) {
        return true;
      }
    }
  }
  return false;
}

/**
 * return the amount of displayable useCases in a useCaseGroup
 * @param useCaseGroup
 * @return {*}
 */
export function countDisplayableUseCasesInGroup(useCaseGroup) {
  let countUseCases = 0;

  for (const key in useCaseGroup.useCases) {
    if (isDisplayableUseCase(useCaseGroup.useCases[key])) {
      countUseCases++;
    }
  }
  return countUseCases;
}

/**
 * return if the given element is a useCaseGroup or not
 * @param element
 * @returns {*}
 */
export function isUseCaseGroup(element) {
  return element.useCases;
}
