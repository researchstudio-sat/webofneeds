/**
 * Created by ksinger on 17.06.2016.
 */

// import undo from 'undo.js';
// window.undo = undo; // medium.js requires this to be in the window-scope

// import * as rangy from 'rangy';
// import rangy from 'rangy';
// window.rangy = rangy; // medium.js requires this to be in the window-scope

import * as Medium from 'Medium.js';
// const Medium = {};
window.Medium = Medium;
console.log('Medium: ', Medium);

export default Medium;
