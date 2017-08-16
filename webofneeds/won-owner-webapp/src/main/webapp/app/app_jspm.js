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
import {
    camel2Hyphen,
    hyphen2Camel,
    firstToLowerCase,
    delay,
} from './utils';

//---------- Config -----------
import { configRouting, runAccessControl } from './configRouting';
import configRedux from './configRedux';

//--------- Actions -----------
import { actionCreators }  from './actions/actions';

//-------- Components ---------
import topnav from './components/topnav';
import createNeedComponent from './components/create-need/create-need';
import overviewIncomingRequestsComponent from './components/overview-incoming-requests/overview-incoming-requests';
import overviewSentRequestsComponent from './components/overview-sent-requests/overview-sent-requests';
import postComponent from './components/post/post';
import landingPageComponent from './components/landingpage/landingpage';
import overviewPostsComponent from './components/overview-posts/overview-posts';
import feedComponent from './components/feed/feed';
import overviewMatchesComponent from './components/overview-matches/overview-matches';
import aboutComponent from './components/about/about';

//settings
import settingsTitleBarModule from './components/settings-title-bar';
import avatarSettingsModule from './components/settings/avatar-settings';
import generalSettingsModule from './components/settings/general-settings';

//won import (used so you can access the debugmode variable without reloading the page)
import won from './service/won';
window.won = won;


/* TODO this fragment is part of an attempt to sketch a different
 * approach to asynchronity (Remove it or the thunk-based
 * solution afterwards)
 */
import { runMessagingAgent } from './messaging-agent';

let app = angular.module('won.owner', [
    ngReduxModule,
    uiRouterModule,
    ngReduxRouterModule,

    //components
    topnav,

    //views
    createNeedComponent,
    overviewIncomingRequestsComponent,
    overviewSentRequestsComponent,
    postComponent,
    landingPageComponent,
    overviewPostsComponent,
    feedComponent,
    overviewMatchesComponent,
    aboutComponent,

    //views.settings
    settingsTitleBarModule,
    avatarSettingsModule,
    generalSettingsModule,

]);

app.config([ '$ngReduxProvider', configRedux ]);
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
