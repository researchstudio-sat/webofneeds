;

import angular from 'angular';

function genComponentConf() {
    let template = `
        <nav class="create-need-title" ng-cloak ng-show="{{true}}">
            <!--<div class="cntb__inner">
                <a class="cntb__inner__left clickable" ng-click="self.back()">
                <svg style="--local-primary:var(--won-primary-color);"
                    class="cntb__icon">
                        <use href="#ico27_close"></use>
                </svg>
                </a>
                <h1 class="cntb__inner__center cntb__title">Post</div>
            </div>-->
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

export default angular.module('won.owner.components.createNeedTitleBar', [])
    .directive('wonCreateNeedTitleBar', genComponentConf)
    .name;
