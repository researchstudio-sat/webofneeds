import Immutable from "immutable";
import { msStringToDate } from "../../utils.js";
import { isUriRead } from "../../won-localstorage.js";

export function parseMessage(wonMessage, alreadyProcessed = false) {
  const contentGraphTrigLines = (wonMessage.contentGraphTrig || "").split("\n");

  //seperating off header/@prefix-statements, so they can be folded in
  const contentGraphTrigPrefixes = contentGraphTrigLines
    .filter(line => line.startsWith("@prefix"))
    .join("\n");

  const contentGraphTrigBody = contentGraphTrigLines
    .filter(line => !line.startsWith("@prefix"))
    .map(line =>
      line
        // add some extra white-space between statements, so they stay readable even when they wrap.
        .replace(/\.$/, ".\n")
        .replace(/;$/, ";\n")
        .replace(/\{$/, "{\n")
        .replace(/^\}$/, "\n}")
    )
    .join("\n")
    .trim();

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
      text: wonMessage.getTextMessage(),
      contentGraphs: wonMessage.getContentGraphs(),
      date: msStringToDate(wonMessage.getTimestamp()),
      outgoingMessage: wonMessage.isFromOwner(),
      unread:
        !wonMessage.isFromOwner() && !isUriRead(wonMessage.getMessageUri()),
      connectMessage: wonMessage.isConnectMessage(),
      //TODO: add all different types
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
        prefixes: contentGraphTrigPrefixes,
        body: contentGraphTrigBody,
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
