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

export const getAccountState = state => get(state, "account");

export const getAtoms = createSelector(
  state => state,
  state => get(state, "atoms")
);

export const getOwnedAtomUris = createSelector(getAccountState, account =>
  get(account, "ownedAtomUris")
);

export const getOwnedAtoms = createSelector(
  getOwnedAtomUris,
  getAtoms,
  (ownedAtomUris, allAtoms) =>
    ownedAtomUris &&
    ownedAtomUris
      .toMap()
      .map(atomUri => get(allAtoms, atomUri))
      .filter(atom => !!atom)
);

export const getWhatsNewUris = state => getIn(state, ["owner", "whatsNewUris"]);
export const getWhatsAroundUris = state =>
  getIn(state, ["owner", "whatsAroundUris"]);

export const getWhatsNewAtoms = createSelector(
  getWhatsNewUris,
  getAtoms,
  (whatsNewAtomUris, allAtoms) =>
    whatsNewAtomUris &&
    whatsNewAtomUris
      .toMap()
      .map(atomUri => get(allAtoms, atomUri))
      .filter(atom => !!atom)
);

export const getWhatsAroundAtoms = createSelector(
  getWhatsAroundUris,
  getAtoms,
  (whatsAroundAtomUris, allAtoms) =>
    whatsAroundAtomUris &&
    whatsAroundAtomUris
      .toMap()
      .map(atomUri => get(allAtoms, atomUri))
      .filter(atom => !!atom)
);

export const getPosts = createSelector(getAtoms, atoms =>
  atoms.filter(atom => {
    if (!atom.getIn(["content", "type"])) return true;

    return atomUtils.isAtom(atom) && !atomUtils.isPersona(atom);
  })
);

export const getOwnedPosts = createSelector(
  getOwnedAtoms,
  ownedAtoms =>
    ownedAtoms &&
    ownedAtoms.filter(
      ownedAtom =>
        !getIn(ownedAtom, ["content", "type"]) ||
        (atomUtils.isAtom(ownedAtom) && !atomUtils.isPersona(ownedAtom))
    )
);

export const getAllChatConnections = createSelector(
  getOwnedAtoms,
  allOwnedAtoms =>
    allOwnedAtoms &&
    allOwnedAtoms
      .filter(atom => atomUtils.isActive(atom))
      .filter(atom => atomUtils.hasChatSocket(atom))
      .flatMap(atom =>
        atomUtils.getAllNonClosedNonSuggestedChatConnections(atom)
      )
);

export const hasChatConnections = createSelector(
  getAllChatConnections,
  chatConnections => chatConnections && chatConnections.size > 0
);

/**
 * Determines if there are any connections that are unread and suggested
 * (used for the inventory unread indicator)
 * @param state
 * @returns {boolean}
 */
export const hasUnreadSuggestedConnections = createSelector(
  getOwnedAtoms,
  ownedAtoms =>
    ownedAtoms &&
    !!ownedAtoms
      .filter(atom => atomUtils.isActive(atom))
      .find(atom => atomUtils.hasUnreadSuggestedConnections(atom))
);

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

export const hasUnreadChatConnections = createSelector(
  getAllChatConnections,
  chatConnections =>
    chatConnections &&
    !!chatConnections.find(
      conn =>
        !(
          connectionUtils.isClosed(conn) || connectionUtils.isSuggested(conn)
        ) && connectionUtils.isUnread(conn)
    )
);

export const getActiveAtoms = createSelector(
  getAtoms,
  allAtoms => allAtoms && allAtoms.filter(atom => atomUtils.isActive(atom))
);

export const getOwnedAtomsInCreation = createSelector(
  getOwnedAtoms,
  ownedAtoms =>
    ownedAtoms && ownedAtoms.filter(atom => get(atom, "isBeingCreated"))
);

export const selectIsConnected = state =>
  !state.getIn(["messages", "reconnecting"]) &&
  !state.getIn(["messages", "lostConnection"]);

/**
 * Get the atom for a given connectionUri
 * @param state to retrieve data from
 * @param connectionUri to find corresponding atom for
 */
export function getOwnedAtomByConnectionUri(state, connectionUri) {
  const atoms = connectionUri && getOwnedAtoms(state); //we only check own atoms as these are the only ones who have connections stored
  return (
    atoms &&
    (getIn(atoms, connectionUri.split("/c")[0]) ||
      atoms.find(atom => atom.getIn(["connections", connectionUri])))
  );
}

export const getOwnedPersonas = createSelector(
  getOwnedAtoms,
  ownedAtoms =>
    ownedAtoms && ownedAtoms.filter(atom => atomUtils.isPersona(atom))
);

export const getOwnedAtomsWithBuddySocket = createSelector(
  getOwnedAtoms,
  ownedAtoms =>
    ownedAtoms && ownedAtoms.filter(atom => atomUtils.hasBuddySocket(atom))
);

/**
 * Returns all owned Personas as a List, condenses the information of the persona so that only some attributes are included.
 * This Function is currently used for persona lists/views based on elm (as they are not based on our general atom-structure)
 * @param state
 * @returns {Iterable<K, {website: *, saved: boolean, displayName: *, url: *, aboutMe: *, timestamp: string | * | number | void}>}
 */
export const getOwnedCondensedPersonaList = createSelector(
  getOwnedPersonas,
  ownedPersonas => {
    return (
      ownedPersonas &&
      ownedPersonas
        .filter(persona => atomUtils.isActive(persona))
        .map(persona => ({
          displayName: getIn(persona, ["content", "personaName"]),
          website: getIn(persona, ["content", "website"]),
          aboutMe: getIn(persona, ["content", "description"]),
          url: get(persona, "uri"),
          saved: !get(persona, "isBeingCreated"),
          timestamp: get(persona, "creationDate").toISOString(),
        }))
        .filter(persona => !!persona.displayName)
        .toList()
    );
  }
);

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
    const accountState = getAccountState(state);
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

export const isLocationAccessDenied = createSelector(
  state => get(state, "view"),
  viewState => viewState && viewUtils.isLocationAccessDenied(viewState)
);

export const getCurrentLocation = createSelector(
  state => get(state, "view"),
  viewState => viewState && viewUtils.getCurrentLocation(viewState)
);

export function getAtomUriBySocketUri(socketUri) {
  return socketUri && socketUri.split("#")[0];
}
