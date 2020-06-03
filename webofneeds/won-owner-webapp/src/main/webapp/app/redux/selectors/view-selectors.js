/**
 * Created by quasarchimaere on 21.01.2019.
 */
import { createSelector } from "reselect";

import { getIn } from "../../utils.js";
import * as viewUtils from "../utils/view-utils.js";
import * as accountUtils from "../utils/account-utils.js";
import { getAccountState, getViewState } from "./general-selectors.js";
import { getQueryParams } from "../../utils";

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

export const showSlideInConnectionLost = createSelector(
  state => getIn(state, ["messages", "lostConnection"]),
  lostConnection => lostConnection
);

export const showSlideInAnonymousSuccess = createSelector(
  getAccountState,
  showSlideInConnectionLost,
  isAnonymousLinkSent,
  isAnonymousLinkCopied,
  (
    accountState,
    isShowSlideInConnectionLost,
    _isAnonymousLinkSent,
    _isAnonymousLinkCopied
  ) =>
    !isShowSlideInConnectionLost &&
    accountUtils.isAnonymous(accountState) &&
    (_isAnonymousLinkSent || _isAnonymousLinkCopied)
);

export const showSlideInAnonymous = createSelector(
  getAccountState,
  showSlideInConnectionLost,
  isAnonymousLinkSent,
  isAnonymousLinkCopied,
  (
    accountState,
    isShowSlideInConnectionLost,
    _isAnonymousLinkSent,
    _isAnonymousLinkCopied
  ) =>
    !isShowSlideInConnectionLost &&
    accountUtils.isAnonymous(accountState) &&
    !_isAnonymousLinkSent &&
    !_isAnonymousLinkCopied
);

export const showSlideInDisclaimer = createSelector(
  getAccountState,
  showSlideInConnectionLost,
  (accountState, isShowSlideInConnectionLost) =>
    !isShowSlideInConnectionLost &&
    !accountUtils.isDisclaimerAccepted(accountState)
);

export const showSlideInTermsOfService = createSelector(
  getAccountState,
  showSlideInConnectionLost,
  (accountState, isShowSlideInConnectionLost) =>
    accountUtils.isLoggedIn(accountState) &&
    !isShowSlideInConnectionLost &&
    !accountUtils.isTermsOfServiceAccepted(accountState)
);

export const showSlideInEmailVerification = history =>
  createSelector(
    getAccountState,
    showSlideInConnectionLost,
    (accountState, isShowSlideInConnectionLost) => {
      const { token } = getQueryParams(history.location);

      return !!(
        !isShowSlideInConnectionLost &&
        (token ||
          (accountUtils.isLoggedIn(accountState) &&
            !accountUtils.isEmailVerified(accountState) &&
            !accountUtils.isAnonymous(accountState)))
      );
    }
  );

export const hasSlideIns = history => state =>
  !!(
    showSlideInAnonymous(state) ||
    showSlideInAnonymousSuccess(state) ||
    showSlideInDisclaimer(state) ||
    showSlideInTermsOfService(state) ||
    showSlideInEmailVerification(history)(state) ||
    showSlideInConnectionLost(state)
  );

export const showSlideIns = history => state =>
  isSlideInsVisible(state) && hasSlideIns(history)(state);
