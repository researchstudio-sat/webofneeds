/**
 * Created by ksinger on 08.10.2015.
 */

import Immutable from "immutable";
import { actionCreators } from "./actions/actions.js";
import { accountLogin } from "./actions/account-actions.js";
import { getCurrentParamsFromRoute } from "./selectors/general-selectors.js";
import { privateId2Credentials } from "./won-utils.js";

import { get, getIn, firstToLowerCase, hyphen2Camel } from "./utils.js";

import * as accountUtils from "./account-utils.js";
import * as processUtils from "./process-utils.js";

/**
 * As we have configured our router to keep parameters unchanged,
 * that aren't mentioned in the `stateGo`-calls, you can use this
 * array to reset them explicitly (or merge anything else on top,
 * by using the `makeParams`-function).
 *
 * NOTE: WHEN INTRODUCING NEW PARAMETERS, ADD THEM HERE
 * AS WELL.
 */
export const resetParams = Object.freeze({
  connectionUri: undefined,
  viewAtomUri: undefined,
  viewConnUri: undefined,
  postUri: undefined,
  useCase: undefined,
  useCaseGroup: undefined,
  token: undefined,
  privateId: undefined,
  fromAtomUri: undefined,
  mode: undefined,
});

export const resetParamsImm = Immutable.fromJS(resetParams);

/**
 * Default Route
 */
export const defaultRoute = "connections";

/**
 * Adapted from https://github.com/neilff/redux-ui-router/blob/master/example/index.js
 * @param $urlRouterProvider
 * @param $stateProvider
 */
export const configRouting = [
  "$urlRouterProvider",
  "$stateProvider",
  ($urlRouterProvider, $stateProvider) => {
    $urlRouterProvider.otherwise(($injector, $location) => {
      $location.path(`/${defaultRoute}`); // change route to connections overview as default

      const origParams = $location.search();
      if (origParams) {
        const onlyConstParams = addConstParams({}, origParams);
        //updatedRoute.search(onlyConstParams); // strip all but "constant" parameters
        $location.search(onlyConstParams);
      }

      //return updatedRoute;
    });

    [
      { path: "/about?aboutSection", component: "about" },
      { path: "/overview?viewAtomUri?viewConnUri", component: "overview" },
      { path: "/map?viewAtomUri?viewConnUri", component: "map" },
      { path: "/signup", component: "signup" },
      { path: "/settings", component: "settings" },
      {
        path:
          "/connections?privateId?postUri?connectionUri?useCase?useCaseGroup?token?viewAtomUri?viewConnUri?fromAtomUri?mode",
        component: "connections",
        as: "connections",
      },
      {
        path: "/post/?postUri?viewAtomUri?viewConnUri",
        component: "post",
        as: "post",
      },
    ].forEach(({ path, component, as }) => {
      const cmlComponent = hyphen2Camel(component);

      if (!path) path = `/${component}`;
      if (!as) as = firstToLowerCase(cmlComponent);

      $stateProvider.state(as, {
        url: path,
        templateUrl: `./app/components/${component}/${component}.html`,
        // template: `<${component}></${component>` TODO use directives instead of view+ctrl
        controller: `${cmlComponent}Controller`,
        controllerAs: "self",
        scope: {},
      });
    });
  },
];

export const runAccessControl = [
  "$transitions",
  "$rootScope",
  "$ngRedux",
  ($transitions, $rootScope, $ngRedux) => {
    //TODO use access-control provided by $transitions.onStart()
    $rootScope.$on(
      "$stateChangeStart",
      (event, toState, toParams, fromState, fromParams, options) =>
        accessControl({
          event,
          toState,
          toParams,
          fromState,
          fromParams,
          options,
          dispatch: $ngRedux.dispatch,
          getState: $ngRedux.getState,
        })
    );
  },
];

