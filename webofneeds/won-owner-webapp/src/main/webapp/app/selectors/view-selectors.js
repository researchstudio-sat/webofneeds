/**
 * Created by quasarchimaere on 21.01.2019.
 */
import { get } from "../utils.js";
import * as viewUtils from "../view-utils.js";

/**
 * Check if showSlideIns is true
 * @param state (full redux-state)
 * @returns {*}
 */
export function showSlideIns(state) {
  return viewUtils.showSlideIns(get(state, "view"));
}

export function isAnonymousLinkSent(state) {
  return viewUtils.isAnonymousLinkSent(get(state, "view"));
}

export function isAnonymousLinkCopied(state) {
  return viewUtils.isAnonymousLinkCopied(get(state, "view"));
}

export function isAnonymousSlideInExpanded(state) {
  return viewUtils.isAnonymousSlideInExpanded(get(state, "view"));
}

export function showAnonymousSlideInEmailInput(state) {
  return viewUtils.showAnonymousSlideInEmailInput(get(state, "view"));
}
