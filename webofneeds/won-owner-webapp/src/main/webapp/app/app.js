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
import PropTypes from "prop-types";
import { applyMiddleware, createStore } from "redux";
import { composeWithDevTools } from "redux-devtools-extension";
import * as generalSelectors from "./redux/selectors/general-selectors.js";
import {
  HashRouter,
  Switch,
  Route,
  useLocation,
  useHistory,
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
import PageActivities from "./pages/react/activities";
import PageOverview from "./pages/react/overview";
import PagePost from "./pages/react/post";
import PageSettings from "./pages/react/settings";
import PageForgotPassword from "./pages/react/forgotPassword";
import { Provider, useSelector, useDispatch } from "react-redux";
import reducer from "./reducers/reducers.js";
import thunk from "redux-thunk";
import { piwikMiddleware } from "./piwik.js";
import { runMessagingAgent } from "./messaging-agent";
import Immutable from "immutable";
import { getQueryParams, generateLink } from "./utils.js";
import * as accountUtils from "./redux/utils/account-utils.js";
import * as processUtils from "./redux/utils/process-utils.js";
import { runPushAgent } from "./push-agent";
import ico_loading_anim from "~/images/won-icons/ico_loading_anim.svg";
import WonModalDialog from "~/app/components/modal-dialog";
import * as viewSelectors from "~/app/redux/selectors/view-selectors";

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

function AppRoutes({ processState }) {
  const dispatch = useDispatch();
  const history = useHistory();
  const location = useLocation();
  const accountState = useSelector(generalSelectors.getAccountState);
  const showModalDialog = useSelector(viewSelectors.showModalDialog);

  const isLoggedIn = accountUtils.isLoggedIn(accountState);
  const hasLoginError = !!accountUtils.getLoginError(accountState);
  const isAnonymous = accountUtils.isAnonymous(accountState);

  const { postUri, token, privateId, requireLogin } = getQueryParams(location);

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

  if (requireLogin && !loginProcessing) {
    dispatch(
      actionCreators.account__requireLogin(() => {
        dispatch(actionCreators.view__hideModalDialog());
        history.replace(
          generateLink(history.location, { requireLogin: undefined })
        );
      })
    );

    return (
      <Switch>
        <Route path="/">
          <main className="ownerloading">
            {showModalDialog && <WonModalDialog />}
            <svg className="ownerloading__spinner hspinner">
              <use xlinkHref={ico_loading_anim} href={ico_loading_anim} />
            </svg>
            <span className="ownerloading__label">Login required...</span>
          </main>
        </Route>
      </Switch>
    );
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
      <Route path="/forgotPassword">
        {isLoggedIn && !isAnonymous ? (
          <Redirect to="/" />
        ) : (
          <PageForgotPassword />
        )}
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
      <Route path="/activities">
        <PageActivities />
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
AppRoutes.propTypes = { processState: PropTypes.object.isRequired };

function App() {
  const processState = useSelector(generalSelectors.getProcessState);

  if (processUtils.isProcessingInitialLoad(processState)) {
    return (
      <main className="ownerloading">
        <svg className="ownerloading__spinner hspinner">
          <use xlinkHref={ico_loading_anim} href={ico_loading_anim} />
        </svg>
        <span className="ownerloading__label">Gathering your Atoms...</span>
      </main>
    );
  } else {
    return (
      <HashRouter hashType="hashbang">
        <AppRoutes processState={processState} />
      </HashRouter>
    );
  }
}

ReactDOM.render(
  <Provider store={store}>
    <App />
  </Provider>,
  document.getElementById("root")
);
