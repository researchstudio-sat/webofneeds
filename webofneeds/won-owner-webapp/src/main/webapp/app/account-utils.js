/**
 * Created by fsuda on 19.03.2019.
 */

import { get } from "./utils.js";
/**
 * Determines if a there is currently a user logged in or not
 * @param accountState
 * @returns {*|boolean}
 */
export function isLoggedIn(accountState) {
  return get(accountState, "loggedIn");
}
