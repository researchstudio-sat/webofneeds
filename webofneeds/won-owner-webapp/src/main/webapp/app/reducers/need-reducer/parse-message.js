import Immutable from "immutable";
import {
  msStringToDate,
  trigPrefixesAndBody,
  isValidNumber,
} from "../../utils.js";
import { isUriRead } from "../../won-localstorage.js";
import { getAllDetails } from "../../won-utils.js";

export function parseMessage(wonMessage, alreadyProcessed = false) {
  //seperating off header/@prefix-statements, so they can be folded in
  const { trigPrefixes, trigBody } = trigPrefixesAndBody(
    wonMessage.contentGraphTrig
  );

  const injectInto = wonMessage.getInjectIntoConnection();

  const proposedMessages = wonMessage.getProposedMessages();
  const proposedToCancelMessages = wonMessage.getProposedToCancelMessages();
  const acceptedMessages = wonMessage.getAcceptedMessages();
  const rejectedMessages = wonMessage.getRejectsMessages();
  const retractedMessages = wonMessage.getRetractMessages();
  //const forwardedMessages = wonMessage.getForwardedMessage();

  const matchScoreFloat = parseFloat(wonMessage.getMatchScore());

  const detailsToParse = getAllDetails();

  let parsedMessage = {
    belongsToUri: undefined,
    data: {
      uri: wonMessage.getMessageUri(),
      remoteUri: !wonMessage.isFromOwner() //THIS HAS TO STAY UNDEFINED If the message is not a received message
        ? wonMessage.getRemoteMessageUri()
        : undefined,
      content: {
        text: wonMessage.getTextMessage(),
        matchScore:
          isValidNumber(matchScoreFloat) && isFinite(matchScoreFloat)
            ? matchScoreFloat
            : undefined,
      },
      injectInto:
        !injectInto || Array.isArray(injectInto) ? injectInto : [injectInto],
      references: {
        proposes:
          !proposedMessages || Array.isArray(proposedMessages)
            ? proposedMessages
            : [proposedMessages],
        proposesToCancel:
          !proposedToCancelMessages || Array.isArray(proposedToCancelMessages)
            ? proposedToCancelMessages
            : [proposedToCancelMessages],
        accepts:
          !acceptedMessages || Array.isArray(acceptedMessages)
            ? acceptedMessages
            : [acceptedMessages],
        rejects:
          !rejectedMessages || Array.isArray(rejectedMessages)
            ? rejectedMessages
            : [rejectedMessages],
        retracts:
          !retractedMessages || Array.isArray(retractedMessages)
            ? retractedMessages
            : [retractedMessages],
      },
      hasReferences: false, //will be determined by the hasReferences function
      hasContent: false, //will be determined by the hasContent function
      isParsable: false, //will be determined by the clause (hasReferences || hasContent) function
      date: msStringToDate(wonMessage.getTimestamp()),
      outgoingMessage: wonMessage.isOutgoingMessage(),
      systemMessage:
        !wonMessage.isFromOwner() &&
        !wonMessage.getSenderNeed() &&
        wonMessage.getSenderNode(),
      senderUri: wonMessage.getSenderNeed() || wonMessage.getSenderNode(),
      messageType: wonMessage.getMessageType(),
      messageStatus: {
        isRetracted: false,
        isRejected: false,
        isAccepted: false,
        isCancellationPending: false,
        isCancelled: false,
      },
      isMessageStatusUpToDate: false,
      contentGraphTrig: {
        prefixes: trigPrefixes,
        body: trigBody,
      },
      contentGraphs: wonMessage.getContentGraphs(),
      contentGraphTrigRaw: wonMessage.contentGraphTrig,
      contentGraphTrigError: wonMessage.contentGraphTrigError,
      //Receive Status Flags
      unread:
        !wonMessage.isFromOwner() && !isUriRead(wonMessage.getMessageUri()),
      //Send Status Flags
      isReceivedByOwn: alreadyProcessed || !wonMessage.isFromOwner(), //if the message is not from the owner we know it has been received anyway
      isReceivedByRemote: alreadyProcessed || !wonMessage.isFromOwner(), //if the message is not from the owner we know it has been received anyway
      isSelected: false,
      failedToSend: false,
    },
  };

  if (wonMessage.isFromOwner()) {
    parsedMessage.belongsToUri = wonMessage.getSender();
  } else if (wonMessage.isFromSystem()) {
    parsedMessage.belongsToUri = wonMessage.getSender();
  } else {
    parsedMessage.belongsToUri = wonMessage.getReceiver();
  }

  if (wonMessage.getCompactFramedMessageContent()) {
    parsedMessage.data.content = generateContent(
      Immutable.fromJS(wonMessage.getCompactFramedMessageContent()),
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
    !parsedMessage.belongsToUri ||
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
    if (content[prop] !== undefined && content[prop] != null) {
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
function generateContent(contentJsonLd, detailsToParse, content) {
  if (detailsToParse) {
    for (const detailKey in detailsToParse) {
      const detailToParse = detailsToParse[detailKey];
      const detailIdentifier = detailToParse && detailToParse.identifier;
      const detailValue =
        detailToParse && detailToParse.parseFromRDF(contentJsonLd);

      if (detailIdentifier && detailValue) {
        content[detailIdentifier] = detailValue;
      }
    }
  }

  return content;
}
