/**
 * Created by ksinger on 22.01.2016.
 */

import { createSelector } from "reselect";
import Immutable from "immutable";
import won from "./won-es6.js";
import { decodeUriComponentProperly, getIn } from "./utils.js";
import Color from "color";

export const selectLastUpdateTime = state => state.get("lastUpdateTime");
export const selectRouterParams = state =>
  getIn(state, ["router", "currentParams"]);

export const selectAllNeeds = state => state.get("needs");
export const selectAllOwnNeeds = state =>
  selectAllNeeds(state).filter(need => need.get("ownNeed"));
export const selectAllTheirNeeds = state =>
  selectAllNeeds(state).filter(need => !need.get("ownNeed"));

export function selectAllPosts(state) {
  const needs = selectAllNeeds(state);
  return needs.filter(need => {
    if (!need.get("types")) return true;

    return Immutable.is(need.get("types"), Immutable.Set(["won:Need"]));
  });
}

export const selectAllOwnPosts = state =>
  selectAllPosts(state).filter(need => need.get("ownNeed"));

export function selectOpenNeeds(state) {
  const allOwnNeeds = selectAllOwnNeeds(state);
  return (
    allOwnNeeds &&
    allOwnNeeds.filter(post => post.get("state") === won.WON.ActiveCompacted)
  );
}

export function selectOpenPosts(state) {
  const allOwnNeeds = selectAllOwnPosts(state);
  return (
    allOwnNeeds &&
    allOwnNeeds.filter(post => post.get("state") === won.WON.ActiveCompacted)
  );
}

