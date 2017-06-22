;

import angular from 'angular';

function genComponentConf() {
    let template = `
            <div class="accordion__element clickable" ng-click="self.openElement($index)" ng-repeat="item in self.items">
                <div class="header clickable">{{item.title}}</div>
                <img class="arrow clickable" ng-show="$index !== self.selectedIdx" src="generated/icon-sprite.svg#ico16_arrow_down"/>
                <img class="arrow clickable" ng-show="$index === self.selectedIdx" src="generated/icon-sprite.svg#ico16_arrow_up"/>
                <div class="detail" ng-show="$index === self.selectedIdx">{{item.detail}}</div>
            </div>
    `;

    class Controller {
        constructor() { }

        openElement(index) {
            if(index === this.selectedIdx){
                this.selectedIdx = undefined;
            }else{
                this.selectedIdx = index;
            }
        }
    }

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {items: '='},
        template: template
    }
}

export default angular.module('won.owner.components.accordion', [])
    .directive('wonAccordion', genComponentConf)
    .name;

