/**
 * Created by fsuda on 05.11.2018.
 */

import won from "./won-es6.js";
import { hasGroupFacet, hasChatFacet } from "./need-utils.js";
import { get, getIn } from "./utils.js";
/**
 * Determines if a given connection is a chatConnection
 * @param conn
 * @returns {*|boolean}
 */
export function isChatConnection(conn) {
  return (
    conn &&
    conn.get("facet") &&
    conn.get("facet") === won.WON.ChatFacetCompacted
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
    conn.get("facet") &&
    conn.get("facet") === won.WON.GroupFacetCompacted
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
    conn.get("facet") &&
    conn.get("facet") === won.WON.ChatFacetCompacted
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
    conn.get("remoteFacet") &&
    conn.get("remoteFacet") === won.WON.GroupFacetCompacted
  );
}

/**
 * Determines if a given connection is a chatConnection connected to a groupFacet on the remoteSide
 * @param conn
 * @returns {*|boolean}
 */
function isChatConnectionToGroup(conn) {
  return isChatConnection(conn) && isRemoteGroupChatConnection(conn);
}

/**
 * Determines if the connection is between a single need and a groupChat
 * This method is only necessary because certain connections (e.g. suggested connections), do not have a remoteConnection
 * and therefore we can't distinguish between chatToGroup and chatToChat connections without looking into the remoteNeed
 * in addition to the connection itself -> should be resolved once the hints store the remoteFacet in addition to the facet
 * FIXME: Current workaround for suggestedConnections that do not contains a remoteFacet
 * FIXME: once both remote and own facet are present in the connection we should be able to use the isChatConnectionToGroup function without any workaround
 * @param needs reduxState needs
 * @param connUri to check
 * @param needUri to check
 * @returns {boolean}
 */
export function isChatToGroup(needs, needUri, connUri) {
  const need = get(needs, needUri);
  const conn = getIn(need, ["connections", connUri]);

  if (isChatConnectionToGroup(conn)) {
    return true;
  } else {
    const remoteNeedUri = get(conn, "remoteNeedUri");
    const remoteNeed = get(needs, remoteNeedUri);

    if (hasGroupFacet(remoteNeed)) {
      if (hasChatFacet(remoteNeed)) {
        console.warn(
          "The remoteNeed contains the chatFacet as well as the groupFacet, we do not know if this connection is connecting with the groupFacet or with the chatFacet just yet: remoteNeed:",
          remoteNeed,
          ", need:",
          need,
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

/**
 * Creates a label of the participants and suggestions/requests/invites of a given set of groupChatConnections
 * @param groupChatConnections
 */
export function generateGroupChatParticipantsLabel(groupChatConnections) {
  const participantsSize = groupChatConnections.filter(
    conn => conn.get("state") === won.WON.Connected
  ).size;
  const suggestedSize = groupChatConnections.filter(
    conn => conn.get("state") === won.WON.Suggested
  ).size;
  const invitedSize = groupChatConnections.filter(
    conn => conn.get("state") === won.WON.RequestSent
  ).size;
  const requestedSize = groupChatConnections.filter(
    conn => conn.get("state") === won.WON.RequestReceived
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

  let participantsLabel = createPartOfLabel(
    participantsSize,
    "Participant",
    "Participants"
  );

  if (invitedSize > 0) {
    if (participantsLabel.length > 0) {
      participantsLabel += ", ";
    }
    participantsLabel += createPartOfLabel(invitedSize, "Invited", "Invited");
  }

  if (requestedSize > 0) {
    if (participantsLabel.length > 0) {
      participantsLabel += ", ";
    }
    participantsLabel += createPartOfLabel(
      requestedSize,
      "Request",
      "Requests"
    );
  }

  if (suggestedSize > 0) {
    if (participantsLabel.length > 0) {
      participantsLabel += ", ";
    }
    participantsLabel += createPartOfLabel(
      suggestedSize,
      "Suggested",
      "Suggested"
    );
  }

  return participantsLabel;
}
