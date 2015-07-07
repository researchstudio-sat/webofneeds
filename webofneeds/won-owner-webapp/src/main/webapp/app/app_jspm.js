/**
 *
 * Created by ksinger on 06.07.2015.
 */

// enable es6 in jshint:
/* jshint esnext: true */

console.log('System.import working');


import angular from 'angular';

import wonAppTag from 'app/wonAppTag';

let app = angular.module('won.owner',[])
                 .directive('wonApp', wonAppTag);
//let app = angular.module('won.owner',[]);
angular.bootstrap(document, ['won.owner'], {
    // make sure dependency injection works after minification
    // see https://docs.angularjs.org/guide/production
    // and https://docs.angularjs.org/guide/di#dependency-annotation
    strictDi: true
});

console.log('app_jspm.js: ', angular);
window.app = app; //TODO for debugging only. remove me.
window.wonAppTag = wonAppTag; //TODO for debugging only. remove me.


function appTag() {
    let template = '<h1>Hello, from your lovely app-directive!</h1>'

    function link() {
    }

    let directive = {
        restrict: 'E',
        link: link,
        template: template
    }
    return directive
}

class Foo{}

