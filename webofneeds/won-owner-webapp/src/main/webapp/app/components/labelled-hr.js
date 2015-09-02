/**
 * Created by ksinger on 20.08.2015.
 */
;

import angular from 'angular';

function genComponentConf() {
    let template = `<div class="wlh__label">or</div>`;

    class Controller {
        constructor() { }
    }

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {},
        template: template
    }
}

export default angular.module('won.owner.components.labelledHr', [])
    .directive('wonLabelledHr', genComponentConf)
    .name;

