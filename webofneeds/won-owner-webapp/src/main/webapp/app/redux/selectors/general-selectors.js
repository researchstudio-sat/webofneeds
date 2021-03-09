/**
 * Created by ksinger on 22.01.2016.
 */

import { createSelector } from "reselect";

import {
  getIn,
  get,
  getUri,
  extractAtomUriFromConnectionUri,
} from "../../utils.js";
import * as atomUtils from "../utils/atom-utils.js";
import * as connectionUtils from "../utils/connection-utils.js";
import * as accountUtils from "../utils/account-utils.js";
import * as processUtils from "../utils/process-utils.js";
import * as viewUtils from "../utils/view-utils.js";
import Immutable from "immutable";
import vocab from "../../service/vocab";

export const selectLastUpdateTime = state => get(state, "lastUpdateTime");

export const emptyMapSelector = createSelector(() => Immutable.Map());

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

export const getActivePinnedAtom = atomUri =>
  createSelector(getOwnedPinnedAtoms, atoms => get(atoms, atomUri));

export const getAtom = atomUri =>
  createSelector(getAtoms, atoms => get(atoms, atomUri));

export const getOwnedConnection = connectionUri =>
  createSelector(getOwnedConnections, conns => get(conns, connectionUri));

export const getOwnedAtoms = createSelector(
  getOwnedAtomUris,
  getAtoms,
  (ownedAtomUris, allAtoms) =>
    ownedAtomUris
      ? ownedAtomUris
          .toMap()
          .map(atomUri => get(allAtoms, atomUri))
          .filter(atom => !!atom)
      : Immutable.Map()
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

export const getUnassignedUnpinnedAtoms = createSelector(
  getOwnedAtoms,
  ownedAtoms =>
    ownedAtoms.filter(
      atom =>
        !atomUtils.isPinnedAtom(atom) &&
        !atomUtils.isPinnedAtom(get(ownedAtoms, atomUtils.getHeldByUri(atom)))
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
      const isAtomOwned = accountUtils.isAtomOwned(accountState, getUri(atom));

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
                const targetSocketUri = connectionUtils.getTargetSocketUri(
                  conn
                );
                return !ownedConnectionsToSocketUri.find(ownedConnection =>
                  connectionUtils.hasSocketUri(ownedConnection, targetSocketUri)
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
        .filter(atomUtils.isActive)
        .flatMap(atom =>
          atomUtils.getAllConnectionsWithTargetSocketUri(atom, targetSocketUri)
        )
  );

export const getAllUnassignedUnpinnedChatConnections = createSelector(
  getUnassignedUnpinnedAtoms,
  allOwnedAtoms =>
    allOwnedAtoms &&
    allOwnedAtoms
      .filter(atomUtils.isActive)
      .filter(atomUtils.hasChatSocket)
      .flatMap(atom =>
        atomUtils.getConnections(atom, vocab.CHAT.ChatSocketCompacted)
      )
      .toOrderedMap()
      .sortBy(conn => {
        const lastUpdateDate = connectionUtils.getLastUpdateDate(conn);
        return lastUpdateDate && lastUpdateDate.getTime();
      })
      .reverse()
);

export const getAllPartnerActivityConnections = createSelector(
  getOwnedAtoms,
  allOwnedAtoms =>
    allOwnedAtoms &&
    allOwnedAtoms
      .filter(atomUtils.isActive)
      .filter(atomUtils.hasPartnerActivitySocket)
      .flatMap(atomUtils.getAllNonClosedNonSuggestedPartnerActivityConnections)
      .toOrderedMap()
      .sortBy(conn => {
        const lastUpdateDate = connectionUtils.getLastUpdateDate(conn);
        return lastUpdateDate && lastUpdateDate.getTime();
      })
      .reverse()
);

export const getAllConnectedChatAndGroupConnections = createSelector(
  getOwnedAtoms,
  allOwnedAtoms =>
    allOwnedAtoms &&
    allOwnedAtoms
      .filter(atomUtils.isActive)
      .filter(
        atom => atomUtils.hasChatSocket(atom) || atomUtils.hasGroupSocket(atom)
      )
      .flatMap(atomUtils.getAllConnectedChatAndGroupConnections)
);

/**
 * Determines if there are any UnassignedUnpinnedAtoms that have connections that are unread
 * (used for the inventory unread indicator)
 * @param state
 * @returns {boolean}
 */
export const hasUnassignedUnpinnedAtomUnreads = createSelector(
  getUnassignedUnpinnedAtoms,
  ownedAtoms =>
    !!ownedAtoms
      .filter(atomUtils.isActive)
      .find(atom => atomUtils.hasUnreadConnections(atom))
);

export const getOwnedPinnedAtomsUnreads = createSelector(
  getOwnedAtoms,
  ownedAtoms =>
    ownedAtoms.filter(atomUtils.isPinnedAtom).map(atom => {
      if (atomUtils.hasUnreadConnections(atom)) {
        return true;
      } else {
        for (let holdableSocketType in vocab.holderSockets) {
          const targetAtomUris = atomUtils
            .getConnections(atom, vocab.holderSockets[holdableSocketType])
            .map(connectionUtils.getTargetAtomUri)
            .valueSeq()
            .toSet();

          if (
            targetAtomUris.find(atomUri =>
              atomUtils.hasUnreadConnections(
                get(ownedAtoms, atomUri),
                holdableSocketType,
                true
              )
            )
          ) {
            return true;
          }
        }
        return false;
      }
    })
);

/**
 * Returns all chatConnections of a pinnedAtom (includes all chatConnections of held atoms of the pinned atoms)
 * @param atomUri
 * @returns {Reselect.Selector<unknown, *|string|boolean|undefined>}
 */
export const getChatConnectionsOfActivePinnedAtom = atomUri =>
  createSelector(
    getOwnedAtoms,
    getActivePinnedAtom(atomUri),
    (ownedAtoms, activePinnedAtom) => {
      if (activePinnedAtom) {
        let chatConnections = atomUtils.getConnections(
          activePinnedAtom,
          vocab.CHAT.ChatSocketCompacted
        );

        for (let holdableSocketType in vocab.holderSockets) {
          atomUtils
            .getConnections(
              activePinnedAtom,
              vocab.holderSockets[holdableSocketType]
            )
            .map(conn => {
              const targetAtom = get(
                ownedAtoms,
                connectionUtils.getTargetAtomUri(conn)
              );
              if (targetAtom) {
                chatConnections = chatConnections.merge(
                  atomUtils.getConnections(
                    targetAtom,
                    vocab.CHAT.ChatSocketCompacted
                  )
                );
              }
            });
        }

        return chatConnections
          .toOrderedMap()
          .sortBy(conn => {
            const lastUpdateDate = connectionUtils.getLastUpdateDate(conn);
            return lastUpdateDate && lastUpdateDate.getTime();
          })
          .reverse();
      }
    }
  );

export const hasUnassignedUnpinnedAtomChatUnreads = createSelector(
  getUnassignedUnpinnedAtoms,
  ownedAtoms =>
    !!ownedAtoms
      .filter(atomUtils.isActive)
      .find(atom =>
        atomUtils.hasUnreadConnections(atom, vocab.CHAT.ChatSocketCompacted)
      )
);

export const hasUnassignedUnpinnedAtomNonChatUnreads = createSelector(
  getUnassignedUnpinnedAtoms,
  ownedAtoms =>
    !!ownedAtoms
      .filter(atomUtils.isActive)
      .find(atom =>
        atomUtils.hasUnreadConnections(
          atom,
          vocab.CHAT.ChatSocketCompacted,
          true
        )
      )
);

export const getActiveAtoms = createSelector(
  getAtoms,
  allAtoms => allAtoms && allAtoms.filter(atomUtils.isActive)
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

export const getOwnedPersonas = createSelector(getOwnedAtoms, ownedAtoms =>
  ownedAtoms.filter(atomUtils.isPersona)
);

export const getOwnedPinnedAtoms = createSelector(getOwnedAtoms, ownedAtoms =>
  ownedAtoms.filter(atomUtils.isPinnedAtom)
);

export const getOwnedAtomsWithBuddySocket = createSelector(
  getOwnedAtoms,
  ownedAtoms => ownedAtoms.filter(atomUtils.hasBuddySocket)
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
  const connectionUri = getUri(connection);
  const senderSocketUri = connectionUtils.getSocketUri(connection);
  const senderAtom =
    connectionUri &&
    allAtoms &&
    (get(allAtoms, extractAtomUriFromConnectionUri(connectionUri)) ||
      allAtoms.find(atom => atomUtils.getConnection(atom, connectionUri)));

  return senderAtom && atomUtils.getSocketType(senderAtom, senderSocketUri);
};

export const getTargetSocketType = (allAtoms, connection) => {
  const targetAtomUri = connectionUtils.getTargetAtomUri(connection);
  const targetSocketUri = connectionUtils.getTargetSocketUri(connection);

  return (
    targetSocketUri &&
    getIn(allAtoms, [targetAtomUri, "content", "sockets", targetSocketUri])
  );
};

/**
 * Get all connections stored within your own atoms as a map
 * @returns Immutable.Map with all connections
 */
export const getOwnedConnections = createSelector(getOwnedAtoms, ownedAtoms =>
  ownedAtoms.flatMap(atom => atomUtils.getConnections(atom))
);

export const getConnections = createSelector(
  getAtoms,
  atoms => atoms && atoms.flatMap(atom => atomUtils.getConnections(atom))
);

export const getConnectionsWithTargetAtomUri = targetAtomUri =>
  createSelector(
    getConnections,
    connections =>
      connections &&
      connections.filter(
        conn => connectionUtils.getTargetAtomUri(conn) === targetAtomUri
      )
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

/**
 * Get a prioritized List of ConnectionContainers To Crawl
 * The priority is as follows (ignore Atoms that aren't loaded yet):
 *  1. Connection Container of activePinnedAtom
 *  2. Connection Containers of pinnedAtoms
 *  3. Connection Containers of ownedAtoms
 *  4. Connection Containers of otherAtoms
 *
 */
export const getConnectionContainersToCrawl = createSelector(
  getProcessState,
  getViewState,
  getOwnedPinnedAtoms,
  getOwnedAtoms,
  getAtoms,
  (processState, viewState, ownedPinnedAtoms, ownedAtoms, atoms) => {
    const activePinnedAtomUri = viewUtils.getActivePinnedAtomUri(viewState);

    if (
      !processUtils.isAtomToLoad(processState, activePinnedAtomUri) &&
      processUtils.isConnectionContainerToLoad(
        processState,
        activePinnedAtomUri
      )
    ) {
      console.debug("Fetch connectionContainer of activePinnedAtom");
      return (
        ownedPinnedAtoms &&
        ownedPinnedAtoms
          .filter(atom => getUri(atom) === activePinnedAtomUri)
          .filter(
            atom => !processUtils.isAtomToLoad(processState, getUri(atom))
          )
          .map(atom =>
            processUtils.getConnectionContainerStatus(
              processState,
              getUri(atom)
            )
          )
      );
    }

    const ownedPinnedAtomsConnectionContainersToCrawl =
      ownedPinnedAtoms &&
      ownedPinnedAtoms
        .filter(atom => !processUtils.isAtomToLoad(processState, getUri(atom)))
        .filter(atom =>
          processUtils.isConnectionContainerToLoad(processState, getUri(atom))
        )
        .map(atom =>
          processUtils.getConnectionContainerStatus(processState, getUri(atom))
        );

    if (
      ownedPinnedAtomsConnectionContainersToCrawl &&
      ownedPinnedAtomsConnectionContainersToCrawl.size > 0
    ) {
      console.debug("Fetch connectionContainers of ownedPinnedAtoms");
      return ownedPinnedAtomsConnectionContainersToCrawl;
    }

    const ownedAtomsConnectionContainersToCrawl = ownedAtoms
      .filter(atom => !processUtils.isAtomToLoad(processState, getUri(atom)))
      .filter(atom =>
        processUtils.isConnectionContainerToLoad(processState, getUri(atom))
      )
      .map(atom =>
        processUtils.getConnectionContainerStatus(processState, getUri(atom))
      );

    if (
      ownedAtomsConnectionContainersToCrawl &&
      ownedAtomsConnectionContainersToCrawl.size > 0
    ) {
      console.debug("Fetch connectionContainers of ownedAtoms");
      return ownedAtomsConnectionContainersToCrawl;
    }

    const atomsConnectionContainersToCrawl =
      atoms &&
      atoms
        .filter(atom => !processUtils.isAtomToLoad(processState, getUri(atom)))
        .filter(atom =>
          processUtils.isConnectionContainerToLoad(processState, getUri(atom))
        )
        .map(atom =>
          processUtils.getConnectionContainerStatus(processState, getUri(atom))
        );

    if (
      atomsConnectionContainersToCrawl &&
      atomsConnectionContainersToCrawl.size > 0
    ) {
      console.debug("Fetch connectionContainers of atoms");
      return atomsConnectionContainersToCrawl;
    }

    return undefined;
  }
);

export const getAllValidRequestCredentialsForAtom = atomUri =>
  createSelector(
    getAtoms,
    getOwnedAtoms,
    getConnectionsWithTargetAtomUri(atomUri),
    (atoms, ownedAtoms, connectionsToTargetAtom) => {
      const validRequestCredentials = [];

      if (get(ownedAtoms, atomUri)) {
        validRequestCredentials.push({ requesterWebId: atomUri });
      } else {
        const ownedConnectionsToTargetAtom = connectionsToTargetAtom.filter(
          (_, connUri) =>
            !!get(ownedAtoms, extractAtomUriFromConnectionUri(connUri))
        );

        ownedConnectionsToTargetAtom.map((_, connUri) => {
          validRequestCredentials.push({
            requesterWebId: extractAtomUriFromConnectionUri(connUri),
          });
        });

        //TODO: ADD POSSIBLE TOKEN CREDENTIALS/RETRIEVALS as seen in determineRequestCredentials function
        //TODO: EITHER REMOVE ALREADY FETCHED AND THEREFORE DISCARDED CREDENTIALS HERE OR IMPLEMENT ANOTHER METHOD THAT INCLUDES THAT
      }
      return Immutable.fromJS(validRequestCredentials);
    }
  );
