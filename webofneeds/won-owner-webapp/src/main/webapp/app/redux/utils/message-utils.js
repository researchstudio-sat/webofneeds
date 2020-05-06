/**
 * Created by fsuda on 05.11.2018.
 */

import vocab from "../../service/vocab.js";
import { get } from "../../utils.js";

/**
 * Determines if a given message can be Proposed
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageProposable(con, msg) {
  //TODO: should a message be proposable if it was already proposed? or even accepted? and what if the ref are only forwardedMessages?
  return (
    msg &&
    msg.get("hasContent") &&
    msg.get("messageType") !== vocab.WONMSG.connectMessage &&
    msg.get("messageType") !== vocab.WONMSG.changeNotificationMessage &&
    !msg.get("hasReferences") &&
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
    msg &&
    msg.get("hasContent") &&
    msg.get("messageType") !== vocab.WONMSG.connectMessage &&
    msg.get("messageType") !== vocab.WONMSG.changeNotificationMessage &&
    !msg.get("hasReferences") &&
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
    msg &&
    msg.get("hasContent") &&
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
    msg &&
    (hasClaimsReferences(msg) ||
      hasProposesReferences(msg) ||
      hasProposesToCancelReferences(msg)) &&
    isMessageAccepted(con, msg) &&
    !(hasProposesToCancelReferences(msg) && isMessageAccepted(con, msg)) &&
    !isMessageCancelled(con, msg) &&
    !isMessageCancellationPending(con, msg)
  );
}

/**
 * Determines if a given message can be Accepted
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageAcceptable(con, msg) {
  return (
    msg &&
    (hasClaimsReferences(msg) ||
      hasProposesReferences(msg) ||
      hasProposesToCancelReferences(msg)) &&
    !msg.get("outgoingMessage") &&
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
    msg &&
    msg.get("outgoingMessage") &&
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
    msg &&
    (hasClaimsReferences(msg) ||
      hasProposesReferences(msg) ||
      hasProposesToCancelReferences(msg)) &&
    !msg.get("outgoingMessage") &&
    !isMessageAccepted(con, msg) &&
    !isMessageAgreedOn(con, msg) &&
    !isMessageCancelled(con, msg) &&
    !isMessageCancellationPending(con, msg) &&
    !isMessageRetracted(con, msg) &&
    !isMessageRejected(con, msg)
  );
}

/**
 * Determines if a given message has proposes references
 * @param msg
 * @returns {*|boolean}
 */
export function hasProposesReferences(msg) {
  const references = msg && msg.get("references");
  return (
    references &&
    references.get("proposes") &&
    references.get("proposes").size > 0
  );
}

/**
 * Determines if a given message has claims references
 * @param msg
 * @returns {*|boolean}
 */
export function hasClaimsReferences(msg) {
  const references = msg && msg.get("references");
  return (
    references && references.get("claims") && references.get("claims").size > 0
  );
}

/**
 * Determines if a given message has proposesToCancel references
 * @param msg
 * @returns {*|boolean}
 */
export function hasProposesToCancelReferences(msg) {
  const references = msg && msg.get("references");
  return (
    references &&
    references.get("proposesToCancel") &&
    references.get("proposesToCancel").size > 0
  );
}

/**
 * Determines if a given message is in the state proposed
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageProposed(con, msg) {
  const agreementData = con && con.get("agreementData");
  const proposedMessageUris =
    agreementData && agreementData.get("proposedMessageUris");
  return proposedMessageUris && proposedMessageUris.has(msg.get("uri"));
}

/**
 * Determines if a given message is in the state claimed
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageClaimed(con, msg) {
  const agreementData = con && con.get("agreementData");
  const claimedMessageUris =
    agreementData && agreementData.get("claimedMessageUris");
  return claimedMessageUris && claimedMessageUris.has(msg.get("uri"));
}

/**
 * Determines if a given message is in the state rejected
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageRejected(con, msg) {
  const agreementData = con && con.get("agreementData");
  const rejectedMessageUris =
    agreementData && agreementData.get("rejectedMessageUris");
  return rejectedMessageUris && rejectedMessageUris.has(msg.get("uri"));
}

/**
 * Determines if a given message is in the state accepted
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageAccepted(con, msg) {
  const agreementData = con && con.get("agreementData");
  const acceptedMessageUris =
    agreementData && agreementData.get("agreementUris");

  const acceptedCancellationProposalUris =
    agreementData && agreementData.get("acceptedCancellationProposalUris");

  return (
    (acceptedMessageUris && acceptedMessageUris.has(msg.get("uri"))) ||
    (acceptedCancellationProposalUris &&
      acceptedCancellationProposalUris.has(msg.get("uri")))
  );
}

/**
 * Determines if a given message is in the state agreed
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageAgreedOn(con, msg) {
  const agreementData = con && con.get("agreementData");
  const agreedMessageUris =
    agreementData && agreementData.get("agreedMessageUris");
  return agreedMessageUris && agreedMessageUris.has(msg.get("uri"));
}

/**
 * Determines if a given message is in the state retracted
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageRetracted(con, msg) {
  const agreementData = con && con.get("agreementData");
  const retractedMessageUris =
    agreementData && agreementData.get("retractedMessageUris");
  return retractedMessageUris && retractedMessageUris.has(msg.get("uri"));
}

/**
 * Determines if a given message is in the state Cancelled
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageCancelled(con, msg) {
  const agreementData = con && con.get("agreementData");
  const cancelledMessageUris =
    agreementData && agreementData.get("cancelledAgreementUris");
  return cancelledMessageUris && cancelledMessageUris.has(msg.get("uri"));
}

/**
 * Determines if a given message is in the state CancellationPending
 * @param con
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageCancellationPending(con, msg) {
  const agreementData = con && con.get("agreementData");
  const cancellationPendingUris =
    agreementData && agreementData.get("cancellationPendingAgreementUris");
  return cancellationPendingUris && cancellationPendingUris.has(msg.get("uri"));
}

/**
 * determines whether the given message is unread or not
 * @param msg
 * @returns {*}
 */
export function isMessageUnread(msg) {
  return msg && msg.get("unread");
}

/**
 * determines whether the given message is currently selected or not
 * @param msg
 * @returns {*|any}
 */
export function isMessageSelected(msg) {
  return msg && msg.getIn(["viewState", "isSelected"]);
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

export function isAtomHintMessage(msg) {
  return get(msg, "messageType") === vocab.WONMSG.atomHintMessage;
}

export function isSocketHintMessage(msg) {
  return get(msg, "messageType") === vocab.WONMSG.socketHintMessage;
}

export function isConnectionMessage(msg) {
  return get(msg, "messageType") === vocab.WONMSG.connectionMessage;
}

export function isChangeNotificationMessage(msg) {
  return get(msg, "messageType") === vocab.WONMSG.changeNotificationMessage;
}

export function isParsable(msg) {
  return get(msg, "isParsable");
}
