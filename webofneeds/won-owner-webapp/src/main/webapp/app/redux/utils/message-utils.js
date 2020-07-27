/**
 * Created by fsuda on 05.11.2018.
 */

import vocab from "../../service/vocab.js";
import { get, getIn, getUri } from "../../utils.js";
import { getAgreementData } from "./connection-utils.js";

export function isReceivedByOwn(msg) {
  return !!msg && get(msg, "isReceivedByOwn");
}

export function isReceivedByRemote(msg) {
  return !!msg && get(msg, "isReceivedByRemote");
}

export function hasFailedToSend(msg) {
  return !!msg && get(msg, "failedToSend");
}

/**
 * Determines if a given message can be Proposed
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageProposable(con, msg) {
  //TODO: should a message be proposable if it was already proposed? or even accepted? and what if the ref are only forwardedMessages?
  return (
    hasContent(msg) &&
    !isConnectMessage(msg) &&
    !isChangeNotificationMessage(msg) &&
    !hasReferences(msg) &&
    !isMessageRetracted(con, msg) &&
    !isMessageRejected(con, msg)
  );
}

/**
 * Determines if a given message can be Claimed
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageClaimable(con, msg) {
  //TODO: should a message be claimable if it was already claimed or proposed or even accepted? what if the ref are only forwardedMessages?
  return (
    hasContent(msg) &&
    !isConnectMessage(msg) &&
    !isChangeNotificationMessage(msg) &&
    !hasReferences(msg) &&
    !isMessageRetracted(con, msg) &&
    !isMessageRejected(con, msg)
  );
}

/**
 * Determines if a given message can be agreed on
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageAgreeable(con, msg) {
  return (
    hasContent(msg) &&
    (hasClaimsReferences(con, msg) ||
      hasProposesReferences(con, msg) ||
      hasProposesToCancelReferences(con, msg)) &&
    !isMessageAccepted(con, msg) &&
    !isMessageCancelled(con, msg) &&
    !isMessageCancellationPending(con, msg) &&
    !isMessageRetracted(con, msg) &&
    !isMessageRejected(con, msg)
  );
}

/**
 * Determines if a given message can be Canceled
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageCancelable(con, msg) {
  return (
    (hasClaimsReferences(msg) ||
      hasProposesReferences(msg) ||
      hasProposesToCancelReferences(msg)) &&
    isMessageAccepted(con, msg) &&
    !(hasProposesToCancelReferences(msg) && isMessageAccepted(con, msg)) &&
    !isMessageCancelled(con, msg) &&
    !isMessageCancellationPending(con, msg)
  );
}

export function isOutgoingMessage(msg) {
  return get(msg, "outgoingMessage");
}

export function isIncomingMessage(msg) {
  return !isOutgoingMessage(msg);
}

/**
 * Determines if a given message can be Accepted
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageAcceptable(con, msg) {
  return (
    (hasClaimsReferences(msg) ||
      hasProposesReferences(msg) ||
      hasProposesToCancelReferences(msg)) &&
    isIncomingMessage(msg) &&
    !isMessageAccepted(con, msg) &&
    !isMessageAgreedOn(con, msg) &&
    !isMessageCancelled(con, msg) &&
    !isMessageCancellationPending(con, msg) &&
    !isMessageRetracted(con, msg) &&
    !isMessageRejected(con, msg)
  );
}

/**
 * Determines if a given message can be Retracted
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageRetractable(con, msg) {
  return (
    isOutgoingMessage(msg) &&
    !isMessageAccepted(con, msg) &&
    !isMessageAgreedOn(con, msg) &&
    !isMessageCancelled(con, msg) &&
    !isMessageCancellationPending(con, msg) &&
    !isMessageRetracted(con, msg) &&
    !isMessageRejected(con, msg)
  );
}

/**
 * Determines if a given message can be Rejected
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageRejectable(con, msg) {
  return (
    (hasClaimsReferences(msg) ||
      hasProposesReferences(msg) ||
      hasProposesToCancelReferences(msg)) &&
    isIncomingMessage(msg) &&
    !isMessageAccepted(con, msg) &&
    !isMessageAgreedOn(con, msg) &&
    !isMessageCancelled(con, msg) &&
    !isMessageCancellationPending(con, msg) &&
    !isMessageRetracted(con, msg) &&
    !isMessageRejected(con, msg)
  );
}

export function getOriginatorUri(msg) {
  return get(msg, "originatorUri");
}

export function getForwardMessage(msg) {
  return get(msg, "forwardMessage");
}

export function hasReferences(msg) {
  return get(msg, "hasReferences");
}

export function hasContent(msg) {
  return get(msg, "hasContent");
}

export function getContent(msg) {
  return get(msg, "content");
}

export function hasText(msg) {
  return !!getText(msg);
}

export function getText(msg) {
  return get(getContent(msg), "text");
}

export function isSystemMessage(msg) {
  return get(msg, "systemMessage");
}

export function getReferences(msg) {
  return get(msg, "references");
}

export function getForwardsReferences(msg) {
  const references = getReferences(msg);

  return get(references, "forwards");
}

export function hasForwardsReferences(msg) {
  const forwards = getForwardsReferences(msg);
  return forwards && forwards.size > 0;
}

export function getProposesReferences(msg) {
  const references = getReferences(msg);

  return get(references, "proposes");
}

/**
 * Determines if a given message has proposes references
 * @param msg
 * @returns {*|boolean}
 */
