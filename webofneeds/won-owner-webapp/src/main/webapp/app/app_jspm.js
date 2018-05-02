/**
 *
 * Created by ksinger on 06.07.2015.
 */

// enable es6 in jshint:
/* jshint esnext: true */

//---- app.js-Dependencies ----
import angular from 'angular';
window.angular = angular; // for compatibility with pre-ES6/commonjs scripts

import 'fetch'; //polyfill for window.fetch (for backward-compatibility with older browsers)

import 'redux';
import ngReduxModule from 'ng-redux';
import ngReduxRouterModule from 'redux-ui-router';
import uiRouterModule from 'angular-ui-router';

/* angular-ui-router-shim (release/stateEvents.js) used to enable legacy $stateChange* events in ui-router (see
 * here for details: https://ui-router.github.io/guide/ng1/migrate-to-1_0#state-change-events)
 * 
 * delete at your own peril
 */
import uiRouterShimModule from 'angular-ui-router-shim';
import {
    camel2Hyphen,
    hyphen2Camel,
    firstToLowerCase,
    delay,
    parseSVG,
    inlineSVGSpritesheet,
} from './utils.js';

//---------- Config -----------
import { configRouting, runAccessControl } from './configRouting.js';
import configRedux from './configRedux.js';

//--------- Actions -----------
import { actionCreators }  from './actions/actions.js';

//-------- Components ---------
import topnav from './components/topnav.js';
import connectionsComponent from './components/connections/connections.js';
import postComponent from './components/post/post.js';
import landingPageComponent from './components/landingpage/landingpage.js';
import aboutComponent from './components/about/about.js';
import signupComponent from './components/signup/signup.js';


//settings
import settingsTitleBarModule from './components/settings-title-bar.js';
import avatarSettingsModule from './components/settings/avatar-settings.js';
import generalSettingsModule from './components/settings/general-settings.js';

//won import (used so you can access the debugmode variable without reloading the page)
import won from './service/won.js';
window.won = won;


/* TODO this fragment is part of an attempt to sketch a different
 * approach to asynchronity (Remove it or the thunk-based
 * solution afterwards)
 */
import { runMessagingAgent } from './messaging-agent.js';

let app = angular.module('won.owner', [
    /* to enable legacy $stateChange* events in ui-router (see
     * here for details: https://ui-router.github.io/guide/ng1/migrate-to-1_0#state-change-events)
     */
    'ui.router.state.events',


    ngReduxModule,
    uiRouterModule,
    ngReduxRouterModule,

    //components
    topnav,

    //views
    connectionsComponent,
    postComponent,
    landingPageComponent,
    aboutComponent,
    signupComponent,

    //views.settings
    settingsTitleBarModule,
    avatarSettingsModule,
    generalSettingsModule,

]);

/* create store, register middlewares, set up redux-devtool-support, etc */
configRedux(app);

app.filter('filterByNeedState', function(){
    return function(needs,state){
        var filtered =[];
        angular.forEach(needs,function(need){
            if(need['won:isInState']['@id'] == state){
                filtered.push(need);
            }
        });

        return filtered;
    }
})
    .filter('filterEventByType', function(){
        return function(events,uri,type){
            var filtered =[];
            angular.forEach(events,function(event){
                if(event.hasReceiverNeed == uri && event.eventType == type){
                    filtered.push(event);
                }
            });

            return filtered;
        }
    })
    /*Filters All events so that only the ones with textMessages remain*/
    .filter('filterByEventMsgs', function(){
        return function(events){
            var filtered =[];
            angular.forEach(events,function(event){
                if(event.hasTextMessage !== undefined ||
                    (event.hasCorrespondingRemoteMessage && event.hasCorrespondingRemoteMessage.hasTextMessage )){
                    filtered.push(event);
                }
            });

            return filtered;
        }
    })

app.config(configRouting);
app.run([ '$ngRedux', $ngRedux => runMessagingAgent($ngRedux) ]);


app.run([ '$ngRedux', $ngRedux =>
    $ngRedux.dispatch(actionCreators.config__init())
]);

app.run(runAccessControl);

//check login status. TODO: this should actually be baked-in data (to avoid the extra roundtrip)
//app.run([ '$ngRedux', $ngRedux => $ngRedux.dispatch(actionCreators.verifyLogin())]);
app.run([ '$ngRedux', '$state', '$urlRouter', ($ngRedux, $uiRouterState, $urlRouter) => {
    $urlRouter.sync();
    delay(0).then(() => { //to make sure the the route is synchronised and in the state.
        $ngRedux.dispatch(actionCreators.initialPageLoad())
    })
}]);

/*
 * this action-creator dispatches once per minute thus making
 * sure the gui is updated at least that often (so relative
 * timestamps are up-to-date)
 */
app.run([ '$ngRedux', $ngRedux => $ngRedux.dispatch(actionCreators.tick())]);

//let app = angular.module('won.owner',[...other modules...]);
angular.bootstrap(document, ['won.owner'], {
    // make sure dependency injection works after minification (or
    // at least angular explains about sloppy imports with a
    // reference to the right place)
    // see https://docs.angularjs.org/guide/production
    // and https://docs.angularjs.org/guide/di#dependency-annotation
    strictDi: true
});


//inlineSVGSpritesheet("./generated/icon-sprite.svg", "icon-sprite");
inlineSVGSpritesheet("./generated/symbol/svg/sprite.symbol.svg", "icon-sprite");
