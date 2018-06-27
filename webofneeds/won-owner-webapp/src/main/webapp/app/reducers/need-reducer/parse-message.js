import Immutable from "immutable";
import { msStringToDate, trigPrefixesAndBody } from "../../utils.js";
import { isUriRead } from "../../won-localstorage.js";

export function parseMessage(wonMessage, alreadyProcessed = false) {
  //seperating off header/@prefix-statements, so they can be folded in
  const { trigPrefixes, trigBody } = trigPrefixesAndBody(
    wonMessage.contentGraphTrig
  );

  let clauses = undefined;

  if (
    wonMessage.isProposeMessage() ||
    wonMessage.isAcceptMessage() ||
    wonMessage.isProposeToCancel()
  ) {
    if (wonMessage.isProposeMessage()) {
      clauses = wonMessage.getProposedMessages();
    } else if (wonMessage.isAcceptMessage()) {
      clauses = wonMessage.getAcceptedMessages();
    } else {
      clauses = wonMessage.getProposedToCancelMessages();
    }
  }

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
        proposes: wonMessage.getProposedMessages(),
        proposesToCancel: wonMessage.getProposedToCancelMessages(),
        accepts: wonMessage.getAcceptedMessages(),
        rejects: wonMessage.getRejectsMessages(),
        retracts: wonMessage.getRetractMessages(),
      },
      isParsable: false, //will be determined by the hasParsableContent function
      contentGraphs: wonMessage.getContentGraphs(),
      date: msStringToDate(wonMessage.getTimestamp()),
      outgoingMessage: wonMessage.isFromOwner(),
      unread:
        !wonMessage.isFromOwner() && !isUriRead(wonMessage.getMessageUri()),
      messageType: wonMessage.getMessageType(),
      clauses: clauses,
      isReceivedByOwn: alreadyProcessed || !wonMessage.isFromOwner(), //if the message is not from the owner we know it has been received anyway
      isReceivedByRemote: alreadyProcessed || !wonMessage.isFromOwner(), //if the message is not from the owner we know it has been received anyway
      failedToSend: false,
      isProposeMessage: wonMessage.isProposeMessage(),
      isAcceptMessage: wonMessage.isAcceptMessage(),
      isProposeToCancel: wonMessage.isProposeToCancel(),
      isRejectMessage: wonMessage.isRejectMessage(),
      isRetractMessage: wonMessage.isRetractMessage(),
      isRelevant: true,
      contentGraphTrig: {
        prefixes: trigPrefixes,
        body: trigBody,
      },
      contentGraphTrigRaw: wonMessage.contentGraphTrig,
      contentGraphTrigError: wonMessage.contentGraphTrigError,
    },
  };

  if (wonMessage.isFromOwner()) {
    parsedMessage.belongsToUri = wonMessage.getSender();
  } else {
    parsedMessage.belongsToUri = wonMessage.getReceiver();
  }

  parsedMessage.data.isParsable = hasParsableContent(
    parsedMessage.data.content
  );

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

function hasParsableContent(content) {
  for (let prop in content) {
    if (content[prop] !== undefined && content[prop] != null) {
      return true;
    }
  }
  return false;
}
