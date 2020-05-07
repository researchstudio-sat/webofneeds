import { parseMessage } from "./parse-message.js";
import { markUriAsRead, isUriRead } from "../../won-localstorage.js";
import { markConnectionAsRead } from "./reduce-connections.js";
import { addAtomStub } from "./reduce-atoms.js";
import * as connectionSelectors from "../../redux/selectors/connection-selectors.js";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import * as connectionUtils from "../../redux/utils/connection-utils.js";
import { get, getIn } from "../../utils.js";
import Immutable from "immutable";

export function addMessage(
  state,
  wonMessage,
  alreadyProcessed = false,
  eventUriOverride = undefined
) {
  if (!wonMessage.isResponse()) {
    let parsedMessage = parseMessage(wonMessage, alreadyProcessed, false);

    if (parsedMessage) {
      if (eventUriOverride) {
        // In Some cases (like if we send a message) we need to override the messageUri in the wonMessage with the correct one
        parsedMessage = parsedMessage.setIn(["data", "uri"], eventUriOverride);
      }
      const allAtomsInState = state;
      const senderSocketUri = wonMessage.getSenderSocket();
      const targetSocketUri = wonMessage.getTargetSocket();

      const senderAtomUri = generalSelectors.getAtomUriBySocketUri(
        wonMessage.getSenderSocket()
      );
      const targetAtomUri = generalSelectors.getAtomUriBySocketUri(
        wonMessage.getTargetSocket()
      );
      const senderAtom = get(allAtomsInState, senderAtomUri);
      const targetAtom = get(allAtomsInState, targetAtomUri);

      const senderConnection =
        senderAtom &&
        get(senderAtom, "connections").find(conn =>
          connectionUtils.hasSocketUris(conn, senderSocketUri, targetSocketUri)
        );
      const targetConnection =
        targetAtom &&
        get(targetAtom, "connections").find(conn =>
          connectionUtils.hasSocketUris(conn, targetSocketUri, senderSocketUri)
        );

      if (targetAtomUri && !state.has(targetAtomUri)) {
        console.debug("Target Atom is not in the state yet, we need to add it");
        state = addAtomStub(state, targetAtomUri);
      }
      if (senderAtomUri && !state.has(senderAtomUri)) {
        console.debug("Sender Atom is not in the state yet, we need to add it");
        state = addAtomStub(state, senderAtomUri);
      }

      if (senderConnection) {
        const senderConnectionUri = get(senderConnection, "uri");
        parsedMessage = parsedMessage.setIn(["data", "outgoingMessage"], true);

        let messages = state.getIn([
          senderAtomUri,
          "connections",
          senderConnectionUri,
          "messages",
        ]);
        if (messages) {
          //ignore messages for nonexistant connections

          const existingMessage = messages.get(
            parsedMessage.getIn(["data", "uri"])
          );

          const isReceivedByOwn = !!get(existingMessage, "isReceivedByOwn");
          const isReceivedByRemote = !!get(
            existingMessage,
            "isReceivedByRemote"
          );
          if (existingMessage) {
            parsedMessage = parsedMessage.setIn(
              ["data", "messageStatus"],
              existingMessage.get("messageStatus")
            );
            parsedMessage = parsedMessage.setIn(
              ["data", "viewState"],
              existingMessage.get("viewState")
            );
          }
          if (!alreadyProcessed && (isReceivedByOwn || isReceivedByRemote)) {
            parsedMessage = parsedMessage
              .setIn(["data", "isReceivedByOwn"], isReceivedByOwn)
              .setIn(["data", "isReceivedByRemote"], isReceivedByRemote);
          }

          messages = messages.set(
            parsedMessage.getIn(["data", "uri"]),
            parsedMessage.get("data")
          );

          state = state.setIn(
            [senderAtomUri, "connections", senderConnectionUri, "messages"],
            messages
          );
        }
      }

      if (targetConnection) {
        const targetConnectionUri = get(targetConnection, "uri");
        parsedMessage = parsedMessage
          .setIn(["data", "outgoingMessage"], false)
          .setIn(
            ["data", "unread"],
            !wonMessage.isAtomHintMessage() &&
              !wonMessage.isSocketHintMessage() &&
              !isUriRead(getIn(parsedMessage, ["data", "uri"]))
          )
          .setIn(["data", "isReceivedByOwn"], true)
          .setIn(["data", "isReceivedByRemote"], true);

        if (
          getIn(parsedMessage, ["data", "unread"]) &&
          !connectionSelectors.isChatToGroupConnection(
            state,
            getIn(state, [targetAtomUri, "connections", targetConnectionUri])
          )
        ) {
          //If there is a new message for the connection we will set the connection to newConnection
          state = state.setIn(
            [targetAtomUri, "lastUpdateDate"],
            parsedMessage.getIn(["data", "date"])
          );
          state = state.setIn([targetAtomUri, "unread"], true);
          state = state.setIn(
            [
              targetAtomUri,
              "connections",
              targetConnectionUri,
              "lastUpdateDate",
            ],
            parsedMessage.getIn(["data", "date"])
          );
          state = state.setIn(
            [targetAtomUri, "connections", targetConnectionUri, "unread"],
            true
          );
        }

        let messages = state.getIn([
          targetAtomUri,
          "connections",
          targetConnectionUri,
          "messages",
        ]);
        if (messages) {
          //ignore messages for nonexistant connections

          /*
            Group Chat messages that are received are in the form of injected/referenced messages
            But we do not want to display these messages in that manner, therefore we simply ignore
            the encapsulating message and not store it within our state.
            */
          if (
            !connectionSelectors.isChatToGroupConnection(
              state,
              getIn(state, [targetAtomUri, "connections", targetConnectionUri])
            )
          ) {
            /**
             * if this message has any higher level protocol references, we need to update the state of the referenced messages as well
             */
            const references = parsedMessage.getIn(["data", "references"]);
            let hadReferences = false;
            if (references.get("claims")) {
              const claimedMessageUris = references.get("claims");
              claimedMessageUris.forEach(messageUri => {
                state = markMessageAsClaimed(
                  state,
                  messageUri,
                  targetConnectionUri,
                  targetAtomUri,
                  true
                );
              });
              hadReferences = true;
            }
            if (references.get("proposes")) {
              const proposedMessageUris = references.get("proposes");
              proposedMessageUris.forEach(messageUri => {
                state = markMessageAsProposed(
                  state,
                  messageUri,
                  targetConnectionUri,
                  targetAtomUri,
                  true
                );
              });
              hadReferences = true;
            }
            if (references.get("proposesToCancel")) {
              const proposesToCancelMessageUris = references.get(
                "proposesToCancel"
              );
              proposesToCancelMessageUris.forEach(messageUri => {
                state = markMessageAsCancellationPending(
                  state,
                  messageUri,
                  targetConnectionUri,
                  targetAtomUri,
                  true
                );
              });
              hadReferences = true;
            }
            if (references.get("accepts")) {
              const acceptedMessageUris = references.get("accepts");
              acceptedMessageUris.forEach(messageUri => {
                state = markMessageAsAccepted(
                  state,
                  messageUri,
                  targetConnectionUri,
                  targetAtomUri,
                  true
                );
              });
              hadReferences = true;
            }
            if (references.get("rejects")) {
              const rejectedMessageUris = references.get("rejects");
              rejectedMessageUris.forEach(messageUri => {
                state = markMessageAsRejected(
                  state,
                  messageUri,
                  targetConnectionUri,
                  targetAtomUri,
                  true
                );
              });
              hadReferences = true;
            }
            if (references.get("retracts")) {
              const retractedMessageUris = references.get("retracts");
              retractedMessageUris.forEach(messageUri => {
                state = markMessageAsRetracted(
                  state,
                  messageUri,
                  targetConnectionUri,
                  targetAtomUri,
                  true
                );
              });
              hadReferences = true;
            }

            //re-get messages after state changes from references
            if (hadReferences) {
              messages = state.getIn([
                targetAtomUri,
                "connections",
                targetConnectionUri,
                "messages",
              ]);
            }

            const existingMessage = messages.get(
              parsedMessage.getIn(["data", "uri"])
            );

            const isReceivedByOwn = !!get(existingMessage, "isReceivedByOwn");
            const isReceivedByRemote = !!get(
              existingMessage,
              "isReceivedByRemote"
            );

            if (!alreadyProcessed && (isReceivedByOwn || isReceivedByRemote)) {
              parsedMessage = parsedMessage
                .setIn(["data", "isReceivedByOwn"], isReceivedByOwn)
                .setIn(["data", "isReceivedByRemote"], isReceivedByRemote);
            }

            messages = messages.set(
              parsedMessage.getIn(["data", "uri"]),
              parsedMessage.get("data")
            );
          }

          state = state.setIn(
            [targetAtomUri, "connections", targetConnectionUri, "messages"],
            messages
          );
        }
      }

      const connections = connectionSelectors.getConnectionsToInjectMsgInto(
        state,
        targetSocketUri,
        getIn(parsedMessage, ["data", "uri"])
      );
      if (connections && connections.size > 0) {
        connections.map(conn => {
          let forwardMessage = parseMessage(wonMessage, alreadyProcessed, true);
          if (forwardMessage) {
            if (eventUriOverride) {
              // In Some cases (like if we send a message) we need to override the messageUri in the wonMessage with the correct one
              forwardMessage = forwardMessage.setIn(
                ["data", "uri"],
                eventUriOverride
              );
            }

            const connUri = get(conn, "uri");
            const atomUri = get(
              state.find(atom => !!getIn(atom, ["connections", connUri])),
              "uri"
            );

            if (
              connectionSelectors.isChatToGroupConnection(
                state,
                getIn(state, [atomUri, "connections", connUri])
              ) &&
              senderSocketUri !== get(conn, "socketUri")
            ) {
              let messages = state.getIn([
                atomUri,
                "connections",
                connUri,
                "messages",
              ]);
              if (messages) {
                //ignore messages for nonexistant connections
                const existingMessage = messages.get(
                  forwardMessage.getIn(["data", "uri"])
                );

                const isReceivedByOwn = !!get(
                  existingMessage,
                  "isReceivedByOwn"
                );
                const isReceivedByRemote = !!get(
                  existingMessage,
                  "isReceivedByRemote"
                );

                if (
                  !alreadyProcessed &&
                  (isReceivedByOwn || isReceivedByRemote)
                ) {
                  forwardMessage = forwardMessage
                    .setIn(["data", "isReceivedByOwn"], isReceivedByOwn)
                    .setIn(["data", "isReceivedByRemote"], isReceivedByRemote);
                }

                forwardMessage = forwardMessage.setIn(
                  ["data", "unread"],
                  !wonMessage.isAtomHintMessage() &&
                    !wonMessage.isSocketHintMessage() &&
                    !isUriRead(getIn(parsedMessage, ["data", "uri"]))
                );

                messages = messages.set(
                  forwardMessage.getIn(["data", "uri"]),
                  forwardMessage.get("data")
                );

                state = state.setIn(
                  [atomUri, "connections", connUri, "messages"],
                  messages
                );
              }
            }
          }
        });
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
  atomUri,
  isSelected
) {
  const atom = state.get(atomUri);
  const connection = atom && atom.getIn(["connections", connectionUri]);
  const message = connection && connection.getIn(["messages", messageUri]);

  markUriAsRead(messageUri);

  if (!message) {
    console.error(
      "No message with messageUri: <",
      messageUri,
      "> found within atomUri: <",
      atomUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  }

  return state.setIn(
    [
      atomUri,
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
  atomUri,
  isCollapsed
) {
  const atom = state.get(atomUri);
  const connection = atom && atom.getIn(["connections", connectionUri]);
  const message = connection && connection.getIn(["messages", messageUri]);

  markUriAsRead(messageUri);

  if (!message) {
    console.error(
      "No message with messageUri: <",
      messageUri,
      "> found within atomUri: <",
      atomUri,
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
      atomUri,
      false
    );

    state = markMessageExpandAllReferences(
      state,
      messageUri,
      connectionUri,
      atomUri,
      false
    );
  }

  return state.setIn(
    [
      atomUri,
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
 * @param atomUri
 * @param isExpanded
 * @param reference
 * @returns {*}
 */
export function markMessageExpandAllReferences(
  state,
  messageUri,
  connectionUri,
  atomUri,
  isExpanded
) {
  const atom = state.get(atomUri);
  const connection = atom && atom.getIn(["connections", connectionUri]);
  const message = connection && connection.getIn(["messages", messageUri]);

  if (!message) {
    console.error(
      "No message with messageUri: <",
      messageUri,
      "> found within atomUri: <",
      atomUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  }

  const expandedReferences = state.getIn([
    atomUri,
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
      "> found within atomUri: <",
      atomUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  }

  return state.setIn(
    [
      atomUri,
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
 * @param atomUri
 * @param isExpanded
 * @param reference
 * @returns {*}
 */
export function markMessageExpandReferences(
  state,
  messageUri,
  connectionUri,
  atomUri,
  isExpanded,
  reference
) {
  const atom = state.get(atomUri);
  const connection = atom && atom.getIn(["connections", connectionUri]);
  const message = connection && connection.getIn(["messages", messageUri]);

  if (!message) {
    console.error(
      "No message with messageUri: <",
      messageUri,
      "> found within atomUri: <",
      atomUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  }

  return state.setIn(
    [
      atomUri,
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
  atomUri,
  showActions
) {
  const atom = state.get(atomUri);
  const connection = atom && atom.getIn(["connections", connectionUri]);
  const message = connection && connection.getIn(["messages", messageUri]);

  markUriAsRead(messageUri);

  if (!message) {
    console.error(
      "No message with messageUri: <",
      messageUri,
      "> found within atomUri: <",
      atomUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  }

  return state.setIn(
    [
      atomUri,
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

export function markMessageAsRead(state, messageUri, connectionUri, atomUri) {
  const atom = state.get(atomUri);
  const connection = atom && atom.getIn(["connections", connectionUri]);
  const message = connection && connection.getIn(["messages", messageUri]);

  markUriAsRead(messageUri);

  if (!message) {
    console.error(
      "No message with messageUri: <",
      messageUri,
      "> found within atomUri: <",
      atomUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  }

  state = state.setIn(
    [atomUri, "connections", connectionUri, "messages", messageUri, "unread"],
    false
  );

  if (
    state
      .getIn([atomUri, "connections", connectionUri, "messages"])
      .filter(msg => msg.get("unread")).size == 0
  ) {
    state = markConnectionAsRead(state, connectionUri, atomUri);
  }

  return state.setIn(
    [atomUri, "connections", connectionUri, "messages", messageUri, "unread"],
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
 * @param atomUri
 * @param rejected
 * @returns {*}
 */
export function markMessageAsRejected(
  state,
  messageUri,
  connectionUri,
  atomUri,
  rejected
) {
  let atom = state.get(atomUri);
  let connection = atom && atom.getIn(["connections", connectionUri]);
  let messages = connection && connection.get("messages");
  let message = messages && messages.get(messageUri);

  if (!message) {
    console.error(
      "No message with messageUri: <",
      messageUri,
      "> found within atomUri: <",
      atomUri,
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
      state = markMessageAsCancellationPending(
        state,
        proposedToCancelRef,
        connectionUri,
        atomUri,
        false
      );
    });
  }

  const proposesReferences = message.getIn(["references", "proposes"]);

  if (proposesReferences) {
    proposesReferences.forEach(proposesRef => {
      state = markMessageAsProposed(
        state,
        proposesRef,
        connectionUri,
        atomUri,
        false
      );
    });
  }

  const claimsReferences = message.getIn(["references", "claims"]);

  if (claimsReferences) {
    claimsReferences.forEach(claimsRef => {
      state = markMessageAsClaimed(
        state,
        claimsRef,
        connectionUri,
        atomUri,
        false
      );
    });
  }

  state = markMessageAsCollapsed(
    state,
    messageUri,
    connectionUri,
    atomUri,
    rejected
  );

  const rejectedMessageUris =
    connection && connection.getIn(["agreementData", "rejectedMessageUris"]);

  return state.setIn(
    [
      atomUri,
      "connections",
      connectionUri,
      "agreementData",
      "rejectedMessageUris",
    ],
    rejected
      ? (rejectedMessageUris && rejectedMessageUris.add(messageUri)) ||
        Immutable.Set(messageUri)
      : (rejectedMessageUris &&
          rejectedMessageUris.filter(uri => uri != messageUri)) ||
        Immutable.Set()
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
 * @param atomUri
 * @param retracted
 * @returns {*}
 */
export function markMessageAsRetracted(
  state,
  messageUri,
  connectionUri,
  atomUri,
  retracted
) {
  let atom = state.get(atomUri);
  let connection = atom && atom.getIn(["connections", connectionUri]);
  let messages = connection && connection.get("messages");
  let message = messages && messages.get(messageUri);

  if (!message) {
    console.error(
      "No message with messageUri: <",
      messageUri,
      "> found within atomUri: <",
      atomUri,
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
      state = markMessageAsCancellationPending(
        state,
        proposedToCancelRef,
        connectionUri,
        atomUri,
        false
      );
    });
  }

  const proposesReferences = message.getIn(["references", "proposes"]);

  if (proposesReferences) {
    proposesReferences.forEach(proposesRef => {
      state = markMessageAsProposed(
        state,
        proposesRef,
        connectionUri,
        atomUri,
        false
      );
    });
  }

  const claimsReferences = message.getIn(["references", "claims"]);

  if (claimsReferences) {
    claimsReferences.forEach(claimsRef => {
      state = markMessageAsClaimed(
        state,
        claimsRef,
        connectionUri,
        atomUri,
        false
      );
    });
  }

  state = markMessageAsCollapsed(
    state,
    messageUri,
    connectionUri,
    atomUri,
    retracted
  );

  const retractedMessageUris =
    connection && connection.getIn(["agreementData", "retractedMessageUris"]);

  return state.setIn(
    [
      atomUri,
      "connections",
      connectionUri,
      "agreementData",
      "retractedMessageUris",
    ],
    retracted
      ? (retractedMessageUris && retractedMessageUris.add(messageUri)) ||
        Immutable.Set(messageUri)
      : (retractedMessageUris &&
          retractedMessageUris.filter(uri => uri != messageUri)) ||
        Immutable.Set()
  );
}

/**
 * Sets the given messageUri messageStatus->isClaimed to the given parameter (claimed).
 * Additionally calls markMessageAsCollapsed to the given parameter (claimed) as well
 * @param state
 * @param messageUri
 * @param connectionUri
 * @param atomUri
 * @param claimed
 * @returns {*}
 */
export function markMessageAsClaimed(
  state,
  messageUri,
  connectionUri,
  atomUri,
  claimed
) {
  let atom = state.get(atomUri);
  let connection = atom && atom.getIn(["connections", connectionUri]);

  if (!connection) {
    console.error(
      "No connection with connectionUri: <",
      connectionUri,
      "> found within atomUri: <",
      atomUri,
      ">"
    );
    return state;
  }

  state = markMessageAsCollapsed(
    state,
    messageUri,
    connectionUri,
    atomUri,
    claimed
  );

  const claimedMessageUris =
    connection && connection.getIn(["agreementData", "claimedMessageUris"]);

  return state.setIn(
    [
      atomUri,
      "connections",
      connectionUri,
      "agreementData",
      "claimedMessageUris",
    ],
    claimed
      ? (claimedMessageUris && claimedMessageUris.add(messageUri)) ||
        Immutable.Set(messageUri)
      : (claimedMessageUris &&
          claimedMessageUris.filter(uri => uri != messageUri)) ||
        Immutable.Set()
  );
}

/**
 * Sets the given messageUri messageStatus->isAgreed to the given parameter (isAgreedOn).
 * Additionally calls markMessageAsCollapsed to the given parameter (isAgreedOn) as well
 * @param state
 * @param messageUri
 * @param connectionUri
 * @param atomUri
 * @param isAgreedOn
 * @returns {*}
 */
export function markMessageAsAgreed(
  state,
  messageUri,
  connectionUri,
  atomUri,
  isAgreedOn
) {
  let atom = state.get(atomUri);
  let connection = atom && atom.getIn(["connections", connectionUri]);

  if (!connection) {
    console.error(
      "No connection with connectionUri: <",
      connectionUri,
      "> found within atomUri: <",
      atomUri,
      ">"
    );
    return state;
  }

  state = markMessageAsCollapsed(
    state,
    messageUri,
    connectionUri,
    atomUri,
    isAgreedOn
  );

  const agreedMessageUris =
    connection && connection.getIn(["agreementData", "agreedMessageUris"]);

  return state.setIn(
    [
      atomUri,
      "connections",
      connectionUri,
      "agreementData",
      "agreedMessageUris",
    ],
    isAgreedOn
      ? (agreedMessageUris && agreedMessageUris.add(messageUri)) ||
        Immutable.Set(messageUri)
      : (agreedMessageUris &&
          agreedMessageUris.filter(uri => uri != messageUri)) ||
        Immutable.Set()
  );
}

/**
 * Sets the given messageUri messageStatus->isProposed to the given parameter (proposed).
 * Additionally calls markMessageAsCollapsed to the given parameter (proposed) as well
 * @param state
 * @param messageUri
 * @param connectionUri
 * @param atomUri
 * @param proposed
 * @returns {*}
 */
export function markMessageAsProposed(
  state,
  messageUri,
  connectionUri,
  atomUri,
  proposed
) {
  let atom = state.get(atomUri);
  let connection = atom && atom.getIn(["connections", connectionUri]);

  if (!connection) {
    console.error(
      "No connection with connectionUri: <",
      connectionUri,
      "> found within atomUri: <",
      atomUri,
      ">"
    );
    return state;
  }

  state = markMessageAsCollapsed(
    state,
    messageUri,
    connectionUri,
    atomUri,
    proposed
  );

  //if !proposed remove from set because message was retracted/rejected
  const proposedMessageUris =
    connection && connection.getIn(["agreementData", "proposedMessageUris"]);

  return state.setIn(
    [
      atomUri,
      "connections",
      connectionUri,
      "agreementData",
      "proposedMessageUris",
    ],
    proposed
      ? (proposedMessageUris && proposedMessageUris.add(messageUri)) ||
        Immutable.Set([messageUri])
      : (proposedMessageUris &&
          proposedMessageUris.filter(uri => uri != messageUri)) ||
        Immutable.Set()
  );
}

export function markMessageAsAccepted(
  state,
  messageUri,
  connectionUri,
  atomUri,
  accepted
) {
  let atom = state.get(atomUri);
  let connection = atom && atom.getIn(["connections", connectionUri]);
  let messages = connection && connection.get("messages");
  let message = messages && messages.get(messageUri);

  if (!message) {
    console.error(
      "No message with messageUri: <",
      messageUri,
      "> found within atomUri: <",
      atomUri,
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
      state = markMessageAsCancelled(
        state,
        proposedToCancelRef,
        connectionUri,
        atomUri,
        true
      );
    });
  }

  // remove from cancelled and cancellationPending sets because of not pending anymore
  if (accepted) {
    state = markMessageAsCancellationPending(
      state,
      messageUri,
      connectionUri,
      atomUri,
      false
    );
  }

  const agreementUris =
    connection && connection.getIn(["agreementData", "agreementUris"]);

  return state.setIn(
    [atomUri, "connections", connectionUri, "agreementData", "agreementUris"],
    (agreementUris && agreementUris.add(messageUri)) ||
      Immutable.Set(messageUri)
  );
}

export function markMessageAsCancelled(
  state,
  messageUri,
  connectionUri,
  atomUri,
  cancelled
) {
  let atom = state.get(atomUri);
  let connection = atom && atom.getIn(["connections", connectionUri]);

  if (!connection) {
    console.error(
      "No connection with connectionUri: <",
      connectionUri,
      "> found within atomUri: <",
      atomUri,
      ">"
    );
    return state;
  }

  //if cancelled remove from accepted/agreementUris and cancellationPending sets
  if (cancelled) {
    const agreementUris =
      connection && connection.getIn(["agreementData", "agreementUris"]);

    state = state.setIn(
      [atomUri, "connections", connectionUri, "agreementData", "agreementUris"],
      (agreementUris && agreementUris.filter(uri => uri != messageUri)) ||
        Immutable.Set()
    );

    const cancellationPendingAgreementUris =
      connection &&
      connection.getIn(["agreementData", "cancellationPendingAgreementUris"]);

    state = state.setIn(
      [
        atomUri,
        "connections",
        connectionUri,
        "agreementData",
        "cancellationPendingAgreementUris",
      ],
      (cancellationPendingAgreementUris &&
        cancellationPendingAgreementUris.filter(uri => uri != messageUri)) ||
        Immutable.Set()
    );
  }

  const cancelledAgreementUris =
    connection && connection.getIn(["agreementData", "cancelledAgreementUris"]);
  return state.setIn(
    [
      atomUri,
      "connections",
      connectionUri,
      "agreementData",
      "cancelledAgreementUris",
    ],
    cancelled
      ? (cancelledAgreementUris && cancelledAgreementUris.add(messageUri)) ||
        Immutable.Set(messageUri)
      : (cancelledAgreementUris &&
          cancelledAgreementUris.filter(uri => uri != messageUri)) ||
        Immutable.Set()
  );
}

export function markMessageAsCancellationPending(
  state,
  messageUri,
  connectionUri,
  atomUri,
  cancellationPending
) {
  let atom = state.get(atomUri);
  let connection = atom && atom.getIn(["connections", connectionUri]);

  if (!connection) {
    console.error(
      "No connection with connectionUri: <",
      connectionUri,
      "> found within atomUri: <",
      atomUri,
      ">"
    );
    return state;
  }

  const cancellationPendingUris =
    connection &&
    connection.getIn(["agreementData", "cancellationPendingAgreementUris"]);

  // if !cancellationPending remove from set because message was retracted/rejected
  return state.setIn(
    [
      atomUri,
      "connections",
      connectionUri,
      "agreementData",
      "cancellationPendingAgreementUris",
    ],

    cancellationPending
      ? (cancellationPendingUris && cancellationPendingUris.add(messageUri)) ||
        Immutable.Set([messageUri])
      : (cancellationPendingUris &&
          cancellationPendingUris.filter(uri => uri != messageUri)) ||
        Immutable.Set()
  );
}

/**
 * Sets the given messageUri messageStatus to the given parameter (messageStatus).
 * Additionally calls markMessageAsCollapsed to the bool-exp from messageStatus data => (isProposed || isClaimed ||isRejected || isRetracted)
 * @param state
 * @param messageUri
 * @param connectionUri
 * @param atomUri
 * @returns {*}
 *
 */
export function updateMessageStatus(state, messageUri, connectionUri, atomUri) {
  let atom = state.get(atomUri);
  let connection = atom && atom.getIn(["connections", connectionUri]);
  let message = connection && connection.getIn(["messages", messageUri]);

  if (!message) {
    console.error(
      "No message with messageUri: <",
      messageUri,
      "> found within atomUri: <",
      atomUri,
      "> connectionUri: <",
      connectionUri,
      ">"
    );
    return state;
  }

  //Check if there is any "positive" messageStatus, we assume that we do not want to display this message "fully"
  const agreementData = connection && connection.get("agreementData");

  const isProposed =
    agreementData && !!agreementData.getIn(["proposedMessageUris", messageUri]);
  const isClaimed =
    agreementData && !!agreementData.getIn(["claimedMessageUris", messageUri]);
  const isAgreed =
    agreementData && !!agreementData.getIn(["agreedMessageUris", messageUri]);
  const isRejected =
    agreementData && !!agreementData.getIn(["rejectedMessageUris", messageUri]);
  const isRetracted =
    agreementData &&
    !!agreementData.getIn(["retractedMessageUris", messageUri]);

  const hasCollapsedMessageState =
    isProposed || isClaimed || isAgreed || isRejected || isRetracted;

  state = markMessageAsCollapsed(
    state,
    messageUri,
    connectionUri,
    atomUri,
    hasCollapsedMessageState
  );

  return state.setIn(
    [
      atomUri,
      "connections",
      connectionUri,
      "messages",
      messageUri,
      "isMessageStatusUpToDate",
    ],
    true
  );
}
