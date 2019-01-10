import { parseMessage } from "./parse-message.js";
import { markUriAsRead } from "../../won-localstorage.js";
import { markConnectionAsRead } from "./reduce-connections.js";
import { getOwnMessageUri } from "../../message-utils.js";
import { isChatToGroup } from "../../connection-utils.js";

/*
 "alreadyProcessed" flag, which indicates that we do not care about the
 sent status anymore and assume that it has been successfully sent to each server (incl. the remote)
 "insertIntoConnUri" and "insertIntoNeedUri" are used for forwardedMessages so that the message is
 stored within the given connection/need and not in the original need or connection as we might not
 have these stored in the state
 */
export function addMessage(
  state,
  wonMessage,
  alreadyProcessed = false,
  insertIntoConnUri = undefined,
  insertIntoNeedUri = undefined
) {
  // we used to exclude messages without content here, using
  // if (wonMessage.getContentGraphs().length > 0) as the condition
  // however, after moving the facet info of connect/open messages from
  // content to envelope and making them optional, connect messages
  // actually can have no content. This never happened before, and
  // as one might expect, caused very weird behaviour when it did:
  // It was processed correctly after a reload, but as an
  // outgoing message, the success/failure responses coming in
  // would still cause an entry to be created in the messages array,
  // but holding only the 'isReceivedByOwn','isReceivedByRemote' etc fields,
  // throwing off the message rendering.
  // New solution: parse anything that is not a response, but allow responses with content
  if (!wonMessage.isResponse() || wonMessage.getContentGraphs().length > 0) {
    let parsedMessage = parseMessage(
      wonMessage,
      alreadyProcessed,
      insertIntoConnUri && insertIntoNeedUri
    );
    if (parsedMessage) {
      const connectionUri =
        insertIntoConnUri || parsedMessage.get("belongsToUri");

      let needUri = insertIntoNeedUri;
      if (!needUri && parsedMessage.getIn(["data", "outgoingMessage"])) {
        // needUri is the message's hasSenderNeed
        needUri = wonMessage.getSenderNeed();
      } else if (!needUri) {
        // needUri is the remote message's hasReceiverNeed
        needUri = wonMessage.getReceiverNeed();
        if (parsedMessage.getIn(["data", "unread"])) {
          //If there is a new message for the connection we will set the connection to newConnection
          state = state.setIn(
            [needUri, "lastUpdateDate"],
            parsedMessage.getIn(["data", "date"])
          );
          state = state.setIn([needUri, "unread"], true);
          state = state.setIn(
            [needUri, "connections", connectionUri, "lastUpdateDate"],
            parsedMessage.getIn(["data", "date"])
          );
          state = state.setIn(
            [needUri, "connections", connectionUri, "unread"],
            true
          );
        }
      }

      if (needUri) {
        const hasContainedForwardedWonMessages = wonMessage.hasContainedForwardedWonMessages();

        if (hasContainedForwardedWonMessages) {
          const containedForwardedWonMessages = wonMessage.getContainedForwardedWonMessages();
          containedForwardedWonMessages.map(forwardedWonMessage => {
            state = addMessage(
              state,
              forwardedWonMessage,
              true,
              connectionUri,
              needUri
            );
            //PARSE MESSAGE DIFFERENTLY FOR FORWARDED MESSAGES
          });
        }

        let messages = state.getIn([
          needUri,
          "connections",
          connectionUri,
          "messages",
        ]);
        if (!messages) {
          //ignore messages for nonexistant connections
          return state;
        }

        /*
        Group Chat messages that are received are in the form of injected/referenced messages
        But we do not want to display these messages in that manner, therefore we simply ignore
        the encapsulating message and not store it within our state.
        */
        if (
          !(
            hasContainedForwardedWonMessages &&
            isChatToGroup(state, needUri, connectionUri)
          )
        ) {
          messages = messages.set(
            parsedMessage.getIn(["data", "uri"]),
            parsedMessage.get("data")
          );
        }

        return state.setIn(
          [needUri, "connections", connectionUri, "messages"],
          messages
        );
      }
    }
  }
  return state;
}

/*
 This method should only be called to for messages that are already stored on the server (reload, initial login etc)
 because we will add all the messages with the "alreadyProcessed" flag, which indicates that we do not care about the
 sent status anymore and assume that it has been successfully sent to each server (incl. the remote)
 */
