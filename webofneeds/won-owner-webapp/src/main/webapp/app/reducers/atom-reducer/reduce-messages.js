import { parseMessage } from "./parse-message.js";
import { markUriAsRead, isUriRead } from "../../won-localstorage.js";
import { markConnectionAsRead } from "./reduce-connections.js";
import { addAtomStub } from "./reduce-atoms.js";
import vocab from "../../service/vocab.js";
import * as connectionUtils from "../../redux/utils/connection-utils.js";
import * as messageUtils from "../../redux/utils/message-utils.js";
import {
  get,
  getIn,
  getUri,
  extractAtomUriBySocketUri,
  extractAtomUriFromConnectionUri,
} from "../../utils.js";
import Immutable from "immutable";
import {
  getSenderSocketType,
  getTargetSocketType,
} from "../../redux/selectors/general-selectors";

export function addMessage(
  allAtomsInState,
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
      const parsedMessageUri = getIn(parsedMessage, ["data", "uri"]);
      const senderSocketUri = wonMessage.getSenderSocket();
      const targetSocketUri = wonMessage.getTargetSocket();

      const senderAtomUri = extractAtomUriBySocketUri(
        wonMessage.getSenderSocket()
      );
      const targetAtomUri = extractAtomUriBySocketUri(
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

      if (targetAtomUri && !allAtomsInState.has(targetAtomUri)) {
        console.debug("Target Atom is not in the state yet, we need to add it");
        allAtomsInState = addAtomStub(allAtomsInState, targetAtomUri);
      }
      if (senderAtomUri && !allAtomsInState.has(senderAtomUri)) {
        console.debug("Sender Atom is not in the state yet, we need to add it");
        allAtomsInState = addAtomStub(allAtomsInState, senderAtomUri);
      }

      if (senderConnection) {
        const senderConnectionUri = getUri(senderConnection);
        parsedMessage = parsedMessage.setIn(["data", "outgoingMessage"], true);

        let messages = connectionUtils.getMessages(senderConnection);
        if (messages) {
          //ignore messages for nonexistant connections

          const existingMessage = get(messages, parsedMessageUri);

          if (existingMessage) {
            parsedMessage = parsedMessage.setIn(
              ["data", "messageStatus"],
              get(existingMessage, "messageStatus")
            );
            parsedMessage = parsedMessage.setIn(
              ["data", "viewState"],
              get(existingMessage, "viewState")
            );
          }

          const isReceivedByOwn = messageUtils.isReceivedByOwn(existingMessage);
          const isReceivedByRemote = messageUtils.isReceivedByRemote(
            existingMessage
          );

          if (!alreadyProcessed && (isReceivedByOwn || isReceivedByRemote)) {
            parsedMessage = parsedMessage
              .setIn(["data", "isReceivedByOwn"], isReceivedByOwn)
              .setIn(["data", "isReceivedByRemote"], isReceivedByRemote);
          }

          allAtomsInState = allAtomsInState.setIn(
            [senderAtomUri, "connections", senderConnectionUri, "messages"],
            messages
              .set(parsedMessageUri, get(parsedMessage, "data"))
              .toOrderedMap()
              .sortBy(sortByMessageTimeStamp)
          );
        }
      }

      if (targetConnection) {
        const targetConnectionUri = getUri(targetConnection);
        parsedMessage = parsedMessage
          .setIn(["data", "outgoingMessage"], false)
          .setIn(
            ["data", "unread"],
            !wonMessage.isAtomHintMessage() &&
              !wonMessage.isSocketHintMessage() &&
              !isUriRead(parsedMessageUri)
          )
          .setIn(["data", "isReceivedByOwn"], true)
          .setIn(["data", "isReceivedByRemote"], true);

        if (
          getIn(parsedMessage, ["data", "unread"]) &&
          !isChatToGroupConnection(
            allAtomsInState,
            getIn(allAtomsInState, [
              targetAtomUri,
              "connections",
              targetConnectionUri,
            ])
          )
        ) {
          //If there is a new message for the connection we will set the connection to newConnection
          allAtomsInState = allAtomsInState
            .setIn(
              [targetAtomUri, "lastUpdateDate"],
              getIn(parsedMessage, ["data", "date"])
            )
            .setIn([targetAtomUri, "unread"], true)
            .setIn(
              [
                targetAtomUri,
                "connections",
                targetConnectionUri,
                "lastUpdateDate",
              ],
              getIn(parsedMessage, ["data", "date"])
            )
            .setIn(
              [targetAtomUri, "connections", targetConnectionUri, "unread"],
              true
            );
        }

        let messages = connectionUtils.getMessages(targetConnection);
        if (messages) {
          //ignore messages for nonexistant connections

          /*
            Group Chat messages that are received are in the form of injected/referenced messages
            But we do not want to display these messages in that manner, therefore we simply ignore
            the encapsulating message and not store it within our state.
            */
          if (
            !isChatToGroupConnection(
              allAtomsInState,
              getIn(allAtomsInState, [
                targetAtomUri,
                "connections",
                targetConnectionUri,
              ])
            )
          ) {
            /**
             * if this message has any higher level protocol references, we need to update the state of the referenced messages as well
             */
            const references = getIn(parsedMessage, ["data", "references"]);
            let hadReferences = false;
            if (get(references, "claims")) {
              const claimedMessageUris = get(references, "claims");
              claimedMessageUris.forEach(messageUri => {
                allAtomsInState = markMessageAsClaimed(
                  allAtomsInState,
                  messageUri,
                  targetConnectionUri,
                  targetAtomUri,
                  true
                );
              });
              hadReferences = true;
            }
            if (get(references, "proposes")) {
              const proposedMessageUris = get(references, "proposes");
              proposedMessageUris.forEach(messageUri => {
                allAtomsInState = markMessageAsProposed(
                  allAtomsInState,
                  messageUri,
                  targetConnectionUri,
                  targetAtomUri,
                  true
                );
              });
              hadReferences = true;
            }
            if (get(references, "proposesToCancel")) {
              const proposesToCancelMessageUris = get(
                references,
                "proposesToCancel"
              );
              proposesToCancelMessageUris.forEach(messageUri => {
                allAtomsInState = markMessageAsCancellationPending(
                  allAtomsInState,
                  messageUri,
                  targetConnectionUri,
                  targetAtomUri,
                  true
                );
              });
              hadReferences = true;
            }
            if (get(references, "accepts")) {
              const acceptedMessageUris = get(references, "accepts");
              acceptedMessageUris.forEach(messageUri => {
                allAtomsInState = markMessageAsAccepted(
                  allAtomsInState,
                  messageUri,
                  targetConnectionUri,
                  targetAtomUri,
                  true
                );
              });
              hadReferences = true;
            }
            if (get(references, "rejects")) {
              const rejectedMessageUris = get(references, "rejects");
              rejectedMessageUris.forEach(messageUri => {
                allAtomsInState = markMessageAsRejected(
                  allAtomsInState,
                  messageUri,
                  targetConnectionUri,
                  targetAtomUri,
                  true
                );
              });
              hadReferences = true;
            }
            if (get(references, "retracts")) {
              const retractedMessageUris = get(references, "retracts");
              retractedMessageUris.forEach(messageUri => {
                allAtomsInState = markMessageAsRetracted(
                  allAtomsInState,
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
              parsedMessage = parsedMessage.setIn(["data", "unread"], false);
              messages = connectionUtils.getMessages(targetConnection);
            }

            const existingMessage = get(messages, parsedMessageUri);

            const isReceivedByOwn = messageUtils.isReceivedByOwn(
              existingMessage
            );
            const isReceivedByRemote = messageUtils.isReceivedByRemote(
              existingMessage
            );

            if (!alreadyProcessed && (isReceivedByOwn || isReceivedByRemote)) {
              parsedMessage = parsedMessage
                .setIn(["data", "isReceivedByOwn"], isReceivedByOwn)
                .setIn(["data", "isReceivedByRemote"], isReceivedByRemote);
            }

            messages = messages.set(
              parsedMessageUri,
              get(parsedMessage, "data")
            );
          }

          allAtomsInState = allAtomsInState.setIn(
            [targetAtomUri, "connections", targetConnectionUri, "messages"],
            messages.toOrderedMap().sortBy(sortByMessageTimeStamp)
          );
        }
      }

      const getConnectionsToInjectMsgInto = (
        allAtomsInState,
        targetSocketUri,
        msgUri
      ) => {
        const allConnections =
          allAtomsInState &&
          allAtomsInState.flatMap(atom => get(atom, "connections"));

        return allConnections
          .filter(conn => connectionUtils.isConnected(conn))
          .filter(conn =>
            connectionUtils.hasTargetSocketUri(conn, targetSocketUri)
          )
          .filter(conn => !connectionUtils.getMessage(conn, msgUri));
      };

      const connections = getConnectionsToInjectMsgInto(
        allAtomsInState,
        targetSocketUri,
        parsedMessageUri
      );
      if (connections && connections.size > 0) {
        const forwardMessage = parseMessage(wonMessage, alreadyProcessed, true);
        let forwardMessageData = get(forwardMessage, "data");
        if (eventUriOverride) {
          // In Some cases (like if we send a message) we need to override the messageUri in the wonMessage with the correct one
          forwardMessageData = forwardMessageData.set("uri", eventUriOverride);
        }
        const forwardMessageUri = getUri(forwardMessageData);

        forwardMessageData &&
          connections.map((conn, connUri) => {
            const atomUri = get(
              get(allAtomsInState, extractAtomUriFromConnectionUri(connUri)) ||
                allAtomsInState.find(
                  atom => !!getIn(atom, ["connections", connUri])
                ),
              "uri"
            );

            if (
              isChatToGroupConnection(
                allAtomsInState,
                getIn(allAtomsInState, [atomUri, "connections", connUri])
              ) &&
              senderSocketUri !== get(conn, "socketUri")
            ) {
              let messages = connectionUtils.getMessages(conn);
              if (messages) {
                //ignore messages for nonexistant connections
                const existingMessage = get(messages, forwardMessageUri);

                const isReceivedByOwn = messageUtils.isReceivedByOwn(
                  existingMessage
                );
                const isReceivedByRemote = messageUtils.isReceivedByRemote(
                  existingMessage
                );

                if (
                  !alreadyProcessed &&
                  (isReceivedByOwn || isReceivedByRemote)
                ) {
                  forwardMessageData = forwardMessageData
                    .set("isReceivedByOwn", isReceivedByOwn)
                    .set("isReceivedByRemote", isReceivedByRemote);
                }

                forwardMessageData = forwardMessageData.set(
                  "unread",
                  !wonMessage.isAtomHintMessage() &&
                    !wonMessage.isSocketHintMessage() &&
                    !isUriRead(forwardMessageUri)
                );

                allAtomsInState = allAtomsInState.setIn(
                  [atomUri, "connections", connUri, "messages"],
                  messages
                    .set(forwardMessageUri, forwardMessageData)
                    .toOrderedMap()
                    .sortBy(sortByMessageTimeStamp)
                );
              }
            }
          });
      }
    }
  }
  return allAtomsInState;
}

/*
 This method should only be called to for messages that are already stored on the server (reload, initial login etc)
 because we will add all the messages with the "alreadyProcessed" flag, which indicates that we do not care about the
 sent status anymore and assume that it has been successfully sent to each server (incl. the remote)
 */
export function addExistingMessages(allAtomsInState, wonMessages) {
  if (wonMessages && wonMessages.size > 0) {
    wonMessages.map(wonMessage => {
      allAtomsInState = addMessage(allAtomsInState, wonMessage, true);
    });
  }
  return allAtomsInState;
}

export function markMessageAsSelected(
  allAtomsInState,
  messageUri,
  connectionUri,
  atomUri,
  isSelected
) {
  const atom = get(allAtomsInState, atomUri);
  const connection = getIn(atom, ["connections", connectionUri]);
  let messages = connectionUtils.getMessages(connection);
  const message = get(messages, messageUri);

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
    return allAtomsInState;
  }

  return allAtomsInState.setIn(
    [atomUri, "connections", connectionUri, "messages"],
    messages.set(
      messageUri,
      message
        .setIn(["viewState", "isSelected"], isSelected)
        .toOrderedMap()
        .sortBy(sortByMessageTimeStamp)
    )
  );
}

export function markMessageAsCollapsed(
  allAtomsInState,
  messageUri,
  connectionUri,
  atomUri,
  isCollapsed
) {
  const atom = get(allAtomsInState, atomUri);
  const connection = getIn(atom, ["connections", connectionUri]);
  let messages = connectionUtils.getMessages(connection);
  const message = get(messages, messageUri);

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
    return allAtomsInState;
  }

  if (isCollapsed) {
    allAtomsInState = markMessageShowActions(
      allAtomsInState,
      messageUri,
      connectionUri,
      atomUri,
      false
    );

    allAtomsInState = markMessageExpandAllReferences(
      allAtomsInState,
      messageUri,
      connectionUri,
      atomUri,
      false
    );
  }
  return allAtomsInState.setIn(
    [atomUri, "connections", connectionUri, "messages"],
    messages
      .setIn(
        messageUri,
        message.setIn(["viewState", "isCollapsed"], isCollapsed)
      )
      .toOrderedMap()
      .sortBy(sortByMessageTimeStamp)
  );
}

