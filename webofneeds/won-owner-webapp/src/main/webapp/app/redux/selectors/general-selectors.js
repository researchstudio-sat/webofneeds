/**
 * Created by ksinger on 22.01.2016.
 */

import { createSelector } from "reselect";

import { getIn, get, extractAtomUriFromConnectionUri } from "../../utils.js";
import * as atomUtils from "../utils/atom-utils.js";
import * as connectionUtils from "../utils/connection-utils.js";
import * as accountUtils from "../utils/account-utils.js";
import * as processUtils from "../utils/process-utils.js";
import * as viewUtils from "../utils/view-utils.js";
import Immutable from "immutable";
import vocab from "../../service/vocab";

export const selectLastUpdateTime = state => get(state, "lastUpdateTime");

export const getAccountState = createSelector(
  state => get(state, "account"),
  state => state
);
export const getProcessState = createSelector(
  state => get(state, "process"),
  state => state
);
export const getViewState = createSelector(
  state => get(state, "view"),
  state => state
);

export const getExternalDataState = createSelector(
  state => get(state, "externalData"),
  state => state
);

export const getAtoms = createSelector(
  state => state,
  state => get(state, "atoms")
);

export const getConfigState = createSelector(
  state => state,
  state => get(state, "config")
);

export const getMessagesState = createSelector(
  state => state,
  state => get(state, "messages")
);

export const isWaitingForAnswer = msgUri =>
  createSelector(getMessagesState, messagesState =>
    getIn(messagesState, ["waitingForAnswer", msgUri])
  );

export const getTheme = createSelector(getConfigState, configState =>
  get(configState, "theme")
);

export const getVisibleUseCasesByConfig = createSelector(getTheme, theme =>
  get(theme, "visibleUseCases")
);

export const getDefaultNodeUri = createSelector(getConfigState, configState =>
  get(configState, "defaultNodeUri")
);

export const getOwnedAtomUris = createSelector(getAccountState, account =>
  get(account, "ownedAtomUris")
);

export const getAtom = atomUri =>
  createSelector(getAtoms, atoms => get(atoms, atomUri));

export const getOwnedConnection = connectionUri =>
  createSelector(getOwnedConnections, conns => get(conns, connectionUri));

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

/**
 * Returns all connections of the Atom if the atom is owned,
 * if the atom is not owned, we take all the connections of the non owned atom, filter out all the connections
 * that exists from our own atoms to the atom, and merge the owned connections into the connections Map that is
 * returned
 * @param atomUri
 * @returns A Map of Maps {[socketType]: Map<ConnUri>: [connections]}
 */
export const getConnectionsOfAtomWithOwnedTargetConnections = atomUri =>
  createSelector(
    state => state,
    getAccountState,
    getAtom(atomUri),
    (state, accountState, atom) => {
      const isAtomOwned = accountUtils.isAtomOwned(
        accountState,
        get(atom, "uri")
      );

      return atomUtils
        .getSockets(atom)
        .flip()
        .map((_socketUri, _socketType) => {
          if (isAtomOwned) {
            return atomUtils.getConnections(atom, _socketType);
          } else {
            const ownedConnectionsToSocketUri = getAllOwnedConnectionsWithTargetSocketUri(
              _socketUri
            )(state);
            return atomUtils
              .getConnections(atom, _socketType)
              .filter(conn => {
                //Filters out all connections that have a "counterpart" connection stored in another atom we own
                const targetSocketUri = get(conn, "targetSocketUri");
                return !ownedConnectionsToSocketUri.find(
                  ownedConnection =>
                    get(ownedConnection, "socketUri") === targetSocketUri
                );
              })
              .merge(ownedConnectionsToSocketUri);
          }
        });
    }
  );

export const getAllOwnedConnectionsWithTargetSocketUri = targetSocketUri =>
  createSelector(
    getOwnedAtoms,
    allOwnedAtoms =>
      allOwnedAtoms &&
      allOwnedAtoms
        .filter(atom => atomUtils.isActive(atom))
        .flatMap(atom =>
          atomUtils.getAllConnectionsWithTargetSocketUri(atom, targetSocketUri)
        )
  );

