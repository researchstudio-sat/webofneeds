/**
 * Created by fsuda on 05.11.2018.
 */

import {
  getOwnedNeedByConnectionUri,
  getOwnedNeeds,
  getOwnedPosts,
} from "./general-selectors.js";
import won from "../won-es6.js";

/**
 * Get the connection for a given connectionUri
 * @param state to retrieve data from
 * @param connectionUri to find corresponding connection for
 */
export function getOwnedConnectionByUri(state, connectionUri) {
  let need = getOwnedNeedByConnectionUri(state, connectionUri);
  return need.getIn(["connections", connectionUri]);
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
export function selectAllConnectionUris(state) {
  const connections = getOwnedConnections(state);
  return connections && connections.keySeq().toSet();
}

/**
 * Get all post connections stored within your own needs as a map
 * @returns Immutable.Map with all connections
 */
function selectAllPostConnections(state) {
  const needs = getOwnedPosts(state); //we only check own posts as these are the only ones who have connections stored
  const connections = needs && needs.flatMap(need => need.get("connections"));
  return connections;
}

/**
 * Get all connections stored within your own needs as a map with a status of Connected
 * @returns Immutable.Map with all connections
 */
function selectAllPostConnectionsInStateConnected(state) {
  const allConnections = selectAllPostConnections(state);
  return (
    allConnections &&
    allConnections.filter(conn => conn.get("state") === won.WON.Connected)
  );
}

/**
 * TODO: REFACTOR THE METHOD/METHODNAME this method returns all the connections that need to be crawled (all chatconnections basically)
 * @param state
 * @returns {Immutable.Map|*}
 */
export function selectPostConnectionsWithoutConnectMessage(state) {
  const connectionsInStateConnected = selectAllPostConnectionsInStateConnected(
    state
  );

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
