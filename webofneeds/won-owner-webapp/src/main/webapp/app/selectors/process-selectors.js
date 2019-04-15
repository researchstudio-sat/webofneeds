/**
 * Created by quasarchimaere on 21.01.2019.
 */
import { get, getIn } from "../utils.js";
import * as processUtils from "../process-utils.js";

/**
 * Check if anything in the state sub-map of process is currently marked as loading
 * @param state (full redux-state)
 * @returns true if anything is currently loading
 */
export function isLoading(state) {
  const process = get(state, "process");

  return (
    processUtils.isProcessingInitialLoad(process) ||
    processUtils.isProcessingNeedUrisFromOwnerLoad(process) ||
    processUtils.isProcessingLogin(process) ||
    processUtils.isProcessingLogout(process) ||
    processUtils.isProcessingPublish(process) ||
    processUtils.isProcessingAcceptTermsOfService(process) ||
    processUtils.isProcessingVerifyEmailAddress(process) ||
    processUtils.isProcessingResendVerificationEmail(process) ||
    processUtils.isProcessingSendAnonymousLinkEmail(process) ||
    processUtils.isAnyNeedLoading(process) ||
    processUtils.isAnyConnectionLoading(process, true) ||
    processUtils.isAnyMessageLoading(process)
  );
}

export function isProcessingAcceptTermsOfService(state) {
  return processUtils.isProcessingAcceptTermsOfService(get(state, "process"));
}

export function isProcessingVerifyEmailAddress(state) {
  return processUtils.isProcessingVerifyEmailAddress(get(state, "process"));
}

export function isProcessingResendVerificationEmail(state) {
  return processUtils.isProcessingResendVerificationEmail(
    get(state, "process")
  );
}

export function isProcessingSendAnonymousLinkEmail(state) {
  return processUtils.isProcessingSendAnonymousLinkEmail(get(state, "process"));
}

export function isNeedLoading(state, needUri) {
  return processUtils.isNeedLoading(get(state, "process"), needUri);
}

export function isNeedToLoad(state, needUri) {
  return (
    !getIn(state, ["needs", needUri]) ||
    processUtils.isNeedToLoad(get(state, "process"), needUri)
  );
}

export function hasNeedFailedToLoad(state, needUri) {
  return processUtils.hasNeedFailedToLoad(get(state, "process"), needUri);
}
