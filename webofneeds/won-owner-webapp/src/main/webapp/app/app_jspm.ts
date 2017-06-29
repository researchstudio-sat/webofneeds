import angular from 'angular';
//window.angular = angular; // for compatibility with pre-ES6/commonjs scripts

//import 'fetch'; //polyfill for window.fetch (for backward-compatibility with older browsers)

console.log(angular);


import {foobar} from "./testmodule2"
import {
    camel2Hyphen,
    hyphen2Camel,
    firstToLowerCase,
    delay,
} from './utils';

console.log('loaded');

foobar('asdf');

delay(3000).then(() => console.log('delay working'));