/**
 * Created by quasarchimaere on 22.01.2019.
 *
 * This Utils-File contains methods to check the status/vars stored in our process state.
 * These methods should be used in favor of accessing the process-state directly in the component
 */

//import won from "./won-es6.js";
import { get, getIn } from "../../utils.js";
import Immutable from "immutable";

/**
 * Return true if processingInitialLoad is currently active
 * @param process (full process from state)
 * @returns {*}
 */
export function isProcessingInitialLoad(process) {
  return get(process, "processingInitialLoad");
}

/**
 * Return true if fetchWhatsNew is currently active
 * @param process (full process from state)
 * @returns {*}
 */
export function isProcessingWhatsNew(process) {
  return get(process, "processingWhatsNew");
}

/**
 * Return true if fetching of MetaAtoms is currently active (e.g when fetchPersonas is running)
 * @param process (full process from state)
 * @returns {*}
 */
export function isProcessingMetaAtoms(process) {
  return get(process, "processingMetaAtoms");
}

/**
 * Return true if fetchWhatsAround is currently active
 * @param process (full process from state)
 * @returns {*}
 */
export function isProcessingWhatsAround(process) {
  return get(process, "processingWhatsAround");
}

/**
 * Return true if processingLogin is currently active
 * @param process (full process from state)
 * @returns {*}
 */
export function isProcessingLogin(process) {
  return get(process, "processingLogin");
}

/**
 * Return true if processingLoginForEmail is currently active
 * @param process (full process from state)
 * @returns {*}
 */
export function isProcessingLoginForEmail(process) {
  return get(process, "processingLoginForEmail");
}

/**
 * Return true if processingLogout is currently active
 * @param process (full process from state)
 * @returns {*}
 */
export function isProcessingLogout(process) {
  return get(process, "processingLogout");
}

/**
 * Return true if processingPublish is currently active
 * @param process (full process from state)
 * @returns {*}
 */
export function isProcessingPublish(process) {
  return get(process, "processingPublish");
}

/**
 * Return true if processingAcceptTermsOfService is currently active
 * @param process (full process from state)
 * @returns {*}
 */
export function isProcessingAcceptTermsOfService(process) {
  return get(process, "processingAcceptTermsOfService");
}

/**
 * Return true if processingVerifyEmailAddress is currently active
 * @param process (full process from state)
 * @returns {*}
 */
export function isProcessingVerifyEmailAddress(process) {
  return get(process, "processingVerifyEmailAddress");
}

/**
 * Return true if processingResendVerificationEmail is currently active
 * @param process (full process from state)
 * @returns {*}
 */
export function isProcessingResendVerificationEmail(process) {
  return get(process, "processingResendVerificationEmail");
}

/**
 * Return true if processingSendAnonymousLinkEmail is currently active
 * @param process (full process from state)
 * @returns {*}
 */
export function isProcessingSendAnonymousLinkEmail(process) {
  return get(process, "processingSendAnonymousLinkEmail");
}

/**
 * Return true if given atomUri is currently processing an update
 * @param process (full process from state)
 * @param atomUri
 * @returns {*}
 */

export function isAtomProcessingUpdate(process, atomUri) {
  return atomUri && getIn(process, ["atoms", atomUri, "processUpdate"]);
}

/**
 * Return true if given atomUri is currently loading
 * @param process (full process from state)
 * @param atomUri
 * @returns {*}
 */
export function isAtomLoading(process, atomUri) {
  return atomUri && getIn(process, ["atoms", atomUri, "loading"]);
}

export function isAtomLoaded(process, atomUri) {
  return atomUri && getIn(process, ["atoms", atomUri, "loaded"]);
}

export function isExternalDataLoading(process, uri) {
  return uri && getIn(process, ["externalData", uri, "loading"]);
}

export function isAtomProcessExisting(process, atomUri) {
  return atomUri && !!getIn(process, ["atoms", atomUri]);
}

/**
 * Return true if given atomUri is set toLoad
 * @param process (full process from state)
 * @param atomUri
 * @returns {*}
 */
export function isAtomToLoad(process, atomUri) {
  return atomUri && getIn(process, ["atoms", atomUri, "toLoad"]);
}

export function isConnectionContainerToLoad(process, atomUri) {
  return atomUri && getIn(process, ["connectionContainers", atomUri, "toLoad"]);
}

export function isConnectionContainerLoading(process, atomUri) {
  return (
    atomUri && getIn(process, ["connectionContainers", atomUri, "loading"])
  );
}

export function isConnectionContainerLoaded(process, atomUri) {
  return atomUri && getIn(process, ["connectionContainers", atomUri, "loaded"]);
}

/**
 * Return true if given atomUri has failedToLoad
 * @param process (full process from state)
 * @param atomUri
 * @returns {*}
 */
export function hasAtomFailedToLoad(process, atomUri) {
  return atomUri && getIn(process, ["atoms", atomUri, "failedToLoad"]);
}

/**
 * Return true if any atomUri is currently loading
 * @param process
 * @returns {boolean}
 */
