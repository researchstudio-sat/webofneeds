/**
 * Created by fsuda on 05.11.2018.
 */

import {
  isMessageCancellationPending,
  isMessageProposal,
  isMessageClaim,
  isMessageUnread,
  isMessageAgreement,
} from "../message-utils.js";
import { getOwnedNeedByConnectionUri } from "./general-selectors.js";

export function getMessagesByConnectionUri(state, connectionUri) {
  const need = getOwnedNeedByConnectionUri(state, connectionUri);
  return need && need.getIn(["connections", connectionUri, "messages"]);
}

export function getAgreementMessagesByConnectionUri(state, connectionUri) {
  const messages = getMessagesByConnectionUri(state, connectionUri);
  return messages && messages.filter(msg => isMessageAgreement(msg));
}

export function getCancellationPendingMessagesByConnectionUri(
  state,
  connectionUri
) {
  const messages = getMessagesByConnectionUri(state, connectionUri);
  return messages && messages.filter(msg => isMessageCancellationPending(msg));
}

export function getProposalMessagesByConnectionUri(state, connectionUri) {
  const messages = getMessagesByConnectionUri(state, connectionUri);
  return messages && messages.filter(msg => isMessageProposal(msg));
}

export function getClaimMessagesByConnectionUri(state, connectionUri) {
  const messages = getMessagesByConnectionUri(state, connectionUri);
  return messages && messages.filter(msg => isMessageClaim(msg));
}

export function getUnreadMessagesByConnectionUri(state, connectionUri) {
  const messages = getMessagesByConnectionUri(state, connectionUri);
  return messages && messages.filter(msg => isMessageUnread(msg));
}
