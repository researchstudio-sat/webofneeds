/**
 * Created by ksinger on 21.08.2017.
 */

import { getParameters } from "./utils.js";

import { resetParamsImm, addConstParams } from "./configRouting.js";

/**
 * Generates an object that contains the constant parameters unless
 * explicitly overridden in queryParams. This can be used together
 * with `ui-state-params` (and thus `ui-state`)
 * NOTE: depending on your version of ui-router, interpolation might not update the hrefs
 * @param queryParams
 * @returns {*}
 */
function absParams(queryParams) {
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
