;

import angular from 'angular';

function genComponentConf() {
    let template = `<div class="wlh__label" ng-show="self.label">{{self.label}}</div>
    				<div class="wlh__label" ng-show="self.arrow">
    					<svg class="wlh__label__carret">
	                    	<use href="#ico16_arrow_up"></use>
	                    </svg>
	                </div>`;

    class Controller {
        constructor() { }
    }

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {
            label: '=',
            arrow: '=',
        },
        template: template
    }
}

export default angular.module('won.owner.components.labelledHr', [])
    .directive('wonLabelledHr', genComponentConf)
    .name;

