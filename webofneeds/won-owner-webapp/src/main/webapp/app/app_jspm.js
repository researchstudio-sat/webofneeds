/**
 *
 * Created by ksinger on 06.07.2015.
 */

// enable es6 in jshint:
/* jshint esnext: true */

console.log('System.import working');


import angular from 'angular';
window.angular = angular; // for compatibility with pre-ES6/commonjs scripts

import appTagModule from './components/wonAppTag';
import newRouter from 'angular-new-router';
import createNeedViewModule from './components/create-need/create-need';
import settingsViewModule from './components/settings/settings';

window.newRouter = newRouter; //TODO deletme

/*
 * TODO would be nice if components could specify their own
 * dependencies (and these only got loaded once each)
 */
//let app = angular.module('won.owner', [appTagModule, createNeedViewModule]);
let app = angular.module('won.owner', [
    'ngNewRouter',
    appTagModule,
    createNeedViewModule,
    settingsViewModule
]);

/*
 * Taken from https://github.com/htdt/ng-es6-router/blob/master/app/app.js
 */
app.config(['$componentLoaderProvider', setTemplatesPath]);
function setTemplatesPath ($componentLoaderProvider){
    window.componentLoaderProviderFoo = $componentLoaderProvider;
    $componentLoaderProvider.setTemplateMapping(name => `app/components/${name}/${name}.html`);
}

class AppController {
    constructor ($router) {
        console.log('in appcontroller constructor');
        window.routerfoo = $router;
        $router.config([
            { path: '/', redirectTo: '/create-need'},
            { path: '/create-need', component: 'create-need'},
            { path: '/settings', component: 'settings'}
        ]);
    }
}
//AppController.$inject = ['$router'];
app.controller('AppController', ['$router', AppController]);
//app.controller('AppController', [AppController]);

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


