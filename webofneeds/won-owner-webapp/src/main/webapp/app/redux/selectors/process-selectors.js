/**
 * Created by quasarchimaere on 21.01.2019.
 */
import { get, getIn } from "../../utils.js";
import * as processUtils from "../utils/process-utils.js";

/**
 * Check if anything in the state sub-map of process is currently marked as loading
 * @param state (full redux-state)
 * @returns true if anything is currently loading
 */
export function isLoading(state) {
  const process = get(state, "process");

  return (
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
}

export function isProcessingWhatsNew(state) {
  return processUtils.isProcessingWhatsNew(state);
}

export function isProcessingPublish(state) {
  return processUtils.isProcessingPublish(get(state, "process"));
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

export function isAtomLoading(state, atomUri) {
  return processUtils.isAtomLoading(get(state, "process"), atomUri);
}

export function isAtomToLoad(state, atomUri) {
  return (
    !getIn(state, ["atoms", atomUri]) ||
    processUtils.isAtomToLoad(get(state, "process"), atomUri)
  );
}

export function hasAtomFailedToLoad(state, atomUri) {
  return processUtils.hasAtomFailedToLoad(get(state, "process"), atomUri);
}
