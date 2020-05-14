/**
 * Created by fsuda on 05.11.2018.
 */

import { createSelector } from "reselect";

import Immutable from "immutable";
import {
  getOwnedAtomByConnectionUri,
  getOwnedAtoms,
  getSenderSocketType,
  getTargetSocketType,
  getAllConnectedChatAndGroupConnections,
} from "./general-selectors.js";
import * as connectionUtils from "../utils/connection-utils.js";
import vocab from "../../service/vocab.js";
import { get, getIn } from "../../utils.js";
import * as processUtils from "../utils/process-utils.js";

/**
 * Get the connection for a given connectionUri
 * @param state to retrieve data from
 * @param connectionUri to find corresponding connection for
 */
export function getOwnedConnectionByUri(state, connectionUri) {
  let atom = getOwnedAtomByConnectionUri(state, connectionUri);
  return getIn(atom, ["connections", connectionUri]);
}

/**
 * Get all connections stored within your own atoms as a map
 * @returns Immutable.Map with all connections
 */
export const getOwnedConnections = createSelector(
  getOwnedAtoms,
  ownedAtoms =>
    ownedAtoms && ownedAtoms.flatMap(atom => get(atom, "connections"))
);

/**
 * Get all the connectionUris storid within the state
 */
export const getOwnedConnectionUris = createSelector(
  getOwnedConnections,
  ownedConnections => ownedConnections && ownedConnections.keySeq().toSet()
);

export const getConnectionsToCrawl = createSelector(
  state => get(state, "process"),
  getAllConnectedChatAndGroupConnections,
  (process, connections) =>
    connections
      ? connections
          .filter(
            conn => !conn.get("messages") || conn.get("messages").size === 0
            // the check below (if connectMessage was present) was replaced by if any messages are available (if any are there this connection is not to be fetched anymore)
            // !!conn
            //   .get("messages")
            //   .find(msg => msg.get("messageType") === vocab.WONMSG.connectMessage)
          )
          .filter(conn => {
            const connUri = get(conn, "uri");

            return (
              !processUtils.isConnectionLoading(process, connUri) &&
              !processUtils.isConnectionLoadingMessages(process, connUri) &&
              !processUtils.hasConnectionFailedToLoad(process, connUri) &&
              processUtils.hasMessagesToLoad(process, connUri)
            );
          })
      : Immutable.Map()
);

export function getConnectionsToInjectMsgInto(atoms, targetSocketUri, msgUri) {
  const allConnections =
    atoms && atoms.flatMap(atom => atom.get("connections"));

  return allConnections
    .filter(conn => connectionUtils.isConnected(conn))
    .filter(conn => get(conn, "targetSocketUri") === targetSocketUri)
    .filter(conn => !get(conn, "messages").contains(msgUri));
}

export function hasMessagesToLoad(state, connUri) {
  const messageProcess = getIn(state, [
    "process",
    "connections",
    connUri,
    "messages",
  ]);

  return messageProcess && !!messageProcess.find(msg => msg.get("toLoad"));
}

/**
 * Returns true if socket is a ChatSocket and targetSocket is a GroupSocket
 * @param allAtoms all atoms of the state
 * @param connection to check sockettypes of
 * @returns {boolean}
 */
export function isChatToGroupConnection(allAtoms, connection) {
  return (
    getSenderSocketType(allAtoms, connection) ===
      vocab.CHAT.ChatSocketCompacted &&
    getTargetSocketType(allAtoms, connection) ===
      vocab.GROUP.GroupSocketCompacted
  );
}
