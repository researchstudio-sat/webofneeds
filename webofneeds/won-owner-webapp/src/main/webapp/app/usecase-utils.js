/**
 * Created by ksinger on 11.08.2016.
 */

import * as useCaseDefinitions from "useCaseDefinitions";
import { messageDetails } from "detailDefinitions";
import { getIn } from "./utils.js";

import Immutable from "immutable";

/**
 * Returns all the details that are defined in any useCase Defined in the useCaseDefinitions
 * and in the messageDetails
 */
export function getAllDetails() {
  let allDetails = {};
  const useCases = useCaseDefinitions.getUseCases();

  if (hasSubElements(useCases)) {
    for (const useCaseKey in useCases) {
      const useCase = useCases[useCaseKey];
      if (useCase) {
        const details = useCase.details ? useCase.details : {};
        const seeksDetails = useCase.seeksDetails ? useCase.seeksDetails : {};
        allDetails = { ...allDetails, ...details, ...seeksDetails };
      }
    }
  }

  return Object.assign({}, messageDetails, allDetails);
}

/**
 * Returns the corresponding UseCase to a given need,
 * return undefined if no useCase is found,
 * @param needImm
 */
export function findUseCaseByNeed(needImm) {
  const seeksTypes =
    getIn(needImm, ["seeks", "type"]) &&
    getIn(needImm, ["seeks", "type"])
      .toSet()
      .remove("won:Need");

  const contentTypes =
    getIn(needImm, ["content", "type"]) &&
    getIn(needImm, ["content", "type"])
      .toSet()
      .remove("won:Need");
  const useCases = useCaseDefinitions.getUseCases();

  if (hasSubElements(useCases)) {
    const useCasesImm = Immutable.fromJS(useCases);

    if (useCasesImm && useCasesImm.size > 0) {
      const hasExactMatchingTypes = (useCase, types, branch) => {
        const typesSize = types ? types.size : 0;
        const ucTypes =
          useCase.getIn(["draft", branch, "type"]) &&
          useCase
            .getIn(["draft", branch, "type"])
            .toSet()
            .remove("won:Need");

        const ucTypesSize = ucTypes ? ucTypes.size : 0;

        if (typesSize != ucTypesSize) {
          return false;
        }

        const hasTypes = typesSize > 0;

        return !hasTypes || (ucTypes && ucTypes.isSubset(types));
      };

      let matchingUseCases = useCasesImm.filter(useCase =>
        hasExactMatchingTypes(useCase, contentTypes, "content")
      );

      if (matchingUseCases && matchingUseCases.size > 1) {
        //If there are multiple matched found based on the content type(s) alone we refine based on the seeks type as well
        matchingUseCases = matchingUseCases.filter(useCase =>
          hasExactMatchingTypes(useCase, seeksTypes, "seeks")
        );
      }

      if (matchingUseCases && matchingUseCases.size > 1) {
        console.warn(
          "Found multiple matching UseCases for: ",
          needImm,
          " matching UseCases: ",
          matchingUseCases,
          " -> returning undefined"
        );
        return undefined;
      } else if (matchingUseCases && matchingUseCases.size == 1) {
        return matchingUseCases.first().toJS();
      } else {
        console.warn(
          "Found no matching UseCase for:",
          needImm,
          " within, ",
          useCases,
          " -> returning undefined"
        );
        return undefined;
      }
    }
  }

  return undefined;
}

window.findUseCaseByNeed4Dbg = findUseCaseByNeed;
/**
 * Returns all the details that are defined in any useCase in the useCaseDefinitions
 * and has the messageEnabled Flag set to true
 *
 * as well as every detail that is defined in the messageDetail object from detail-definitions (for details that are only available in
 * messages)
 *
 * the messageEnabled-flag indicates if the detail is allowed to be sent as a part of a connectionMessage
 * @returns {{}}
 */
export function getAllMessageDetails() {
  let allDetails = {};
  const useCases = useCaseDefinitions.getUseCases();

  if (hasSubElements(useCases)) {
    for (const useCaseKey in useCases) {
      const useCase = useCases[useCaseKey];
      if (useCase) {
        const details = useCase.details ? useCase.details : {};
        const seeksDetails = useCase.seeksDetails ? useCase.seeksDetails : {};
        allDetails = { ...allDetails, ...details, ...seeksDetails };
      }
    }
  }

  let usecaseMessageDetails = {};

  for (const detailKey in allDetails) {
    if (allDetails[detailKey].messageEnabled) {
      usecaseMessageDetails[detailKey] = allDetails[detailKey];
    }
  }
  const allMessageDetails = Object.assign(
    {},
    messageDetails,
    usecaseMessageDetails
  );
  return allMessageDetails;
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
    for (const useCase in group.subItems) {
      if (group.subItems[useCase].subItems) {
        for (const subUseCase in group.subItems[useCase].subItems) {
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
    const useCaseGroups = getUseCaseGroups();
    for (const groupName in useCaseGroups) {
      const element = useCaseGroups[groupName];

      if (groupIdentifier === element["identifier"]) {
        return element;
      } else if (isUseCaseGroup(element)) {
        for (const subGroupName in element.subItems) {
          // FIXME: DOES NOT WORK FOR SUBSUB GROUPS
          if (
            groupIdentifier === element.subItems[subGroupName]["identifier"]
          ) {
            return element.subItems[subGroupName];
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
    useCaseGroup.subItems;

  if (useCaseGroupValid) {
    for (const key in useCaseGroup.subItems) {
      if (isDisplayableUseCase(useCaseGroup.subItems[key])) {
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

  for (const key in useCaseGroup.subItems) {
    if (isDisplayableUseCase(useCaseGroup.subItems[key])) {
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
  return element.subItems;
}

export function filterUseCasesBySearchQuery(queryString) {
  let results = new Map();
  const useCaseGroups = getUseCaseGroups();
  for (const key in useCaseGroups) {
    const group = Object.values(useCaseGroups[key].subItems);

    for (const useCase of group) {
      //TODO: SUBGROUP HANDLING
      if (!isUseCaseGroup(useCase) && searchFunction(useCase, queryString)) {
        results.set(useCase.identifier, useCase);
      }
    }
  }

  if (results.size === 0) {
    return undefined;
  }

  return Array.from(results.values());
}

function searchFunction(useCase, searchString) {
  // don't treat use cases that can't be displayed as results
  if (!isDisplayableUseCase(useCase)) {
    return false;
  }
  // check for searchString in use case label and draft
  const useCaseLabel = useCase.label
    ? JSON.stringify(useCase.label).toLowerCase()
    : "";
  const useCaseDraft = useCase.draft
    ? JSON.stringify(useCase.draft).toLowerCase()
    : "";

  const useCaseString = useCaseLabel.concat(useCaseDraft);
  const queries = searchString.toLowerCase().split(" ");

  for (let query of queries) {
    if (useCaseString.includes(query)) {
      return true;
    }
  }

  return false;
}

function hasSubElements(obj) {
  return obj && obj !== {} && Object.keys(obj).length > 0;
}

export function getUseCaseGroups() {
  return useCaseDefinitions.getUseCaseGroups();
}

export function getUseCases() {
  return useCaseDefinitions.getUseCases();
}
