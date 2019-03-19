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
  return !!get(accountState, "loggedIn");
}

/**
 * Determines if a the currently logged in user is an anonymous one
 * @param accountState
 * @returns {*|boolean}
 */
export function isAnonymous(accountState) {
  return !!get(accountState, "isAnonymous");
}

/**
 * Determines if the email address of the currently logged in user is verified
 * @param accountState
 * @returns {*|boolean}
 */
export function isEmailVerified(accountState) {
  return !!get(accountState, "emailVerified");
}

/**
 * Returns the email of the currently logged in user
 *
 * @param accountState
 * @returns {*}
 */
export function getEmail(accountState) {
  return get(accountState, "email");
}

/**
 * Returns the privateId of the currently logged in user
 *
 * @param accountState
 * @returns {*}
 */
export function getPrivateId(accountState) {
  return get(accountState, "privateId");
}
