/**
 * Created by ksinger on 08.10.2015.
 */

import Immutable from "immutable";
import { actionCreators } from "./actions/actions.js";
// import { accountLogin } from "./actions/account-actions.js";
import { getCurrentParamsFromRoute } from "./redux/selectors/general-selectors.js";
// import * as wonUtils from "./won-utils.js";

import { get, getIn } from "./utils.js";
import * as accountUtils from "./redux/utils/account-utils.js";
// import * as processUtils from "./redux/utils/process-utils.js";

// import settingsComponent from "./pages/settings.jsx";
// import postComponent from "./pages/post.jsx";
// import overviewComponent from "./pages/overview.jsx";
// import mapComponent from "./pages/map.jsx";
// import createComponent from "./pages/create.jsx";
// import aboutComponent from "./pages/about.jsx";
// import connectionsComponent from "./pages/connections.jsx";
// import signupComponent from "./pages/signup.jsx";
// import inventoryComponent from "./pages/inventory.jsx";

// import jsxRenderer from "@depack/render";

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
  viewConnUri: undefined,
  postUri: undefined,
  useCase: undefined,
  useCaseGroup: undefined,
  token: undefined,
  privateId: undefined,
  fromAtomUri: undefined,
  senderSocketType: undefined,
  targetSocketType: undefined,
  mode: undefined,
});

export const resetParamsImm = Immutable.fromJS(resetParams);

/**
 * Default Route
 */
export const defaultRoute = "inventory";

/**
 * Adapted from https://github.com/neilff/redux-ui-router/blob/master/example/index.js
 * @param $urlRouterProvider
 * @param $stateProvider
 */
// export const configRouting = [
//   "$urlRouterProvider",
//   "$stateProvider",
//   ($urlRouterProvider, $stateProvider) => {
//     $urlRouterProvider.otherwise(($injector, $location) => {
//       $location.path(`/${defaultRoute}`); // change route to connections overview as default
//
//       const origParams = $location.search();
//       if (origParams) {
//         $location.search(addConstParams({}, origParams));
//       }
//
//       //return updatedRoute;
//     });
//
//     [
//       {
//         path: "/signup",
//         component: signupComponent,
//         as: "signup",
//       },
//       {
//         path: "/about?aboutSection",
//         component: aboutComponent,
//         as: "about",
//       },
//       {
//         path: "/map?viewConnUri",
//         component: mapComponent,
//         as: "map",
//       },
//       {
//         path:
//           "/create?useCase?useCaseGroup?fromAtomUri?mode?holderUri?senderSocketType?targetSocketType",
//         component: createComponent,
//         as: "create",
//       },
//       {
//         path: "/inventory?viewConnUri?token?privateId",
//         component: inventoryComponent,
//         as: "inventory",
//       },
//       {
//         path: "/connections?connectionUri?viewConnUri",
//         component: connectionsComponent,
//         as: "connections",
//       },
//       {
//         path: "/overview?viewConnUri",
//         component: overviewComponent,
//         as: "overview",
//       },
//       {
//         path: "/post/?postUri?viewConnUri",
//         component: postComponent,
//         as: "post",
//       },
//       {
//         //path: "/settings{settingsParams:.*}",
//         path: "/settings",
//         component: settingsComponent,
//         as: "settings",
//       },
//     ].forEach(({ path, component, as }) => {
//       $stateProvider.state(as, {
//         url: path,
//         template: component.template.children
//           .map(vnode => jsxRenderer(vnode))
//           .join(""),
//         controller: component.controller,
//         controllerAs: "self",
//         scope: {},
//       });
//     });
//   },
// ];

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

export function accessControl(/*{
  event,
  toState,
  toParams,
  fromParams,
  dispatch,
  getState,
}*/) {
  return;
  // reactToPrivateIdChanges(
  //   fromParams && fromParams["privateId"],
  //   toParams && toParams["privateId"],
  //   dispatch,
  //   getState
  // );
  //
  // const state = getState();
  // const errorString =
  //   'Tried to access view "' +
  //   (toState && toState.name) +
  //   "\" that won't work" +
  //   "without logging in. Blocking route-change.";
  // switch (toState.name) {
  //   case "post": {
  //     const postUriFromRoute = toParams["postUri"];
  //
  //     if (!postUriFromRoute) {
  //       dispatch(actionCreators.router__stateGoResetParams(defaultRoute));
  //     }
  //     return;
  //   }
  //
  //   case "settings":
  //     if (!accountUtils.isLoggedIn(get(state, "account"))) {
  //       if (event) {
  //         event.preventDefault();
  //       }
  //       dispatch(actionCreators.router__stateGoResetParams(defaultRoute));
  //     }
  //     return;
  //
  //   case defaultRoute:
  //   case "connections":
  //   case "create":
  //   case "inventory":
  //   case "overview":
  //   case "map":
  //   case "signup":
  //   case "about":
  //     return; // can always access these pages.
  //
  //   default:
  //     //FOR ALL OTHER ROUTES
  //     if (!processUtils.isProcessingInitialLoad(get(state, "process"))) {
  //       if (accountUtils.isLoggedIn(get(state, "account"))) {
  //         return; // logged in. continue route-change as intended.
  //       } else {
  //         //sure to be logged out
  //         if (event) {
  //           event.preventDefault();
  //         } else {
  //           // this is a check with a route that's already fixed, redirect to defaultRoute instead.
  //           dispatch(actionCreators.router__stateGoResetParams(defaultRoute));
  //         }
  //
  //         console.error(errorString);
  //       }
  //     } else {
  //       // still loading
  //       return; // no access control while loading. the page-load actions will call `accessControl` once the logged-in status has been checked
  //     }
  // }
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

/*function reactToPrivateIdChanges(
  fromPrivateId,
  toPrivateId,
  dispatch,
  getState
) {
  const state = getState();

  const { email } = toPrivateId
    ? wonUtils.privateId2Credentials(toPrivateId)
    : {};
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
}*/

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

/**
 * Checks if a email-verification-token has been added
 * and if so, initiates the verification process.
 */
export const registerEmailVerificationTrigger = [
  "$rootScope",
  "$ngRedux",
  ($rootScope, $ngRedux) => {
    $rootScope.$on("$stateChangeStart", (
      event,
      toState,
      toParams,
      fromState,
      fromParams
      //, options
    ) => verifyEmailIfNecessary(toParams, fromParams, $ngRedux));
  },
];

function verifyEmailIfNecessary(toParams, fromParams, $ngRedux) {
  const dispatch = $ngRedux.dispatch;
  const state = $ngRedux.getState();
  const accountState = get(state, "account");
  const isEmailVerified = accountUtils.isEmailVerified(accountState);
  const emailVerificationError = accountUtils.getEmailVerificationError(
    accountState
  );

  const previousToken = get(fromParams, "token");
  const verificationToken = get(toParams, "token");
  const tokenHasChanged =
    verificationToken && previousToken !== verificationToken;

  const verificationNeeded = !(isEmailVerified || emailVerificationError);

  const alreadyProcessing = getIn(state, [
    "process",
    "processingVerifyEmailAddress",
  ]);
  // const alreadyProcessing = processSelectors.isProcessingVerifyEmailAddress(
  //   state
  // );

  if (tokenHasChanged && !alreadyProcessing && verificationNeeded) {
    dispatch(actionCreators.account__verifyEmailAddress(verificationToken));
  }
}
