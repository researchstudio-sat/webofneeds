;

import angular from 'angular';
import labelledHrModule from './labelled-hr.js';

function genComponentConf() {
    let template = `
            <div class="flexgrid__item" ng-repeat="item in self.items">
                <div class="fgi__block" ng-class="{'clickable' : item.detail !== undefined}" ng-click="self.openElement($index)">
                    <img class="fgi__image" 
                        ng-show="item.imageSrc !== undefined" 
                        ng-src="{{item.imageSrc}}"/>
                    <svg style="--local-primary:var(--won-primary-color);"
                        class="fgi__image" 
                        ng-show="item.svgSrc !== undefined">
                            <use href="{{item.svgSrc}}"/>
                    </svg>
                    <span class="fgi__text" ng-show="item.text2 === undefined && item.separatorText === undefined">
                        {{item.text}}
                    </span>
                    <span class="fgi__text" ng-show="item.text2 !== undefined && item.separatorText !== undefined">
                        {{item.text}}
                        <won-labelled-hr label="::item.separatorText"></won-labelled-hr>
                        {{item.text2}}
                    </span>
                    <svg style="--local-primary:var(--won-primary-color);"
                        class="fgi__arrow"
                        ng-show="item.detail !== undefined && $index === self.selectedIdx">
                            <use href="#ico16_arrow_up"></use>
                    </svg>
                    <svg style="--local-primary:var(--won-primary-color);"
                        class="fgi__arrow"
                        ng-show="item.detail !== undefined && $index !== self.selectedIdx">
                            <use href="#ico16_arrow_down"></use>
                    </svg>
                </div>
                <span class="fgi__additionaltext" ng-show="item.detail !== undefined && $index === self.selectedIdx">{{item.detail}}</span>
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

export default angular.module('won.owner.components.flexgrid', [
    labelledHrModule
])
    .directive('wonFlexGrid', genComponentConf)
    .name;

