;

import angular from 'angular';

function genComponentConf() {
    let template = `
        <nav class="create-need-title" ng-cloak ng-show="{{true}}">
            <div class="cnt__inner">
                <a class="cnt__inner__left clickable" ng-click="self.back()">
                    <img  src="generated/icon-sprite.svg#ico27_close" class="cnt__icon">
                </a>
                <h1 class="cnt__inner__center cnt__title">What is your need?</div>
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
        scope: {}
    }
}

export default angular.module('won.owner.components.createNeedTitleBar', [])
    .directive('wonCreateNeedTitleBar', genComponentConf)
    .name;