export function hasProposesReferences(msg) {
  const proposes = getProposesReferences(msg);
  return proposes && proposes.size > 0;
}

export function getClaimsReferences(msg) {
  const references = getReferences(msg);

  return get(references, "claims");
}

/**
 * Determines if a given message has claims references
 * @param msg
 * @returns {*|boolean}
 */
export function hasClaimsReferences(msg) {
  const claims = getClaimsReferences(msg);
  return claims && claims.size > 0;
}

export function getProposesToCancelReferences(msg) {
  const references = getReferences(msg);

  return get(references, "proposesToCancel");
}
/**
 * Determines if a given message has proposesToCancel references
 * @param msg
 * @returns {*|boolean}
 */
export function hasProposesToCancelReferences(msg) {
  const proposesToCancel = getProposesToCancelReferences(msg);
  return proposesToCancel && proposesToCancel.size > 0;
}

/**
 * Determines if a given message is in the state proposed
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageProposed(con, msg) {
  const agreementData = getAgreementData(con);
  const proposedMessageUris = get(agreementData, "proposedMessageUris");
  return proposedMessageUris && proposedMessageUris.has(getUri(msg));
}

/**
 * Determines if a given message is in the state claimed
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageClaimed(con, msg) {
  const agreementData = getAgreementData(con);
  const claimedMessageUris = get(agreementData, "claimedMessageUris");
  return claimedMessageUris && claimedMessageUris.has(getUri(msg));
}

/**
 * Determines if a given message is in the state rejected
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageRejected(con, msg) {
  const agreementData = getAgreementData(con);
  const rejectedMessageUris = get(agreementData, "rejectedMessageUris");
  return rejectedMessageUris && rejectedMessageUris.has(getUri(msg));
}

/**
 * Determines if a given message is in the state accepted
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageAccepted(con, msg) {
  const agreementData = getAgreementData(con);
  const acceptedMessageUris = get(agreementData, "agreementUris");

  const acceptedCancellationProposalUris = get(
    agreementData,
    "acceptedCancellationProposalUris"
  );

  return (
    (acceptedMessageUris && acceptedMessageUris.has(getUri(msg))) ||
    (acceptedCancellationProposalUris &&
      acceptedCancellationProposalUris.has(getUri(msg)))
  );
}

/**
 * Determines if a given message is in the state agreed
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageAgreedOn(con, msg) {
  const agreementData = getAgreementData(con);
  const agreedMessageUris = get(agreementData, "agreedMessageUris");
  return agreedMessageUris && agreedMessageUris.has(getUri(msg));
}

/**
 * Determines if a given message is in the state retracted
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageRetracted(con, msg) {
  const agreementData = getAgreementData(con);
  const retractedMessageUris = get(agreementData, "retractedMessageUris");
  return retractedMessageUris && retractedMessageUris.has(getUri(msg));
}

/**
 * Determines if a given message is in the state Cancelled
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageCancelled(con, msg) {
  const agreementData = getAgreementData(con);
  const cancelledMessageUris = get(agreementData, "cancelledAgreementUris");
  return cancelledMessageUris && cancelledMessageUris.has(getUri(msg));
}

/**
 * Determines if a given message is in the state CancellationPending
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageCancellationPending(con, msg) {
  const agreementData = getAgreementData(con);
  const cancellationPendingUris = get(
    agreementData,
    "cancellationPendingAgreementUris"
  );
  return cancellationPendingUris && cancellationPendingUris.has(getUri(msg));
}

/**
 * determines whether the given message is unread or not
 * @param msg
 * @returns {*}
 */
