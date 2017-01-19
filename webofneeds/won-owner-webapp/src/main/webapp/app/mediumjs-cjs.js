/**
 * Created by ksinger on 19.01.2017.
 */

'rang'
var rangy = require('rangy');
var Undo = require('undo.js');
window.rangy = rangy;
window.Undo = Undo;

var Medium = require('Medium.js');

/*
if(!module) {
    module = {};
}

 module.exports = { foo: 'bar' };
 */
module.exports = Medium;

