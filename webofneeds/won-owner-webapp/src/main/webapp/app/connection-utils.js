/**
 * Created by fsuda on 05.11.2018.
 */

import won from "./won-es6.js";
import { hasGroupSocket, hasChatSocket } from "./atom-utils.js";
import { get, getIn } from "./utils.js";
/**
 * Determines if a given connection is a chatConnection
 * @param conn
 * @returns {*|boolean}
 */
export function isChatConnection(conn) {
  return (
    conn &&
    conn.get("socket") &&
    conn.get("socket") === won.CHAT.ChatSocketCompacted
  );
}

/**
 * Determines if a given connection is a groupChatConnection
 * @param conn
 * @returns {*|boolean}
 */
export function isGroupChatConnection(conn) {
  return (
    conn &&
    conn.get("socket") &&
    conn.get("socket") === won.GROUP.GroupSocketCompacted
  );
}

/**
 * Determines if a given connection is a chatConnection on the remoteSide
 * @param conn
 * @returns {*|boolean}
 */
export function isRemoteChatConnection(conn) {
  return (
    conn &&
    conn.get("socket") &&
    conn.get("socket") === won.CHAT.ChatSocketCompacted
  );
}

/**
 * Determines if a given connection is a groupChatConnection on the remoteSide
 * @param conn
 * @returns {*|boolean}
 */
export function isRemoteGroupChatConnection(conn) {
  return (
    conn &&
    conn.get("targetSocket") &&
    conn.get("targetSocket") === won.GROUP.GroupSocketCompacted
  );
}

/**
 * Determines if a given connection is a chatConnection connected to a groupSocket on the remoteSide
 * @param conn
 * @returns {*|boolean}
 */
function isChatConnectionToGroup(conn) {
  return isChatConnection(conn) && isRemoteGroupChatConnection(conn);
}

/**
 * Determines if the connection is between a single atom and a groupChat
 * This method is only necessary because certain connections (e.g. suggested connections), do not have a targetConnection
 * and therefore we can't distinguish between chatToGroup and chatToChat connections without looking into the targetAtom
 * in addition to the connection itself -> should be resolved once the hints store the targetSocket in addition to the socket
 * FIXME: Current workaround for suggestedConnections that do not contains a targetSocket
 * FIXME: once both remote and own socket are present in the connection we should be able to use the isChatConnectionToGroup function without any workaround
 * @param atoms reduxState atoms
 * @param connUri to check
 * @param atomUri to check
 * @returns {boolean}
 */
export function isChatToGroup(atoms, atomUri, connUri) {
  const atom = get(atoms, atomUri);
  const conn = getIn(atom, ["connections", connUri]);

  if (isChatConnectionToGroup(conn)) {
    return true;
  } else {
    const targetAtomUri = get(conn, "targetAtomUri");
    const targetAtom = get(atoms, targetAtomUri);

    if (hasGroupSocket(targetAtom)) {
      if (hasChatSocket(targetAtom)) {
        console.warn(
          "The targetAtom contains the chatSocket as well as the groupSocket, we do not know if this connection is connecting with the groupSocket or with the chatSocket just yet: targetAtom:",
          targetAtom,
          ", atom:",
          atom,
          " conn:",
          conn
        );
      }
      return true;
    } else {
      return false;
    }
  }
}

export function isRequestSent(connection) {
  return get(connection, "state") === won.WON.RequestSent;
}

export function isRequestReceived(connection) {
  return get(connection, "state") === won.WON.RequestReceived;
}

export function isSuggested(connection) {
  return get(connection, "state") === won.WON.Suggested;
}

export function isConnected(connection) {
  return get(connection, "state") === won.WON.Connected;
}

export function isClosed(connection) {
  return get(connection, "state") === won.WON.Closed;
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
