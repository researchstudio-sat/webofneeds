/**
 * Created by ksinger on 22.12.2016.
 * Adapted from <https://github.com/jakiestfu/Medium.js/issues/104#issuecomment-71995117>
 */
define([
    'rangy',
    //'rangy-core',
    //'rangy-class',
    'undo.js',
    //'css!medium-css'
], function(rangy, Undo) {
    console.log("medium-import-helper");
    window.rangy = rangy;
    window.Undo = Undo;
});