/**
 * Created by quasarchimaere on 21.01.2019.
 */
import { getIn, get } from "../../utils.js";
import * as processUtils from "../utils/process-utils.js";
import { createSelector } from "reselect";

const getProcessState = createSelector(
  state => get(state, "process"),
  state => state
);

/**
 * Check if anything in the state sub-map of process is currently marked as loading
 * @param state (full redux-state)
 * @returns true if anything is currently loading
 */
export const isLoading = createSelector(
  getProcessState,
  process =>
    processUtils.isProcessingInitialLoad(process) ||
    processUtils.isProcessingWhatsAround(process) ||
    processUtils.isProcessingMetaAtoms(process) ||
    processUtils.isProcessingWhatsNew(process) ||
    processUtils.isProcessingLogin(process) ||
    processUtils.isProcessingLogout(process) ||
    processUtils.isProcessingPublish(process) ||
    processUtils.isProcessingAcceptTermsOfService(process) ||
    processUtils.isProcessingVerifyEmailAddress(process) ||
    processUtils.isProcessingResendVerificationEmail(process) ||
    processUtils.isProcessingSendAnonymousLinkEmail(process) ||
    processUtils.isAnyAtomLoading(process) ||
    processUtils.isAnyConnectionLoading(process, true) ||
    processUtils.isAnyMessageLoading(process)
);

export const isProcessingWhatsNew = createSelector(getProcessState, process =>
  processUtils.isProcessingWhatsNew(process)
);

export const isProcessingPublish = createSelector(getProcessState, process =>
  processUtils.isProcessingPublish(process)
);

export const isProcessingAcceptTermsOfService = createSelector(
  getProcessState,
  process => processUtils.isProcessingAcceptTermsOfService(process)
);

export const isProcessingVerifyEmailAddress = createSelector(
  getProcessState,
  process => processUtils.isProcessingVerifyEmailAddress(process)
);

export const isProcessingResendVerificationEmail = createSelector(
  getProcessState,
  process => processUtils.isProcessingResendVerificationEmail(process)
);

export const isProcessingSendAnonymousLinkEmail = createSelector(
  getProcessState,
  process => processUtils.isProcessingSendAnonymousLinkEmail(process)
);

export function isAtomLoading(state, atomUri) {
  return processUtils.isAtomLoading(getProcessState(state), atomUri);
}

export function isAtomToLoad(state, atomUri) {
  return (
    !getIn(state, ["atoms", atomUri]) ||
    processUtils.isAtomToLoad(getProcessState(state), atomUri)
  );
}

export function hasAtomFailedToLoad(state, atomUri) {
  return processUtils.hasAtomFailedToLoad(getProcessState(state), atomUri);
}
