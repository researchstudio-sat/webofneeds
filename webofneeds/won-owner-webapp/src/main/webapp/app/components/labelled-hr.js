;

import angular from 'angular';

function genComponentConf() {
    let template = `<div class="wlh__label" ng-show="self.label">{{self.label}}</div>`;

    class Controller {
        constructor() { }
    }

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {
            label: '='
        },
        template: template
    }
}

export default angular.module('won.owner.components.labelledHr', [])
    .directive('wonLabelledHr', genComponentConf)
    .name;

