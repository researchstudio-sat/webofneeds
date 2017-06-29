
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