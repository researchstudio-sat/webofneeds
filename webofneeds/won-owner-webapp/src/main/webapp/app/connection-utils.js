/**
 * Created by fsuda on 05.11.2018.
 */

import won from "./won-es6.js";

/**
 * Determines if a given connection is a chatConnection or a groupChatConnection
 * @param msg
 * @returns {*|boolean}
 */
export function isChatConnection(conn) {
  return (
    conn &&
    conn.get("facet") &&
    (conn.get("facet") === won.WON.ChatFacetCompacted ||
      isGroupChatConnection(conn))
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
