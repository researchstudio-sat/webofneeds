;

import angular from 'angular';

function genComponentConf() {
    let template = `
            <div class="accordion__element clickable" ng-click="self.openElement($index)" ng-repeat="item in self.items">
                <div class="header clickable">{{item.title}}</div>

                <svg style="--local-primary:var(--won-primary-color);"
                    class="arrow clickable"
                    ng-show="$index !== self.selectedIdx">
                        <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
                </svg>
                <svg style="--local-primary:var(--won-primary-color);"
                    class="arrow clickable"
                    ng-show="$index === self.selectedIdx">
                        <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
                </svg>
                <div class="detail" ng-show="$index === self.selectedIdx && item.unsafeHtmlEnabled" ng-bind-html="item.detail"></div>
                <div class="detail" ng-show="$index === self.selectedIdx && !item.unsafeHtmlEnabled">{{item.detail}}</div>         
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

