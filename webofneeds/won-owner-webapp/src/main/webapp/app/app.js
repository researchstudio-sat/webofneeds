/**
 *
 * Created by ksinger on 06.07.2015.
 */

// enable es6 in jshint:
/* jshint esnext: true */

//---- app.js-Dependencies ----
import "babel-polyfill";
import "../style/won.scss";

import React from "react";
import ReactDOM from "react-dom";
import { applyMiddleware, createStore } from "redux";
import { composeWithDevTools } from "redux-devtools-extension";
import * as generalSelectors from "./redux/selectors/general-selectors.js";
import {
  HashRouter,
  Switch,
  Route,
  useLocation,
  Redirect,
} from "react-router-dom";
import "whatwg-fetch"; //polyfill for window.fetch (for backward-compatibility with older browsers)
import "redux";

// //--------- Actions -----------
import { actionCreators } from "./actions/actions.js";

//won import (used so you can access the debugmode variable without reloading the page)
import won from "./service/won.js";
import { enableNotifications } from "../config/default";
import PageInventory from "./pages/react/inventory";
import PageSignUp from "./pages/react/signup";
import PageAbout from "./pages/react/about";
import PageMap from "./pages/react/map";
import PageCreate from "./pages/react/create";
import PageConnections from "./pages/react/connections";
import PageOverview from "./pages/react/overview";
import PagePost from "./pages/react/post";
import PageSettings from "./pages/react/settings";
import { Provider, useSelector, useDispatch } from "react-redux";
import reducer from "./reducers/reducers.js";
import thunk from "redux-thunk";
import { piwikMiddleware } from "./piwik.js";
import { runMessagingAgent } from "./messaging-agent";
import Immutable from "immutable";
import { getQueryParams } from "./utils.js";
import * as accountUtils from "./redux/utils/account-utils.js";
import * as processUtils from "./redux/utils/process-utils.js";
import { runPushAgent } from "./push-agent";

window.won = won;

/*
* store enhancer that allows using the redux-devtools
* see https://github.com/zalmoxisus/redux-devtools-extension and
* https://github.com/reduxjs/redux-devtools for details.
*/
const composeEnhancers = composeWithDevTools({
  trace: true,
  traceLimit: 10,
  serialize: {
    immutable: Immutable,
  },
});

console.debug("create reduxStore");
export const store = createStore(
  reducer,
  composeEnhancers(applyMiddleware(thunk, piwikMiddleware))
);

window.store4dbg = store;

// Initialize Configuration (set IconColors, imprint etc.)
console.debug("dispatch config__init");
store.dispatch(actionCreators.config__init());

console.debug("runMessagingAgent");
runMessagingAgent(store);

if (enableNotifications) {
  console.debug("runPushAgent");
  runPushAgent(store); // TODO: runPushAgent used to get $ngRedux and ngRedux had a connect method attached -> not sure if that can be applied to the given store though
}

// Initiate the initial load
store.dispatch(actionCreators.initialPageLoad());

/*
 * this action-creator dispatches once per minute thus making
 * sure the gui is updated at least that often (so relative
 * timestamps are up-to-date)
 */
//app.run(["$ngRedux", $ngRedux => $ngRedux.dispatch(actionCreators.tick())]);
store.dispatch(actionCreators.tick());

function AppRoutes() {
  const dispatch = useDispatch();
  const location = useLocation();
  const accountState = useSelector(generalSelectors.getAccountState);
  const processState = useSelector(generalSelectors.getProcessState);

  const isLoggedIn = accountUtils.isLoggedIn(accountState);
  const hasLoginError = !!accountUtils.getLoginError(accountState);
  const isAnonymous = accountUtils.isAnonymous(accountState);

  const { postUri, token, privateId } = getQueryParams(location);

  //******************************** EMAIL TOKEN VERIFICATION
  const isEmailVerified = accountUtils.isEmailVerified(accountState);
  const emailVerificationError = accountUtils.getEmailVerificationError(
    accountState
  );

  const verificationNeeded = !(isEmailVerified || emailVerificationError);

  const alreadyProcessing = processUtils.isProcessingVerifyEmailAddress(
    processState
  );
  const loginProcessing = processUtils.isProcessingLogin(processState);

  if (token && !alreadyProcessing && verificationNeeded) {
    console.debug("Dispatching account__verifyEmailAddress");
    dispatch(actionCreators.account__verifyEmailAddress(token));
  }

  if (privateId && !loginProcessing && !isLoggedIn && !hasLoginError) {
    console.debug("Dispatchin privateId Login");
    dispatch(actionCreators.account__login({ privateId: privateId }));
  }
  //********************************

  return (
    <Switch>
      <Route exact path="/">
        <PageInventory />
      </Route>
      <Route path="/create">
        <PageCreate />
      </Route>
      <Route path="/signup">
        {isLoggedIn && !isAnonymous ? <Redirect to="/" /> : <PageSignUp />}
      </Route>
      <Route path="/about">
        <PageAbout />
      </Route>
      <Route path="/map">
        <PageMap />
      </Route>
      <Route path="/inventory">
        {privateId ? <Redirect to="/" /> : <PageInventory />}
      </Route>
      <Route path="/connections">
        <PageConnections />
      </Route>
      <Route path="/overview">
        <PageOverview />
      </Route>
      <Route path="/post">{postUri ? <PagePost /> : <Redirect to="/" />}</Route>
      <Route path="/settings">
        {isLoggedIn ? <PageSettings /> : <Redirect to="/" />}
      </Route>
    </Switch>
  );
}

ReactDOM.render(
  <Provider store={store}>
    <HashRouter hashType="hashbang">
      <AppRoutes />
    </HashRouter>
  </Provider>,
  document.getElementById("root")
);

/*<Route exact path="/"><PageInventory />:token:privateId}

<Route path="/create"><PageCreate />:useCase:useCaseGroup:fromAtomUri:mode:holderUri:senderSocketType:targetSocketType
<Route path="/signup"><PageSignUp />
<Route path="/about"><PageAbout />:aboutSection
<Route path="/map"><PageMap />
<Route path="/inventory"><PageInventory />:token:privateId
<Route path="/connections"><PageConnections />:connectionUri
<Route path="/overview"><PageOverview />
<Route path="/post"><PagePost />:postUri
<Route path="/settings"><PageSettings />
*/
