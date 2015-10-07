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
import settingsComponent from './components/settings/settings';
import overviewIncomingRequestsComponent from './components/overview-incoming-requests/overview-incoming-requests';
import matchesComponent from './components/matches/matches';
import postVisitorComponent from './components/post-visitor/post-visitor';
import { camel2Hyphen, hyphen2Camel, firstToLowerCase } from './utils';
import landingPageComponent from './components/landingpage/landingpage';
import overviewPostsComponent from './components/overview-posts/overview-posts';
import feedComponent from './components/feed/feed';
import overviewMatchesComponent from './components/overview-matches/overview-matches';

//import * as reducers from './reducers/reducers';
import reducer from './reducers/reducers';
import 'redux';
//import { combineReducers } from 'redux';
//import { combineReducers } from 'redux-immutable';
import { combineReducers } from 'redux-immutablejs';
import Immutable from 'immutable';
import thunk from 'redux-thunk';
import ngReduxModule from 'ng-redux';

/*
import {
    router,
    stateGo,
    stateReload,
    stateTransitionTo
} from 'redux-ui-router';
 const routerActions = {
 stateGo,
 stateReload,
 stateTransitionTo
 };
window.routerFooFoo = router;
*/

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
    settingsComponent,
    overviewIncomingRequestsComponent,
    matchesComponent,
    postVisitorComponent,
    landingPageComponent,
    overviewPostsComponent,
    feedComponent,
    overviewMatchesComponent
]);

app.config([
    /*'$componentLoaderProvider',*/ '$ngReduxProvider',
    '$urlRouterProvider', '$stateProvider' /*of routerstate*/,
    (/*$componentLoaderProvider,*/ $ngReduxProvider, $urlRouterProvider, $stateProvider) => {
        //configComponentLoading($componentLoaderProvider);
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

/*
 * Taken from https://github.com/htdt/ng-es6-router/blob/master/app/app.js
 */
function configComponentLoading($componentLoaderProvider) {
    //the default wouldn't include 'app/'
    $componentLoaderProvider.setTemplateMapping(name => `app/components/${name}/${name}.html`);
    $componentLoaderProvider.setCtrlNameMapping(componentName =>
        hyphen2Camel(componentName) + 'Controller'
    )
    $componentLoaderProvider.setComponentFromCtrlMapping(ctrlName =>
            camel2Hyphen(ctrlName.replace(/Controller$/, ''))
    )
    window.loader = $componentLoaderProvider;

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
        { path: '/settings', component: 'settings' },
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
                controller: `${cmlComponent}Controller`,
                controllerAs: 'self'
            });
    })

    $stateProvider
        .state('routerDemo', {
            url: '/router-demo/:demoVar',
            template: "demoVar = {{self.state.getIn(['router', 'currentParams', 'demoVar'])}}",
            controller: 'DemoController',
            controllerAs: 'self'
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
        /*
        $router.config([

            //TODO should be landing page if not logged in or feed if logged in
            {
            },
            //TODO database id needs to be send to the client after the create-msg acknowledgment
            { path: '/post/:id/visitor', component: 'post-visitor'},
            { path: '/post/:id/owner/matches', component: 'matches'}
            //{ path: '/post/:id/owner/messages', component: 'need-messages'} //TODO
            //{ path: '/post/:id/owner/incoming-requests', component: 'need-incoming-requests'} //TODO
            //{ path: '/post/:id/owner/outgoing-requests', component: 'need-outgoing-requests'} //TODO
        ]);
        */
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

