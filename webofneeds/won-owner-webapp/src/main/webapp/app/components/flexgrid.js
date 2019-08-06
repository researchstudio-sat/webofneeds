import angular from "angular";
import WonLabelledHr from "./labelled-hr.jsx";

import "~/style/_flexgrid.scss";

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
                        <won-preact component="self.WonLabelledHr" class="labelledHr" props="{label: item.separatorText}"></won-preact>
                        {{item.text2}}
                    </span>
                    <svg style="--local-primary:var(--won-primary-color);"
                        class="fgi__arrow"
                        ng-show="item.detail !== undefined && $index === self.selectedIdx">
                            <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
                    </svg>
                    <svg style="--local-primary:var(--won-primary-color);"
                        class="fgi__arrow"
                        ng-show="item.detail !== undefined && $index !== self.selectedIdx">
                            <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
                    </svg>
                </div>
                <span class="fgi__additionaltext" ng-show="item.detail !== undefined && $index === self.selectedIdx">{{item.detail}}</span>
            </div>
    `;

  class Controller {
    constructor() {
      this.WonLabelledHr = WonLabelledHr;
    }

    openElement(index) {
      if (index === this.selectedIdx) {
        this.selectedIdx = undefined;
      } else {
        this.selectedIdx = index;
      }
    }
  }

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: { items: "=" },
    template: template,
  };
}

export default angular
  .module("won.owner.components.flexgrid", [])
  .directive("wonFlexGrid", genComponentConf).name;
