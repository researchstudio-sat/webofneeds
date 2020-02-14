/**
 * Created by ksinger on 22.01.2016.
 */

import { createSelector } from "reselect";

import { getIn, get } from "../../utils.js";
import * as connectionSelectors from "./connection-selectors.js";
import * as atomUtils from "../utils/atom-utils.js";
import * as connectionUtils from "../utils/connection-utils.js";
import * as accountUtils from "../utils/account-utils.js";
import * as viewUtils from "../utils/view-utils.js";
import Color from "color";

export const selectLastUpdateTime = state => state.get("lastUpdateTime");
export const getRouterParams = state =>
  getIn(state, ["router", "currentParams"]);

export const getAtoms = state => get(state, "atoms");
export const getOwnedAtoms = state => {
  const accountState = get(state, "account");
  return getAtoms(state).filter(atom =>
    accountUtils.isAtomOwned(accountState, get(atom, "uri"))
  );
};

export const getWhatsNewAtoms = state => {
  return getAtoms(state).filter(
    (atom, atomUri) => !!getIn(state, ["owner", "whatsNewUris", atomUri])
  );
};

export const getWhatsAroundAtoms = state => {
  return getAtoms(state).filter(
    (atom, atomUri) => !!getIn(state, ["owner", "whatsAroundUris", atomUri])
  );
};

export function getPosts(state) {
  const atoms = getAtoms(state);
  return atoms.filter(atom => {
    if (!atom.getIn(["content", "type"])) return true;

    return atomUtils.isAtom(atom) && !atomUtils.isPersona(atom);
  });
}

export const getOwnedPosts = state => {
  const accountState = get(state, "account");
  return getPosts(state).filter(atom =>
    accountUtils.isAtomOwned(accountState, get(atom, "uri"))
  );
};

/**
 * Gets all Atoms that...
 *  - are Active
 *  - have the ChatSocket
 *  - have at least one non-closed non-suggested ChatSocket connection
 * @param state
 */
export function getChatAtoms(state) {
  const allOwnedAtoms = getOwnedAtoms(state);

  return (
    allOwnedAtoms &&
    allOwnedAtoms
      .filter(atom => atomUtils.isActive(atom))
      .filter(atom => atomUtils.hasChatSocket(atom))
      .filter(
        atom =>
          !!get(atom, "connections") &&
          !!get(atom, "connections").find(
            conn =>
              !(
                connectionUtils.isClosed(conn) ||
                connectionUtils.isSuggested(conn)
              ) &&
              connectionSelectors.isChatToXConnection(get(state, "atoms"), conn)
          )
      )
  );
}

export function hasChatAtoms(state) {
  const chatAtoms = getChatAtoms(state);
  return chatAtoms && chatAtoms.size > 0;
}

/**
 * Determines if there are any connections that are unread and suggested
 * (used for the inventory unread indicator)
 * @param state
 * @returns {boolean}
 */
export function hasUnreadSuggestedConnections(state) {
  const allOwnedAtoms = getOwnedAtoms(state);

  return (
    allOwnedAtoms &&
    !!allOwnedAtoms
      .filter(atom => atomUtils.isActive(atom))
      .find(atom => atomUtils.hasUnreadSuggestedConnections(atom))
  );
}

export function hasUnreadSuggestedConnectionsInHeldAtoms(state, atomUri) {
  const allAtoms = getAtoms(state);
  const atom = get(allAtoms, atomUri);
  const holds = atomUtils.getHeldAtomUris(atom);

  return (
    holds &&
    !!holds.find(holdsUri =>
      atomUtils.hasUnreadSuggestedConnections(get(allAtoms, holdsUri))
    )
  );
}

/**
 * Determines if there are any buddy connections that are unread
 * (used for the inventory unread indicator)
 * @param state
 * @returns {boolean}
 */
export function hasUnreadBuddyConnections(
  state,
  excludeClosed = false,
  excludeSuggested = false
) {
  const allOwnedAtoms = getOwnedAtoms(state);

  return (
    allOwnedAtoms &&
    !!allOwnedAtoms
      .filter(atom => atomUtils.isActive(atom))
      .find(
        atom =>
          !!connectionSelectors
            .getBuddyConnectionsByAtomUri(
              state,
              get(atom, "uri"),
              excludeClosed,
              excludeSuggested
            )
            .find(conn => connectionUtils.isUnread(conn))
      )
  );
}

