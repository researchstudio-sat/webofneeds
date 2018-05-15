if (module) {
  //commonjs export for importing with jspm
  console.log("getting into module export");
  console.log("requiring angular ", require("angular"));
  var angular = require("angular");
  /*
     //require('app/app_jspm'); //make sure the 'won.owner' module is defined
     -> can't. would lead to dependency cycle.
     */
  module.exports = {
    foo: "bar - in wonservice - commonjs",
  };
}

//DELETE THE CODE BELOW, to make jspm identify the commonjs import/export above correctly

//import angular from 'angular';
//window.angular = angular;
/*
 if(module) { //commonjs export for importing with jspm
 module.exports = { foo: 'bar - in wonservice - commonjs'}
 }
 */
//window.angularity = require('angular');

/*
if(define) { //amd export for importing with jspm
    //window.angularTesti1 = angular;
    define('wonService',
        ['require'], //dependencies
        (require) => {//dependencies
            console.log('GOT TO EXPORT');
            var angular = require('angular');

            window.angularTesti = angular;
            return {foo: 'bar - in wonservice - amd'}
        }
    );
    //define('myModule', ['dep1', 'dep2'], function (dep1, dep2) {

    //Define the module value by returning a value.
    //return function () {};
    //});
}
*/