/**
 * Collapses/Expands all available references within the viewState of a message based on the isExpanded value
 * @param allAtomsInState
 * @param messageUri
 * @param connectionUri
 * @param atomUri
 * @param isExpanded
 * @param reference
 * @returns {*}
 */
export function markMessageExpandAllReferences(
  allAtomsInState,
  messageUri,
  connectionUri,
  atomUri,
  isExpanded
) {
  const atom = get(allAtomsInState, atomUri);
  const connection = getIn(atom, ["connections", connectionUri]);

  let messages = connectionUtils.getMessages(connection);
  const message = get(messages, messageUri);

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
    return allAtomsInState;
  }

  const expandedReferences = getIn(message, [
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
    return allAtomsInState;
  }

  return allAtomsInState.setIn(
    [atomUri, "connections", connectionUri, "messages"],
    messages
      .set(
        messageUri,
        message.setIn(
          ["viewState", "expandedReferences"],
          expandedReferences.map(() => isExpanded)
        )
      )
      .toOrderedMap()
      .sortBy(sortByMessageTimeStamp)
  );
}

/**
 * Collapses/Expands the given reference within the viewState of a message based on the isExpanded value
 * @param allAtomsInState
 * @param messageUri
 * @param connectionUri
 * @param atomUri
 * @param isExpanded
 * @param reference
 * @returns {*}
 */