export function hasUnreadChatConnections(state) {
  const chatAtoms = getChatAtoms(state);

  return (
    chatAtoms &&
    !!chatAtoms.find(
      atom =>
        !!get(atom, "connections") &&
        !!get(atom, "connections").find(
          conn =>
            !(
              connectionUtils.isClosed(conn) ||
              connectionUtils.isSuggested(conn)
            ) &&
            connectionSelectors.isChatToXConnection(
              get(state, "atoms"),
              conn
            ) &&
            connectionUtils.isUnread(conn)
        )
    )
  );
}

export function getActiveAtoms(state) {
  const allAtoms = getAtoms(state);
  return allAtoms && allAtoms.filter(atom => atomUtils.isActive(atom));
}

export function getOwnedAtomsInCreation(state) {
  const allOwnedAtoms = getOwnedAtoms(state);
  // atoms that have been created but are not confirmed by the server yet
  return (
    allOwnedAtoms && allOwnedAtoms.filter(post => post.get("isBeingCreated"))
  );
}

export const selectIsConnected = state =>
  !state.getIn(["messages", "reconnecting"]) &&
  !state.getIn(["messages", "lostConnection"]);

/**
 * Get the atom for a given connectionUri
 * @param state to retrieve data from
 * @param connectionUri to find corresponding atom for
 */
export function getOwnedAtomByConnectionUri(state, connectionUri) {
  let atoms = getOwnedAtoms(state); //we only check own atoms as these are the only ones who have connections stored
  return atoms.find(atom => atom.getIn(["connections", connectionUri]));
}

export const getCurrentParamsFromRoute = createSelector(
  state => state,
  state => {
    return getIn(state, ["router", "currentParams"]);
  }
);

export const getViewConnectionUriFromRoute = createSelector(
  state => state,
  state => {
    const encodedConnUri = getIn(state, [
      "router",
      "currentParams",
      "viewConnUri",
    ]);
    return decodeUriComponentProperly(encodedConnUri);
  }
);

export const getUseCaseFromRoute = createSelector(
  state => state,
  state => {
    return getIn(state, ["router", "currentParams", "useCase"]);
  }
);

export const getTargetSocketTypeFromRoute = createSelector(
  state => state,
  state => {
    return getIn(state, ["router", "currentParams", "targetSocketType"]);
  }
);

export const getSenderSocketTypeFromRoute = createSelector(
  state => state,
  state => {
    return getIn(state, ["router", "currentParams", "senderSocketType"]);
  }
);

export const getUseCaseGroupFromRoute = createSelector(
  state => state,
  state => {
    return getIn(state, ["router", "currentParams", "useCaseGroup"]);
  }
);

export const getPrivateIdFromRoute = createSelector(
  state => state,
  state => {
    return getIn(state, ["router", "currentParams", "privateId"]);
  }
);

export const getVerificationTokenFromRoute = createSelector(
  state => state,
  state => {
    return getIn(state, ["router", "currentParams", "token"]);
  }
);

export const getConnectionUriFromRoute = createSelector(
  getRouterParams,
  routerParams => {
    //de-escaping is lost in transpiling if not done in two steps :|
    const openConnectionUri = decodeUriComponentProperly(
      routerParams["connectionUri"] || routerParams["openConversation"]
    );

    if (openConnectionUri) {
      return openConnectionUri;
    } else {
      return undefined;
    }
  }
);

export const getFromAtomUriFromRoute = createSelector(
  state => state,
  state => {
    const encodedAtomUri = getIn(state, [
      "router",
      "currentParams",
      "fromAtomUri",
    ]);
    return decodeUriComponentProperly(encodedAtomUri);
  }
);

export const getHolderUriFromRoute = createSelector(
  state => state,
  state => {
    const encodedHolderUri = getIn(state, [
      "router",
      "currentParams",
      "holderUri",
    ]);
    return decodeUriComponentProperly(encodedHolderUri);
  }
);

export const getModeFromRoute = createSelector(
  state => state,
  state => {
    const mode = getIn(state, ["router", "currentParams", "mode"]);

    if (mode) {
      if (mode === "CONNECT") {
        return "CONNECT";
      } else if (mode === "EDIT") {
        return "EDIT";
      }
      return "DUPLICATE";
    }
    return undefined;
  }
);

