/**
 *
 * Created by ksinger on 06.07.2015.
 */

// enable es6 in jshint:
/* jshint esnext: true */

console.log('System.import working');


import angular from 'angular';

var app = angular.module('won.owner',[])

angular.bootstrap(document, ['won.owner'], { });

console.log(angular);

