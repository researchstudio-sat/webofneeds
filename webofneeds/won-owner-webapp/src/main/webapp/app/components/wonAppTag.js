/**
 *
 * Created by ksinger on 07.07.2015.
 */
;

import angular from 'angular';
import topnavModule from './topnav';
import createNeedTitleBarModule from './create-need-title-bar';
import mainTabBarModule from './main-tab-bar';
import needTitleBarModule from './need-title-bar';
import settingsTitleBarModule from './settings-title-bar';

function wonAppTag() {
    let template = `
        <header>
            <won-topnav></won-topnav>
            <won-main-tab-bar></won-main-tab-bar>
            <br/>
            <won-create-need-title-bar></won-create-need-title-bar>
            <br/>
            <won-need-title-bar></won-need-title-bar>
            <br/>
            <won-settings-title-bar></won-settings-title-bar>
        </header>
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
        topnavModule,
        createNeedTitleBarModule,
        mainTabBarModule,
        needTitleBarModule,
        settingsTitleBarModule
    ])
    .directive('wonApp', wonAppTag)
    .name;
