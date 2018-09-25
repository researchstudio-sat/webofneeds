/**
 *
 * Created by ksinger on 06.07.2015.
 */

// enable es6 in jshint:
/* jshint esnext: true */

//---- app.js-Dependencies ----
import angular from "angular";
window.angular = angular; // for compatibility with pre-ES6/commonjs scripts

import "fetch"; //polyfill for window.fetch (for backward-compatibility with older browsers)

import "redux";
import ngReduxModule from "ng-redux";
import ngReduxRouterModule from "redux-ui-router";
import uiRouterModule from "angular-ui-router";

/* angular-ui-router-shim (release/stateEvents.js) used to enable legacy $stateChange* events in ui-router (see
 * here for details: https://ui-router.github.io/guide/ng1/migrate-to-1_0#state-change-events)
 * 
 * delete at your own peril
 */
import "angular-ui-router-shim";
import { delay, inlineSVGSpritesheet } from "./utils.js";

//---------- Config -----------
import { configRouting, runAccessControl } from "./configRouting.js";
import configRedux from "./configRedux.js";

//--------- Actions -----------
import { actionCreators } from "./actions/actions.js";

//-------- Components ---------
import topnav from "./components/topnav.js";
import connectionsComponent from "./components/connections/connections.js";
import postComponent from "./components/post/post.js";
import aboutComponent from "./components/about/about.js";
import signupComponent from "./components/signup/signup.js";

//settings
import settingsTitleBarModule from "./components/settings-title-bar.js";
import avatarSettingsModule from "./components/settings/avatar-settings.js";
import generalSettingsModule from "./components/settings/general-settings.js";

//won import (used so you can access the debugmode variable without reloading the page)
import won from "./service/won.js";
window.won = won;

/* TODO this fragment is part of an attempt to sketch a different
 * approach to asynchronity (Remove it or the thunk-based
 * solution afterwards)
 */
import { runMessagingAgent } from "./messaging-agent.js";

//viewer-modules
import personViewerModule from "./components/details/viewer/person-viewer.js";
import descriptionViewerModule from "./components/details/viewer/description-viewer.js";
import locationViewerModule from "./components/details/viewer/location-viewer.js";
import tagsViewerModule from "./components/details/viewer/tags-viewer.js";
import travelActionViewerModule from "./components/details/viewer/travel-action-viewer.js";
import titleViewerModule from "./components/details/viewer/title-viewer.js";
import numberViewerModule from "./components/details/viewer/number-viewer.js";
import priceViewerModule from "./components/details/viewer/price-viewer.js";
import datetimeViewerModule from "./components/details/viewer/datetime-viewer.js";
import dropdownViewerModule from "./components/details/viewer/dropdown-viewer.js";
import selectViewerModule from "./components/details/viewer/select-viewer.js";
import rangeViewerModule from "./components/details/viewer/range-viewer.js";
import fileViewerModule from "./components/details/viewer/file-viewer.js";
import workflowViewerModule from "./components/details/viewer/workflow-viewer.js";
import petrinetViewerModule from "./components/details/viewer/petrinet-viewer.js";

const viewerModules = [
  personViewerModule,
  descriptionViewerModule,
  locationViewerModule,
  tagsViewerModule,
  travelActionViewerModule,
  titleViewerModule,
  numberViewerModule,
  priceViewerModule,
  datetimeViewerModule,
  dropdownViewerModule,
  selectViewerModule,
  rangeViewerModule,
  fileViewerModule,
  workflowViewerModule,
  petrinetViewerModule,
];

//picker-modules
import descriptionPickerModule from "./components/details/picker/description-picker.js";
import locationPickerModule from "./components/details/picker/location-picker.js";
import personPickerModule from "./components/details/picker/person-picker.js";
import travelActionPickerModule from "./components/details/picker/travel-action-picker.js";
import tagsPickerModule from "./components/details/picker/tags-picker.js";
import titlePickerModule from "./components/details/picker/title-picker.js";
import numberPickerModule from "./components/details/picker/number-picker.js";
import pricePickerModule from "./components/details/picker/price-picker.js";
import datetimePickerModule from "./components/details/picker/datetime-picker.js";
import datetimeRangePickerModule from "./components/details/picker/datetime-range-picker.js";
import dropdownPickerModule from "./components/details/picker/dropdown-picker.js";
import selectPickerModule from "./components/details/picker/select-picker.js";
import rangePickerModule from "./components/details/picker/range-picker.js";
import priceRangePickerModule from "./components/details/picker/price-range-picker.js";
import filePickerModule from "./components/details/picker/file-picker.js";
import workflowPickerModule from "./components/details/picker/workflow-picker.js";
import petrinetPickerModule from "./components/details/picker/petrinet-picker.js";

const pickerModules = [
  descriptionPickerModule,
  locationPickerModule,
  personPickerModule,
  travelActionPickerModule,
  tagsPickerModule,
  titlePickerModule,
  numberPickerModule,
  pricePickerModule,
  datetimePickerModule,
  datetimeRangePickerModule,
  dropdownPickerModule,
  selectPickerModule,
  rangePickerModule,
  priceRangePickerModule,
  filePickerModule,
  workflowPickerModule,
  petrinetPickerModule,
];

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

  //views
  connectionsComponent,
  postComponent,
  aboutComponent,
  signupComponent,

  //views.settings
  settingsTitleBarModule,
  avatarSettingsModule,
  generalSettingsModule,

  ...viewerModules,
  ...pickerModules,
]);

/* create store, register middlewares, set up redux-devtool-support, etc */
configRedux(app);

app.config(configRouting).config([
  "$compileProvider",
  "markedProvider",
  function($compileProvider, markedProvider) {
    $compileProvider.aHrefSanitizationWhitelist(
      /^\s*(https?|ftp|mailto|tel|file|blob|data):/
    );
    markedProvider.setOptions({ sanitize: true });
    markedProvider.setRenderer({
      link: function(href, title, text) {
        return (
          '<a href="' +
          href +
          '"' +
          (title ? ' title="' + title + '"' : "") +
          ' target="_blank">' +
          text +
          "</a>"
        );
      },
    });
  },
]);
app.run(["$ngRedux", $ngRedux => runMessagingAgent($ngRedux)]);

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

//let app = angular.module('won.owner',[...other modules...]);
angular.bootstrap(document, ["won.owner"], {
  // make sure dependency injection works after minification (or
  // at least angular explains about sloppy imports with a
  // reference to the right place)
  // see https://docs.angularjs.org/guide/production
  // and https://docs.angularjs.org/guide/di#dependency-annotation
  strictDi: true,
});

//inlineSVGSpritesheet("./generated/icon-sprite.svg", "icon-sprite");
inlineSVGSpritesheet("./generated/symbol/svg/sprite.symbol.svg", "icon-sprite");
