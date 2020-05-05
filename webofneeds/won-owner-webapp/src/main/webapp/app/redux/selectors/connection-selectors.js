/**
 * Created by fsuda on 05.11.2018.
 */

import { createSelector } from "reselect";

import Immutable from "immutable";
import {
  getOwnedAtomByConnectionUri,
  getOwnedAtoms,
  getAtoms,
} from "./general-selectors.js";
import * as connectionUtils from "../utils/connection-utils.js";
import * as atomUtils from "../utils/atom-utils.js";
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

export function getChatConnectionsByAtomUri(state, atomUri) {
  const atoms = getAtoms(state);
  const atom = atoms && atoms.get(atomUri);
  const connections = atom && atom.get("connections");

  return (
    connections && connections.filter(conn => isChatToXConnection(atoms, conn))
  );
}

export function getGroupChatConnectionsByAtomUri(state, atomUri) {
  const atoms = getAtoms(state);
  const atom = atoms && atoms.get(atomUri);
  const connections = atom && atom.get("connections");

  return connections
    ? connections.filter(conn => isGroupToXConnection(atoms, conn))
    : Immutable.Map();
}

export function getSuggestedConnectionsByAtomUri(state, atomUri) {
  const atoms = getAtoms(state);
  const connections = getIn(atoms, [atomUri, "connections"]);

  return connections
    ? connections.filter(conn => connectionUtils.isSuggested(conn))
    : Immutable.Map();
}

/**
 * Returns all buddyConnections of an atom
 * @param state
 * @param atomUri
 * @param excludeClosed  -> exclude Closed connections
 * @param excludeSuggested -> exclude Suggested connections
 * @returns {*}
 */
