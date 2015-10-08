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
import appTag from './components/wonAppTag';
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


//settings
import settingsTitleBarModule from './components/settings-title-bar';
import avatarSettingsModule from './components/settings/avatar-settings';
import generalSettingsModule from './components/settings/general-settings';


import reducer from './reducers/reducers';
import 'redux';
import { combineReducers } from 'redux-immutablejs';
import Immutable from 'immutable';
import thunk from 'redux-thunk';
import ngReduxModule from 'ng-redux';

import { actionCreators }  from './actions';

import ngReduxRouterModule from 'redux-ui-router';
import uiRouterModule from 'angular-ui-router';

let app = angular.module('won.owner', [
    ngReduxModule,
    uiRouterModule,
    ngReduxRouterModule,
    appTag,
    topnav, //used in index.html
    createNeedComponent,
    overviewIncomingRequestsComponent,
    matchesComponent,
    postVisitorComponent,
    landingPageComponent,
    overviewPostsComponent,
    feedComponent,
    overviewMatchesComponent,

    //settings
    settingsTitleBarModule,
    avatarSettingsModule,
    generalSettingsModule,
]);

app.config([
    '$ngReduxProvider',
    '$urlRouterProvider', '$stateProvider' /*of routerstate*/,
    ( $ngReduxProvider, $urlRouterProvider, $stateProvider) => {
        configRedux($ngReduxProvider);
        configRouting($urlRouterProvider, $stateProvider);
    }
]);

function configRedux($ngReduxProvider) {
    const loggingReducer = (state, action) => {
        console.log('changing state from ',
            state && state.toJS?
                state.toJS() : state);
        const updatedState = reducer(state, action);
        console.log('changed state to ',
            updatedState && updatedState.toJS?
                updatedState.toJS() : updatedState);
        return updatedState;
    }
    window.thunk = thunk;
    $ngReduxProvider.createStoreWith(loggingReducer, ['ngUiRouterMiddleware', thunk,/* middlewares here, e.g. 'promiseMiddleware', loggingMiddleware */]);
}

/**
 * Adapted from https://github.com/neilff/redux-ui-router/blob/master/example/index.js
 * @param $urlRouterProvider
 * @param $stateProvider
 */

function configRouting($urlRouterProvider, $stateProvider) {
    $urlRouterProvider.otherwise('/landingpage');

    [
        { path: '/landingpage', component: 'landingpage' },
        { path: '/create-need/:draftId', component: 'create-need' },
        { path: '/feed', component: 'feed' },
        //{ path: '/settings', component: 'settings' },
        { path: '/overview/matches', component: 'overview-matches', as: 'overviewMatches' },
        { path: '/overview/incoming-requests', component: 'overview-incoming-requests', as: 'overviewIncomingRequests' },
        { path: '/overview/posts', component: 'overview-posts', as: 'overviewPosts' },
        { path: '/post/:postId/owner/matches', component: 'landingpage', as: 'postMatches' }, //TODO implement view
        { path: '/post/:postId/visitor', component: 'landingpage', as: 'postVisitor' }, //TODO implement view

    ].forEach( ({path, component, as}) => {

            const cmlComponent = hyphen2Camel(component);

            if(!path) path = `/${component}`;
            if(!as) as = firstToLowerCase(cmlComponent);

            $stateProvider.state(as, {
                url: path,
                templateUrl: `./app/components/${component}/${component}.html`,
                // template: `<${component}></${component>` TODO use directives instead of view+ctrl
                controller: `${cmlComponent}Controller`,
                controllerAs: 'self'
            });
    })
    $urlRouterProvider.when('/settings/', '/settings/general');
    $stateProvider
        .state('settings', {
            url: '/settings',
            templateUrl: './app/components/settings/settings.html'
        })
        .state('settings.avatars', {
            url: '/avatars',
            template: `<won-avatar-settings></won-avatar-settings>`
        })
        .state('settings.general', {
            url: '/general',
            template: `<won-general-settings></won-general-settings>`
        })

    $stateProvider
        .state('routerDemo', {
            url: '/router-demo/:demoVar',
            template: `
                <p>demoVar = {{self.state.getIn(['router', 'currentParams', 'demoVar'])}}</p>
                <div>
                    <a ui-sref="routerDemo.childA">~A~</a> |
                    <a ui-sref="routerDemo.childB">~B~</a>
                </div>
                <div ui-view></div>
            `,
            controller: 'DemoController',
            controllerAs: 'self'
        })
        .state('routerDemo.childA', {
            url: '/router-demo/:demoVar/childA',
            template: ` <p>showing child A {{ self.avatars }}</p> `,
        })
        .state('routerDemo.childB', {
            url: '/router-demo/:demoVar/childB',
            template: ` <p>showing the other child (B)</p>`,
        })
}




import { attach } from './utils';
const serviceDependencies = ['$scope','$ngRedux', /*'$routeParams' /*injections as strings here*/];
class DemoController {
    constructor(/* arguments <- serviceDependencies */) {
        attach(this, serviceDependencies, arguments);

        let disconnect = this.$ngRedux.connect((state) => ({state}), actionCreators)(this);
        this.$scope.$on('$destroy', disconnect)
        window.demoCtrl = this;
    }
}
app.controller('DemoController', [...serviceDependencies, DemoController]);


class AppController {
    constructor (){//($router) {
        console.log('in appcontroller constructor');
    }
}
app.controller('AppController', AppController);

//let app = angular.module('won.owner',[...other modules...]);
angular.bootstrap(document, ['won.owner'], {
    // make sure dependency injection works after minification (or
    // at least angular explains about sloppy imports with a
    // reference to the right place)
    // see https://docs.angularjs.org/guide/production
    // and https://docs.angularjs.org/guide/di#dependency-annotation
    strictDi: true
});

console.log('app_jspm.js: ', angular);

