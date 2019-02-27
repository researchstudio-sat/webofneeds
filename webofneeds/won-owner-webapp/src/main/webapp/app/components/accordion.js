import angular from "angular";
import "angular-marked";

import "style/_accordion.scss";
import "style/_won-markdown.scss";

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
                <div class="detail markdown" ng-show="$index === self.selectedIdx" marked="item.detail"></div>
            </div>
    `;

  class Controller {
    constructor() {}

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
  .module("won.owner.components.accordion", ["hc.marked"])
  .directive("wonAccordion", genComponentConf).name;
