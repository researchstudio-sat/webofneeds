/**
 * Created by quasarchimaere on 21.01.2019.
 */
import { get } from "../../utils.js";
import * as processUtils from "../utils/process-utils.js";
import { createSelector } from "reselect";
import { getPossibleRequestCredentialsForAtom } from "~/app/redux/selectors/general-selectors";

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

export const getFetchTokenRequests = (atomUri, tokenScopeUri) =>
  createSelector(getProcessState, processState =>
    processUtils.getFetchTokenRequests(processState, atomUri, tokenScopeUri)
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

export const getUnusedRequestCredentialsForConnectionContainer = createSelector(
  state => state,
  getProcessState,
  state => get(state, "atoms"),
  (state, processState, atoms) =>
    atoms
      .filter((_, atomUri) => processUtils.isAtomLoaded(processState, atomUri))
      .filterNot((_, atomUri) =>
        processUtils.isConnectionContainerToLoad(processState, atomUri)
      )
      .filterNot((_, atomUri) =>
        processUtils.isConnectionContainerLoading(processState, atomUri)
      )
      .map((_, atomUri) => {
        const priorRequests = getConnectionContainerRequests(atomUri)(state);
        return getPossibleRequestCredentialsForAtom(atomUri)(state).filterNot(
          requestCredentials =>
            processUtils.isUsedCredentials(priorRequests, requestCredentials)
        );
      })
      .filter(
        requestCredentials => requestCredentials && requestCredentials.size > 0
      )
);
