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

function hasSubElements(obj) {
  return obj && obj !== {} && Object.keys(obj).length > 0;
}
