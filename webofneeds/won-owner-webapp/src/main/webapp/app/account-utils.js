/**
 * Created by fsuda on 19.03.2019.
 */

import { get } from "./utils.js";
/**
 * Determines if a given connection is a chatConnection
 * @param conn
 * @returns {*|boolean}
 */
export function isLoggedIn(accountState) {
  return get(accountState, "loggedIn");
}
