/**
 * Created by ksinger on 22.01.2016.
 */

import { createSelector } from "reselect";
import Immutable from "immutable";
import won from "./won-es6.js";
import { decodeUriComponentProperly, getIn } from "./utils.js";

export const selectLastUpdateTime = state => state.get("lastUpdateTime");
export const selectRouterParams = state =>
  getIn(state, ["router", "currentParams"]);

export const selectAllNeeds = state => state.get("needs");
export const selectAllOwnNeeds = state =>
  selectAllNeeds(state).filter(need => need.get("ownNeed"));
export const selectAllTheirNeeds = state =>
  selectAllNeeds(state).filter(need => !need.get("ownNeed"));

export function selectOpenNeeds(state) {
  const allOwnNeeds = selectAllOwnNeeds(state);
  return (
    allOwnNeeds &&
    allOwnNeeds.filter(post => post.get("state") === won.WON.ActiveCompacted)
  );
}
export function selectClosedNeeds(state) {
  const allOwnNeeds = selectAllOwnNeeds(state);
  return (
    allOwnNeeds &&
    allOwnNeeds.filter(
      post =>
        post.get("state") === won.WON.InactiveCompacted &&
        !(post.get("isWhatsAround") || post.get("isWhatsNew"))
    )
  ); //Filter whatsAround and whatsNew needs automatically
}
export function selectNeedsInCreationProcess(state) {
  const allOwnNeeds = selectAllOwnNeeds(state);
  // needs that have been created but are not confirmed by the server yet
  return allOwnNeeds && allOwnNeeds.filter(post => post.get("isBeingCreated"));
}

export const selectIsConnected = state =>
  !state.getIn(["messages", "reconnecting"]) &&
  !state.getIn(["messages", "lostConnection"]);

/**
 * Get the need for a given connectionUri
 * @param state to retrieve data from
 * @param connectionUri to find corresponding need for
 */
export function selectNeedByConnectionUri(state, connectionUri) {
  let needs = selectAllOwnNeeds(state); //we only check own needs as these are the only ones who have connections stored
  return needs
    .filter(need => need.getIn(["connections", connectionUri]))
    .first();
}

/**
 * Get the connection for a given connectionUri
 * @param state to retrieve data from
 * @param connectionUri to find corresponding connection for
 */
export function selectConnection(state, connectionUri) {
  let need = selectNeedByConnectionUri(state, connectionUri);
  return need.getIn(["connections", connectionUri]);
}

/**
 * Get all connections stored within your own needs as a map
 * @returns Immutable.Map with all connections
 */
export function selectAllConnections(state) {
  const needs = selectAllOwnNeeds(state); //we only check own needs as these are the only ones who have connections stored
  const connections = needs && needs.flatMap(need => need.get("connections"));
  return connections;
}

export function selectAllConnectionUris(state) {
  const connections = selectAllConnections(state);
  return connections && connections.keySeq().toSet();
}

/**
 * Get all connections stored within your own needs as a map with a status of Connected
 * @returns Immutable.Map with all connections
 */
export function selectAllConnectionsInStateConnected(state) {
  const allConnections = selectAllConnections(state);
  return (
    allConnections &&
    allConnections.filter(conn => conn.get("state") === won.WON.Connected)
  );
}

export function selectConnectionsWithoutConnectMessage(state) {
  const connectionsInStateConnected = selectAllConnectionsInStateConnected(
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

export function selectAllMessages(state) {
  const connections = selectAllConnections(state);
  const messages = connections && connections.flatMap(c => c.get("messages"));
  return messages;
}

export function selectAllMessagesByNeedUri(state, needUri) {
  const connections = state.getIn(["needs", needUri, "connections"]);
  const messages = connections && connections.flatMap(c => c.get("messages"));
  return messages;
}

export function selectAllMessagesByNeedUriAndConnected(state, needUri) {
  const connections = state.getIn(["needs", needUri, "connections"]);
  const connectionsWithoutClosed =
    connections &&
    connections.filter(conn => conn.get("state") === won.WON.Connected);
  let messages = Immutable.Map();

  if (connectionsWithoutClosed) {
    connectionsWithoutClosed.map(function(conn) {
      messages = messages.merge(conn.get("messages"));
    });
  }

  return messages;
}

export const selectOpenConnectionUri = createSelector(
  selectRouterParams,
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

export const selectOpenPostUri = createSelector(
  state => state,
  state => {
    const encodedPostUri = getIn(state, ["router", "currentParams", "postUri"]);
    return decodeUriComponentProperly(encodedPostUri);
  }
);

export function isPrivateUser(state) {
  return !!getIn(state, ["router", "currentParams", "privateId"]);
}

export function isMessageProposable(msg) {
  return (
    msg &&
    msg.get("hasContent") &&
    msg.get("messageType") !== won.WONMSG.connectMessage &&
    !msg.get("hasReferences")
  );
}

export function isMessageCancelable(msg) {
  return (
    msg &&
    (hasProposesReferences(msg) || hasProposesToCancelReferences(msg)) &&
    isMessageAccepted(msg) &&
    !isMessageCancelled(msg) &&
    !isMessageCancellationPending(msg)
  );
}

export function isMessageRetractable(msg) {
  return (
    msg &&
    msg.get("outgoingMessage") &&
    !isMessageAccepted(msg) &&
    !isMessageCancelled(msg) &&
    !isMessageCancellationPending(msg) &&
    !isMessageRetracted(msg) &&
    !isMessageRejected(msg)
  );
}

export function isMessageAcceptable(msg) {
  return (
    msg &&
    (hasProposesReferences(msg) || hasProposesToCancelReferences(msg)) &&
    !msg.get("outgoingMessage") &&
    !isMessageAccepted(msg) &&
    !isMessageCancelled(msg) &&
    !isMessageCancellationPending(msg) &&
    !isMessageRetracted(msg) &&
    !isMessageRejected(msg)
  );
}

export function isMessageRejectable(msg) {
  return (
    msg &&
    (hasProposesReferences(msg) || hasProposesToCancelReferences(msg)) &&
    !msg.get("outgoingMessage") &&
    !isMessageAccepted(msg) &&
    !isMessageCancelled(msg) &&
    !isMessageCancellationPending(msg) &&
    !isMessageRetracted(msg) &&
    !isMessageRejected(msg)
  );
}

export function hasProposesReferences(msg) {
  const references = msg && msg.get("references");
  return (
    references &&
    references.get("proposes") &&
    references.get("proposes").size > 0
  );
}
export function hasProposesToCancelReferences(msg) {
  const references = msg && msg.get("references");
  return (
    references &&
    references.get("proposesToCancel") &&
    references.get("proposesToCancel").size > 0
  );
}

export function isMessageRejected(msg) {
  const messageStatus = msg && msg.get("messageStatus");
  return messageStatus && messageStatus.get("isRejected");
}

export function isMessageAccepted(msg) {
  const messageStatus = msg && msg.get("messageStatus");
  return messageStatus && messageStatus.get("isAccepted");
}

export function isMessageRetracted(msg) {
  const messageStatus = msg && msg.get("messageStatus");
  return messageStatus && messageStatus.get("isRetracted");
}

export function isMessageCancelled(msg) {
  const messageStatus = msg && msg.get("messageStatus");
  return messageStatus && messageStatus.get("isCancelled");
}

export function isMessageCancellationPending(msg) {
  const messageStatus = msg && msg.get("messageStatus");
  return messageStatus && messageStatus.get("isCancellationPending");
}