export function markMessageExpandReferences(
  allAtomsInState,
  messageUri,
  connectionUri,
  atomUri,
  isExpanded,
  reference
) {
  const atom = get(allAtomsInState, atomUri);
  const connection = getIn(atom, ["connections", connectionUri]);
  let messages = connectionUtils.getMessages(connection);
  const message = get(messages, messageUri);

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
    return allAtomsInState;
  }

  return allAtomsInState.setIn(
    [atomUri, "connections", connectionUri, "messages"],
    messages
      .set(
        messageUri,
        message.setIn(
          ["viewState", "expandedReferences", reference],
          isExpanded
        )
      )
      .toOrderedMap()
      .sortBy(sortByMessageTimeStamp)
  );
}

export function markMessageShowActions(
  allAtomsInState,
  messageUri,
  connectionUri,
  atomUri,
  showActions
) {
  const atom = get(allAtomsInState, atomUri);
  const connection = getIn(atom, ["connections", connectionUri]);

  let messages = connectionUtils.getMessages(connection);
  const message = get(messages, messageUri);

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
    return allAtomsInState;
  }

  return allAtomsInState.setIn(
    [atomUri, "connections", connectionUri, "messages"],
    messages
      .set(messageUri, message.setIn(["viewState", "showActions"], showActions))
      .sortBy(sortByMessageTimeStamp)
  );
}

