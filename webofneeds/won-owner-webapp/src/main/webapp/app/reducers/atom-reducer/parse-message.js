import Immutable from "immutable";
import { isValidNumber, msStringToDate } from "../../utils.js";
import * as useCaseUtils from "../../usecase-utils.js";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";

/*
  "alreadyProcessed" flag sets the sentOwn/Remote flags to true
   "forwardMessage" flag is used to set an originatorUri (uri of the atom that the forwardedMessage was sent from)
   and a flag to indicate that the message should not be displayed in the chat as it is used purely used for reference
   purposes
*/
export function parseMessage(
  wonMessage,
  alreadyProcessed = false,
  forwardMessage = false
) {
  const injectInto = wonMessage.getInjectIntoConnectionUris();
  const forwardedMessages = wonMessage.getForwardedMessageUris();

  const proposedMessages = wonMessage.getProposedMessageUris();
  const proposedToCancelMessages = wonMessage.getProposedToCancelMessageUris();
  const acceptsMessages = wonMessage.getAcceptsMessageUris();
  const rejectsMessages = wonMessage.getRejectsMessageUris();
  const retractsMessages = wonMessage.getRetractsMessageUris();
  const claimsMessages = wonMessage.getClaimsMessageUris();

  const matchScoreFloat = parseFloat(wonMessage.getHintScore());

  const detailsToParse = useCaseUtils.getAllDetails();

  let parsedMessage = {
    data: {
      uri: wonMessage.getMessageUri(),
      forwardMessage: forwardMessage,
      originatorUri: forwardMessage
        ? generalSelectors.getAtomUriBySocketUri(wonMessage.getSenderSocket())
        : undefined,
      content: {
        text: wonMessage.getTextMessage(),
        matchScore:
          isValidNumber(matchScoreFloat) && isFinite(matchScoreFloat)
            ? matchScoreFloat
            : undefined,
      },
      injectInto: injectInto,
      references: {
        forwards: forwardedMessages,
        claims: claimsMessages,
        proposes: proposedMessages,
        proposesToCancel: proposedToCancelMessages,
        accepts: acceptsMessages,
        rejects: rejectsMessages,
        retracts: retractsMessages,
      },
      hasReferences: false, //will be determined by the hasReferences function
      hasContent: false, //will be determined by the hasContent function
      isParsable: false, //will be determined by the clause (hasReferences || hasContent) function
      date: msStringToDate(wonMessage.getTimestamp()),
      outgoingMessage: undefined,
      systemMessage:
        !wonMessage.isFromOwner() &&
        !wonMessage.getSenderAtom() &&
        wonMessage.getSenderNode(),
      senderUri: generalSelectors.getAtomUriBySocketUri(
        wonMessage.getSenderSocket()
      ),
      messageType: wonMessage.getMessageType(),
      messageStatus: {
        isProposed: false,
        isClaimed: false,
        isRetracted: false,
        isRejected: false,
        isAccepted: false,
        isAgreed: false,
        isCancellationPending: false,
        isCancelled: false,
      },
      viewState: {
        isSelected: false,
        isCollapsed: false,
        expandedReferences: {
          forwards: true,
          claims: true,
          proposes: true,
          proposesToCancel: false,
          accepts: false,
          rejects: false,
          retracts: false,
        },
      },
      isMessageStatusUpToDate: false,
      //Receive Status Flags
      unread: false, //default is unread, but the real status is determined for non outgoing messages in the reducer
      //Send Status Flags
      isReceivedByOwn: alreadyProcessed || !wonMessage.isFromOwner(), //this will be set properly in the reducer
      isReceivedByRemote: alreadyProcessed || !wonMessage.isFromOwner(), //this will be set properly in the reducer
      failedToSend: false,
    },
  };

  if (
    wonMessage.getCompactFramedMessageContent() &&
    wonMessage.getCompactRawMessage()
  ) {
    parsedMessage.data.content = generateContent(
      Immutable.fromJS(wonMessage.getCompactFramedMessageContent()),
      Immutable.fromJS(wonMessage.getCompactRawMessage()),
      detailsToParse,
      parsedMessage.data.content
    );
  }

  parsedMessage.data.hasContent = hasContent(parsedMessage.data.content);
  parsedMessage.data.hasReferences = hasReferences(
    parsedMessage.data.references
  );
  parsedMessage.data.isParsable =
    parsedMessage.data.hasContent || parsedMessage.data.hasReferences;

  if (
    !parsedMessage.data.uri ||
    !parsedMessage.data.date ||
    !parsedMessage.data.senderUri
  ) {
    console.error(
      "Cant parse chat-message, data is an invalid message-object: ",
      wonMessage
    );
    return undefined;
  } else {
    return Immutable.fromJS(parsedMessage);
  }
}
function hasContent(content) {
  for (let prop in content) {
    if (
      prop !== "type" &&
      content[prop] !== undefined &&
      content[prop] != null
    ) {
      return true;
    }
  }
  return false;
}

function hasReferences(refContent) {
  for (let prop in refContent) {
    if (refContent[prop] !== undefined && refContent[prop] != null) {
      return true;
    }
  }
  return false;
}

export function getHumanReadableStringFromMessage(message) {
  const text = message && message.getIn(["content", "text"]);
  return text;
}

/**
 * Tries to extract all the detailsToParse from the given contentJsonLd
 * uses the parseFromRdf function defined in the detail to extract the content
 * uses the detail identifier as the key of the contentDetail that is to be added
 * @param contentJsonLd
 * @param detailsToParse
 * @returns {{title: *, type: *}}
 */
function generateContent(
  contentJsonLd,
  rawMessageContentJsonLd,
  detailsToParse,
  content
) {
  if (detailsToParse) {
    for (const detailKey in detailsToParse) {
      const detailToParse = detailsToParse[detailKey];
      const detailIdentifier = detailToParse && detailToParse.identifier;
      const detailValue =
        detailToParse &&
        detailToParse.parseFromRDF(contentJsonLd, rawMessageContentJsonLd);

      if (detailIdentifier && detailValue) {
        content[detailIdentifier] = detailValue;
      }
    }
  }

  return content;
}
