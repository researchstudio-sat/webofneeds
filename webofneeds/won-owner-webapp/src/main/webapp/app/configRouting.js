/**
 * Created by ksinger on 08.10.2015.
 */

import won from "./won-es6.js";
import Immutable from "immutable";
import { actionTypes, actionCreators } from "./actions/actions.js";
import { accountLogin } from "./actions/account-actions.js";

import { privateId2Credentials } from "./won-utils.js";

import { selectAllNeeds } from "./selectors.js";

import {
  decodeUriComponentProperly,
  getIn,
  firstToLowerCase,
  hyphen2Camel,
} from "./utils.js";

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
  postUri: undefined,
  showCreateView: undefined,
  useCase: undefined,
  //sendAdHocRequest: undefined,
  // privateId: undefined,  // global parameter that we don't want to lose. never reset this one.
});

export const resetParamsImm = Immutable.fromJS(resetParams);

/**
 * These should not accidentally be removed from the state. See the `stateGo*`-action creators
 * in `actions.js`
 * @type {string[]}
 */
export const constantParams = ["privateId"];

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
    //$urlRouterProvider.otherwise('/landingpage');
    $urlRouterProvider.otherwise(($injector, $location) => {
      //let updatedRoute =  $location.replace()
      //    .path('/landingpage') // change route to landingpage
      $location.path(`/${defaultRoute}`); // change route to connections overview as default

      const origParams = $location.search();
      if (origParams) {
        const onlyConstParams = addConstParams({}, origParams);
        //updatedRoute.search(onlyConstParams); // strip all but "constant" parameters
        $location.search(onlyConstParams);
      }

      //return updatedRoute;
    });

    //make sure create-need is called with a draftId
    //$urlRouterProvider.when('/create-need/', [() => '/create-need/' + getRandomPosInt()]);

    [
      { path: "/about?privateId", component: "about" },
      { path: "/signup?privateId", component: "signup" },
      { path: "/landingpage?privateId", component: "landingpage" },
      {
        path:
          "/connections?privateId?postUri?connectionUri?showCreateView?useCase",
        component: "connections",
        as: "connections",
      },
      { path: "/post/?privateId?postUri", component: "post", as: "post" },
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

    $urlRouterProvider.when("/settings/", "/settings/general");

    $stateProvider
      .state("settings", {
        url: "/settings",
        templateUrl: "./app/components/settings/settings.html",
      })
      .state("settings.avatars", {
        url: "/avatars",
        template: `<won-avatar-settings></won-avatar-settings>`,
      })
      .state("settings.general", {
        url: "/general",
        template: `<won-general-settings></won-general-settings>`,
      });
  },
];

function postViewEnsureLoaded(dispatch, getState, encodedPostUri) {
  //console.log('in postViewEnsureLoaded');
  const postUri = decodeUriComponentProperly(encodedPostUri);
  const state = getState();

  if (postUri && !selectAllNeeds(state).has(postUri)) {
    /*
         * got an uri but no post loaded to the state yet ->
         * assuming that if you're logged in you either did a
         * page-reload with a valid session or signed in, thus
         * loading your own needs as part of the `initialPageLoad`
         * or `login` action-creators. Thus we assume
         * you loaded the app in some other view,
         * got a link to a non-owned need and pasted it. Thus
         * the `initiaPageLoad` didn't load this need yet. Also
         * we can be sure it's not your need and load it as `theirNeed`.
         */
    won
      .getNeed(postUri)
      .then(need =>
        dispatch({
          type: actionTypes.router.accessedNonLoadedPost,
          payload: Immutable.fromJS({ theirNeed: need }),
        })
      )
      .catch(error => {
        console.log(
          `Failed to load need ${postUri}.`,
          `Reverting to previous router-state.`,
          `Error: `,
          error
        );
        dispatch(actionCreators.router__back());
      });
  }
}

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
    case "post": //Route the 'post' no matter if you are logged in or not since it is accessible at all times
      postViewEnsureLoaded(dispatch, getState, toParams.postUri);
      break;

    case defaultRoute: //Route the 'default' view at all times
      // if(
      //     state.get('initialLoadFinished') &&  // no access control while still loading
      //     getIn(state, ['user', 'loggedIn']))
      // {
      //     //logged in -- re-initiate route-change
      //     console.log("Admiral Ackbar mentioned that this would be a trap, so we will link you to the connections");
      //     if(event) {
      //         event.preventDefault()
      //     } else {
      //         dispatch(
      //             actionCreators.router__stateGoAbs(defaultRoute)
      //         )
      //     }
      // }
      // break;
      return; // default route should be always accessible

    case "signup":
    case "about":
    case "connections":
      return; // can always access this page.

    default:
      //FOR ALL OTHER ROUTES
      if (state.get("initialLoadFinished")) {
        if (state.getIn(["user", "loggedIn"])) {
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
  let params = getIn(appState, ["router", "currentParams"]);
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
  if (state.get("loginInProcessFor")) {
    console.info(
      "There's already a login in process with the email " +
        email +
        " derived from the privateId " +
        toPrivateId +
        "."
    );
    return Promise.resolve();
  }

  if (state.get("logoutInProcess")) {
    // already logging out
    return Promise.resolve();
  }

  // v--- do any login-actions only when privateId is added after initialPageLoad. The latter should handle any necessary logins itself.
  if (state.get("initialLoadFinished")) {
    if (fromPrivateId !== toPrivateId) {
      // privateId has changed or was added
      const credentials = { privateId: toPrivateId };
      const options = { doRedirects: false };
      return accountLogin(credentials, options)(dispatch, getState);
    }
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
    constantParams.map(p => [p, paramsInStateImm.get(p)]) // [ [ paramName, paramValue] ]
  );
  return currentConstParams.merge(params).toJS();
}

/**
 * As we have configured our router to keep parameters unchanged,
 * that aren't mentioned in the `stateGo`-calls, you can use this
 * function to reset all parameters not mentioned in the arguments
 * and set those to their values.
 * @param params: object with params that should be placed
 * in the url.
 */
//export function makeParams(params) {
//    const currentParams = getState().getIn(['router', 'currentParams']);
//    const currentConstParams = Immutable.Map(
//        constantParams.map(p => [p, currentParams.get(p)]) // [ [ paramName, paramValue] ]
//    );
//    return Immutable.Map().merge(params).merge(currentConstParams).toJS();
//    //let resetParamsCopy = Object.assign({}, resetParams);
//    //return Object.assign(resetParamsCopy, params);
//}
export const makeParams = undefined;
