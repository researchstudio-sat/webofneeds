;

import angular from 'angular';

function genComponentConf() {
    let template = `
        <nav class="create-need-title" ng-cloak ng-show="{{true}}">
            <div class="cnt__inner">
                <!--<a href="#" class="cnt__inner__left" ng-click="window.history.back()">-->
                <a class="cnt__inner__left" ng-click="window.console.log(123456)">
                    <img  src="generated/icon-sprite.svg#ico27_close" class="cnt__icon">
                </a>
                <h1 class="cnt__inner__center cnt__title">What is your need?</div>
            </div>
        </nav>
    `;

    return {
        restrict: 'E',
        template: template
    }
}
export default angular.module('won.owner.components.createNeedTitleBar', [])
    .directive('wonCreateNeedTitleBar', genComponentConf)
    .name;