export function isAnyAtomLoading(process) {
  const atomProcess = get(process, "atoms");

  return !!atomProcess.find((atomProcess, atomUri) =>
    isAtomLoading(process, atomUri)
  );
}

/**
 * Return true if any connectionContainer is currently loading
 * @param process
 * @returns {boolean}
 */
export function isAnyConnectionContainerLoading(process) {
  const connectionContainerProcess = get(process, "connectionContainers");

  return !!connectionContainerProcess.find((atomProcess, atomUri) =>
    isConnectionContainerLoading(process, atomUri)
  );
}

/**
 * Return true if given connUri is currently loading, if includeSubData is true, we also check the petriNetData and agreementData
 * as well
 * @param process (full process from state)
 * @param connUri
 * @param includeSubData (default=false, determines if the loading state should be checked for agreementData and petriNetData as well
 * @returns {*}
 */
export function isConnectionLoading(process, connUri, includeSubData = false) {
  if (
    includeSubData &&
    (isConnectionPetriNetDataLoading(process, connUri) ||
      isConnectionAgreementDataLoading(process, connUri))
  ) {
    return true;
  }

  return getIn(process, ["connections", connUri, "loading"]);
}

export function getResumeAfterUriForConnection(process, connUri) {
  return getIn(process, [
    "connections",
    connUri,
    "nextPage",
    "params",
    "resumeafter",
  ]);
}

/**
 * Return true if given atomUri has failedToLoad
 * @param process (full process from state)
 * @param atomUri
 * @returns {*}
 */
export function hasConnectionFailedToLoad(process, connUri) {
  return connUri && getIn(process, ["connections", connUri, "failedToLoad"]);
}

export function hasMessagesToLoad(process, connUri) {
  return !!getIn(process, ["connections", connUri, "nextPage"]);
}

/**
 * Return true if any connUri is currently loading, if includeSubData is true, we also check the petriNetData and agreementData
 * @param process
 * @param includeSubData (default=false, determines if the loading state should be checked for agreementData and petriNetData as well
 * @returns {boolean}
 */
export function isAnyConnectionLoading(process, includeSubData) {
  const connectionProcess = get(process, "connections");

  return !!connectionProcess.find((atomProcess, connUri) =>
    isConnectionLoading(process, connUri, includeSubData)
  );
}

/**
 * Return true if the petriNet-Data for the given connUri is currently loading
 * @param process
 * @param connUri
 * @returns {*}
 */
export function isConnectionPetriNetDataLoading(process, connUri) {
  return (
    connUri &&
    getIn(process, ["connections", connUri, "petriNetData", "loading"])
  );
}

// Return how many times petriNet-Data fetch has failed so far
export function getConnectionPetriNetDataFailCount(process, connUri) {
  return (
    (connUri &&
      getIn(process, ["connections", connUri, "petriNetData", "failCount"])) ||
    0
  );
}

/**
 * Return true if the petriNet-Data for the given connUri has been loaded
 * @param process
 * @param connUri
 * @returns {*}
 */
export function isConnectionPetriNetDataLoaded(process, connUri) {
  return (
    connUri &&
    getIn(process, ["connections", connUri, "petriNetData", "loaded"])
  );
}

export function isConnectionPetriNetDataDirty(process, connUri) {
  return (
    connUri && getIn(process, ["connections", connUri, "petriNetData", "dirty"])
  );
}

/**
 * Return true if the agreement-Data for the given connUri is currently loading
 * @param process
 * @param connUri
 * @returns {*}
 */
export function isConnectionAgreementDataLoading(process, connUri) {
  return (
    connUri &&
    getIn(process, ["connections", connUri, "agreementData", "loading"])
  );
}

// Return how many times agreement-Data fetch has failed so far
export function getConnectionAgreementDataFailCount(process, connUri) {
  return (
    (connUri &&
      getIn(process, ["connections", connUri, "agreementData", "failCount"])) ||
    0
  );
}

/**
 * Return true if the agreement-Dataset for the given connUri is currently loading
 * @param process
 * @param connUri
 * @returns {*}
 */
export function isConnectionAgreementDatasetLoading(process, connUri) {
  return (
    connUri &&
    getIn(process, ["connections", connUri, "agreementDataset", "loading"])
  );
}

// Return how many times agreement-Dataset fetch has failed so far
export function getConnectionAgreementDatasetFailCount(process, connUri) {
  return (
    (connUri &&
      getIn(process, [
        "connections",
        connUri,
        "agreementDataset",
        "failCount",
      ])) ||
    0
  );
}

/**
 * Return true if the agreement-Data for the given connUri has been loaded
 * @param process
 * @param connUri
 * @returns {*}
 */
export function isConnectionAgreementDataLoaded(process, connUri) {
  return (
    connUri &&
    getIn(process, ["connections", connUri, "agreementData", "loaded"])
  );
}

/**
 * Return true if the agreement-Data for the given connUri has been loaded
 * @param process
 * @param connUri
 * @returns {*}
 */
