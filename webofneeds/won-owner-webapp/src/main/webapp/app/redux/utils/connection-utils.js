/**
 * Created by fsuda on 05.11.2018.
 */

import vocab from "../../service/vocab.js";
import { get } from "../../utils.js";
import * as messageUtils from "./message-utils.js";
import { isTagViewSocket } from "../../won-utils.js";

export function getState(connection) {
  return get(connection, "state");
}

export function getTargetAtomUri(connection) {
  return get(connection, "targetAtomUri");
}

export function getTargetConnectionUri(connection) {
  return get(connection, "targetConnectionUri");
}

export function getLastUpdateDate(connection) {
  return get(connection, "lastUpdateDate");
}

export function getMultiSelectType(connection) {
  return get(connection, "multiSelectType");
}

export function getSocketUri(connection) {
  return get(connection, "socketUri");
}

export function getTargetSocketUri(connection) {
  return get(connection, "targetSocketUri");
}

export function isUsingTemporaryUri(connection) {
  return !!get(connection, "usingTemporaryUri");
}

export function isRequestSent(connection) {
  return getState(connection) === vocab.WON.RequestSent;
}

export function isRequestReceived(connection) {
  return getState(connection) === vocab.WON.RequestReceived;
}

export function isSuggested(connection) {
  return getState(connection) === vocab.WON.Suggested;
}

export function isConnected(connection) {
  return getState(connection) === vocab.WON.Connected;
}

export function isClosed(connection) {
  return getState(connection) === vocab.WON.Closed;
}

export function isUnread(connection) {
  return !!get(connection, "unread");
}

export function getAgreementData(connection) {
  return get(connection, "agreementData");
}

export function getAgreementDataset(connection) {
  return get(connection, "agreementDataset");
}

export function getPetriNetData(connection) {
  return get(connection, "petriNetData");
}

export function showPetriNetData(connection) {
  return get(connection, "showPetriNetData");
}

export function showAgreementData(connection) {
  return get(connection, "showAgreementData");
}

export function hasSocketUris(connection, socketUri, targetSocketUri) {
  return (
    hasSocketUri(connection, socketUri) &&
    hasTargetSocketUri(connection, targetSocketUri)
  );
}

export function hasSocketUri(connection, socketUri) {
  return !!connection && !!socketUri && getSocketUri(connection) === socketUri;
}

export function hasTargetSocketUri(connection, socketUri) {
  return (
    !!connection && !!socketUri && getTargetSocketUri(connection) === socketUri
  );
}

export function getMessages(connection) {
  return get(connection, "messages");
}

export function getMessagesSize(connection) {
  const messages = getMessages(connection);

  return messages ? messages.size : 0;
}

export function getMessage(connection, msgUri) {
  const messages = getMessages(connection);

  return get(messages, msgUri);
}
/**
 * Creates a label of the participants and suggestions/requests/invites of a given set of groupChatConnections
 * @param groupChatConnections
 */
export function generateGroupChatMembersLabel(groupChatConnections) {
  const groupMembersSize = groupChatConnections.filter(conn =>
    isConnected(conn)
  ).size;
  const suggestedSize = groupChatConnections.filter(conn => isSuggested(conn))
    .size;
  const invitedSize = groupChatConnections.filter(conn => isRequestSent(conn))
    .size;
  const requestedSize = groupChatConnections.filter(conn =>
    isRequestReceived(conn)
  ).size;

  const createPartOfLabel = (size, entitySingular, entityPlural) => {
    if (size === 0) {
      return "No " + entityPlural + " yet";
    }
    if (size === 1) {
      return size + " " + entitySingular;
    } else if (size > 1) {
      return size + " " + entityPlural;
    }
  };

  let groupMembersLabel = createPartOfLabel(
    groupMembersSize,
    "Group Member",
    "Group Members"
  );

  if (invitedSize > 0) {
    if (groupMembersLabel.length > 0) {
      groupMembersLabel += ", ";
    }
    groupMembersLabel += createPartOfLabel(invitedSize, "Invited", "Invited");
  }

  if (requestedSize > 0) {
    if (groupMembersLabel.length > 0) {
      groupMembersLabel += ", ";
    }
    groupMembersLabel += createPartOfLabel(
      requestedSize,
      "Request",
      "Requests"
    );
  }

  if (suggestedSize > 0) {
    if (groupMembersLabel.length > 0) {
      groupMembersLabel += ", ";
    }
    groupMembersLabel += createPartOfLabel(
      suggestedSize,
      "Suggested",
      "Suggested"
    );
  }

  return groupMembersLabel;
}

/**
 * Removes the socketEntry of a map if the socketCapacity is below 1 (used so that socketCapacity 1 sockets, are not displayed
 * as tabs, and do not show up in the atom-content like the other sockets
 * @param _
 * @param socketType
 * @returns {boolean}
 */
export const filterSingleConnectedSocketCapacityFilter = (_, socketType) =>
  !vocab.socketCapacity[socketType] || vocab.socketCapacity[socketType] > 1;

/**
 * Removes the socketEntry of a map if the tagViewSocket is set to true (used so that tagView Sockets, are not displayed
 * as tabs, and do not show up in the atom-content like the other sockets
 * @param _
 * @param socketType
 * @returns {boolean}
 */
export const filterTagViewSockets = (_, socketType) =>
  !isTagViewSocket(socketType);

/**
 * Removes the socketEntry of a map if the tagViewSocket is set to false or does not exist (used so that tagView Sockets, are not displayed
 * as tabs, and do not show up in the atom-content like the other sockets
 * @param _
 * @param socketType
 * @returns {boolean}
 */
export const filterNonTagViewSockets = (_, socketType) =>
  isTagViewSocket(socketType);

/**
 * Removes the socketEntry of a map if the socketCapacity is not excatly 1 (used so that only socketCapacity 1 sockets, are displayed
 * within the WonAtomContentGeneral component
 * @param _
 * @param socketType
 * @returns {boolean}
 */
export const filterNonSingleConnectedSocketCapacityFilter = (_, socketType) =>
  vocab.socketCapacity[socketType] === 1;

/**
 * (re)sorts the messages stored in the connection
 * @param conn
 * @returns {connection object with sorted messages)
 */
export function sortMessages(conn) {
  const messages = getMessages(conn);

  return conn.set(messageUtils.sortMessages(messages));
}