export function addExistingMessages(state, wonMessages) {
  if (wonMessages && wonMessages.size > 0) {
    wonMessages.map(wonMessage => {
      state = addMessage(state, wonMessage, true);
    });
  }
  return state;
}

export function markMessageAsSelected(
  state,
  messageUri,
  connectionUri,
  needUri,
  isSelected
) {
  const need = state.get(needUri);
  const connection = need && need.getIn(["connections", connectionUri]);
  const message = connection && connection.getIn(["messages", messageUri]);

  markUriAsRead(messageUri);

  if (!message) {
    console.error(
      "No message with messageUri: <",
      messageUri,
      "> found within needUri: <",
      needUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  }

  return state.setIn(
    [
      needUri,
      "connections",
      connectionUri,
      "messages",
      messageUri,
      "viewState",
      "isSelected",
    ],
    isSelected
  );
}

export function markMessageAsCollapsed(
  state,
  messageUri,
  connectionUri,
  needUri,
  isCollapsed
) {
  const need = state.get(needUri);
  const connection = need && need.getIn(["connections", connectionUri]);
  const message = connection && connection.getIn(["messages", messageUri]);

  markUriAsRead(messageUri);

  if (!message) {
    console.error(
      "No message with messageUri: <",
      messageUri,
      "> found within needUri: <",
      needUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  }

  if (isCollapsed) {
    state = markMessageShowActions(
      state,
      messageUri,
      connectionUri,
      needUri,
      false
    );

    state = markMessageExpandAllReferences(
      state,
      messageUri,
      connectionUri,
      needUri,
      false
    );
  }

  return state.setIn(
    [
      needUri,
      "connections",
      connectionUri,
      "messages",
      messageUri,
      "viewState",
      "isCollapsed",
    ],
    isCollapsed
  );
}

/**
 * Collapses/Expands all available references within the viewState of a message based on the isExpanded value
 * @param state
 * @param messageUri
 * @param connectionUri
 * @param needUri
 * @param isExpanded
 * @param reference
 * @returns {*}
 */
export function markMessageExpandAllReferences(
  state,
  messageUri,
  connectionUri,
  needUri,
  isExpanded
) {
  const need = state.get(needUri);
  const connection = need && need.getIn(["connections", connectionUri]);
  const message = connection && connection.getIn(["messages", messageUri]);

  if (!message) {
    console.error(
      "No message with messageUri: <",
      messageUri,
      "> found within needUri: <",
      needUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  }

  const expandedReferences = state.getIn([
    needUri,
    "connections",
    connectionUri,
    "messages",
    messageUri,
    "viewState",
    "expandedReferences",
  ]);

  if (!expandedReferences) {
    console.error(
      "No expandedReferences found within messageUri: <",
      messageUri,
      "> found within needUri: <",
      needUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  }

  return state.setIn(
    [
      needUri,
      "connections",
      connectionUri,
      "messages",
      messageUri,
      "viewState",
      "expandedReferences",
    ],
    expandedReferences.map(() => isExpanded)
  );
}

/**
 * Collapses/Expands the given reference within the viewState of a message based on the isExpanded value
 * @param state
 * @param messageUri
 * @param connectionUri
 * @param needUri
 * @param isExpanded
 * @param reference
 * @returns {*}
 */
export function markMessageExpandReferences(
  state,
  messageUri,
  connectionUri,
  needUri,
  isExpanded,
  reference
) {
  const need = state.get(needUri);
  const connection = need && need.getIn(["connections", connectionUri]);
  const message = connection && connection.getIn(["messages", messageUri]);

  if (!message) {
    console.error(
      "No message with messageUri: <",
      messageUri,
      "> found within needUri: <",
      needUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  }

  return state.setIn(
    [
      needUri,
      "connections",
      connectionUri,
      "messages",
      messageUri,
      "viewState",
      "expandedReferences",
      reference,
    ],
    isExpanded
  );
}

export function markMessageShowActions(
  state,
  messageUri,
  connectionUri,
  needUri,
  showActions
) {
  const need = state.get(needUri);
  const connection = need && need.getIn(["connections", connectionUri]);
  const message = connection && connection.getIn(["messages", messageUri]);

  markUriAsRead(messageUri);

  if (!message) {
    console.error(
      "No message with messageUri: <",
      messageUri,
      "> found within needUri: <",
      needUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  }

  return state.setIn(
    [
      needUri,
      "connections",
      connectionUri,
      "messages",
      messageUri,
      "viewState",
      "showActions",
    ],
    showActions
  );
}

export function markMessageAsRead(state, messageUri, connectionUri, needUri) {
  const need = state.get(needUri);
  const connection = need && need.getIn(["connections", connectionUri]);
  const message = connection && connection.getIn(["messages", messageUri]);

  markUriAsRead(messageUri);

  if (!message) {
    console.error(
      "No message with messageUri: <",
      messageUri,
      "> found within needUri: <",
      needUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  }

  state = state.setIn(
    [needUri, "connections", connectionUri, "messages", messageUri, "unread"],
    false
  );

  if (
    state
      .getIn([needUri, "connections", connectionUri, "messages"])
      .filter(msg => msg.get("unread")).size == 0
  ) {
    state = markConnectionAsRead(state, connectionUri, needUri);
  }

  return state.setIn(
    [needUri, "connections", connectionUri, "messages", messageUri, "unread"],
    false
  );
}

/**
 * Sets the given messageUri messageStatus->isRejected to the given parameter (rejected).
 * Additionally calls markMessageAsCollapsed to the given parameter (rejected) as well
 * Additionally calls markMessageAsCancellationPending to the referencedMessages with the parameter false
 * Additionally calls markMessageAsProposed to the referencedMessages with the parameter false
 * Additionally calls markMessageAsClaimed to the referencedMessages with the parameter false
 * @param state
 * @param messageUri
 * @param connectionUri
 * @param needUri
 * @param rejected
 * @returns {*}
 */
export function markMessageAsRejected(
  state,
  messageUri,
  connectionUri,
  needUri,
  rejected
) {
  let need = state.get(needUri);
  let connection = need && need.getIn(["connections", connectionUri]);
  let messages = connection && connection.get("messages");
  let message = messages && messages.get(messageUri);

  if (!message) {
    console.error(
      "No message with messageUri: <",
      messageUri,
      "> found within needUri: <",
      needUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  }
  const proposedToCancelReferences = message.getIn([
    "references",
    "proposesToCancel",
  ]);

  if (proposedToCancelReferences) {
    proposedToCancelReferences.forEach(proposedToCancelRef => {
      const correctMessageUri = getOwnMessageUri(messages, proposedToCancelRef);
      state = markMessageAsCancellationPending(
        state,
        correctMessageUri,
        connectionUri,
        needUri,
        false
      );
    });
  }

  const proposesReferences = message.getIn(["references", "proposes"]);

  if (proposesReferences) {
    proposesReferences.forEach(proposesRef => {
      const correctMessageUri = getOwnMessageUri(messages, proposesRef);
      state = markMessageAsProposed(
        state,
        correctMessageUri,
        connectionUri,
        needUri,
        false
      );
    });
  }

  const claimsReferences = message.getIn(["references", "claims"]);

  if (claimsReferences) {
    claimsReferences.forEach(claimsRef => {
      const correctMessageUri = getOwnMessageUri(messages, claimsRef);
      state = markMessageAsClaimed(
        state,
        correctMessageUri,
        connectionUri,
        needUri,
        false
      );
    });
  }

  state = markMessageAsCollapsed(
    state,
    messageUri,
    connectionUri,
    needUri,
    rejected
  );

  return state.setIn(
    [
      needUri,
      "connections",
      connectionUri,
      "messages",
      messageUri,
      "messageStatus",
      "isRejected",
    ],
    rejected
  );
}

/**
 * Sets the given messageUri messageStatus->isRetracted to the given parameter (retracted).
 * Additionally calls markMessageAsCollapsed to the given parameter (retracted) as well
 * Additionally calls markMessageAsCancellationPending to the referencedMessages with the parameter false
 * Additionally calls markMessageAsProposed to the referencedMessages with the parameter false
 * Additionally calls markMessageAsClaimed to the referencedMessages with the parameter false
 * @param state
 * @param messageUri
 * @param connectionUri
 * @param needUri
 * @param retracted
 * @returns {*}
 */
export function markMessageAsRetracted(
  state,
  messageUri,
  connectionUri,
  needUri,
  retracted
) {
  let need = state.get(needUri);
  let connection = need && need.getIn(["connections", connectionUri]);
  let messages = connection && connection.get("messages");
  let message = messages && messages.get(messageUri);

  if (!message) {
    console.error(
      "No message with messageUri: <",
      messageUri,
      "> found within needUri: <",
      needUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  }
  const proposedToCancelReferences = message.getIn([
    "references",
    "proposesToCancel",
  ]);

  if (proposedToCancelReferences) {
    proposedToCancelReferences.forEach(proposedToCancelRef => {
      const correctMessageUri = getOwnMessageUri(messages, proposedToCancelRef);
      state = markMessageAsCancellationPending(
        state,
        correctMessageUri,
        connectionUri,
        needUri,
        false
      );
    });
  }

  const proposesReferences = message.getIn(["references", "proposes"]);

  if (proposesReferences) {
    proposesReferences.forEach(proposesRef => {
      const correctMessageUri = getOwnMessageUri(messages, proposesRef);
      state = markMessageAsProposed(
        state,
        correctMessageUri,
        connectionUri,
        needUri,
        false
      );
    });
  }

  const claimsReferences = message.getIn(["references", "claims"]);

  if (claimsReferences) {
    claimsReferences.forEach(claimsRef => {
      const correctMessageUri = getOwnMessageUri(messages, claimsRef);
      state = markMessageAsClaimed(
        state,
        correctMessageUri,
        connectionUri,
        needUri,
        false
      );
    });
  }

  state = markMessageAsCollapsed(
    state,
    messageUri,
    connectionUri,
    needUri,
    retracted
  );

  return state.setIn(
    [
      needUri,
      "connections",
      connectionUri,
      "messages",
      messageUri,
      "messageStatus",
      "isRetracted",
    ],
    retracted
  );
}

/**
 * Sets the given messageUri messageStatus->isClaimed to the given parameter (claimed).
 * Additionally calls markMessageAsCollapsed to the given parameter (claimed) as well
 * @param state
 * @param messageUri
 * @param connectionUri
 * @param needUri
 * @param claimed
 * @returns {*}
 */
export function markMessageAsClaimed(
  state,
  messageUri,
  connectionUri,
  needUri,
  claimed
) {
  let need = state.get(needUri);
  let connection = need && need.getIn(["connections", connectionUri]);
  let messages = connection && connection.get("messages");
  let message = messages && messages.get(messageUri);

  if (!message) {
    console.error(
      "No message with messageUri: <",
      messageUri,
      "> found within needUri: <",
      needUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  }
  state = markMessageAsCollapsed(
    state,
    messageUri,
    connectionUri,
    needUri,
    claimed
  );

  return state.setIn(
    [
      needUri,
      "connections",
      connectionUri,
      "messages",
      messageUri,
      "messageStatus",
      "isClaimed",
    ],
    claimed
  );
}
/**
 * Sets the given messageUri messageStatus->isProposed to the given parameter (proposed).
 * Additionally calls markMessageAsCollapsed to the given parameter (proposed) as well
 * @param state
 * @param messageUri
 * @param connectionUri
 * @param needUri
 * @param proposed
 * @returns {*}
 */
export function markMessageAsProposed(
  state,
  messageUri,
  connectionUri,
  needUri,
  proposed
) {
  let need = state.get(needUri);
  let connection = need && need.getIn(["connections", connectionUri]);
  let messages = connection && connection.get("messages");
  let message = messages && messages.get(messageUri);

  if (!message) {
    console.error(
      "No message with messageUri: <",
      messageUri,
      "> found within needUri: <",
      needUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  }

  state = markMessageAsCollapsed(
    state,
    messageUri,
    connectionUri,
    needUri,
    proposed
  );

  return state.setIn(
    [
      needUri,
      "connections",
      connectionUri,
      "messages",
      messageUri,
      "messageStatus",
      "isProposed",
    ],
    proposed
  );
}

export function markMessageAsAccepted(
  state,
  messageUri,
  connectionUri,
  needUri,
  accepted
) {
  let need = state.get(needUri);
  let connection = need && need.getIn(["connections", connectionUri]);
  let messages = connection && connection.get("messages");
  let message = messages && messages.get(messageUri);

  if (!message) {
    console.error(
      "No message with messageUri: <",
      messageUri,
      "> found within needUri: <",
      needUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  }
  const proposedToCancelReferences = message.getIn([
    "references",
    "proposesToCancel",
  ]);

  if (proposedToCancelReferences) {
    proposedToCancelReferences.forEach(proposedToCancelRef => {
      const correctMessageUri = getOwnMessageUri(messages, proposedToCancelRef);
      state = markMessageAsCancelled(
        state,
        correctMessageUri,
        connectionUri,
        needUri,
        true
      );
    });
  }

  if (accepted) {
    state = state.setIn(
      [
        needUri,
        "connections",
        connectionUri,
        "messages",
        messageUri,
        "messageStatus",
        "isCancelled",
      ],
      false
    );

    state = state.setIn(
      [
        needUri,
        "connections",
        connectionUri,
        "messages",
        messageUri,
        "messageStatus",
        "isCancellationPending",
      ],
      false
    );
  }

  return state.setIn(
    [
      needUri,
      "connections",
      connectionUri,
      "messages",
      messageUri,
      "messageStatus",
      "isAccepted",
    ],
    accepted
  );
}

export function markMessageAsCancelled(
  state,
  messageUri,
  connectionUri,
  needUri,
  cancelled
) {
  let need = state.get(needUri);
  let connection = need && need.getIn(["connections", connectionUri]);
  let messages = connection && connection.get("messages");
  let message = messages && messages.get(messageUri);

  if (!message) {
    console.error(
      "No message with messageUri: <",
      messageUri,
      "> found within needUri: <",
      needUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  }

  state = state.setIn(
    [
      needUri,
      "connections",
      connectionUri,
      "messages",
      messageUri,
      "messageStatus",
      "isCancelled",
    ],
    cancelled
  );

  state = state.setIn(
    [
      needUri,
      "connections",
      connectionUri,
      "messages",
      messageUri,
      "messageStatus",
      "isAccepted",
    ],
    false
  );

  return state.setIn(
    [
      needUri,
      "connections",
      connectionUri,
      "messages",
      messageUri,
      "messageStatus",
      "isCancellationPending",
    ],
    false
  );
}

export function markMessageAsCancellationPending(
  state,
  messageUri,
  connectionUri,
  needUri,
  cancellationPending
) {
  let need = state.get(needUri);
  let connection = need && need.getIn(["connections", connectionUri]);
  let message = connection && connection.getIn(["messages", messageUri]);

  if (!message) {
    console.error(
      "No message with messageUri: <",
      messageUri,
      "> found within needUri: <",
      needUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  }

  return state.setIn(
    [
      needUri,
      "connections",
      connectionUri,
      "messages",
      messageUri,
      "messageStatus",
      "isCancellationPending",
    ],
    cancellationPending
  );
}
/**
 * Sets the given messageUri messageStatus to the given parameter (messageStatus).
 * Additionally calls markMessageAsCollapsed to the bool-exp from messageStatus data => (isProposed || isClaimed ||isRejected || isRetracted)
 * @param state
 * @param messageUri
 * @param connectionUri
 * @param needUri
 * @param messageStatus
 * @returns {*}
 */
export function updateMessageStatus(
  state,
  messageUri,
  connectionUri,
  needUri,
  messageStatus
) {
  let need = state.get(needUri);
  let connection = need && need.getIn(["connections", connectionUri]);
  let message = connection && connection.getIn(["messages", messageUri]);

  if (!message) {
    console.error(
      "No message with messageUri: <",
      messageUri,
      "> found within needUri: <",
      needUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  }

  //Check if there is any "positive" messageStatus, we assume that we do not want to display this message "fully"
  const hasCollapsedMessageState =
    messageStatus.get("isProposed") ||
    messageStatus.get("isClaimed") ||
    messageStatus.get("isRejected") ||
    messageStatus.get("isRetracted");

  state = markMessageAsCollapsed(
    state,
    messageUri,
    connectionUri,
    needUri,
    hasCollapsedMessageState
  );

  return state
    .setIn(
      [
        needUri,
        "connections",
        connectionUri,
        "messages",
        messageUri,
        "messageStatus",
      ],
      messageStatus
    )
    .setIn(
      [
        needUri,
        "connections",
        connectionUri,
        "messages",
        messageUri,
        "isMessageStatusUpToDate",
      ],
      true
    );
}
