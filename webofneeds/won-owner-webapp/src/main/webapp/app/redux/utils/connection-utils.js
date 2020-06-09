/**
 * Created by fsuda on 05.11.2018.
 */

import vocab from "../../service/vocab.js";
import { get } from "../../utils.js";

export function isUsingTemporaryUri(connection) {
  return !!get(connection, "usingTemporaryUri");
}

export function isRequestSent(connection) {
  return get(connection, "state") === vocab.WON.RequestSent;
}

export function isRequestReceived(connection) {
  return get(connection, "state") === vocab.WON.RequestReceived;
}

export function isSuggested(connection) {
  return get(connection, "state") === vocab.WON.Suggested;
}

export function isConnected(connection) {
  return get(connection, "state") === vocab.WON.Connected;
}

export function isClosed(connection) {
  return get(connection, "state") === vocab.WON.Closed;
}

export function isUnread(connection) {
  return !!get(connection, "unread");
}

export function hasSocketUris(connection, socketUri, targetSocketUri) {
  return (
    hasSocketUri(connection, socketUri) &&
    hasTargetSocketUri(connection, targetSocketUri)
  );
}

export function hasSocketUri(connection, socketUri) {
  return (
    !!connection && !!socketUri && get(connection, "socketUri") === socketUri
  );
}

export function hasTargetSocketUri(connection, socketUri) {
  return (
    !!connection &&
    !!socketUri &&
    get(connection, "targetSocketUri") === socketUri
  );
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
    if (size == 0) {
      return "No " + entityPlural + " yet";
    }
    if (size == 1) {
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
