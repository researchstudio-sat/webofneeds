/**
 * Created by ksinger on 08.10.2015.
 */

import reducer from "./reducers/reducers.js";
import thunk from "redux-thunk";

import { piwikMiddleware } from "./piwik.js";

import { devToolsEnhancer } from "redux-devtools-extension";

import { getIn, zipWith } from "./utils.js";

export default function configRedux(appModule) {
  appModule.config(createStore);
  appModule.run(ensureDevToolChangesGetRendered);
}

createStore.$inject = ["$ngReduxProvider"];
function createStore($ngReduxProvider) {
  $ngReduxProvider.createStoreWith(
    reducer,
    [
      /* middlewares, that wrap the reducer 
           *(they get the state, can do stuff, apply the reducer, do stuff and return a modified state) 
           */
      "ngUiRouterMiddleware",
      thunk,
      piwikMiddleware,
    ],
    [
      /* store enhancers (i.e. f::store->store') */
      /*
            * store enhancer that allows using the redux-devtools
            * see https://github.com/zalmoxisus/redux-devtools-extension and
            * https://www.npmjs.com/package/ng-redux#using-devtools for details.
            */
      devToolsEnhancer(),
      // Specify name here, actionsBlacklist, actionsCreators and other options if needed
    ]
  );
}

ensureDevToolChangesGetRendered.$inject = [
  "$ngRedux",
  "$rootScope",
  "$timeout",
];
function ensureDevToolChangesGetRendered($ngRedux, $rootScope, $timeout) {
  // To reflect state changes when disabling/enabling actions via the monitor
  // there is probably a smarter way to achieve that.
  // snippet adapted from https://www.npmjs.com/package/ng-redux#using-devtools.
  if (window.__REDUX_DEVTOOLS_EXTENSION__) {
    $ngRedux.subscribe(() => {
      $timeout(() => {
        $rootScope.$apply(() => {});
      }, 100);
    });
  }
}

/**
 * Connects a component to ng-redux, sets up watches for the
 * properties that `selectFromState` depends on and handles
 * cleanup when the component is destroyed.
 * @param selectFromState
 * @param actionCreators
 * @param properties
 * @param ctrl a controller/component with `$scope` and `$ngRedux` attached
 */
export function connect2Redux(
  selectFromState,
  actionCreators,
  properties,
  ctrl
) {
  const disconnectRdx = ctrl.$ngRedux.connect(
    selectFromState,
    actionCreators
  )(ctrl);
  const disconnectProps = reduxSelectDependsOnProperties(
    properties,
    selectFromState,
    ctrl
  );
  ctrl.$scope.$on("$destroy", () => {
    disconnectRdx();
    disconnectProps();
  });
}

/**
 * Makes sure the select-statement is reevaluated, should
 * one of the watched fields change.
 *
 * example usage:
 * ```
 * reduxSelectDependsOnProperties(['self.atomUri', 'self.timestamp'], selectFromState, this)
 * ```
 *
 * @param properties a list of watch expressions
 * @param selectFromState same as $ngRedux.connect
 * @param ctrl the controller to bind the results to. needs to have `$ngRedux` and `$scope` attached.
 * @returns {*}
 * @returns a function to unregister the watch
 */
function reduxSelectDependsOnProperties(properties, selectFromState, ctrl) {
  const firstVals = properties.map(p => getIn(ctrl.$scope, p.split(".")));
  let firstTime = true;
  return ctrl.$scope.$watchGroup(properties, (newVals, oldVals) => {
    if ((firstTime && !arrEq(newVals, firstVals)) || !arrEq(newVals, oldVals)) {
      const state = ctrl.$ngRedux.getState();
      const stateSlice = selectFromState(state);
      Object.assign(ctrl, stateSlice);
    }
    if (firstTime) {
      firstTime = false;
    }
  });
}

/**
 * compares two arrays and checks if their contents are equal
 */
function arrEq(xs, ys) {
  return (
    xs.length === ys.length &&
    all(
      //elementwise comparison
      zipWith((x, y) => x === y, xs, ys)
    )
  );
}

function all(boolArr) {
  return boolArr.reduce((b1, b2) => b1 && b2, true);
}
