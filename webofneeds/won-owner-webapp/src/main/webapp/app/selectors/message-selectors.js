/**
 * Created by fsuda on 05.11.2018.
 */

import {
  isMessageProposable,
  isMessageClaimable,
  isMessageCancelable,
  isMessageRetractable,
  isMessageAcceptable,
  isMessageRejectable,
  isMessageAccepted,
  isMessageCancellationPending,
  isMessageProposal,
  isMessageClaim,
  isMessageUnread,
} from "../message-utils.js";
import { getOwnedNeedByConnectionUri } from "./general-selectors.js";

export function selectAllMessagesByConnectionUri(state, connectionUri) {
  const need = getOwnedNeedByConnectionUri(state, connectionUri);
  return need && need.getIn(["connections", connectionUri, "messages"]);
}

export function selectAcceptableMessagesByConnectionUri(state, connectionUri) {
  const messages = selectAllMessagesByConnectionUri(state, connectionUri);
  return messages && messages.filter(msg => isMessageAcceptable(msg));
}

export function selectRejectableMessagesByConnectionUri(state, connectionUri) {
  const messages = selectAllMessagesByConnectionUri(state, connectionUri);
  return messages && messages.filter(msg => isMessageRejectable(msg));
}

export function selectRetractableMessagesByConnectionUri(state, connectionUri) {
  const messages = selectAllMessagesByConnectionUri(state, connectionUri);
  return messages && messages.filter(msg => isMessageRetractable(msg));
}

export function selectCancelableMessagesByConnectionUri(state, connectionUri) {
  const messages = selectAllMessagesByConnectionUri(state, connectionUri);
  return messages && messages.filter(msg => isMessageCancelable(msg));
}

export function selectProposableMessagesByConnectionUri(state, connectionUri) {
  const messages = selectAllMessagesByConnectionUri(state, connectionUri);
  return messages && messages.filter(msg => isMessageProposable(msg));
}

export function selectClaimableMessagesByConnectionUri(state, connectionUri) {
  const messages = selectAllMessagesByConnectionUri(state, connectionUri);
  return messages && messages.filter(msg => isMessageClaimable(msg));
}

export function selectSelectedMessagesByConnectionUri(state, connectionUri) {
  const messages = selectAllMessagesByConnectionUri(state, connectionUri);
  return (
    messages && messages.filter(msg => msg.getIn(["viewState", "isSelected"]))
  );
}

export function selectAgreementMessagesByConnectionUri(state, connectionUri) {
  const messages = selectAllMessagesByConnectionUri(state, connectionUri);
  return (
    messages &&
    messages.filter(
      msg => isMessageAccepted(msg) && !isMessageCancellationPending(msg)
    )
  );
}

export function selectCancellationPendingMessagesByConnectionUri(
  state,
  connectionUri
) {
  const messages = selectAllMessagesByConnectionUri(state, connectionUri);
  return messages && messages.filter(msg => isMessageCancellationPending(msg));
}

export function selectProposalMessagesByConnectionUri(state, connectionUri) {
  const messages = selectAllMessagesByConnectionUri(state, connectionUri);
  return messages && messages.filter(msg => isMessageProposal(msg));
}

export function selectClaimMessagesByConnectionUri(state, connectionUri) {
  const messages = selectAllMessagesByConnectionUri(state, connectionUri);
  return messages && messages.filter(msg => isMessageClaim(msg));
}

export function selectUnreadMessagesByConnectionUri(state, connectionUri) {
  const messages = selectAllMessagesByConnectionUri(state, connectionUri);
  return messages && messages.filter(msg => isMessageUnread(msg));
}