export function isConnectionAgreementDatasetLoaded(process, connUri) {
  return (
    connUri &&
    getIn(process, ["connections", connUri, "agreementDataset", "loaded"])
  );
}

export function isConnectionLoadingMessages(process, connUri) {
  return connUri && getIn(process, ["connections", connUri, "loadingMessages"]);
}

/**
 * Return true if there is any connUri that is currently loading messages
 * @param process
 * @param msgUri
 * @param connUri (default=undefined, if present just lookup the msgUri within the connUri, otherwise crawl every connection)
 * @returns {*}
 */
export function isAnyConnectionLoadingMessages(process) {
  return !!get(process, "connections").find((connProcess, connUri) =>
    isConnectionLoadingMessages(process, connUri)
  );
}

export function isExternalDataFetchNecessary(
  process,
  externalDataUri,
  externalDataEntry
) {
  if (externalDataUri) {
    if (!externalDataEntry) {
      return true;
    } else {
      return isExternalDataLoading(process, externalDataUri);
    }
  }
  return false;
}

export function isAtomFetchNecessary(process, atomUri, atom) {
  if (atomUri) {
    if (!atom) {
      return true;
    } else {
      return (
        isAtomToLoad(process, atomUri) &&
        !isAtomLoading(process, atomUri) &&
        !hasAtomFailedToLoad(process, atomUri)
      );
    }
  }
  return false;
}

export function getFetchTokenRequests(process, atomUri, tokenScopeUri) {
  return (
    getIn(process, ["fetchTokens", atomUri, tokenScopeUri, "requests"]) ||
    Immutable.List()
  );
}

export function getAtomRequests(process, atomUri) {
  return getIn(process, ["atoms", atomUri, "requests"]) || Immutable.List();
}

export function isAtomDeleted(process, atomUri) {
  return !!getAtomRequests(process, atomUri).find(
    request => get(request, "code") === 410
  );
}

export function areAtomRequestsAccessDeniedOnly(process, atomUri) {
  const atomRequests = getAtomRequests(process, atomUri);

  if (atomRequests.find(request => get(request, "code") === 200)) {
    return false;
  } else {
    return (
      atomRequests.size ===
      atomRequests.filter(request => get(request, "code") === 403).size
    );
  }
}

export function areConnectionContainerRequestsAccessDeniedOnly(
  process,
  atomUri
) {
  const connectionContainerRequests = getConnectionContainerRequests(
    process,
    atomUri
  );

  if (
    connectionContainerRequests.find(request => get(request, "code") === 200)
  ) {
    return false;
  } else {
    return (
      connectionContainerRequests.size ===
      connectionContainerRequests.filter(
        request => get(request, "code") === 403
      ).size
    );
  }
}

export function areConnectionContainerRequestsFailedOnly(process, atomUri) {
  const connectionContainerRequests = getConnectionContainerRequests(
    process,
    atomUri
  );

  if (
    connectionContainerRequests.find(request => get(request, "code") === 200)
  ) {
    return false;
  }

  return true;
}

export function getConnectionContainerRequests(process, atomUri) {
  return (
    getIn(process, ["connectionContainers", atomUri, "requests"]) ||
    Immutable.List()
  );
}

export function getConnectionRequests(process, connUri) {
  return (
    getIn(process, ["connections", connUri, "requests"]) || Immutable.List()
  );
}

function getRequestForSameCredentials(priorRequests, requestCredentials) {
  const requestTokenFromAtomUri = get(
    requestCredentials,
    "requestTokenFromAtomUri"
  );
  const scope = get(requestCredentials, "scope");
  const requesterWebId = get(requestCredentials, "requesterWebId");

  let foundRequest = undefined;
  if (priorRequests && priorRequests.size > 0 && requestCredentials) {
    foundRequest = priorRequests.find(
      request =>
        requestTokenFromAtomUri
          ? getIn(request, ["requestCredentials", "obtainedFrom", "scope"]) ===
              scope &&
            getIn(request, [
              "requestCredentials",
              "obtainedFrom",
              "requestTokenFromAtomUri",
            ]) === requestTokenFromAtomUri
          : getIn(request, ["requestCredentials", "requesterWebId"]) ===
            requesterWebId
    );
  }
  return foundRequest;
}

export function isUsedCredentials(priorRequests, requestCredentials) {
  return !!getRequestForSameCredentials(priorRequests, requestCredentials);
}

export function isUsedCredentialsUnsuccessfully(
  priorRequests,
  requestCredentials
) {
  const priorRequest = getRequestForSameCredentials(
    priorRequests,
    requestCredentials
  );

  return !!(priorRequest && get(priorRequest, "code") !== 200);
}

export function isUsedCredentialsSuccessfully(
  priorRequests,
  requestCredentials
) {
  const priorRequest = getRequestForSameCredentials(
    priorRequests,
    requestCredentials
  );

  return !!(priorRequest && get(priorRequest, "code") === 200);
}
