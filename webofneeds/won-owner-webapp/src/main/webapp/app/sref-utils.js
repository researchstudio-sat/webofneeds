/**
 * Created by ksinger on 21.08.2017.
 */

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
 * Retrieves parameters from the url-bar or parses them from a passed url.
 * @param url
 * @returns {{}}
 */
function getParameters(url) {
  const url_ = url ? url : window.location.href; // e.g. url_ = "http://example.org/?privateId=5kpskm09-ocri63&foo=bar&asdf"
  const [, paramsString] = url_.split("?"); // e.g. paramsString = "privateId=5kpskm09-ocri63&foo=bar&asdf"

  if (!paramsString) {
    // no parameters present
    return {};
  }

  const paramsKconstray = paramsString
    .split("&") // e.g. ["privateId=5kpskm09-ocri63", "foo=bar", "asdf"]
    .map(p => p.split("=")) // e.g. [["privateId", "5kpskm09-ocri63"], ["foo", "bar"], ["asdf"]]
    .filter(p => p.length === 2); // filter out parameter that's not a proper key-value pair, e.g. "asdf"

  // create object from kv-pairs
  const params = {};
  paramsKconstray.forEach(kv => (params[kv[0]] = kv[1]));

  return params;
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