export const getPostUriFromRoute = createSelector(
  state => state,
  state => {
    const encodedPostUri = getIn(state, ["router", "currentParams", "postUri"]);
    return decodeUriComponentProperly(encodedPostUri);
  }
);

export const getAboutSectionFromRoute = createSelector(
  state => state,
  state => {
    return getIn(state, ["router", "currentParams", "aboutSection"]);
  }
);

export function getOwnedPersonas(state) {
  const atoms = getOwnedAtoms(state);
  return atoms && atoms.filter(atom => atomUtils.isPersona(atom));
}

export function getOwnedAtomsWithBuddySocket(state) {
  const atoms = getOwnedAtoms(state);
  return atoms && atoms.filter(atom => atomUtils.hasBuddySocket(atom));
}

/**
 * Returns all owned Personas as a List, condenses the information of the persona so that only some attributes are included.
 * This Function is currently used for persona lists/views based on elm (as they are not based on our general atom-structure)
 * @param state
 * @returns {Iterable<K, {website: *, saved: boolean, displayName: *, url: *, aboutMe: *, timestamp: string | * | number | void}>}
 */
export function getOwnedCondensedPersonaList(state, includeArchived = false) {
  const atoms = getOwnedAtoms(state);
  const personas = atoms
    .toList()
    .filter(atom => atomUtils.isPersona(atom))
    .filter(atom => includeArchived || atomUtils.isActive(atom));
  return personas
    .map(persona => {
      return {
        displayName: getIn(persona, ["content", "personaName"]),
        website: getIn(persona, ["content", "website"]),
        aboutMe: getIn(persona, ["content", "description"]),
        url: get(persona, "uri"),
        saved: !get(persona, "isBeingCreated"),
        timestamp: get(persona, "creationDate").toISOString(),
      };
    })
    .filter(persona => !!persona.displayName);
}

//TODO: move this method to a place that makes more sense, its not really a selector function
export function currentSkin() {
  const style = getComputedStyle(document.body);
  const getColor = name => {
    const color = Color(style.getPropertyValue(name).trim());
    return color.rgb().object();
  };
  return {
    primaryColor: getColor("--won-primary-color"),
    lightGray: getColor("--won-light-gray"),
    lineGray: getColor("--won-line-gray"),
    subtitleGray: getColor("--won-subtitle-gray"),
  };
}
/**
 * Returns true if the atom is owned by the user who is currently logged in
 * @param state FULL redux state, no substates allowed
 * @param atomUri
 */
export function isAtomOwned(state, atomUri) {
  if (atomUri) {
    const accountState = get(state, "account");
    return accountUtils.isAtomOwned(accountState, atomUri);
  }
  return false;
}

/**
 * This checks if the given atomUri is allowed to be used as a template,
 * it is only allowed if the atom exists is NOT owned, and if it has a matchedUseCase
 * @param atomUri
 * @returns {*|boolean}
 */
export function isAtomUsableAsTemplate(state, atomUri) {
  const atom = getIn(state, ["atoms", atomUri]);

  return (
    !!atom && !isAtomOwned(state, atomUri) && atomUtils.hasMatchedUseCase(atom)
  );
}

/**
 * This checks if the given atomUri is allowed to be edited,
 * it is only allowed if the atom exists, and if it IS owned and has a matchedUseCase
 * @param atom
 * @returns {*|boolean}
 */
export function isAtomEditable(state, atomUri) {
  const atom = getIn(state, ["atoms", atomUri]);

  return (
    !!atom && isAtomOwned(state, atomUri) && atomUtils.hasMatchedUseCase(atom)
  );
}

export function isLocationAccessDenied(state) {
  const viewState = get(state, "view");
  return viewState && viewUtils.isLocationAccessDenied(viewState);
}

export function getCurrentLocation(state) {
  const viewState = get(state, "view");
  return viewState && viewUtils.getCurrentLocation(viewState);
}

export function getAtomUriBySocketUri(socketUri) {
  return socketUri && socketUri.split("#")[0];
}

function decodeUriComponentProperly(encodedUri) {
  if (!encodedUri) return undefined;
  //for some reason decodeUri(undefined) yields "undefined"
  else return decodeURIComponent(encodedUri);
}
