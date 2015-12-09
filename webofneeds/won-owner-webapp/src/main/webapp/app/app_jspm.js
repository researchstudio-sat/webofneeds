/**
 *
 * Created by ksinger on 06.07.2015.
 */

// enable es6 in jshint:
/* jshint esnext: true */

console.log('System.import working');


import angular from 'angular';
window.angular = angular; // for compatibility with pre-ES6/commonjs scripts

// Components
import topnav from './components/topnav';
import createNeedComponent from './components/create-need/create-need';
import overviewIncomingRequestsComponent from './components/overview-incoming-requests/overview-incoming-requests';
import matchesComponent from './components/matches/matches';
import postVisitorComponent from './components/post-visitor/post-visitor';
import { camel2Hyphen, hyphen2Camel, firstToLowerCase } from './utils';
import landingPageComponent from './components/landingpage/landingpage';
import overviewPostsComponent from './components/overview-posts/overview-posts';
import feedComponent from './components/feed/feed';
import overviewMatchesComponent from './components/overview-matches/overview-matches';

import configRouting from './configRouting';
import configRedux from './configRedux';


/* TODO this fragment is part of an attempt to sketch a different
 * approach to asynchronity (Remove it or the thunk-based
 * solution afterwards)
 */
import { runMessagingAgent } from './messaging-agent';

import 'fetch'; //polyfill for window.fetch (for backward-compatibility with older browsers)

//settings
import settingsTitleBarModule from './components/settings-title-bar';
import avatarSettingsModule from './components/settings/avatar-settings';
import generalSettingsModule from './components/settings/general-settings';


import 'redux';
import ngReduxModule from 'ng-redux';

import { actionCreators }  from './actions/actions';

import ngReduxRouterModule from 'redux-ui-router';
import uiRouterModule from 'angular-ui-router';

let app = angular.module('won.owner', [
    ngReduxModule,
    uiRouterModule,
    ngReduxRouterModule,

    //components
    topnav, //used in rework.html/index.html

    //views
    createNeedComponent,
    overviewIncomingRequestsComponent,
    matchesComponent,
    postVisitorComponent,
    landingPageComponent,
    overviewPostsComponent,
    feedComponent,
    overviewMatchesComponent,

    //views.settings
    settingsTitleBarModule,
    avatarSettingsModule,
    generalSettingsModule,

]);

app.config([ '$ngReduxProvider', configRedux ]);
app.config([ '$urlRouterProvider', '$stateProvider', configRouting ]);
app.run([ '$ngRedux', $ngRedux => runMessagingAgent($ngRedux) ]);
//app.run([ '$ngRedux', $ngRedux => $ngRedux.dispatch(actionCreators.runMessagingAgent()) ]);



app.run([ '$ngRedux', $ngRedux =>
    $ngRedux.dispatch(actionCreators.config__init())
]);


//let app = angular.module('won.owner',[...other modules...]);
angular.bootstrap(document, ['won.owner'], {
    // make sure dependency injection works after minification (or
    // at least angular explains about sloppy imports with a
    // reference to the right place)
    // see https://docs.angularjs.org/guide/production
    // and https://docs.angularjs.org/guide/di#dependency-annotation
    strictDi: true
});
