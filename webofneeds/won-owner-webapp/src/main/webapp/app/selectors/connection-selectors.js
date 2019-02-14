/**
 * Created by fsuda on 05.11.2018.
 */

import Immutable from "immutable";
import {
  getOwnedNeedByConnectionUri,
  getOwnedNeeds,
  getOwnedPosts,
  getNeeds,
} from "./general-selectors.js";
import * as connectionUtils from "../connection-utils.js";
import won from "../won-es6.js";
import { getIn } from "../utils.js";

/**
 * Get the connection for a given connectionUri
 * @param state to retrieve data from
 * @param connectionUri to find corresponding connection for
 */
export function getOwnedConnectionByUri(state, connectionUri) {
  let need = getOwnedNeedByConnectionUri(state, connectionUri);
  return getIn(need, ["connections", connectionUri]);
}

/**
 * Get all connections stored within your own needs as a map
 * @returns Immutable.Map with all connections
 */
export function getOwnedConnections(state) {
  const needs = getOwnedNeeds(state); //we only check own needs as these are the only ones who have connections stored
  const connections = needs && needs.flatMap(need => need.get("connections"));
  return connections;
}

/**
 * Get all the connectionUris storid within the state
 */
export function getOwnedConnectionUris(state) {
  const connections = getOwnedConnections(state);
  return connections && connections.keySeq().toSet();
}

export function getChatConnectionsByNeedUri(state, needUri) {
  const needs = getNeeds(state);
  const need = needs && needs.get(needUri);
  const connections = need && need.get("connections");

  return (
    connections &&
    connections.filter(conn => connectionUtils.isChatConnection(conn))
  );
}

export function getGroupChatConnectionsByNeedUri(state, needUri) {
  const needs = getNeeds(state);
  const need = needs && needs.get(needUri);
  const connections = need && need.get("connections");

  return connections
    ? connections.filter(conn => connectionUtils.isGroupChatConnection(conn))
    : Immutable.Map();
}

export function getSuggestedConnectionsByNeedUri(state, needUri) {
  const needs = getNeeds(state);
  const need = needs && needs.get(needUri);
  const connections = need && need.get("connections");

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
  const needs = getOwnedPosts(state); //we only check own posts as these are the only ones who have connections stored
  const allConnections =
    needs && needs.flatMap(need => need.get("connections"));
  const chatConnections =
    allConnections &&
    allConnections
      .filter(
        conn =>
          connectionUtils.isChatConnection(conn) ||
          connectionUtils.isGroupChatConnection(conn)
      )
      .filter(
        conn =>
          !getIn(state, [
            "process",
            "connections",
            conn.get("uri"),
            "loadingMessages",
          ]) &&
          !getIn(state, [
            "process",
            "connections",
            conn.get("uri"),
            "failedToLoad",
          ])
      );

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
