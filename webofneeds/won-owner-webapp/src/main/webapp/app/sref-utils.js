/**
 * Created by ksinger on 21.08.2017.
 */

import { getParameters } from "./utils.js";

import {
  defaultRoute,
  resetParamsImm,
  addConstParams,
} from "./configRouting.js";

/**
 * e.g. `absSRef('post', {postUri: 'http://...'})` + pre-existing private Id =>
 *     `"post({postUri: 'http://..', privatId: '...'})"`
 * NOTE: depending on your version of ui-router, interpolation might not update the hrefs
 * @param toRouterState
 * @param queryParams
 * @returns {string} a string that can be used with `ui-sref` and then the same
 *                   behaviour as e.g. `ng-click="self.router__stateGoAbs('post', {postUri: '...'})"`
 *                   except that it supports middle-mouse-button clicks and shows
 *                   the right cursor by default (i.e. it is a regular link)
 */
export function absSRef(toRouterState, queryParams) {
  //const currentParams = getParameters();
  //const paramsWithConst = addConstParams(resetParamsImm.merge(queryParams), currentParams);
  const paramsWithConst = absParams(queryParams);
  const paramsString = JSON.stringify(paramsWithConst);
  const srefString = toRouterState + "(" + paramsString + ")";
  return srefString;
}

/**
 * Generates an object that contains the constant parameters unless
 * explicitly overridden in queryParams. This can be used together
 * with `ui-state-params` (and thus `ui-state`)
 * NOTE: depending on your version of ui-router, interpolation might not update the hrefs
 * @param queryParams
 * @returns {*}
 */
export function absParams(queryParams) {
  const currentParams = getParameters();
  const paramsWithConst = addConstParams(
    resetParamsImm.merge(queryParams),
    currentParams
  );
  return paramsWithConst;
}

/**
 * Generates an href-string using `$state.href` that makes sure
 * to keep constant parameters (unless they're explicitly overwritten
 * in `queryParams`)
 * @param $state
 * @param toRouterState
 * @param queryParams
 * @returns {*}
 */
export function absHRef($state, toRouterState, queryParams) {
  return $state.href(toRouterState, absParams(queryParams));
}

/**
 * Generates an object that only contains any "constant"
 * parameters that were already present.
 * NOTE: depending on your version of ui-router, interpolation might not update the hrefs
 * @returns {*}
 */
export function onlyConstParams() {
  const currentParams = getParameters();
  return addConstParams(resetParamsImm, currentParams);
}

/**
 * Generates an href that links to `toRouterState` but
 * removes all params except for the "constant" ones.
 * @param $state
 * @param toRouterState
 * @returns {*}
 */
export function resetParamsHRef($state, toRouterState) {
  return $state.href(toRouterState, onlyConstParams());
}

export function defaultRouteHRef($state) {
  return resetParamsHRef($state, defaultRoute);
}