export function accessControl({
  event,
  toState,
  toParams,
  fromParams,
  dispatch,
  getState,
}) {
  reactToPrivateIdChanges(
    fromParams["privateId"],
    toParams["privateId"],
    dispatch,
    getState
  );

  const state = getState();
  const errorString =
    'Tried to access view "' +
    (toState && toState.name) +
    "\" that won't work" +
    "without logging in. Blocking route-change.";
  switch (toState.name) {
    case defaultRoute:
    case "connections": {
      //If we know the user is not loggedIn and there is a postUri in the route, we link to the post-visitor view
      const postUriFromRoute = toParams["postUri"];
      if (
        !accountUtils.isLoggedIn(get(state, "account")) &&
        !!postUriFromRoute
      ) {
        dispatch(actionCreators.router__stateGoResetParams(defaultRoute));
      }
      return;
    }

    case "post": {
      const postUriFromRoute = toParams["postUri"];

      if (!postUriFromRoute) {
        dispatch(actionCreators.router__stateGoResetParams(defaultRoute));
      }
      return;
    }

    case "settings":
      if (!accountUtils.isLoggedIn(get(state, "account"))) {
        if (event) {
          event.preventDefault();
        }
        dispatch(actionCreators.router__stateGoResetParams(defaultRoute));
      }
      return;

    case "overview":
    case "map":
    case "signup":
    case "about":
      return; // can always access these pages.

    default:
      //FOR ALL OTHER ROUTES
      if (!processUtils.isProcessingInitialLoad(get(state, "process"))) {
        if (accountUtils.isLoggedIn(get(state, "account"))) {
          return; // logged in. continue route-change as intended.
        } else {
          //sure to be logged out
          if (event) {
            event.preventDefault();
          } else {
            // this is a check with a route that's already fixed, redirect to defaultRoute instead.
            dispatch(actionCreators.router__stateGoResetParams(defaultRoute));
          }

          console.error(errorString);
        }
      } else {
        // still loading
        return; // no access control while loading. the page-load actions will call `accessControl` once the logged-in status has been checked
      }
  }
}

/**
 * Checks if the current route should be accessible
 * and redirects if not.
 * @param dispatch
 * @param getState
 * @returns {*}
 */
export function checkAccessToCurrentRoute(dispatch, getState) {
  const appState = getState();
  let routingState = getIn(appState, ["router", "currentState"]);
  let params = getCurrentParamsFromRoute(appState);
  return accessControl({
    toState: routingState,
    fromState: routingState,
    toParams: params,
    fromParams: params,
    dispatch,
    getState,
  });
}

function reactToPrivateIdChanges(
  fromPrivateId,
  toPrivateId,
  dispatch,
  getState
) {
  const state = getState();

  const { email } = toPrivateId ? privateId2Credentials(toPrivateId) : {};
  if (processUtils.isProcessingLogin(get(state, "process"))) {
    console.debug(
      "There's already a login in process with the email " +
        email +
        " derived from the privateId " +
        toPrivateId +
        "."
    );
    return Promise.resolve();
  }

  if (processUtils.isProcessingLogout(get(state, "process"))) {
    // already logging out
    return Promise.resolve();
  }

  //If there is a toPrivateId param and it is different than the old one we process a login for that privateId regardless
  if (toPrivateId && fromPrivateId !== toPrivateId) {
    // privateId has changed or was added
    const credentials = {
      privateId: toPrivateId,
    };
    return accountLogin(credentials)(dispatch, getState);
  }
}

/**
 * Merges any "constant"-parameters (e.g. `privateId`) that are contained in `paramsInState`
 * with the `params` object and returns the result.
 *
 * @param params
 * @param paramsInState
 * @returns {*}
 */
export function addConstParams(params, paramsInState) {
  const paramsInStateImm = Immutable.fromJS(paramsInState); // ensure that they're immutable
  const currentConstParams = Immutable.Map(
    [].map(p => [p, paramsInStateImm.get(p)]) // [ [ paramName, paramValue] ]
  );
  return currentConstParams.merge(params).toJS();
}
