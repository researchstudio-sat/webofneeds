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

/**
 * Determines if a the currently logged in user is an anonymous one
 * @param accountState
 * @returns {*|boolean}
 */
export function isAnonymous(accountState) {
  return get(accountState, "isAnonymous");
}

/**
 * Returns the email of the currentyl logged in user
 *
 * @param accountState
 * @returns {*}
 */
export function getEmail(accountState) {
  return get(accountState, "email");
}