export function isMessageUnread(msg) {
  return !!get(msg, "unread");
}

export function getInjectInto(msg) {
  return get(msg, "injectInto");
}

/**
 * determines whether the given message is currently selected or not
 * @param msg
 * @returns {*|any}
 */
export function isMessageSelected(msg) {
  return getIn(msg, ["viewState", "isSelected"]);
}

export function isCollapsed(msg) {
  return getIn(msg, ["viewState", "isCollapsed"]);
}

export function getExpandedReferences(msg) {
  return getIn(msg, ["viewState", "expandedReferences"]);
}

export function showActions(msg) {
  return getIn(msg, ["viewState", "showActions"]);
}

/**
 * Determines if a given message is considered a proposal
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageProposal(con, msg) {
  return (
    (hasProposesToCancelReferences(msg) || hasProposesReferences(msg)) &&
    !(
      isMessageAccepted(con, msg) ||
      isMessageCancellationPending(con, msg) ||
      isMessageCancelled(con, msg) ||
      isMessageRejected(con, msg) ||
      isMessageRetracted(con, msg)
    )
  );
}

/**
 * Determines if a given message is considered a claim
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageClaim(con, msg) {
  return (
    hasClaimsReferences(msg) &&
    !(
      isMessageAccepted(con, msg) ||
      isMessageCancellationPending(con, msg) ||
      isMessageCancelled(con, msg) ||
      isMessageRejected(con, msg) ||
      isMessageRetracted(con, msg)
    )
  );
}

/**
 * Determines if a given message is an agreement
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageAgreement(con, msg) {
  return isMessageAccepted(con, msg) && !isMessageCancellationPending(con, msg);
}

export function getType(msg) {
  return get(msg, "messageType");
}

export function isAtomHintMessage(msg) {
  return getType(msg) === vocab.WONMSG.atomHintMessage;
}

export function isSocketHintMessage(msg) {
  return getType(msg) === vocab.WONMSG.socketHintMessage;
}

export function isConnectionMessage(msg) {
  return getType(msg) === vocab.WONMSG.connectionMessage;
}

export function isConnectMessage(msg) {
  return getType(msg) === vocab.WONMSG.connectMessage;
}

export function isChangeNotificationMessage(msg) {
  return getType(msg) === vocab.WONMSG.changeNotificationMessage;
}

export function isParsable(msg) {
  return get(msg, "isParsable");
}

export function getHumanReadableString(msg) {
  const content = getContent(msg);
  return get(content, "text") || "«Message does not have text»";
}

/**
 * (re)sorts the messages within the given messagesMap
 * @param messagesMap
 * @returns {sorted messagesMap)
 */
export function sortMessages(messagesMap) {
  return messagesMap.toOrderedMap().sortBy(sortByMessageTimeStamp);
}

export function getDate(msg) {
  return get(msg, "date");
}

/**
 * Default message Sorting -> so we can already store a sorted map of messages in the atom
 * @param msg
 * @returns {any}
 */
export function sortByMessageTimeStamp(msg) {
  const messageDate = getDate(msg);
  if (!messageDate) {
    console.warn("messageDate for message is undefinded: ", msg);
  }
  return messageDate && messageDate.getTime();
}
