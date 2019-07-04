/**
 *
 * Created by ksinger on 06.07.2015.
 */

// enable es6 in jshint:
/* jshint esnext: true */

//---- app.js-Dependencies ----
import "babel-polyfill";
import svgs from "../images/won-icons/*.svg";

console.log(svgs);

import "../style/won.scss";

import angular from "angular";
window.angular = angular; // for compatibility with pre-ES6/commonjs scripts

import "whatwg-fetch"; //polyfill for window.fetch (for backward-compatibility with older browsers)

import "redux";
import ngReduxModule from "ng-redux";
import ngReduxRouterModule from "redux-ui-router";
import uiRouterModule from "angular-ui-router";

/* angular-ui-router-shim (release/stateEvents.js) used to enable legacy $stateChange* events in ui-router (see
 * here for details: https://ui-router.github.io/guide/ng1/migrate-to-1_0#state-change-events)
 * 
 * delete at your own peril
 */
import "angular-ui-router/release/stateEvents.js";
import { delay } from "./utils.js";

//---------- Config -----------
import { configRouting, runAccessControl } from "./configRouting.js";
import configRedux from "./configRedux.js";

//--------- Actions -----------
import { actionCreators } from "./actions/actions.js";

//-------- Components ---------
import topnav from "./components/topnav.js";
import menu from "./components/menu.js";
import footer from "./components/footer.js";
import modalDialog from "./components/modal-dialog.js";
import toasts from "./components/toasts.js";
import slideIn from "./components/slide-in.js";
import postInfo from "./components/post-info.js";
import connectionsComponent from "./pages/connections.jsx";
import overviewComponent from "./pages/overview.jsx";
import mapComponent from "./pages/map.jsx";
import postComponent from "./pages/post.jsx";
import aboutComponent from "./pages/about.jsx";
import signupComponent from "./pages/signup.jsx";
import settingsComponent from "./pages/settings.jsx";
import inventoryComponent from "./pages/inventory.jsx";
import createComponent from "./pages/create.jsx";

//won import (used so you can access the debugmode variable without reloading the page)
import won from "./service/won.js";
window.won = won;

/* TODO this fragment is part of an attempt to sketch a different
 * approach to asynchronity (Remove it or the thunk-based
 * solution afterwards)
 */
import { runMessagingAgent } from "./messaging-agent.js";

import detailModules from "./components/details/details.js";
import { runPushAgent } from "./push-agent";
import { enableNotifications } from "../config/default";

let app = angular.module("won.owner", [
  /* to enable legacy $stateChange* events in ui-router (see
     * here for details: https://ui-router.github.io/guide/ng1/migrate-to-1_0#state-change-events)
     */
  "ui.router.state.events",

  ngReduxModule,
  uiRouterModule,
  ngReduxRouterModule,

  //components
  topnav,
  menu,
  footer,
  modalDialog,
  toasts,
  slideIn,
  postInfo,

  //details
  ...detailModules,

  //views
  connectionsComponent.module,
  overviewComponent.module,
  mapComponent.module,
  postComponent.module,
  aboutComponent.module,
  signupComponent.module,
  settingsComponent.module,
  inventoryComponent.module,
  createComponent.module,
]);

/* create store, register middlewares, set up redux-devtool-support, etc */
configRedux(app);

app.config(configRouting).config([
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
              ' target="_blank">' +
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
              ' target="_blank">' +
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
]);
app.run(["$ngRedux", $ngRedux => runMessagingAgent($ngRedux)]);

if (enableNotifications) {
  app.run(["$ngRedux", $ngRedux => runPushAgent($ngRedux)]);
}

app.run([
  "$ngRedux",
  $ngRedux => $ngRedux.dispatch(actionCreators.config__init()),
]);

app.run(runAccessControl);

//check login status. TODO: this should actually be baked-in data (to avoid the extra roundtrip)
//app.run([ '$ngRedux', $ngRedux => $ngRedux.dispatch(actionCreators.verifyLogin())]);
app.run([
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
]);

/*
 * this action-creator dispatches once per minute thus making
 * sure the gui is updated at least that often (so relative
 * timestamps are up-to-date)
 */
app.run(["$ngRedux", $ngRedux => $ngRedux.dispatch(actionCreators.tick())]);

/**
 * create the parent element for angular. This could live in the build config, but since it is very angular specific, it should probably live here
 */
document.addEventListener("DOMContentLoaded", () => {
  const uiView = document.createElement("section");
  uiView.setAttribute("ui-view", "");
  document.body.appendChild(uiView);

  //let app = angular.module('won.owner',[...other modules...]);
  angular.bootstrap(document, ["won.owner"], {
    // make sure dependency injection works after minification (or
    // at least angular explains about sloppy imports with a
    // reference to the right place)
    // see https://docs.angularjs.org/guide/production
    // and https://docs.angularjs.org/guide/di#dependency-annotation
    strictDi: true,
  });
});
