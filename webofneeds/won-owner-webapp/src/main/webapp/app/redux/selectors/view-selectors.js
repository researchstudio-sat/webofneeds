/**
 * Created by quasarchimaere on 21.01.2019.
 */
import { createSelector } from "reselect";

import { get, getIn } from "../../utils.js";
import * as viewUtils from "../utils/view-utils.js";
import * as accountUtils from "../utils/account-utils.js";
import {
  getVerificationTokenFromRoute,
  getAccountState,
} from "./general-selectors.js";

export const getViewState = state => get(state, "view");

/**
 * Check if showSlideIns is true
 * @param state (full redux-state)
 * @returns {*}
 */
export const isSlideInsVisible = createSelector(getViewState, viewState =>
  viewUtils.isSlideInsVisible(viewState)
);

export const showModalDialog = createSelector(getViewState, viewState =>
  viewUtils.showModalDialog(viewState)
);

export const showRdf = createSelector(getViewState, viewState =>
  viewUtils.showRdf(viewState)
);

export const isDebugModeEnabled = createSelector(getViewState, viewState =>
  viewUtils.isDebugModeEnabled(viewState)
);

export const isMenuVisible = createSelector(getViewState, viewState =>
  viewUtils.isMenuVisible(viewState)
);

export const isAnonymousLinkSent = createSelector(getViewState, viewState =>
  viewUtils.isAnonymousLinkSent(viewState)
);

export const isAnonymousLinkCopied = createSelector(getViewState, viewState =>
  viewUtils.isAnonymousLinkCopied(viewState)
);

export const showAnonymousSlideInEmailInput = createSelector(
  getViewState,
  viewState => viewUtils.showAnonymousSlideInEmailInput(viewState)
);

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
  const accountState = getAccountState(state);
  const isLoggedIn = accountUtils.isLoggedIn(accountState);
  const isAnonymous = accountUtils.isAnonymous(accountState);
  const isEmailVerified = accountUtils.isEmailVerified(accountState);

  return !!(
    !showSlideInConnectionLost(state) &&
    (verificationToken || (isLoggedIn && !isEmailVerified && !isAnonymous))
  );
}

export function showSlideInConnectionLost(state) {
  return getIn(state, ["messages", "lostConnection"]);
}

export function hasSlideIns(state) {
  return !!(
    showSlideInAnonymous(state) ||
    showSlideInAnonymousSuccess(state) ||
    showSlideInDisclaimer(state) ||
    showSlideInTermsOfService(state) ||
    showSlideInEmailVerification(state) ||
    showSlideInConnectionLost(state)
  );
}
