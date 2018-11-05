/**
 * Created by fsuda on 05.11.2018.
 */

import won from "./won-es6.js";

/**
 * Determines if a given message can be Proposed
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageProposable(msg) {
  //TODO: should a message be proposable if it was already proposed? or even accepted? and what if the ref are only forwardedMessages?
  return (
    msg &&
    msg.get("hasContent") &&
    msg.get("messageType") !== won.WONMSG.connectMessage &&
    !msg.get("hasReferences")
  );
}

/**
 * Determines if a given message can be Claimed
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageClaimable(msg) {
  //TODO: should a message be claimable if it was already claimed or proposed or even accepted? what if the ref are only forwardedMessages?
  return (
    msg &&
    msg.get("hasContent") &&
    msg.get("messageType") !== won.WONMSG.connectMessage &&
    !msg.get("hasReferences")
  );
}

/**
 * Determines if a given message can be Canceled
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageCancelable(msg) {
  return (
    msg &&
    (hasClaimsReferences(msg) ||
      hasProposesReferences(msg) ||
      hasProposesToCancelReferences(msg)) &&
    isMessageAccepted(msg) &&
    !isMessageCancelled(msg) &&
    !isMessageCancellationPending(msg)
  );
}

/**
 * Determines if a given message can be Retracted
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageRetractable(msg) {
  return (
    msg &&
    msg.get("outgoingMessage") &&
    !isMessageAccepted(msg) &&
    !isMessageCancelled(msg) &&
    !isMessageCancellationPending(msg) &&
    !isMessageRetracted(msg) &&
    !isMessageRejected(msg)
  );
}

/**
 * Determines if a given message can be Accepted
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageAcceptable(msg) {
  return (
    msg &&
    (hasClaimsReferences(msg) ||
      hasProposesReferences(msg) ||
      hasProposesToCancelReferences(msg)) &&
    !msg.get("outgoingMessage") &&
    !isMessageAccepted(msg) &&
    !isMessageCancelled(msg) &&
    !isMessageCancellationPending(msg) &&
    !isMessageRetracted(msg) &&
    !isMessageRejected(msg)
  );
}

/**
 * Determines if a given message can be Rejected
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageRejectable(msg) {
  return (
    msg &&
    (hasClaimsReferences(msg) ||
      hasProposesReferences(msg) ||
      hasProposesToCancelReferences(msg)) &&
    !msg.get("outgoingMessage") &&
    !isMessageAccepted(msg) &&
    !isMessageCancelled(msg) &&
    !isMessageCancellationPending(msg) &&
    !isMessageRetracted(msg) &&
    !isMessageRejected(msg)
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
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageProposed(msg) {
  const messageStatus = msg && msg.get("messageStatus");
  return messageStatus && messageStatus.get("isProposed");
}

/**
 * Determines if a given message is in the state claimed
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageClaimed(msg) {
  const messageStatus = msg && msg.get("messageStatus");
  return messageStatus && messageStatus.get("isClaimed");
}

/**
 * Determines if a given message is in the state rejected
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageRejected(msg) {
  const messageStatus = msg && msg.get("messageStatus");
  return messageStatus && messageStatus.get("isRejected");
}

/**
 * Determines if a given message is in the state accepted
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageAccepted(msg) {
  const messageStatus = msg && msg.get("messageStatus");
  return messageStatus && messageStatus.get("isAccepted");
}

/**
 * Determines if a given message is in the state retracted
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageRetracted(msg) {
  const messageStatus = msg && msg.get("messageStatus");
  return messageStatus && messageStatus.get("isRetracted");
}

/**
 * Determines if a given message is in the state Cancelled
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageCancelled(msg) {
  const messageStatus = msg && msg.get("messageStatus");
  return messageStatus && messageStatus.get("isCancelled");
}

/**
 * Determines if a given message is in the state CancellationPending
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageCancellationPending(msg) {
  const messageStatus = msg && msg.get("messageStatus");
  return messageStatus && messageStatus.get("isCancellationPending");
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
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageProposal(msg) {
  return (
    (hasProposesToCancelReferences(msg) || hasProposesReferences(msg)) &&
    !(
      isMessageAccepted(msg) ||
      isMessageCancellationPending(msg) ||
      isMessageCancelled(msg) ||
      isMessageRejected(msg) ||
      isMessageRetracted(msg)
    )
  );
}

/**
 * Determines if a given message is considered a claim
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageClaim(msg) {
  return (
    hasClaimsReferences(msg) &&
    !(
      isMessageAccepted(msg) ||
      isMessageCancellationPending(msg) ||
      isMessageCancelled(msg) ||
      isMessageRejected(msg) ||
      isMessageRetracted(msg)
    )
  );
}

/**
 * Determines if a given message is an agreement
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageAgreement(msg) {
  return isMessageAccepted(msg) && !isMessageCancellationPending(msg);
}

/**
 * Returns the own-messageUri for the given messageUri, this is due to the fact that referenced messages are
 * referenced by a messageUri that is not necessarily the owned messageUri, since our state maps are based
 * on the owned messageUri we need to determine the correct/own messageUri to ensure correct state updates
 * @param messages
 * @param messageUri
 * @returns {*}
 */
export function getOwnMessageUri(messages, messageUri) {
  if (messageUri) {
    if (messages.filter(msg => msg.get("uri") === messageUri).size > 0) {
      return messageUri;
    } else {
      const messagesOfRemoteUri = messages.filter(
        msg => msg.get("remoteUri") === messageUri
      );
      if (messagesOfRemoteUri.size > 0) {
        return messagesOfRemoteUri.first().get("uri");
      }
    }
  }
  return undefined;
}
