/**
 * Created by quasarchimaere on 21.01.2019.
 */
import { get } from "../../utils.js";
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
    processUtils.isAnyConnectionLoadingMessages(process) ||
    processUtils.isAnyConnectionLoading(process, true) ||
    processUtils.isAnyConnectionContainerLoading(process)
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

export const isAtomLoading = atomUri =>
  createSelector(getProcessState, processState =>
    processUtils.isAtomLoading(processState, atomUri)
  );

export const getAtomRequests = atomUri =>
  createSelector(getProcessState, processState =>
    processUtils.getAtomRequests(processState, atomUri)
  );

export const getConnectionContainerRequests = atomUri =>
  createSelector(getProcessState, processState =>
    processUtils.getConnectionContainerRequests(processState, atomUri)
  );

export const getAllConnectionContainerRequests = createSelector(
  getProcessState,
  processState =>
    get(processState, ["connectionContainers"]).map(atom =>
      get(atom, "connectionContainers")
    )
);

export const getAllAtomRequests = createSelector(
  getProcessState,
  processState =>
    get(processState, ["atoms"]).map(atom => get(atom, "requests"))
);
