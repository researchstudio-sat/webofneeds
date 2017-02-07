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
gulp.task('build', ['sass', 'iconsprite', 'bundlejs', 'copy-static-res']);
gulp.task('watch', ['sass', 'iconsprite', 'bundlejs', 'copy-static-res'], function() {
    gulp.watch('./app/**/*.js', ['bundlejs']);
    gulp.watch('./style/**/*.scss', ['sass']);
    gulp.watch('./style/**/_*.scss', ['sass']);
    gulp.watch('./images/won-icons/**/*.svg', ['iconsprite']);
});

gulp.task('bundlejs', function(){
    return gulp.src('app/app_jspm.js')
        .pipe(sourcemaps.init())
        .pipe(gulp_jspm())
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
 * Copies over static resources, that libraries in
 * the bundle need to be in a place relativ to
 * themselves -- and thus the bundle.
 */
gulp.task('copy-static-res', function(done) {
    return gulp.src([
        './jspm_packages/npm/leaflet@0.7.7/dist/images/**/*'
    ])
    .pipe(gulp.dest('./generated/images/'))
});



var svgSpriteConf = {
    shape : {
        dimension : {
            //default would be just 2
            precision: 4
        }
    },
    mode : {
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

