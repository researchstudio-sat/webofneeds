;

import angular from 'angular';

function genComponentConf() {
    let template = `
        <nav class="create-need-title" ng-cloak ng-show="{{true}}">
            <div class="cnt__inner">
                <div class="cnt__inner__left">
                    <img src="generated/icon-sprite.svg#ico27_close" class="cnt__icon">
                </div>
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
