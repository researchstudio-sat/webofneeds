/**
 * Created by quasarchimaere on 11.02.2019.
 */

import * as useCaseDefinitions from "../config/usecase-definitions.js";
import { messageDetails } from "../config/detail-definitions.js";
import { getIn, get } from "./utils.js";
import { values } from "min-dash";
import won from "./won-es6.js";

import Immutable from "immutable";

/**
 * Returns all the details that are defined in any useCase Defined in the useCaseDefinitions
 * and in the messageDetails
 */
export function getAllDetails() {
  let allDetails = {};
  const useCases = useCaseDefinitions.getAllUseCases();

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
 * Returns the corresponding UseCase to a given atom,
 * return undefined if no useCase is found,
 * @param atomImm
 */
export function findUseCaseByAtom(atomImm) {
  const seeksTypes =
    getIn(atomImm, ["seeks", "type"]) &&
    getIn(atomImm, ["seeks", "type"])
      .toSet()
      .remove("won:Atom");

  const contentTypes =
    getIn(atomImm, ["content", "type"]) &&
    getIn(atomImm, ["content", "type"])
      .toSet()
      .remove("won:Atom");
  const useCases = useCaseDefinitions.getAllUseCases();

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
            .remove("won:Atom");

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

      if (
        matchingUseCases.size > 1 &&
        contentTypes &&
        contentTypes.includes("s:PlanAction")
      ) {
        matchingUseCases = matchingUseCases.filter(useCase => {
          const draftEventObject = getIn(useCase, [
            "draft",
            "content",
            "eventObject",
          ]);
          const atomEventObject = getIn(atomImm, ["content", "eventObject"]);
          if (Immutable.List.isList(draftEventObject)) {
            const eventObjectSize = draftEventObject.size;
            const matchingEventObjectSize = draftEventObject.filter(
              object => atomEventObject && atomEventObject.includes(object)
            ).size;
            return eventObjectSize == matchingEventObjectSize;
          }
          return atomEventObject && atomEventObject.includes(draftEventObject);
        });
      }

      if (matchingUseCases.size > 1) {
        //If there are multiple matched found based on the content type(s) alone we refine based on the seeks type as well
        matchingUseCases = matchingUseCases.filter(useCase =>
          hasExactMatchingTypes(useCase, seeksTypes, "seeks")
        );
      }

      if (
        matchingUseCases.size > 1 &&
        seeksTypes &&
        seeksTypes.includes("s:PlanAction")
      ) {
        matchingUseCases = matchingUseCases.filter(useCase => {
          const draftEventObject = getIn(useCase, [
            "draft",
            "seeks",
            "eventObject",
          ]);
          const atomEventObject = getIn(atomImm, ["seeks", "eventObject"]);

          if (Immutable.List.isList(draftEventObject)) {
            //Fixme: work with check for all list objects
            const eventObjectSize = draftEventObject.size;
            const matchingEventObjectSize = draftEventObject.filter(
              object => atomEventObject && atomEventObject.includes(object)
            ).size;
            return eventObjectSize == matchingEventObjectSize;
          }
          return atomEventObject && atomEventObject.includes(draftEventObject);
        });
      }

      if (matchingUseCases.size > 1) {
        console.warn(
          "Found multiple matching UseCases for: ",
          atomImm,
          " matching UseCases: ",
          matchingUseCases,
          " -> returning undefined"
        );
        return undefined;
      } else if (matchingUseCases.size == 1) {
        return matchingUseCases.first().toJS();
      } else {
        console.warn(
          "Found no matching UseCase for:",
          atomImm,
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

window.findUseCaseByAtom4Dbg = findUseCaseByAtom;
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
  const useCases = useCaseDefinitions.getAllUseCases();

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
  const useCaseGroups = useCaseDefinitions.getAllUseCaseGroups();
  let ungroupedUseCases = JSON.parse(
    JSON.stringify(useCaseDefinitions.getAllUseCases())
  );

  for (const identifier in useCaseGroups) {
    const group = useCaseGroups[identifier];
    // show use cases from groups that can't be displayed
    // show use cases from groups that have no more than threshold use cases
    if (
      !isDisplayableUseCaseGroup(group) ||
      countDisplayableItemsInGroup(group) <= threshold
    ) {
      continue;
    }
    // don't show usecases in groups as sinle use cases
    for (const useCase in group.subItems) {
      if (!group.subItems[useCase].subItems) {
        delete ungroupedUseCases[useCase];
      }
    }
  }
  return ungroupedUseCases;
}

/**
 * Returns a copy of the customUseCase
 * @param useCaseString
 * @returns {*}
 */
export function getCustomUseCase() {
  return getUseCase("customUseCase");
}

/**
 * Returns a copy of the useCase with the given useCaseString as an identifier
 * @param useCaseString
 * @returns {*}
 */
export function getUseCase(useCaseString) {
  if (useCaseString) {
    const useCasesImm = Immutable.fromJS(useCaseDefinitions.getAllUseCases());
    const foundUseCase =
      useCasesImm &&
      useCasesImm.find(useCase => get(useCase, "identifier") === useCaseString);
    return foundUseCase && foundUseCase.toJS();
  }
  return undefined;
}

export function getUseCaseGroupByIdentifier(groupIdentifier) {
  if (groupIdentifier) {
    const useCaseGroups = useCaseDefinitions.getAllUseCaseGroups();
    for (const groupName in useCaseGroups) {
      const element = useCaseGroups[groupName];

      if (groupIdentifier === element["identifier"]) {
        return element;
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
  return (
    useCase &&
    useCase.identifier &&
    !useCase.hidden &&
    (useCase.label || useCase.icon) &&
    !useCase.subItems
  );
}

/**
 * return whether the given useCase is displayable or not
 * @param useCase
 * @returns {*}
 */
export function isDisplayableItem(item) {
  return isDisplayableUseCase(item) || isDisplayableUseCaseGroup(item);
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
      if (isDisplayableItem(useCaseGroup.subItems[key])) {
        return true;
      }
    }
  }
  return false;
}

/**
 * return the amount of displayable items in a useCaseGroup
 * @param useCaseGroup
 * @return {*}
 */
export function countDisplayableItemsInGroup(useCaseGroup) {
  let countItems = 0;

  for (const key in useCaseGroup.subItems) {
    if (isDisplayableItem(useCaseGroup.subItems[key])) {
      countItems++;
    }
  }
  return countItems;
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
  const useCases = useCaseDefinitions.getAllUseCases();
  for (const useCaseKey in useCases) {
    if (searchFunction(useCases[useCaseKey], queryString)) {
      results.set(useCases[useCaseKey].identifier, useCases[useCaseKey]);
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

export function isHoldable(useCase) {
  return (
    useCase &&
    useCase.draft &&
    useCase.draft.content &&
    useCase.draft.content.sockets &&
    values(useCase.draft.content.sockets).includes(
      won.HOLD.HoldableSocketCompacted
    )
  );
}

export function getUseCaseLabel(identifier) {
  const useCase = getUseCase(identifier);
  return useCase.label;
}

export function getUseCaseIcon(identifier) {
  const useCase = getUseCase(identifier);
  return useCase.icon;
}