export function getBuddyConnectionsByAtomUri(
  state,
  atomUri,
  excludeClosed = false,
  excludeSuggested = false
) {
  const atoms = getAtoms(state);
  const connections = getIn(atoms, [atomUri, "connections"]);

  return connections
    ? connections
        .filter(conn => !(excludeClosed && connectionUtils.isClosed(conn)))
        .filter(
          conn => !(excludeSuggested && connectionUtils.isSuggested(conn))
        )
        .filter(conn => isBuddyConnection(atoms, conn))
    : Immutable.Map();
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

export const getChatConnectionsToCrawl = createSelector(
  getAtoms,
  state => get(state, "process"),
  getOwnedConnections,
  (allAtoms, process, allConnections) => {
    const chatConnections =
      allConnections &&
      allConnections
        .filter(conn => connectionUtils.isConnected(conn))
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
        .filter(
          conn =>
            isChatToXConnection(allAtoms, conn) ||
            isGroupToXConnection(allAtoms, conn)
        );

    return chatConnections || Immutable.Map();
  }
);

export function getConnectionsToInjectMsgInto(atoms, targetSocketUri, msgUri) {
  const allConnections =
    atoms && atoms.flatMap(atom => atom.get("connections"));

  return allConnections
    .filter(conn => connectionUtils.isConnected(conn))
    .filter(conn => get(conn, "targetSocketUri") === targetSocketUri)
    .filter(conn => !get(conn, "messages").contains(msgUri));
}

/**
 * Returns all connections of an atom that have the status "requestReceived".
 * @param state
 * @param atom
 */
export function getRequestedConnections(state, atom) {
  const atoms = getAtoms(state);
  const connections = get(atom, "connections");
  return (
    connections &&
    connections.filter(
      conn =>
        connectionUtils.isRequestReceived(conn) &&
        isChatToXConnection(atoms, conn)
    )
  );
}

/**
 * Returns all connections of an atom that have the status "requestReceived" and are unread.
 * @param state
 * @param atom
 */
export function getUnreadRequestedConnections(state, atom) {
  const requestedConnections = getRequestedConnections(state, atom);
  return (
    requestedConnections &&
    requestedConnections.filter(conn => get(conn, "unread"))
  );
}

/**
 * Returns all chat connections that are open and unread, which should cover only chat messages.
 * @param state
 * @param atom
 */
export function getUnreadChatMessageConnections(state, atom) {
  const atoms = getAtoms(state);
  const connections = get(atom, "connections");
  return (
    connections &&
    connections.filter(
      conn =>
        isChatToXConnection(atoms, conn) &&
        connectionUtils.isConnected(conn) &&
        connectionUtils.isUnread(conn)
    )
  );
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
 * Returns true if both sockets are ChatSockets
 * @param allAtoms all atoms of the state
 * @param connection to check sockettypes of
 * @returns {boolean}
 */
export function isChatToChatConnection(allAtoms, connection) {
  return (
    getSenderSocketType(allAtoms, connection) ===
      vocab.CHAT.ChatSocketCompacted &&
    getTargetSocketType(allAtoms, connection) === vocab.CHAT.ChatSocketCompacted
  );
}

/**
 * Returns true if both sockets are BuddySockets
 * @param allAtoms all atoms of the state
 * @param connection to check sockettypes of
 * @returns {boolean}
 */
export function isBuddyConnection(allAtoms, connection) {
  return (
    getSenderSocketType(allAtoms, connection) ===
      vocab.BUDDY.BuddySocketCompacted &&
    getTargetSocketType(allAtoms, connection) ===
      vocab.BUDDY.BuddySocketCompacted
  );
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

/**
 * Returns true if socket is a GroupSocket and targetSocket is a ChatSocket
 * @param allAtoms all atoms of the state
 * @param connection to check sockettypes of
 * @returns {boolean}
 */
export function isGroupToChatConnection(allAtoms, connection) {
  return (
    getSenderSocketType(allAtoms, connection) ===
      vocab.GROUP.GroupSocketCompacted &&
    getTargetSocketType(allAtoms, connection) === vocab.CHAT.ChatSocketCompacted
  );
}

/**
 * Returns true if both sockets are GroupSockets
 * @param allAtoms all atoms of the state
 * @param connection to check sockettypes of
 * @returns {boolean}
 */
export function isGroupToGroupConnection(allAtoms, connection) {
  return (
    getSenderSocketType(allAtoms, connection) ===
      vocab.GROUP.GroupSocketCompacted &&
    getTargetSocketType(allAtoms, connection) ===
      vocab.GROUP.GroupSocketCompacted
  );
}

/**
 * Returns true if socket is GroupSocket and targetSocket is any socket
 * @param allAtoms
 * @param connection
 * @returns {boolean}
 */
export function isGroupToXConnection(allAtoms, connection) {
  const senderSocketType = getSenderSocketType(allAtoms, connection);

  return senderSocketType === vocab.GROUP.GroupSocketCompacted;
}

/**
 * Returns true if socket is ChatSocket and targetSocket is any socket
 * @param allAtoms
 * @param connection
 * @returns {boolean}
 */
export function isChatToXConnection(allAtoms, connection) {
  const senderSocketType = getSenderSocketType(allAtoms, connection);

  return senderSocketType === vocab.CHAT.ChatSocketCompacted;
}

export const getSenderSocketType = (allAtoms, connection) => {
  const connectionUri = get(connection, "uri");
  const senderSocketUri = get(connection, "socketUri");
  const senderAtom =
    connectionUri &&
    allAtoms &&
    (getIn(allAtoms, connectionUri.split("/c")[0]) ||
      allAtoms.find(atom => getIn(atom, ["connections", connectionUri])));

  return senderAtom && atomUtils.getSocketType(senderAtom, senderSocketUri);
};

export const getTargetSocketType = (allAtoms, connection) => {
  const targetAtomUri = get(connection, "targetAtomUri");
  const targetSocketUri = get(connection, "targetSocketUri");

  return (
    targetSocketUri &&
    getIn(allAtoms, [targetAtomUri, "content", "sockets", targetSocketUri])
  );
};
