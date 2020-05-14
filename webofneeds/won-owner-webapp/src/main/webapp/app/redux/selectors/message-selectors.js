/**
 * Created by fsuda on 05.11.2018.
 */

import {
  isMessageCancellationPending,
  isMessageProposal,
  isMessageClaim,
  isMessageUnread,
  isMessageAgreement,
} from "../utils/message-utils.js";
import { getOwnedAtomByConnectionUri } from "./general-selectors.js";

function getMessagesByConnectionUri(state, connectionUri) {
  const atom = getOwnedAtomByConnectionUri(state, connectionUri);
  return atom && atom.getIn(["connections", connectionUri, "messages"]);
}

export function getAgreementMessagesByConnectionUri(state, connectionUri) {
  const messages = getMessagesByConnectionUri(state, connectionUri);
  const atom = getOwnedAtomByConnectionUri(state, connectionUri);
  const connection = atom.getIn(["connections", connectionUri]);
  return (
    messages && messages.filter(msg => isMessageAgreement(connection, msg))
  );
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
  const atom = getOwnedAtomByConnectionUri(state, connectionUri);
  const connection = atom.getIn(["connections", connectionUri]);
  return messages && messages.filter(msg => isMessageProposal(connection, msg));
}

export function getClaimMessagesByConnectionUri(state, connectionUri) {
  const messages = getMessagesByConnectionUri(state, connectionUri);
  const atom = getOwnedAtomByConnectionUri(state, connectionUri);
  const connection = atom.getIn(["connections", connectionUri]);
  return messages && messages.filter(msg => isMessageClaim(connection, msg));
}

export function getUnreadMessagesByConnectionUri(state, connectionUri) {
  const messages = getMessagesByConnectionUri(state, connectionUri);
  return messages && messages.filter(msg => isMessageUnread(msg));
}
