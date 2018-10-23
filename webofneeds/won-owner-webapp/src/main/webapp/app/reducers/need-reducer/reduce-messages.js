import { parseMessage } from "./parse-message.js";
import { markUriAsRead } from "../../won-localstorage.js";
import { markConnectionAsRead } from "./reduce-connections.js";
import { getCorrectMessageUri } from "../../selectors.js";

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
        if (wonMessage.hasContainedForwardedWonMessages()) {
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
        messages = messages.set(
          parsedMessage.getIn(["data", "uri"]),
          parsedMessage.get("data")
        );
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
  } else {
    console.log("no messages to add");
  }
  return state;
}

export function setMessageSelected(
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
      "no message with messageUri: <",
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
      "isSelected",
    ],
    isSelected
  );
}

export function markMessageAsRead(state, messageUri, connectionUri, needUri) {
  const need = state.get(needUri);
  const connection = need && need.getIn(["connections", connectionUri]);
  const message = connection && connection.getIn(["messages", messageUri]);

  markUriAsRead(messageUri);

  if (!message) {
    console.error(
      "no message with messageUri: <",
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
      "no message with messageUri: <",
      messageUri,
      "> found within needUri: <",
      needUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  } else {
    const proposedToCancelReferences = message.getIn([
      "references",
      "proposesToCancel",
    ]);

    if (proposedToCancelReferences) {
      proposedToCancelReferences.forEach(proposedToCancelRef => {
        const correctMessageUri = getCorrectMessageUri(
          messages,
          proposedToCancelRef
        );
        state = markMessageAsCancellationPending(
          state,
          correctMessageUri,
          connectionUri,
          needUri,
          false
        );
      });
    }

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
}

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
      "no message with messageUri: <",
      messageUri,
      "> found within needUri: <",
      needUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  }
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
      "no message with messageUri: <",
      messageUri,
      "> found within needUri: <",
      needUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  } else {
    const proposedToCancelReferences = message.getIn([
      "references",
      "proposesToCancel",
    ]);

    if (proposedToCancelReferences) {
      proposedToCancelReferences.forEach(proposedToCancelRef => {
        const correctMessageUri = getCorrectMessageUri(
          messages,
          proposedToCancelRef
        );
        state = markMessageAsCancellationPending(
          state,
          correctMessageUri,
          connectionUri,
          needUri,
          false
        );
      });
    }

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
}

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
      "no message with messageUri: <",
      messageUri,
      "> found within needUri: <",
      needUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  } else {
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
}

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
      "no message with messageUri: <",
      messageUri,
      "> found within needUri: <",
      needUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  } else {
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
      "no message with messageUri: <",
      messageUri,
      "> found within needUri: <",
      needUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  } else {
    const proposedToCancelReferences = message.getIn([
      "references",
      "proposesToCancel",
    ]);

    if (proposedToCancelReferences) {
      proposedToCancelReferences.forEach(proposedToCancelRef => {
        const correctMessageUri = getCorrectMessageUri(
          messages,
          proposedToCancelRef
        );
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
      "no message with messageUri: <",
      messageUri,
      "> found within needUri: <",
      needUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  } else {
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
      "no message with messageUri: <",
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
