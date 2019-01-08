/**
 * Created by fsuda on 05.11.2018.
 */

import won from "./won-es6.js";

/**
 * Determines if a given connection is a chatConnection
 * @param msg
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
 * @param msg
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
 * @param msg
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
 * @param msg
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
 * @param msg
 * @returns {*|boolean}
 */
export function isChatConnectionToGroup(conn) {
  return isChatConnection(conn) && isRemoteGroupChatConnection(conn);
}
