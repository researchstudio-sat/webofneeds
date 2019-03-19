/**
 * Created by quasarchimaere on 22.01.2019.
 *
 * This Utils-File contains methods to check the status/vars stored in our view state.
 * These methods should be used in favor of accessing the view-state directly in the component
 */

//import won from "./won-es6.js";
import { get, getIn } from "./utils.js";

/**
 * Check if showSlideIns is true
 * @param state (view-state)
 * @returns {*}
 */
export function showSlideIns(viewState) {
  return get(viewState, "showSlideIns");
}

export function showModalDialog(viewState) {
  return get(viewState, "showModalDialog");
}

export function showRdf(viewState) {
  return get(viewState, "showRdf");
}

export function showClosedNeeds(viewState) {
  return get(viewState, "showClosedNeeds");
}

export function isAnonymousLinkSent(viewState) {
  return getIn(viewState, ["anonymousSlideIn", "linkSent"]);
}

export function isAnonymousLinkCopied(viewState) {
  return getIn(viewState, ["anonymousSlideIn", "linkCopied"]);
}

export function isAnonymousSlideInExpanded(viewState) {
  return getIn(viewState, ["anonymousSlideIn", "expanded"]);
}

export function showAnonymousSlideInEmailInput(viewState) {
  return getIn(viewState, ["anonymousSlideIn", "showEmailInput"]);
}

/**
 * Return the visible Tab of a certain need, and if there was no tab stored in the state, return
 * the notFoundTab, which defaults to DETAIL
 * @param viewState
 * @param needUri
 * @param notFoundTab
 * @returns {*|string}
 */
export function getVisibleTabByNeedUri(
  viewState,
  needUri,
  notFoundTab = "DETAIL"
) {
  const tab = getIn(viewState, ["needs", needUri, "visibleTab"]);
  return tab || notFoundTab;
}
