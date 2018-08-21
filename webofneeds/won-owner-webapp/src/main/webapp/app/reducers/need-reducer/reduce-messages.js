import { parseMessage } from "./parse-message.js";
import { markUriAsRead } from "../../won-localstorage.js";
import { markConnectionAsRead } from "./reduce-connections.js";
import { getCorrectMessageUri } from "../../selectors.js";

/*
 "alreadyProcessed" flag, which indicates that we do not care about the
 sent status anymore and assume that it has been successfully sent to each server (incl. the remote)
 */
export function addMessage(state, wonMessage, alreadyProcessed = false) {
  if (wonMessage.getContentGraphs().length > 0) {
    // we only want to add messages to the state that actually contain text
    // content. (no empty connect messages, for example)
    let parsedMessage = parseMessage(wonMessage, alreadyProcessed);

    if (parsedMessage) {
      const connectionUri = parsedMessage.get("belongsToUri");
      let needUri = null;
      if (parsedMessage.getIn(["data", "outgoingMessage"])) {
        // needUri is the message's hasSenderNeed
        needUri = wonMessage.getSenderNeed();
      } else {
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
        let messages = state.getIn([
          needUri,
          "connections",
          connectionUri,
          "messages",
        ]);
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