export function markMessageAsRead(
  allAtomsInState,
  messageUri,
  connectionUri,
  atomUri,
  read = true
) {
  const atom = get(allAtomsInState, atomUri);
  const connection = getIn(atom, ["connections", connectionUri]);

  let messages = connectionUtils.getMessages(connection);
  const message = get(messages, messageUri);

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
    return allAtomsInState;
  }

  allAtomsInState = allAtomsInState.setIn(
    [atomUri, "connections", connectionUri, "messages"],
    messages
      .set(messageUri, message.set("unread", !read))
      .sortBy(sortByMessageTimeStamp)
  );

  if (!messages.find(msg => get(msg, "unread"))) {
    allAtomsInState = markConnectionAsRead(
      allAtomsInState,
      connectionUri,
      atomUri
    );
  }

  return allAtomsInState;
}

/**
 * Sets the given messageUri messageStatus->isRejected to the given parameter (rejected).
 * Additionally calls markMessageAsCollapsed to the given parameter (rejected) as well
 * Additionally calls markMessageAsCancellationPending to the referencedMessages with the parameter false
 * Additionally calls markMessageAsProposed to the referencedMessages with the parameter false
 * Additionally calls markMessageAsClaimed to the referencedMessages with the parameter false
 * @param allAtomsInState
 * @param messageUri
 * @param connectionUri
 * @param atomUri
 * @param rejected
 * @returns {*}
 */
