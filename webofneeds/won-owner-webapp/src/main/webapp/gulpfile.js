'use strict';

var gulp = require('gulp');
var sass = require('gulp-sass');
var minifyCss = require('gulp-minify-css');
var rename = require('gulp-rename');
var autoprefixer = require('gulp-autoprefixer');
var svgSprite = require('gulp-svg-sprite');

//gulp.task('sass', function () {
//    gulp.src('./sass/**/*.scss')
//        .pipe(sass().on('error', sass.logError))
//        .pipe(gulp.dest('./css'));
//});

//gulp.task('sass:watch', function () {
//    gulp.watch('./sass/**/*.scss', ['sass']);
//});

gulp.task('default', ['sass', 'iconsprite']);
gulp.task('watch', function() {
    gulp.watch('./style/**/*.scss', ['sass']);
    gulp.watch('./images/won-icons/**/*.svg', ['iconsprite']);
});



gulp.task('sass', function(done) {
    var generatedStyleFolder =  './generated/';
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
