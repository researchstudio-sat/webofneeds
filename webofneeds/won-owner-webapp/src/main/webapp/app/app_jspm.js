/**
 *
 * Created by ksinger on 06.07.2015.
 */

// enable es6 in jshint:
/* jshint esnext: true */

console.log('System.import working');


import angular from 'angular';
window.angular = angular; // for compatibility with pre-ES6/commonjs scripts

import newRouter from 'angular-new-router';

// Components
import appTag from './components/wonAppTag';
import topnav from './components/topnav';
import createNeedComponent from './components/create-need/create-need';
import settingsComponent from './components/settings/settings';
import overviewIncomingRequestsComponent from './components/overview-incoming-requests/overview-incoming-requests';
import matchesComponent from './components/matches/matches';
import postVisitorComponent from './components/post-visitor/post-visitor';
import {camel2Hyphen, hyphen2Camel} from './utils';

let app = angular.module('won.owner', [
    'ngNewRouter',
    appTag,
    topnav, //used in index.html
    createNeedComponent,
    settingsComponent,
    overviewIncomingRequestsComponent,
    matchesComponent,
    postVisitorComponent
]);

/*
 * Taken from https://github.com/htdt/ng-es6-router/blob/master/app/app.js
 */
app.config(['$componentLoaderProvider', setTemplatesPath]);
function setTemplatesPath ($componentLoaderProvider){
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

class AppController {
    constructor ($router) {
        console.log('in appcontroller constructor');
        window.routerfoo = $router;
        $router.config([

            //TODO should be landing page if not logged in or feed if logged in
            { path: '/', redirectTo: '/incoming-requests'},

            { path: '/create-need', component: 'create-need', as: 'createNeed'},
            { path: '/settings', component: 'settings'},

            { path: '/overview/incoming-requests', component: 'overview-incoming-requests'},
            //{ path: '/overview/matches', component: 'matches'}, //TODO
            //{ path: '/overview/posts', component: 'posts'}, //TODO
            //{ path: '/overview/feed', component: 'posts'}, //TODO

            //TODO database id needs to be send to the client after the create-msg acknowledgment
            { path: '/post/:id/visitor', component: 'post-visitor'},
            { path: '/post/:id/owner/matches', component: 'matches'}
            //{ path: '/post/:id/owner/messages', component: 'need-messages'} //TODO
            //{ path: '/post/:id/owner/incoming-requests', component: 'need-incoming-requests'} //TODO
            //{ path: '/post/:id/owner/outgoing-requests', component: 'need-outgoing-requests'} //TODO
        ]);
    }
}
app.controller('AppController', ['$router', AppController]);

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
window.app = app; //TODO for debugging only. remove me.


