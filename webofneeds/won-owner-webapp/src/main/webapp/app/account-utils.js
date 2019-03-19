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

/**
 * Returns the loginError if present
 *
 * @param accountState
 * @returns {*}
 */
export function getLoginError(accountState) {
  return get(accountState, "loginError");
}

/**
 * Returns the registerError if present
 *
 * @param accountState
 * @returns {*}
 */
export function getRegisterError(accountState) {
  return get(accountState, "registerError");
}

/**
 * Returns the emailVerificationError if present
 *
 * @param accountState
 * @returns {*}
 */
export function getEmailVerificationError(accountState) {
  return get(accountState, "emailVerificationError");
}

/**
 * Return true if the disclaimer is already accepted
 * @param accountState
 * @returns {boolean}
 */
export function isDisclaimerAccepted(accountState) {
  return !!get(accountState, "acceptedDisclaimer");
}
