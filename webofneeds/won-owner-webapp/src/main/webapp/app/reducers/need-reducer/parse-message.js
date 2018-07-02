import Immutable from "immutable";
import { msStringToDate, trigPrefixesAndBody } from "../../utils.js";
import { isUriRead } from "../../won-localstorage.js";

export function parseMessage(wonMessage, alreadyProcessed = false) {
  //seperating off header/@prefix-statements, so they can be folded in
  const { trigPrefixes, trigBody } = trigPrefixesAndBody(
    wonMessage.contentGraphTrig
  );

  const proposedMessages = wonMessage.getProposedMessages();
  const proposedToCancelMessages = wonMessage.getProposedToCancelMessages();
  const acceptedMessages = wonMessage.getAcceptedMessages();
  const rejectedMessages = wonMessage.getRejectsMessages();
  const retractedMessages = wonMessage.getRetractMessages();

  let parsedMessage = {
    belongsToUri: undefined,
    data: {
      uri: wonMessage.getMessageUri(),
      remoteUri: !wonMessage.isFromOwner()
        ? wonMessage.getRemoteMessageUri()
        : undefined,
      content: {
        text: wonMessage.getTextMessage(),
        matchScore: wonMessage.getMatchScore(),
      },
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
      outgoingMessage: wonMessage.isFromOwner(),
      messageType: wonMessage.getMessageType(),
      messageStatus: {
        isRetracted: false,
        isRejected: false,
        isAccepted: false,
        isCancellationPending: false,
        isCancelled: false,
      },
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
      failedToSend: false,
    },
  };

  if (wonMessage.isFromOwner()) {
    parsedMessage.belongsToUri = wonMessage.getSender();
  } else {
    parsedMessage.belongsToUri = wonMessage.getReceiver();
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
    !parsedMessage.data.date
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
