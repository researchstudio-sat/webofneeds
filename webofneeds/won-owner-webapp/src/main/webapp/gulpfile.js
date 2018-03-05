'use strict';

var gulp = require('gulp');
var sass = require('gulp-sass');
var minifyCss = require('gulp-minify-css');
var rename = require('gulp-rename');
var autoprefixer = require('gulp-autoprefixer');
var svgSprite = require('gulp-svg-sprite');
var sassImportOnce = require('node-sass-import-once');
var gulp_jspm = require('gulp-jspm');
var sourcemaps = require('gulp-sourcemaps');


gulp.task('default', ['build']);
gulp.task('build', ['config', 'sass', 'iconsprite', 'bundlejs', 'copy-static-res', 'copy-static-scripts']);
gulp.task('watch', ['config', 'sass', 'iconsprite', 'bundlejs', 'copy-static-res'], function() {
    gulp.watch('./*.js', ['bundlejs']);
    gulp.watch('./app/**/*.js', ['bundlejs']);
    gulp.watch('./style/**/*.scss', ['sass']);
    gulp.watch('./style/**/_*.scss', ['sass']);
    gulp.watch('./images/won-icons/**/*.svg', ['iconsprite']);
});


// config task loads the config file depending on the environment the build is meant for
var settings = {
    environment : process.env.WON_DEPLOY_NODE_ENV || 'default',
};

gulp.task('config', function(){
    return gulp.src('./config/' + settings.environment + '.js')
        .pipe(rename('config.js'))
        .pipe(gulp.dest('./app/'));
});

gulp.task('bundlejs', function(){
    return gulp.src('app/app_jspm.js')
        .pipe(sourcemaps.init())
        //.pipe(gulp_jspm())
        .pipe(gulp_jspm({
            selfExecutingBundle: true,
            minify: false,
        }))
        .pipe(sourcemaps.write('.'))
        .pipe(gulp.dest('./generated/'));
});

gulp.task('sass', function(done) {
    var generatedStyleFolder =  './generated/';
    gulp.src('./style/won.scss')
        .pipe(sass({
            // TODO get import-once working
            importer: sassImportOnce,
            importOnce: {
                index: false,
                css: false,
                bower: false
            },
            errLogToConsole: true
        }))
        .pipe(autoprefixer({
            browsers: ['last 3 versions'],
            cascade: false
        }))
        .pipe(gulp.dest(generatedStyleFolder))
        .pipe(minifyCss({
            keepSpecialComments: 0
        }))
        .pipe(rename({ extname: '.min.css' }))
        .pipe(gulp.dest(generatedStyleFolder))
        .on('end', done);
});

/**
 * Copies over static resources that libraries in
 * the bundle need to be in a place relativ to
 * themselves -- and thus the bundle.
 */
gulp.task('copy-static-res', function(done) {
    return gulp.src([
        getRelModuleFolderPath('leaflet') + 'dist/images/**/*',
    ])
    .pipe(gulp.dest('./generated/images/'))
});
/**
 * Copies over scripts that are required outside the bundle
 * themselves -- and thus the bundle.
 */
gulp.task('copy-static-scripts', function(done) {
    return gulp.src([
        //'node_modules/medium.js/medium.js',
    ])
        .pipe(gulp.dest('./generated/scripts/'))
});


var svgSpriteConf = {
    shape : {
        dimension : {
            //default would be just 2
            precision: 4
        }
    },
    mode : {
        inline: true,
        symbol: true, // generate sprite with icons wrapped in <symbol>-tags, that can be used via `<svg><use href="#.."></use></svg>`
        view : {
            //example: true //generates html with usage-demo

            /*
             Problem: Glitches in layouting where viewports show parts
             of adjacent svgs as well.
             Solution:
                * `root.shape.spacing.padding: 1` -> It has the side effect to make icons a bit
                  smaller which needs to be accounted for during styling. Margin would be
                  better but, alas, is not available.
                * `mode.view.layout: 'diagonal'` seems to be a better solution, which solves the issue for all
                  except adjacent rectangular icons.
             */
            layout: 'diagonal',

            //default would be './view'
            dest: '.',

            //instead of default 'svg/sprite.view.svg'
            sprite: 'icon-sprite.svg',

            // don't add a hash to the name (it's intended to
            // circumvent cache staleness, but makes referencing the sprite hard)
            bust: false
        }
    }
};
gulp.task('iconsprite', function(done) {
    gulp.src('**/*.svg', {cwd: 'images/won-icons'})
        .pipe(svgSprite(svgSpriteConf))
        .pipe(gulp.dest('generated'))
        .on('end', done);
});

var rimraf = require('rimraf'); // rimraf directly
gulp.task('clean', function () {
    //(['generated', 'jspm_packages', 'node_modules'])
    //.forEach((folder) => rimraf(`./${folder}`, ()=>{}));
    rimraf('./generated', function(){});
});



require('./jspm_packages/system.js');
require('./jspm_config.js');

function getRelModuleFolderPath(moduleName) {
    var absolutePath = getAbsModuleFolderPath(moduleName);

    // example:
    // * abs: https://localhost:8443/owner/jspm_packages/npm/leaflet@0.7.7.js
    // * baseURL: https://localhost:8443/
    // ==> ./jspm_packages/npm/leaflet@0.7.7/dist/images/**/*

    return './' + absolutePath.substr(System.baseURL.length)
}

function getAbsModuleFolderPath(moduleName) {

    // e.g. "https://localhost:8443/owner/jspm_packages/npm/leaflet@0.7.7.js"
    var modulePath = System.normalizeSync(moduleName);

    var folderPath = modulePath.substr(0, modulePath.length - 3) + '/'; // remove `.js`
    return folderPath;
}

//npm install --save-dev "gulp-dgeni" "gulp-esdoc" "gulp-jsdoc" "dgeni" "dgeni-packages"
//var dgeni = require('gulp-dgeni');A
//var ngdoc = require('dgeni-packages/ngdoc');
//
//gulp.task('docs-dgeni', function () {
//    return gulp.src(['docs/**/*.ngdoc'])
//        .pipe(dgeni({packages: [ngdoc]}))
//        .pipe(gulp.dest('./generated/dgeni'));
//});
//
//var jsdoc = require("gulp-jsdoc");
//
//gulp.task('docs-jsdoc', function () {
//    return gulp.src('./app/**/*.js')
//        .pipe(jsdoc.parser({plugins: ['plugins/commentsOnly']}))
//        .pipe(jsdoc.generator('./generated/jsdoc'));
//
//});
//
//var esdoc = require("gulp-esdoc");
//
//gulp.task('docs-esdoc', function () {
//    return gulp.src('./app/components')
//        .pipe(esdoc({ destination: "./generated/esdoc" }));
//});

