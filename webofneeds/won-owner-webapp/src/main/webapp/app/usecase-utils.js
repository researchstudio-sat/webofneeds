/**
 * Created by quasarchimaere on 11.02.2019.
 */

import * as useCaseDefinitions from "../config/usecase-definitions.js";
import { messageDetails } from "../config/detail-definitions.js";
import { get, getIn } from "./utils.js";
import { values } from "min-dash";
import won from "./won-es6.js";

import Immutable from "immutable";

const useCasesImm = Immutable.fromJS(useCaseDefinitions.getAllUseCases());
const useCaseGroupsImm = Immutable.fromJS(
  useCaseDefinitions.getAllUseCaseGroups()
);
const allDetailsImm = Immutable.fromJS(initializeAllDetails());
const allMessageDetailsImm = Immutable.fromJS(initializeAllMessageDetails());

console.debug("useCasesImm: ", useCasesImm);
console.debug("useCaseGroupsImm:", useCaseGroupsImm);
console.debug("allDetailsImm: ", allDetailsImm);
console.debug("allMessageDetailsImm: ", allMessageDetailsImm);

window.useCasesImm4dbg = useCasesImm;
window.useCaseGroupsImm4dbg = useCaseGroupsImm;
window.allDetailsImm4dbg = allDetailsImm;
window.allMessageDetailsImm4dbg = allMessageDetailsImm;

/**
 * Returns all the details that are defined in any useCase Defined in the useCaseDefinitions
 * and in the messageDetails
 */
export function getAllDetails() {
  return allDetailsImm.toJS();
}

/**
 * Returns all the details that are defined in any useCase Defined in the useCaseDefinitions
 * and in the messageDetails as an ImmutableJS Object
 */
export function getAllDetailsImm() {
  return allDetailsImm;
}

/**
 * Initialize all the details that are defined in any useCase Defined in the useCaseDefinitions
 * and in the messageDetails
 */
function initializeAllDetails() {
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
      .remove(won.WON.AtomCompacted);

  const contentTypes =
    getIn(atomImm, ["content", "type"]) &&
    getIn(atomImm, ["content", "type"])
      .toSet()
      .remove(won.WON.AtomCompacted);

  if (useCasesImm && useCasesImm.size > 0) {
    const hasExactMatchingTypes = (useCase, types, branch) => {
      const typesSize = types ? types.size : 0;
      const ucTypes =
        useCase.getIn(["draft", branch, "type"]) &&
        useCase
          .getIn(["draft", branch, "type"])
          .toSet()
          .remove(won.WON.AtomCompacted);

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
          "eventObjectAboutUris",
        ]);
        const atomEventObject = getIn(atomImm, [
          "content",
          "eventObjectAboutUris",
        ]);
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
          "eventObjectAboutUris",
        ]);
        const atomEventObject = getIn(atomImm, [
          "seeks",
          "eventObjectAboutUris",
        ]);

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
        useCasesImm,
        " -> returning undefined"
      );
      return undefined;
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
 * as immutable Object
 * @returns {{}}
 */
export function getAllMessageDetailsImm() {
  return allMessageDetailsImm;
}

/**
 * Initialize all the details that are defined in any useCase Defined in the useCaseDefinitions
 * and in the messageDetails
 */
function initializeAllMessageDetails() {
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
 * @param visibleUseCasesArray => array of useCaseIdentifier that are visible
 * @returns {*}
 */
export function getUnGroupedUseCases(threshold = 0, visibleUseCasesArray) {
  let ungroupedUseCasesImm = useCasesImm;

  useCaseGroupsImm
    .filter(
      useCaseGroup =>
        isDisplayableUseCaseGroupImm(useCaseGroup, visibleUseCasesArray) &&
        countDisplayableItemsInGroupImm(useCaseGroup, visibleUseCasesArray) >
          threshold
    )
    .map(useCaseGroup => {
      // don't show usecases in groups as single use cases
      const subItems = get(useCaseGroup, "subItems");
      subItems &&
        subItems.map(subItem => {
          if (!get(subItem, "subItems")) {
            ungroupedUseCasesImm = ungroupedUseCasesImm.delete(
              get(subItem, "identifier")
            );
          }
        });
    });

  return (
    ungroupedUseCasesImm &&
    ungroupedUseCasesImm
      .filter(useCase => isDisplayableUseCaseImm(useCase, visibleUseCasesArray))
      .toJS()
  );
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
    const foundUseCase = get(useCasesImm, useCaseString);
    return foundUseCase && foundUseCase.toJS();
  }
  return undefined;
}

export function getUseCaseGroupByIdentifier(groupIdentifier) {
  const foundUseCaseGroup =
    groupIdentifier &&
    useCaseGroupsImm.find(
      useCaseGroup => get(useCaseGroup, "identifier") === groupIdentifier
    );
  return foundUseCaseGroup && foundUseCaseGroup.toJS();
}

/**
 * return if the given useCase is displayable or not
 * @param useCase
 * @param visibleUseCasesArray => array of useCaseIdentifier that are visible
 * @returns {*}
 */
export function isDisplayableUseCase(useCase, visibleUseCasesArray) {
  return (
    useCase &&
    isInVisibleUseCaseArray(useCase, visibleUseCasesArray) &&
    useCase.identifier &&
    !useCase.hidden &&
    (useCase.label || useCase.icon) &&
    !useCase.subItems
  );
}

