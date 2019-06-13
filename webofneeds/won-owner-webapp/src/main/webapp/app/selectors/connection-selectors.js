/**
 * Created by fsuda on 05.11.2018.
 */

import Immutable from "immutable";
import {
  getOwnedAtomByConnectionUri,
  getOwnedAtoms,
  getOwnedPosts,
  getAtoms,
} from "./general-selectors.js";
import * as connectionUtils from "../connection-utils.js";
import won from "../won-es6.js";
import { get, getIn } from "../utils.js";
import * as processUtils from "../process-utils.js";

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
export function getOwnedConnections(state) {
  const atoms = getOwnedAtoms(state); //we only check own atoms as these are the only ones who have connections stored
  const connections = atoms && atoms.flatMap(atom => atom.get("connections"));
  return connections;
}

/**
 * Get all the connectionUris storid within the state
 */
export function getOwnedConnectionUris(state) {
  const connections = getOwnedConnections(state);
  return connections && connections.keySeq().toSet();
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
        .filter(conn => isBuddyConnection(atoms, conn))
        .filter(conn => !(excludeClosed && connectionUtils.isClosed(conn)))
        .filter(
          conn => !(excludeSuggested && connectionUtils.isSuggested(conn))
        )
    : Immutable.Map();
}

/**
 * @param state
 * @returns {Immutable.Map|*}
 */
export function getChatConnectionsToCrawl(state) {
  const atoms = getOwnedPosts(state); //we only check own posts as these are the only ones who have connections stored
  const allConnections =
    atoms && atoms.flatMap(atom => atom.get("connections"));
  const chatConnections =
    allConnections &&
    allConnections
      .filter(
        conn =>
          isChatToXConnection(atoms, conn) || isGroupToXConnection(atoms, conn)
      )
      .filter(conn => {
        const connUri = get(conn, "uri");
        const process = get(state, "process");

        return (
          !processUtils.isConnectionLoading(process, connUri) &&
          !processUtils.isConnectionLoadingMessages(process, connUri) &&
          !processUtils.hasConnectionFailedToLoad(process, connUri)
        );
      });

  const connectionsInStateConnected =
    chatConnections &&
    chatConnections.filter(conn => connectionUtils.isConnected(conn));

  const connectionsWithoutConnectMessage =
    connectionsInStateConnected &&
    connectionsInStateConnected.filter(
      conn =>
        !conn.get("messages") ||
        conn
          .get("messages")
          .filter(msg => msg.get("messageType") === won.WONMSG.connectMessage)
          .size == 0
    );
  return connectionsWithoutConnectMessage;
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
  const { socket, targetSocket } = getSockets(allAtoms, connection);

  return (
    socket === won.CHAT.ChatSocketCompacted &&
    targetSocket === won.CHAT.ChatSocketCompacted
  );
}

/**
 * Returns true if both sockets are BuddySockets
 * @param allAtoms all atoms of the state
 * @param connection to check sockettypes of
 * @returns {boolean}
 */
export function isBuddyConnection(allAtoms, connection) {
  const { socket, targetSocket } = getSockets(allAtoms, connection);

  return (
    socket === won.BUDDY.BuddySocketCompacted &&
    targetSocket === won.BUDDY.BuddySocketCompacted
  );
}

/**
 * Returns true if socket is a ChatSocket and targetSocket is a GroupSocket
 * @param allAtoms all atoms of the state
 * @param connection to check sockettypes of
 * @returns {boolean}
 */
export function isChatToGroupConnection(allAtoms, connection) {
  const { socket, targetSocket } = getSockets(allAtoms, connection);

  return (
    socket === won.CHAT.ChatSocketCompacted &&
    targetSocket === won.GROUP.GroupSocketCompacted
  );
}

/**
 * Returns true if socket is a GroupSocket and targetSocket is a ChatSocket
 * @param allAtoms all atoms of the state
 * @param connection to check sockettypes of
 * @returns {boolean}
 */
export function isGroupToChatConnection(allAtoms, connection) {
  const { socket, targetSocket } = getSockets(allAtoms, connection);

  return (
    socket === won.GROUP.GroupSocketCompacted &&
    targetSocket === won.CHAT.ChatSocketCompacted
  );
}

/**
 * Returns true if both sockets are GroupSockets
 * @param allAtoms all atoms of the state
 * @param connection to check sockettypes of
 * @returns {boolean}
 */
export function isGroupToGroupConnection(allAtoms, connection) {
  const { socket, targetSocket } = getSockets(allAtoms, connection);

  return (
    socket === won.GROUP.GroupSocketCompacted &&
    targetSocket === won.GROUP.GroupSocketCompacted
  );
}

/**
 * Returns true if socket is GroupSocket and targetSocket is either GroupSocket or ChatSocket
 * @param allAtoms
 * @param connection
 * @returns {boolean}
 */
export function isGroupToXConnection(allAtoms, connection) {
  return (
    isGroupToChatConnection(allAtoms, connection) ||
    isGroupToGroupConnection(allAtoms, connection)
  );
}

/**
 * Returns true if socket is ChatSocket and targetSocket is either GroupSocket or ChatSocket
 * @param allAtoms
 * @param connection
 * @returns {boolean}
 */
export function isChatToXConnection(allAtoms, connection) {
  return (
    isChatToChatConnection(allAtoms, connection) ||
    isChatToGroupConnection(allAtoms, connection)
  );
}

/**
 * Retrieves the used sockets of the given connection
 * @param allAtoms
 * @param connection
 * @returns {{socket, targetSocket}}
 */
function getSockets(allAtoms, connection) {
  let socket = undefined;
  let targetSocket = undefined;

  if (connection && allAtoms) {
    const socketUri = get(connection, "socketUri");
    const targetSocketUri = get(connection, "targetSocketUri");

    if (socketUri && targetSocketUri) {
      const atom =
        allAtoms &&
        allAtoms.find(atom =>
          getIn(atom, ["connections", get(connection, "uri")])
        );
      socket = getIn(atom, ["content", "sockets", socketUri]);
      targetSocket = getIn(allAtoms, [
        get(connection, "targetAtomUri"),
        "content",
        "sockets",
        targetSocketUri,
      ]);

      // Uncomment lines below for verbose debug output
      // if (socket && targetSocket) {
      //  console.debug(get(connection, "uri"), ":", socket, "-->", targetSocket);
      //}
    }
  }
  return { socket, targetSocket };
}