export function markMessageAsRejected(
  allAtomsInState,
  messageUri,
  connectionUri,
  atomUri,
  rejected
) {
  let atom = get(allAtomsInState, atomUri);
  let connection = getIn(atom, ["connections", connectionUri]);
  let messages = connectionUtils.getMessages(connection);
  let message = get(messages, messageUri);

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
    return allAtomsInState;
  }
  const proposedToCancelReferences = messageUtils.getProposesToCancelReferences(
    message
  );

  if (proposedToCancelReferences) {
    proposedToCancelReferences.forEach(proposedToCancelRef => {
      allAtomsInState = markMessageAsCancellationPending(
        allAtomsInState,
        proposedToCancelRef,
        connectionUri,
        atomUri,
        false
      );
    });
  }

  const proposesReferences = messageUtils.getProposesReferences(message);

  if (proposesReferences) {
    proposesReferences.forEach(proposesRef => {
      allAtomsInState = markMessageAsProposed(
        allAtomsInState,
        proposesRef,
        connectionUri,
        atomUri,
        false
      );
    });
  }

  const claimsReferences = messageUtils.getClaimsReferences(message);

  if (claimsReferences) {
    claimsReferences.forEach(claimsRef => {
      allAtomsInState = markMessageAsClaimed(
        allAtomsInState,
        claimsRef,
        connectionUri,
        atomUri,
        false
      );
    });
  }

  allAtomsInState = markMessageAsRead(
    allAtomsInState,
    messageUri,
    connectionUri,
    atomUri,
    false
  );

  allAtomsInState = markMessageAsCollapsed(
    allAtomsInState,
    messageUri,
    connectionUri,
    atomUri,
    rejected
  );

  const rejectedMessageUris = getIn(connection, [
    "agreementData",
    "rejectedMessageUris",
  ]);

  return allAtomsInState.setIn(
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
 * @param allAtomsInState
 * @param messageUri
 * @param connectionUri
 * @param atomUri
 * @param retracted
 * @returns {*}
 */
export function markMessageAsRetracted(
  allAtomsInState,
  messageUri,
  connectionUri,
  atomUri,
  retracted
) {
  let atom = get(allAtomsInState, atomUri);
  let connection = getIn(atom, ["connections", connectionUri]);
  let messages = connectionUtils.getMessages(connection);
  let message = get(messages, messageUri);

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
    return allAtomsInState;
  }
  const proposedToCancelReferences = messageUtils.getProposesToCancelReferences(
    message
  );

  if (proposedToCancelReferences) {
    proposedToCancelReferences.forEach(proposedToCancelRef => {
      allAtomsInState = markMessageAsCancellationPending(
        allAtomsInState,
        proposedToCancelRef,
        connectionUri,
        atomUri,
        false
      );
    });
  }

  const proposesReferences = messageUtils.getProposesReferences(message);

  if (proposesReferences) {
    proposesReferences.forEach(proposesRef => {
      allAtomsInState = markMessageAsProposed(
        allAtomsInState,
        proposesRef,
        connectionUri,
        atomUri,
        false
      );
    });
  }

  const claimsReferences = messageUtils.getClaimsReferences(message);

  if (claimsReferences) {
    claimsReferences.forEach(claimsRef => {
      allAtomsInState = markMessageAsClaimed(
        allAtomsInState,
        claimsRef,
        connectionUri,
        atomUri,
        false
      );
    });
  }

  allAtomsInState = markMessageAsRead(
    allAtomsInState,
    messageUri,
    connectionUri,
    atomUri,
    false
  );

  allAtomsInState = markMessageAsCollapsed(
    allAtomsInState,
    messageUri,
    connectionUri,
    atomUri,
    retracted
  );

  const retractedMessageUris = getIn(connection, [
    "agreementData",
    "retractedMessageUris",
  ]);

  return allAtomsInState.setIn(
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
 * @param allAtomsInState
 * @param messageUri
 * @param connectionUri
 * @param atomUri
 * @param claimed
 * @returns {*}
 */
export function markMessageAsClaimed(
  allAtomsInState,
  messageUri,
  connectionUri,
  atomUri,
  claimed
) {
  let atom = get(allAtomsInState, atomUri);
  let connection = getIn(atom, ["connections", connectionUri]);

  if (!connection) {
    console.error(
      "No connection with connectionUri: <",
      connectionUri,
      "> found within atomUri: <",
      atomUri,
      ">"
    );
    return allAtomsInState;
  }

  allAtomsInState = markMessageAsCollapsed(
    allAtomsInState,
    messageUri,
    connectionUri,
    atomUri,
    claimed
  );

  allAtomsInState = markMessageAsRead(
    allAtomsInState,
    messageUri,
    connectionUri,
    atomUri,
    false
  );

  const claimedMessageUris = getIn(connection, [
    "agreementData",
    "claimedMessageUris",
  ]);

  return allAtomsInState.setIn(
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
 * @param allAtomsInState
 * @param messageUri
 * @param connectionUri
 * @param atomUri
 * @param isAgreedOn
 * @returns {*}
 */
export function markMessageAsAgreed(
  allAtomsInState,
  messageUri,
  connectionUri,
  atomUri,
  isAgreedOn
) {
  let atom = get(allAtomsInState, atomUri);
  let connection = getIn(atom, ["connections", connectionUri]);

  if (!connection) {
    console.error(
      "No connection with connectionUri: <",
      connectionUri,
      "> found within atomUri: <",
      atomUri,
      ">"
    );
    return allAtomsInState;
  }

  allAtomsInState = markMessageAsRead(
    allAtomsInState,
    messageUri,
    connectionUri,
    atomUri,
    false
  );

  allAtomsInState = markMessageAsCollapsed(
    allAtomsInState,
    messageUri,
    connectionUri,
    atomUri,
    isAgreedOn
  );

  const agreedMessageUris = getIn(connection, [
    "agreementData",
    "agreedMessageUris",
  ]);

  return allAtomsInState.setIn(
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
 * @param allAtomsInState
 * @param messageUri
 * @param connectionUri
 * @param atomUri
 * @param proposed
 * @returns {*}
 */
export function markMessageAsProposed(
  allAtomsInState,
  messageUri,
  connectionUri,
  atomUri,
  proposed
) {
  let atom = get(allAtomsInState, atomUri);
  let connection = getIn(atom, ["connections", connectionUri]);

  if (!connection) {
    console.error(
      "No connection with connectionUri: <",
      connectionUri,
      "> found within atomUri: <",
      atomUri,
      ">"
    );
    return allAtomsInState;
  }

  allAtomsInState = markMessageAsRead(
    allAtomsInState,
    messageUri,
    connectionUri,
    atomUri,
    false
  );

  allAtomsInState = markMessageAsCollapsed(
    allAtomsInState,
    messageUri,
    connectionUri,
    atomUri,
    proposed
  );

  //if !proposed remove from set because message was retracted/rejected
  const proposedMessageUris = getIn(connection, [
    "agreementData",
    "proposedMessageUris",
  ]);

  return allAtomsInState.setIn(
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
  allAtomsInState,
  messageUri,
  connectionUri,
  atomUri,
  accepted
) {
  let atom = get(allAtomsInState, atomUri);
  let connection = getIn(atom, ["connections", connectionUri]);
  let messages = connectionUtils.getMessages(connection);
  let message = get(messages, messageUri);

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
    return allAtomsInState;
  }

  const proposedToCancelReferences = messageUtils.getProposesToCancelReferences(
    message
  );

  if (proposedToCancelReferences) {
    proposedToCancelReferences.forEach(proposedToCancelRef => {
      allAtomsInState = markMessageAsCancelled(
        allAtomsInState,
        proposedToCancelRef,
        connectionUri,
        atomUri,
        true
      );
    });
  }

  // remove from cancelled and cancellationPending sets because of not pending anymore
  if (accepted) {
    allAtomsInState = markMessageAsCancellationPending(
      allAtomsInState,
      messageUri,
      connectionUri,
      atomUri,
      false
    );
  }

  allAtomsInState = markMessageAsRead(
    allAtomsInState,
    messageUri,
    connectionUri,
    atomUri,
    false
  );

  const agreementUris = getIn(connection, ["agreementData", "agreementUris"]);

  return allAtomsInState.setIn(
    [atomUri, "connections", connectionUri, "agreementData", "agreementUris"],
    (agreementUris && agreementUris.add(messageUri)) ||
      Immutable.Set(messageUri)
  );
}

export function markMessageAsCancelled(
  allAtomsInState,
  messageUri,
  connectionUri,
  atomUri,
  cancelled
) {
  let atom = get(allAtomsInState, atomUri);
  let connection = getIn(atom, ["connections", connectionUri]);

  if (!connection) {
    console.error(
      "No connection with connectionUri: <",
      connectionUri,
      "> found within atomUri: <",
      atomUri,
      ">"
    );
    return allAtomsInState;
  }

  //if cancelled remove from accepted/agreementUris and cancellationPending sets
  if (cancelled) {
    const agreementUris = getIn(connection, ["agreementData", "agreementUris"]);

    allAtomsInState = allAtomsInState.setIn(
      [atomUri, "connections", connectionUri, "agreementData", "agreementUris"],
      (agreementUris && agreementUris.filter(uri => uri != messageUri)) ||
        Immutable.Set()
    );

    const cancellationPendingAgreementUris = getIn(connection, [
      "agreementData",
      "cancellationPendingAgreementUris",
    ]);

    allAtomsInState = allAtomsInState.setIn(
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

  allAtomsInState = markMessageAsRead(
    allAtomsInState,
    messageUri,
    connectionUri,
    atomUri,
    false
  );

  const cancelledAgreementUris = getIn(connection, [
    "agreementData",
    "cancelledAgreementUris",
  ]);
  return allAtomsInState.setIn(
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
  allAtomsInState,
  messageUri,
  connectionUri,
  atomUri,
  cancellationPending
) {
  let atom = get(allAtomsInState, atomUri);
  let connection = getIn(atom, ["connections", connectionUri]);

  if (!connection) {
    console.error(
      "No connection with connectionUri: <",
      connectionUri,
      "> found within atomUri: <",
      atomUri,
      ">"
    );
    return allAtomsInState;
  }

  allAtomsInState = markMessageAsRead(
    allAtomsInState,
    messageUri,
    connectionUri,
    atomUri,
    false
  );

  const cancellationPendingUris = getIn(connection, [
    "agreementData",
    "cancellationPendingAgreementUris",
  ]);

  // if !cancellationPending remove from set because message was retracted/rejected
  return allAtomsInState.setIn(
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
 * @param allAtomsInState
 * @param messageUri
 * @param connectionUri
 * @param atomUri
 * @returns {*}
 *
 */
export function updateMessageStatus(
  allAtomsInState,
  messageUri,
  connectionUri,
  atomUri
) {
  let atom = get(allAtomsInState, atomUri);
  let connection = getIn(atom, ["connections", connectionUri]);
  let messages = connectionUtils.getMessages(connection);
  let message = get(messages, messageUri);

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
    return allAtomsInState;
  }

  //Check if there is any "positive" messageStatus, we assume that we do not want to display this message "fully"
  const agreementData = get(connection, "agreementData");

  const isProposed = !!getIn(agreementData, [
    "proposedMessageUris",
    messageUri,
  ]);
  const isClaimed = !!getIn(agreementData, ["claimedMessageUris", messageUri]);
  const isAgreed = !!getIn(agreementData, ["agreedMessageUris", messageUri]);
  const isRejected = !!getIn(agreementData, [
    "rejectedMessageUris",
    messageUri,
  ]);
  const isRetracted = !!getIn(agreementData, [
    "retractedMessageUris",
    messageUri,
  ]);

  const hasCollapsedMessageState =
    isProposed || isClaimed || isAgreed || isRejected || isRetracted;

  return markMessageAsCollapsed(
    allAtomsInState,
    messageUri,
    connectionUri,
    atomUri,
    hasCollapsedMessageState
  );
}

/**
 * Returns true if socket is a ChatSocket and targetSocket is a GroupSocket
 * @param allAtoms all atoms of the allAtomsInState
 * @param connection to check sockettypes of
 * @returns {boolean}
 */
function isChatToGroupConnection(allAtomsInState, connection) {
  return (
    getSenderSocketType(allAtomsInState, connection) ===
      vocab.CHAT.ChatSocketCompacted &&
    getTargetSocketType(allAtomsInState, connection) ===
      vocab.GROUP.GroupSocketCompacted
  );
}

/**
 * Default message Sorting -> so we can already store a sorted map of messages in the atom
 * @param message
 * @returns {any}
 */
export function sortByMessageTimeStamp(message) {
  const messageDate = get(message, "date");
  if (!messageDate) {
    console.warn("messageDate for message is undefinded: ", message);
  }
  return messageDate && messageDate.getTime();
}
