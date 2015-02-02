// Configuration was done based on examples from:
// http://lostechies.com/gabrielschenker/2013/12/30/angularjspart-7-getting-ready-to-test/
// and
// https://github.com/yearofmoo-articles/AngularJS-Testing-Article
// and
// https://ronaldharing.wordpress.com/2013/11/17/referenceerror-module-is-not-defined/

module.exports = function() {
    return {
        basePath: '../',
        frameworks: ['jasmine'],
        plugins: [
            'karma-chrome-launcher',
            //'karma-firefox-launcher',
            //'karma-script-launcher',
            'karma-jasmine'
        ],
        reporters: ['progress'],
        // port: 9876,
        // logLevel: config.LOG_INFO,
        // captureTimeout: 60000,
        browsers: ['Chrome'],
        autoWatch: true,
// these are default values anyway
        singleRun: false,
        colors: true,

        exclude: [ "bower_components/**/test/*.js"],

        files : [
//3rd Party Code
//the source files that you own code depends on, should be described in the correct order
            //'bower_components/angular/angular.js',
            //'bower_components/angular-mocks/angular-mocks.js',
            //'bower_components/angular-route/angular-route.js',
            //'bower_components/angularjs-scope.safeapply/src/Scope.SafeApply.js',
            "bower_components/jquery/jquery.js",
            "bower_components/angular/angular.js",
            'bower_components/angular-mocks/angular-mocks.js',
            "bower_components/angular-route/angular-route.js",
            //"bower_components/angular-bootstrap/ui-bootstrap.js",
            "bower_components/angular-bootstrap/ui-bootstrap.js",

            "bower_components/angular-ui-map/ui-map.js",
            //"bower_components/angular-ui-utils/modules/event/event.js ",
            "bower_components/angular-ui-utils/ui-utils.js ",
            "bower_components/js-md5/js/md5.js",
            "bower_components/sockjs/sockjs.js",
            "bower_components/ng-scrollbar/src/ng-scrollbar.js",

            //'bower_components/**/*.js',


            'scripts/upload/vendor/jquery.ui.widget.js',
            'scripts/upload/jquery.fileupload.js',
            'scripts/upload/jquery.iframe-transport.js',
            'scripts/upload/jquery.fileupload-process.js',
            'scripts/upload/jquery.fileupload-angular.js',
            'scripts/bootstrap-datepicker.js',
            'scripts/lightbox.min.js',
            'scripts/jquery.bootpag.min.js',
            'scripts/smart-table.min.js',
            'scripts/bootstrap-tagsinput.min.js',
            'scripts/jsonld.js',
            'scripts/rdfstore-js/rdf_store.js',
            'scripts/angular-scrollable-table/angular-scrollable-table.js',
            "scripts/star-rating.min.js",

             "resources/leaflet-0.7.3/leaflet.js",
//            'scripts/rdf-ext/rdf-ext.js',
//            'scripts/**/*.js',
//            'scripts/**/*.js',

            //instead of the http://maps.google.com/maps/api script below, the 'test/lib/google-maps-api-for-testing.js' is used
            //"http://maps.google.com/maps/api/js?sensor=false&callback=onGoogleReady",

//App-specific Code
            'app/*.js',
            'app/**/*.js',
//Test-Specific Code
            //'test/lib/jasmine-standalone-2.1.3/lib/jasmine-2.1.3/jasmine.js'
            'test/lib/google-maps-api-for-testing.js'
        ]
    }
};