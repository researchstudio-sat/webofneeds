/**
 * Created by quasarchimaere on 21.01.2019.
 */
import { get, getIn } from "../utils.js";
import * as viewUtils from "../view-utils.js";
import { getVerificationTokenFromRoute } from "./general-selectors.js";

/**
 * Check if showSlideIns is true
 * @param state (full redux-state)
 * @returns {*}
 */
export function showSlideIns(state) {
  return viewUtils.showSlideIns(get(state, "view"));
}

export function showModalDialog(state) {
  return viewUtils.showModalDialog(get(state, "view"));
}

export function showRdf(state) {
  return viewUtils.showRdf(get(state, "view"));
}

export function isAnonymousLinkSent(state) {
  return viewUtils.isAnonymousLinkSent(get(state, "view"));
}

export function isAnonymousLinkCopied(state) {
  return viewUtils.isAnonymousLinkCopied(get(state, "view"));
}

export function isAnonymousSlideInExpanded(state) {
  return viewUtils.isAnonymousSlideInExpanded(get(state, "view"));
}

export function showAnonymousSlideInEmailInput(state) {
  return viewUtils.showAnonymousSlideInEmailInput(get(state, "view"));
}

export function showSlideInAnonymousSuccess(state) {
  const isAnonymous = getIn(state, ["account", "isAnonymous"]);

  return (
    !showSlideInConnectionLost(state) &&
    isAnonymous &&
    (isAnonymousLinkSent(state) || isAnonymousLinkCopied(state))
  );
}

export function showSlideInAnonymous(state) {
  const isAnonymous = getIn(state, ["account", "isAnonymous"]);

  return (
    !showSlideInConnectionLost(state) &&
    isAnonymous &&
    !isAnonymousLinkSent(state) &&
    !isAnonymousLinkCopied(state)
  );
}

export function showSlideInDisclaimer(state) {
  const isDisclaimerAccepted = getIn(state, ["account", "acceptedDisclaimer"]);

  return !showSlideInConnectionLost(state) && !isDisclaimerAccepted;
}

export function showSlideInTermsOfService(state) {
  const isLoggedIn = getIn(state, ["account", "loggedIn"]);
  const isTermsOfServiceAccepted = getIn(state, [
    "account",
    "acceptedTermsOfService",
  ]);

  return (
    isLoggedIn && !showSlideInConnectionLost(state) && !isTermsOfServiceAccepted
  );
}

export function showSlideInEmailVerification(state) {
  const verificationToken = getVerificationTokenFromRoute(state);
  const isLoggedIn = getIn(state, ["account", "loggedIn"]);
  const isAnonymous = getIn(state, ["account", "isAnonymous"]);
  const isEmailVerified = getIn(state, ["account", "emailVerified"]);

  return (
    !showSlideInConnectionLost(state) &&
    (verificationToken || (isLoggedIn && !isEmailVerified && !isAnonymous))
  );
}

export function showSlideInConnectionLost(state) {
  return getIn(state, ["messages", "lostConnection"]);
}

export function hasSlideIns(state) {
  return (
    showSlideInAnonymous(state) ||
    showSlideInAnonymousSuccess(state) ||
    showSlideInDisclaimer(state) ||
    showSlideInTermsOfService(state) ||
    showSlideInEmailVerification(state) ||
    showSlideInConnectionLost(state)
  );
}
