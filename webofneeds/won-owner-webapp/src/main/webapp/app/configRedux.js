/**
 * Created by ksinger on 08.10.2015.
 */

import Immutable from "immutable";

import reducer from "./reducers/reducers.js";
import thunk from "redux-thunk";

import { piwikMiddleware } from "./piwik.js";

import { devToolsEnhancer } from "redux-devtools-extension";

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
      devToolsEnhancer({
        trace: true,
        traceLimit: 10,
        serialize: {
          immutable: Immutable,
        },
      }),
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
