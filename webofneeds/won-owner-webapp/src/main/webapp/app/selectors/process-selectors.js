/**
 * Created by quasarchimaere on 21.01.2019.
 */
import { get } from "../utils.js";
import {
  isProcessingInitialLoad,
  isProcessingLogin,
  isProcessingLogout,
  isProcessingPublish,
  isProcessingAcceptTermsOfService,
  isProcessingVerifyEmailAddress,
  isProcessingResendVerificationEmail,
  isProcessingSendAnonymousLinkEmail,
  isAnyNeedLoading,
  isAnyConnectionLoading,
  isAnyMessageLoading,
} from "../process-utils.js";

/**
 * Check if anything in the state sub-map of process is currently marked as loading
 * @param state (full redux-state)
 * @returns true if anything is currently loading
 */
export function isLoading(state) {
  const process = get(state, "process");

  return (
    isProcessingInitialLoad(process) ||
    isProcessingLogin(process) ||
    isProcessingLogout(process) ||
    isProcessingPublish(process) ||
    isProcessingAcceptTermsOfService(process) ||
    isProcessingVerifyEmailAddress(process) ||
    isProcessingResendVerificationEmail(process) ||
    isProcessingSendAnonymousLinkEmail(process) ||
    isAnyNeedLoading(process) ||
    isAnyConnectionLoading(process, true) ||
    isAnyMessageLoading(process)
  );
}
