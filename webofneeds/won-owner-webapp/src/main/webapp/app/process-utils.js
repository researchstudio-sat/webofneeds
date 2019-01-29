/**
 * Created by quasarchimaere on 22.01.2019.
 *
 * This Utils-File contains methods to check the status/vars stored in our process state.
 * These methods should be used in favor of accessing the process-state directly in the component
 */

//import won from "./won-es6.js";
import { get, getIn } from "./utils.js";

/**
 * Return true if processingInitialLoad is currently active
 * @param process (full process from state)
 * @returns {*}
 */
export function isProcessingInitialLoad(process) {
  return get(process, "processingInitialLoad");
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
 * Return true if given needUri is currently loading
 * @param process (full process from state)
 * @param needUri
 * @returns {*}
 */
export function isNeedLoading(process, needUri) {
  return needUri && getIn(process, ["needs", needUri, "loading"]);
}

/**
 * Return true if any needUri is currently loading
 * @param process
 * @returns {boolean}
 */
export function isAnyNeedLoading(process) {
  const needProcess = get(process, "needs");

  return !!needProcess.find((needProcess, needUri) =>
    isNeedLoading(process, needUri)
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

/**
 * Return true if any connUri is currently loading, if includeSubData is true, we also check the petriNetData and agreementData
 * @param process
 * @param includeSubData (default=false, determines if the loading state should be checked for agreementData and petriNetData as well
 * @returns {boolean}
 */
export function isAnyConnectionLoading(process, includeSubData) {
  const connectionProcess = get(process, "connections");

  return !!connectionProcess.find((needProcess, connUri) =>
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

/**
 * Return true if the msgUri is currently loading
 * @param process
 * @param msgUri
 * @param connUri (default=undefined, if present just lookup the msgUri within the connUri, otherwise crawl every connection)
 * @returns {*}
 */
export function isMessageLoading(process, msgUri, connUri = undefined) {
  if (connUri) {
    return (
      msgUri &&
      getIn(process, ["connections", connUri, "messages", msgUri, "loading"])
    );
  }

  return !!get(process, "connections")
    .flatMap(conn => conn.get("messages"))
    .find(msgProcess => msgProcess.get("loading"));
}

/**
 * Return true if the any msgUri is currently loading, if connUri is present just check messages within this connUri-process
 * @param process
 * @param msgUri
 * @param connUri (default=undefined, if present just lookup the msgUri within the connUri, otherwise crawl every connection)
 * @returns {*}
 */
export function isAnyMessageLoading(process, connUri = undefined) {
  if (connUri) {
    const msgProcess = getIn(process, ["connections", connUri, "messages"]);

    return !!msgProcess.find((msgProcess, msgUri) =>
      isMessageLoading(process, msgUri, connUri)
    );
  }

  return !!get(process, "connections").find((connProcess, connUri) =>
    isAnyMessageLoading(process, connUri)
  );
}