export function selectAllOpenPosts(state) {
  const allPosts = selectAllPosts(state);
  return (
    allPosts &&
    allPosts.filter(post => post.get("state") === won.WON.ActiveCompacted)
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

export function selectClosedPosts(state) {
  const allOwnNeeds = selectAllOwnPosts(state);
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

/**
 * Get all post connections stored within your own needs as a map
 * @returns Immutable.Map with all connections
 */
export function selectAllPostConnections(state) {
  const needs = selectAllOwnPosts(state); //we only check own posts as these are the only ones who have connections stored
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
export function selectAllPostConnectionsInStateConnected(state) {
  const allConnections = selectAllPostConnections(state);
  return (
    allConnections &&
    allConnections.filter(conn => conn.get("state") === won.WON.Connected)
  );
}

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

export function selectAllMessagesByConnectionUri(state, connectionUri) {
  const need = selectNeedByConnectionUri(state, connectionUri);
  return need && need.getIn(["connections", connectionUri, "messages"]);
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
  //TODO: should a message be proposable if it was already proposed? or even accepted? and what if the ref are only forwardedMessages?
  return (
    msg &&
    msg.get("hasContent") &&
    msg.get("messageType") !== won.WONMSG.connectMessage &&
    !msg.get("hasReferences")
  );
}

export function isMessageClaimable(msg) {
  //TODO: should a message be claimable if it was already claimed or proposed or even accepted? what if the ref are only forwardedMessages?
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
    (hasClaimsReferences(msg) ||
      hasProposesReferences(msg) ||
      hasProposesToCancelReferences(msg)) &&
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
    (hasClaimsReferences(msg) ||
      hasProposesReferences(msg) ||
      hasProposesToCancelReferences(msg)) &&
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
    (hasClaimsReferences(msg) ||
      hasProposesReferences(msg) ||
      hasProposesToCancelReferences(msg)) &&
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

export function hasClaimsReferences(msg) {
  const references = msg && msg.get("references");
  return (
    references && references.get("claims") && references.get("claims").size > 0
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

export function isMessageProposed(msg) {
  const messageStatus = msg && msg.get("messageStatus");
  return messageStatus && messageStatus.get("isProposed");
}

export function isMessageClaimed(msg) {
  const messageStatus = msg && msg.get("messageStatus");
  return messageStatus && messageStatus.get("isClaimed");
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

export function isMessageUnread(msg) {
  return msg && msg.get("unread");
}

export function isMessageProposal(msg) {
  return (
    (hasProposesToCancelReferences(msg) || hasProposesReferences(msg)) &&
    !(
      isMessageAccepted(msg) ||
      isMessageCancellationPending(msg) ||
      isMessageCancelled(msg) ||
      isMessageRejected(msg) ||
      isMessageRetracted(msg)
    )
  );
}

export function isMessageClaim(msg) {
  return (
    hasClaimsReferences(msg) &&
    !(
      isMessageAccepted(msg) ||
      isMessageCancellationPending(msg) ||
      isMessageCancelled(msg) ||
      isMessageRejected(msg) ||
      isMessageRetracted(msg)
    )
  );
}

export function selectAcceptableMessagesByConnectionUri(state, connectionUri) {
  const messages = selectAllMessagesByConnectionUri(state, connectionUri);
  return messages && messages.filter(msg => isMessageAcceptable(msg));
}

export function selectRejectableMessagesByConnectionUri(state, connectionUri) {
  const messages = selectAllMessagesByConnectionUri(state, connectionUri);
  return messages && messages.filter(msg => isMessageRejectable(msg));
}

export function selectRetractableMessagesByConnectionUri(state, connectionUri) {
  const messages = selectAllMessagesByConnectionUri(state, connectionUri);
  return messages && messages.filter(msg => isMessageRetractable(msg));
}

export function selectCancelableMessagesByConnectionUri(state, connectionUri) {
  const messages = selectAllMessagesByConnectionUri(state, connectionUri);
  return messages && messages.filter(msg => isMessageCancelable(msg));
}

export function selectProposableMessagesByConnectionUri(state, connectionUri) {
  const messages = selectAllMessagesByConnectionUri(state, connectionUri);
  return messages && messages.filter(msg => isMessageProposable(msg));
}

export function selectClaimableMessagesByConnectionUri(state, connectionUri) {
  const messages = selectAllMessagesByConnectionUri(state, connectionUri);
  return messages && messages.filter(msg => isMessageClaimable(msg));
}

export function selectSelectedMessagesByConnectionUri(state, connectionUri) {
  const messages = selectAllMessagesByConnectionUri(state, connectionUri);
  return messages && messages.filter(msg => msg.get("isSelected"));
}

export function selectAgreementMessagesByConnectionUri(state, connectionUri) {
  const messages = selectAllMessagesByConnectionUri(state, connectionUri);
  return (
    messages &&
    messages.filter(
      msg => isMessageAccepted(msg) && !isMessageCancellationPending(msg)
    )
  );
}

export function selectCancellationPendingMessagesByConnectionUri(
  state,
  connectionUri
) {
  const messages = selectAllMessagesByConnectionUri(state, connectionUri);
  return messages && messages.filter(msg => isMessageCancellationPending(msg));
}

export function selectProposalMessagesByConnectionUri(state, connectionUri) {
  const messages = selectAllMessagesByConnectionUri(state, connectionUri);
  return messages && messages.filter(msg => isMessageProposal(msg));
}

export function selectClaimMessagesByConnectionUri(state, connectionUri) {
  const messages = selectAllMessagesByConnectionUri(state, connectionUri);
  return messages && messages.filter(msg => isMessageClaim(msg));
}

export function selectUnreadMessagesByConnectionUri(state, connectionUri) {
  const messages = selectAllMessagesByConnectionUri(state, connectionUri);
  return messages && messages.filter(msg => isMessageUnread(msg));
}

export function getCorrectMessageUri(messages, messageUri) {
  if (messageUri) {
    if (messages.filter(msg => msg.get("uri") === messageUri).size > 0) {
      return messageUri;
    } else {
      const messagesOfRemoteUri = messages.filter(
        msg => msg.get("remoteUri") === messageUri
      );
      if (messagesOfRemoteUri.size > 0) {
        return messagesOfRemoteUri.first().get("uri");
      }
    }
  }
  return undefined;
}

export function getPersonas(needs) {
  const personas = needs
    .toList()
    .filter(need => need.get("types") && need.get("types").has("won:Persona"));
  return personas.map(persona => {
    const graph = persona.get("jsonld");
    return {
      displayName: graph.get("s:name"),
      website: graph.get("s:url"),
      aboutMe: graph.get("s:description"),
      url: persona.get("uri"),
      saved: !persona.get("isBeingCreated"),
      timestamp: persona.get("creationDate").toISOString(),
    };
  });
}

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
