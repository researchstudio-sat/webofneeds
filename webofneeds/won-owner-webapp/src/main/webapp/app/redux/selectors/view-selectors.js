/**
 * Created by quasarchimaere on 21.01.2019.
 */
import { get, getIn } from "../../utils.js";
import * as viewUtils from "../utils/view-utils.js";
import * as accountUtils from "../utils/account-utils.js";
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

export function showMenu(state) {
  return viewUtils.showMenu(get(state, "view"));
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
  const isAnonymous = accountUtils.isAnonymous(get(state, "account"));

  return (
    !showSlideInConnectionLost(state) &&
    isAnonymous &&
    (isAnonymousLinkSent(state) || isAnonymousLinkCopied(state))
  );
}

export function showSlideInAnonymous(state) {
  const isAnonymous = accountUtils.isAnonymous(get(state, "account"));

  return (
    !showSlideInConnectionLost(state) &&
    isAnonymous &&
    !isAnonymousLinkSent(state) &&
    !isAnonymousLinkCopied(state)
  );
}

export function showSlideInDisclaimer(state) {
  const isDisclaimerAccepted = accountUtils.isDisclaimerAccepted(
    get(state, "account")
  );

  return !showSlideInConnectionLost(state) && !isDisclaimerAccepted;
}

export function showSlideInTermsOfService(state) {
  const accountState = get(state, "account");
  const isLoggedIn = accountUtils.isLoggedIn(accountState);
  const isTermsOfServiceAccepted = accountUtils.isTermsOfServiceAccepted(
    accountState
  );

  return (
    isLoggedIn && !showSlideInConnectionLost(state) && !isTermsOfServiceAccepted
  );
}

export function showSlideInEmailVerification(state) {
  const verificationToken = getVerificationTokenFromRoute(state);
  const accountState = get(state, "account");
  const isLoggedIn = accountUtils.isLoggedIn(accountState);
  const isAnonymous = accountUtils.isAnonymous(accountState);
  const isEmailVerified = accountUtils.isEmailVerified(accountState);

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