export const getOwnedAllConnections = createSelector(
  getOwnedAtoms,
  allOwnedAtoms =>
    allOwnedAtoms &&
    allOwnedAtoms.flatMap(atom => atomUtils.getConnections(atom))
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
      .toOrderedMap()
      .sortBy(conn => {
        const lastUpdateDate = get(conn, "lastUpdateDate");
        return lastUpdateDate && lastUpdateDate.getTime();
      })
      .reverse()
);

export const getAllConnectedChatAndGroupConnections = createSelector(
  getOwnedAtoms,
  allOwnedAtoms =>
    allOwnedAtoms &&
    allOwnedAtoms
      .filter(atom => atomUtils.isActive(atom))
      .filter(
        atom => atomUtils.hasChatSocket(atom) || atomUtils.hasGroupSocket(atom)
      )
      .flatMap(atom => atomUtils.getAllConnectedChatAndGroupConnections(atom))
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

/**
 * Determines if there are any buddy connections that are unread
 * (used for the inventory unread indicator)
 * @param state
 * @returns {function(state)}
 */
export const hasUnreadBuddyConnections = (excludeClosed, excludeSuggested) =>
  createSelector(
    state => state,
    getOwnedAtoms,
    (state, allOwnedAtoms) =>
      allOwnedAtoms &&
      !!allOwnedAtoms
        .filter(atomUtils.isActive)
        .find(
          atom =>
            !!getBuddyConnectionsByAtomUri(
              get(atom, "uri"),
              excludeClosed,
              excludeSuggested
            )(state).find(conn => connectionUtils.isUnread(conn))
        )
  );

/**
 * Returns all buddyConnections of an atom
 * @param state
 * @param atomUri
 * @param excludeClosed  -> exclude Closed connections
 * @param excludeSuggested -> exclude Suggested connections
 * @returns {function(state)}
 */
const getBuddyConnectionsByAtomUri = (
  atomUri,
  excludeClosed,
  excludeSuggested
) =>
  createSelector(getAtoms, atoms => {
    const atom = get(atoms, atomUri);
    const connections = atomUtils.getConnections(atom);

    return connections
      ? connections
          .filter(conn => !(excludeClosed && connectionUtils.isClosed(conn)))
          .filter(
            conn => !(excludeSuggested && connectionUtils.isSuggested(conn))
          )
          .filter(conn => isBuddyConnection(atoms, conn))
      : Immutable.Map();
  });

/**
 * Returns true if both sockets are BuddySockets
 * @param allAtoms all atoms of the state
 * @param connection to check sockettypes of
 * @returns {boolean}
 */
function isBuddyConnection(allAtoms, connection) {
  return (
    getSenderSocketType(allAtoms, connection) ===
      vocab.BUDDY.BuddySocketCompacted &&
    getTargetSocketType(allAtoms, connection) ===
      vocab.BUDDY.BuddySocketCompacted
  );
}

export const hasUnreadChatConnections = createSelector(
  getAllChatConnections,
  chatConnections =>
    chatConnections &&
    !!chatConnections.find(
      conn =>
        connectionUtils.isUnread(conn) &&
        !(connectionUtils.isClosed(conn) || connectionUtils.isSuggested(conn))
    )
);

export const getActiveAtoms = createSelector(
  getAtoms,
  allAtoms => allAtoms && allAtoms.filter(atom => atomUtils.isActive(atom))
);

export const selectIsConnected = createSelector(
  getMessagesState,
  messagesState =>
    !get(messagesState, "reconnecting") && !get(messagesState, "lostConnection")
);

/**
 * Get the atom for a given connectionUri
 * @param state to retrieve data from
 * @param connectionUri to find corresponding atom for
 */
export const getOwnedAtomByConnectionUri = connectionUri =>
  createSelector(
    getOwnedAtoms,
    atoms =>
      connectionUri &&
      atoms &&
      (get(atoms, extractAtomUriFromConnectionUri(connectionUri)) ||
        atoms.find(atom => atomUtils.getConnection(atom, connectionUri)))
  );

export const getAtomByConnectionUri = connectionUri =>
  createSelector(
    getAtoms,
    atoms =>
      connectionUri &&
      atoms &&
      (get(atoms, extractAtomUriFromConnectionUri(connectionUri)) ||
        atoms.find(atom => atomUtils.getConnection(atom, connectionUri)))
  );

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
 * Returns true if the atom is owned by the user who is currently logged in
 * @param atomUri
 * @return function(state)
 */
export const isAtomOwned = atomUri =>
  createSelector(getAccountState, accountState =>
    accountUtils.isAtomOwned(accountState, atomUri)
  );

/**
 * This checks if the given atomUri is allowed to be used as a template,
 * it is only allowed if the atom exists is NOT owned, and if it has a matchedUseCase
 * @param atomUri
 * @returns {func(state) -> returns bool}
 */
export const isAtomUsableAsTemplate = atomUri =>
  createSelector(
    getAtom(atomUri),
    isAtomOwned(atomUri),
    (atom, isOwned) => !!atom && !isOwned && atomUtils.hasMatchedUseCase(atom)
  );

/**
 * This checks if the given atomUri is allowed to be edited,
 * it is only allowed if the atom exists, and if it IS owned and has a matchedUseCase
 * @param atom
 * @returns {func(state) -> returns bool}
 */
export const isAtomEditable = atomUri =>
  createSelector(
    getAtom(atomUri),
    isAtomOwned(atomUri),
    (atom, isOwned) => !!atom && isOwned && atomUtils.hasMatchedUseCase(atom)
  );

export const isLocationAccessDenied = createSelector(
  getViewState,
  viewState => viewState && viewUtils.isLocationAccessDenied(viewState)
);

export const getCurrentLocation = createSelector(
  getViewState,
  viewState => viewState && viewUtils.getCurrentLocation(viewState)
);

export const getSenderSocketType = (allAtoms, connection) => {
  const connectionUri = get(connection, "uri");
  const senderSocketUri = get(connection, "socketUri");
  const senderAtom =
    connectionUri &&
    allAtoms &&
    (get(allAtoms, extractAtomUriFromConnectionUri(connectionUri)) ||
      allAtoms.find(atom => atomUtils.getConnection(atom, connectionUri)));

  return senderAtom && atomUtils.getSocketType(senderAtom, senderSocketUri);
};

export const getTargetSocketType = (allAtoms, connection) => {
  const targetAtomUri = connectionUtils.getTargetAtomUri(connection);
  const targetSocketUri = get(connection, "targetSocketUri");

  return (
    targetSocketUri &&
    getIn(allAtoms, [targetAtomUri, "content", "sockets", targetSocketUri])
  );
};

/**
 * Get all connections stored within your own atoms as a map
 * @returns Immutable.Map with all connections
 */
export const getOwnedConnections = createSelector(
  getOwnedAtoms,
  ownedAtoms =>
    ownedAtoms && ownedAtoms.flatMap(atom => atomUtils.getConnections(atom))
);

/**
 * Get all the connectionUris storid within the state
 */
export const getOwnedConnectionUris = createSelector(
  getOwnedConnections,
  ownedConnections => ownedConnections && ownedConnections.keySeq().toSet()
);

export const getConnectionsToCrawl = createSelector(
  getProcessState,
  getAllConnectedChatAndGroupConnections,
  (process, connections) =>
    connections
      ? connections
          .filter(conn => connectionUtils.getMessagesSize(conn) === 0)
          .filter(
            (conn, connUri) =>
              !processUtils.isConnectionLoading(process, connUri) &&
              !processUtils.isConnectionLoadingMessages(process, connUri) &&
              !processUtils.hasConnectionFailedToLoad(process, connUri) &&
              processUtils.hasMessagesToLoad(process, connUri)
          )
      : Immutable.Map()
);
