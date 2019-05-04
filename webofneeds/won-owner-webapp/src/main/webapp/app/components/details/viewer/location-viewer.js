import angular from "angular";
import { attach } from "../../../utils.js";
import atomMapModule from "../../atom-map.js";

import "style/_location-viewer.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
        <div class="lv__header">
          <svg class="lv__header__icon" ng-if="self.detail.icon">
              <use xlink:href={{self.detail.icon}} href={{self.detail.icon}}></use>
          </svg>
          <span class="lv__header__label" ng-if="self.detail.label">{{self.detail.label}}</span>
        </div>
        <div class="lv__content">
          <div class="lv__content__text clickable"
               ng-if="self.content.get('address')" ng-click="self.toggleMap()">
                {{ self.content.get('address') }}
				        <svg class="lv__content__text__carret">
                  <use xlink:href="#ico-filter_map" href="#ico-filter_map"></use>
                </svg>
				        <svg class="lv__content__text__carret" ng-show="!self.showMap">
	                <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
	              </svg>
                <svg class="lv__content__text__carret" ng-show="self.showMap">
                   <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
                </svg>
          </div>
          <won-atom-map
            locations="[self.content]"
            ng-if="self.content && self.showMap">
          </won-atom-map>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.lv4dbg = this;
      this.showMap = false;

      this.$scope.$watch("self.content", (newContent, prevContent) =>
        this.updatedContent(newContent, prevContent)
      );
      this.$scope.$watch("self.details", (newDetails, prevDetails) =>
        this.updatedDetails(newDetails, prevDetails)
      );
    }

    updatedDetails(newDetails, prevDetails) {
      if (newDetails && newDetails != prevDetails) {
        this.details = newDetails;
      }
    }
    updatedContent(newContent, prevContent) {
      if (newContent && newContent != prevContent) {
        this.content = newContent;
      }
    }

    toggleMap() {
      this.showMap = !this.showMap;
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      content: "=",
      detail: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.locationViewer", [atomMapModule])
  .directive("wonLocationViewer", genComponentConf).name;
