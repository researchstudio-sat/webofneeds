/**
 *
 * Created by ksinger on 06.07.2015.
 */

// enable es6 in jshint:
/* jshint esnext: true */

//---- app.js-Dependencies ----
import "babel-polyfill";
import svgs from "../images/won-icons/*.svg";
import "../style/won.scss";

import React from "react";
import ReactDOM from "react-dom";
import { applyMiddleware, createStore } from "redux";
import { composeWithDevTools } from "redux-devtools-extension";
import { HashRouter, Switch, Route } from "react-router-dom";
import "whatwg-fetch"; //polyfill for window.fetch (for backward-compatibility with older browsers)
import "redux";

//---------- Config -----------
// import {
//   configRouting,
//   runAccessControl,
//   registerEmailVerificationTrigger,
// } from "./configRouting.js";
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
import { Provider } from "react-redux";
import reducer from "./reducers/reducers.js";
import thunk from "redux-thunk";
import { piwikMiddleware } from "./piwik.js";
import { runMessagingAgent } from "./messaging-agent";
import Immutable from "immutable";
// import { runPushAgent } from "./push-agent";
// import PropTypes from "prop-types";

console.log(svgs);

window.won = won;

/*app.config(configRouting).config([
  "$compileProvider",
  "markedProvider",
  function($compileProvider, markedProvider) {
    const urlSanitizationRegex = /^\s*(https?|ftp|mailto|tel|file|blob|data):/;

    $compileProvider.aHrefSanitizationWhitelist(urlSanitizationRegex);
    markedProvider.setOptions({ sanitize: true });
    //removed this codesnippet due to problems with link rendering -> xss vulnerability
    markedProvider.setRenderer({
      link: function(href, title, text) {
        if (urlSanitizationRegex.test(href)) {
          if (text === href) {
            return (
              '<a href="' +
              href +
              '"' +
              (title ? ' title="' + title + '"' : "") +
              ' target="_blank" rel="noopener noreferrer">' +
              text +
              "</a>"
            );
          } else {
            return (
              "[" +
              text +
              '](<a href="' +
              href +
              '"' +
              (title ? ' title="' + title + '"' : "") +
              ' target="_blank" rel="noopener noreferrer">' +
              href +
              "</a>)"
            );
          }
        } else {
          return text;
        }
      },
    });
  },
]);*/

/*
* store enhancer that allows using the redux-devtools
* see https://github.com/zalmoxisus/redux-devtools-extension and
* https://www.npmjs.com/package/ng-redux#using-devtools for details.
*/
const composeEnhancers = composeWithDevTools({
  trace: true,
  traceLimit: 10,
  serialize: {
    immutable: Immutable,
  },
});

export const store = createStore(
  reducer,
  composeEnhancers(applyMiddleware(thunk, piwikMiddleware))
);

window.store4dbg = store;

export const states = [];

// Initialize Configuration (set IconColors, imprint etc.)
store.dispatch(actionCreators.config__init());

// app.run(["$ngRedux", $ngRedux => runMessagingAgent($ngRedux)]);
runMessagingAgent(store);

if (enableNotifications) {
  //app.run(["$ngRedux", $ngRedux => runPushAgent($ngRedux)]);
  // runPushAgent(store); // TODO: runPushAgent used to get $ngRedux and ngRedux had a connect method attached -> not sure if that can be applied to the given store though
}

// app.run(runAccessControl);

// app.run(registerEmailVerificationTrigger);

//check login status. TODO: this should actually be baked-in data (to avoid the extra roundtrip)
//app.run([ '$ngRedux', $ngRedux => $ngRedux.dispatch(actionCreators.verifyLogin())]); //FIXME: SEEMS LIKE THE verifyLogin function doesnt even exist anymore

// import { delay } from "./utils.js";
/*app.run([
  "$ngRedux",
  "$state",
  "$urlRouter",
  ($ngRedux, $uiRouterState, $urlRouter) => {
    $urlRouter.sync();
    delay(0).then(() => {
      //to make sure the the route is synchronised and in the state.
      $ngRedux.dispatch(actionCreators.initialPageLoad());
    });
  },
]);*/
// TODO: ALLOW EVERY PAGE TO BE ACCESSED WITHOUT A USER EXCEPT SETTINGS
// TODO: SIGNUP SHOULD REDIRECT TO INVENTORY IF THERE IS A LOGGED IN USER THAT IS NOT A PRIVATE USER
store.dispatch(actionCreators.initialPageLoad());

/*
 * this action-creator dispatches once per minute thus making
 * sure the gui is updated at least that often (so relative
 * timestamps are up-to-date)
 */
//app.run(["$ngRedux", $ngRedux => $ngRedux.dispatch(actionCreators.tick())]);
store.dispatch(actionCreators.tick());

ReactDOM.render(
  <Provider store={store}>
    <HashRouter hashType="hashbang">
      <Switch>
        <Route exact path="/">
          <PageInventory />
        </Route>
        <Route path="/create">
          <PageCreate />
        </Route>
        <Route path="/signup">
          <PageSignUp />
        </Route>
        <Route path="/about">
          <PageAbout />
        </Route>
        <Route path="/map">
          <PageMap />
        </Route>
        <Route path="/inventory">
          <PageInventory />
        </Route>
        <Route path="/connections">
          <PageConnections />
        </Route>
        <Route path="/overview">
          <PageOverview />
        </Route>
        <Route path="/post">
          <PagePost />
        </Route>
        <Route path="/settings">
          <PageSettings />
        </Route>
      </Switch>
    </HashRouter>
  </Provider>,
  document.getElementById("root")
);

/*<Route exact path="/"><PageInventory />:viewConnUri:token:privateId}

<Route path="/create"><PageCreate />:useCase:useCaseGroup:fromAtomUri:mode:holderUri:senderSocketType:targetSocketType
<Route path="/signup"><PageSignUp />
<Route path="/about"><PageAbout />:aboutSection
<Route path="/map"><PageMap />:viewConnUri
<Route path="/inventory"><PageInventory />:viewConnUri:token:privateId
<Route path="/connections"><PageConnections />:connectionUri:viewConnUri
<Route path="/overview"><PageOverview />:viewConnUri
<Route path="/post"><PagePost />:postUri:viewConnUri
<Route path="/settings"><PageSettings />
*/
