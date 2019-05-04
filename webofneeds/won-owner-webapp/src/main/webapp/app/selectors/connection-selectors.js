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
    connections &&
    connections.filter(conn => connectionUtils.isChatConnection(conn))
  );
}

export function getGroupChatConnectionsByAtomUri(state, atomUri) {
  const atoms = getAtoms(state);
  const atom = atoms && atoms.get(atomUri);
  const connections = atom && atom.get("connections");

  return connections
    ? connections.filter(conn => connectionUtils.isGroupChatConnection(conn))
    : Immutable.Map();
}

export function getSuggestedConnectionsByAtomUri(state, atomUri) {
  const atoms = getAtoms(state);
  const atom = atoms && atoms.get(atomUri);
  const connections = atom && atom.get("connections");

  return connections
    ? connections
        .filter(conn => connectionUtils.isSuggested(conn))
        .filter(
          conn =>
            connectionUtils.isChatConnection(conn) ||
            connectionUtils.isGroupChatConnection(conn)
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
          connectionUtils.isChatConnection(conn) ||
          connectionUtils.isGroupChatConnection(conn)
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
    chatConnections.filter(conn => conn.get("state") === won.WON.Connected);

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
