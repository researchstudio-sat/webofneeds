/**
 * Created by ksinger on 23.08.2017.
 */

;

import angular from 'angular';

function genComponentConf() {
    let template = `
        <nav class="signup-title-bar" ng-cloak ng-show="{{true}}">
            <div class="sutb__inner">
                <a class="sutb__inner__left clickable" ng-click="self.back()">
                    <img  src="generated/icon-sprite.svg#ico27_close" class="sutb__icon">
                </a>
                <h1 class="sutb__inner__center sutb__title">Sign Up</div>
            </div>
        </nav>
    `;

    class Controller {
        constructor() {
            //this.testVar = 42;
        }
        back() { window.history.back() }
    }

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        template: template,
        bindToController: true, //scope-bindings -> ctrl
        scope: { }
    }
}

export default angular.module('won.owner.components.signupTitleBar', [])
    .directive('wonSignupTitleBar', genComponentConf)
    .name;
