/**
 *
 * Created by ksinger on 07.07.2015.
 */
;

import angular from 'angular';

function wonAppTag() {
    let template = `
        <section id="main" class="contentArea">
            <h1 ng-click="ctrl.fooFun()">Hello, from your lovely app-directive! Foo{{ctrl.foo}}! </h1>
        </section>
        <footer></footer>
        `;

    console.log('updated this file at last');

    function link() {
        console.log(`multiline
        template
        string`)
    }

    /*
    function wonAppCtrl() {
        this.foo = 'bar';
        this.fooFun = () => {
            console.log('fooFun called!');
        }
    }
    */


    class WonAppCtrl {
        constructor() {
            this.foo = 'bar';
        }
        fooFun () {
            console.log('fooFun called!');
        }
    }
    WonAppCtrl.$inject = ['$http'/*injections as strings here*/];


    let directive = {
        restrict: 'E',
        link: link,
        //http://blog.thoughtram.io/angularjs/2015/01/02/exploring-angular-1.3-bindToController.html
        controllerAs: 'ctrl',
        controller: WonAppCtrl,
        template: template
    }
    return directive
}


export default angular.module('won.owner.components.app',
    [   /* angular module-dependencies */
    ])
    .directive('wonApp', wonAppTag)
    .name;