function isDisplayableUseCaseImm(useCaseImm, visibleUseCasesArray) {
  return (
    useCaseImm &&
    get(useCaseImm, "identifier") &&
    isInVisibleUseCaseArray(useCaseImm, visibleUseCasesArray) &&
    !get(useCaseImm, "hidden") &&
    (get(useCaseImm, "label") || get(useCaseImm, "icon")) &&
    !get(useCaseImm, "subItems")
  );
}

/**
 * return whether the given useCase is displayable or not
 * @param useCase
 * @param visibleUseCasesArray => array of useCaseIdentifier that are visible
 * @returns {*}
 */
export function isDisplayableItem(item, visibleUseCasesArray) {
  return (
    isDisplayableUseCase(item, visibleUseCasesArray) ||
    isDisplayableUseCaseGroup(item, visibleUseCasesArray)
  );
}

function isDisplayableItemImm(itemImm, visibleUseCasesArray) {
  return (
    isDisplayableUseCaseImm(itemImm, visibleUseCasesArray) ||
    isDisplayableUseCaseGroupImm(itemImm, visibleUseCasesArray)
  );
}

/**
 * return if the given useCaseGroup is displayable or not
 * needs to have at least one displayable UseCase
 * @param useCase
 * @param visibleUseCasesArray => array of useCaseIdentifier that are visible
 * @returns {*}
 */
export function isDisplayableUseCaseGroup(useCaseGroup, visibleUseCasesArray) {
  const useCaseGroupValid =
    useCaseGroup &&
    (useCaseGroup.label || useCaseGroup.icon) &&
    useCaseGroup.subItems;

  if (useCaseGroupValid) {
    for (const key in useCaseGroup.subItems) {
      if (isDisplayableItem(useCaseGroup.subItems[key], visibleUseCasesArray)) {
        return true;
      }
    }
  }
  return false;
}

function isDisplayableUseCaseGroupImm(useCaseGroupImm, visibleUseCasesArray) {
  const useCaseGroupValid =
    useCaseGroupImm &&
    (get(useCaseGroupImm, "label") || get(useCaseGroupImm, "icon")) &&
    get(useCaseGroupImm, "subItems");

  if (useCaseGroupValid) {
    const subItems = get(useCaseGroupImm, "subItems");
    return !!(
      subItems &&
      subItems.find(subItem =>
        isDisplayableItemImm(subItem, visibleUseCasesArray)
      )
    );
  }
  return false;
}

/**
 * return the amount of displayable items in a useCaseGroup
 * @param useCaseGroup
 * @param visibleUseCasesArray => array of useCaseIdentifier that are visible
 * @return {*}
 */
function countDisplayableItemsInGroupImm(
  useCaseGroupImm,
  visibleUseCasesArray
) {
  const subItems = get(useCaseGroupImm, "subItems");

  const size = subItems
    ? subItems.filter(subItem =>
        isDisplayableItemImm(subItem, visibleUseCasesArray)
      ).size
    : 0;

  return size;
}

/**
 * return if the given element is a useCaseGroup or not
 * @param element
 * @returns {*}
 */
export function isUseCaseGroup(element) {
  return element.subItems;
}

export function filterUseCasesBySearchQuery(queryString, visibleUseCasesArray) {
  const resultSet =
    useCasesImm &&
    useCasesImm
      .filter(useCase =>
        searchFunctionImm(useCase, queryString, visibleUseCasesArray)
      )
      .toSet();

  if (!resultSet || resultSet.size === 0) {
    return undefined;
  }

  return Array.from(resultSet.toJS());
}

function searchFunctionImm(useCaseImm, searchString, visibleUseCasesArray) {
  // don't treat use cases that can't be displayed as results
  if (!isDisplayableUseCaseImm(useCaseImm, visibleUseCasesArray)) {
    return false;
  }
  // check for searchString in use case label and draft
  const useCaseLabel = get(useCaseImm, "label")
    ? JSON.stringify(get(useCaseImm, "label")).toLowerCase()
    : "";
  const useCaseDraft = get(useCaseImm, "draft")
    ? JSON.stringify(get(useCaseImm, "draft").toJS()).toLowerCase()
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

export function getVisibleUseCaseGroups(threshold, visibleUseCasesArray) {
  const visibleUseCaseGroups =
    useCaseGroupsImm &&
    useCaseGroupsImm.filter(
      useCaseGroup =>
        isDisplayableUseCaseGroupImm(useCaseGroup, visibleUseCasesArray) &&
        countDisplayableItemsInGroupImm(useCaseGroup, visibleUseCasesArray) >
          threshold
    );

  return visibleUseCaseGroups && visibleUseCaseGroups.toJS();
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
  return getIn(useCasesImm, [identifier, "label"]);
}

export function getUseCaseIcon(identifier) {
  return getIn(useCasesImm, [identifier, "icon"]);
}

function isInVisibleUseCaseArray(useCase, visibleUseCasesArray) {
  if (
    !visibleUseCasesArray ||
    visibleUseCasesArray.size == 0 ||
    visibleUseCasesArray.contains("*")
  ) {
    console.debug(
      "No specific useCases within visibleUseCasesArray, allow all"
    );
    return true;
  } else {
    const isUseCaseConfigured = !!visibleUseCasesArray.contains(
      get(useCase, "identifier") || useCase.identifier
    );
    console.debug(
      "UseCase [",
      get(useCase, "identifier") || useCase.identifier,
      "] is ",
      isUseCaseConfigured ? "" : "NOT",
      "in visibleUseCaseArray"
    );
    return isUseCaseConfigured;
  }
}
