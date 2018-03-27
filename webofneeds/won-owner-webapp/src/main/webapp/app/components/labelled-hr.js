;

import angular from 'angular';

function genComponentConf() {
    let template = `<div class="wlh__label">
    					<span class="wlh__label__text" ng-show="!self.arrow"> {{ self.label }} </span>
    					<svg class="wlh__label__carret" ng-show="self.arrow">
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

