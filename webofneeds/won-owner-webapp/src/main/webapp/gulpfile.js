'use strict';

var gulp = require('gulp');
var sass = require('gulp-sass');
var minifyCss = require('gulp-minify-css');
var rename = require('gulp-rename');
var autoprefixer = require('gulp-autoprefixer');

//gulp.task('sass', function () {
//    gulp.src('./sass/**/*.scss')
//        .pipe(sass().on('error', sass.logError))
//        .pipe(gulp.dest('./css'));
//});

//gulp.task('sass:watch', function () {
//    gulp.watch('./sass/**/*.scss', ['sass']);
//});

gulp.task('default', ['sass']);

gulp.task('sass', function(done) {
    var generatedStyleFolder =  './style/generated/';
    gulp.src('./style/won.scss')
        .pipe(sass({
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

gulp.task('watch', function() {
    gulp.watch('./style/won.scss', ['sass']);
});