import angular from "angular";
import { attach } from "../../../cstm-ng-utils.js";
import preactModule from "../../preact-module.js";
import WonAtomMap from "../../atom-map.jsx";

import "~/style/_travel-action-viewer.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
        <div class="rv__header">
          <svg class="rv__header__icon" ng-if="self.detail.icon">
              <use xlink:href={{self.detail.icon}} href={{self.detail.icon}}></use>
          </svg>
          <span class="rv__header__label" ng-if="self.detail.label">{{self.detail.label}}</span>
        </div>
        <div class="rv__content">
          <div class="rv__content__text clickable"
               ng-if="self.content.get('fromAddress') || self.content.get('toAddress')" ng-click="self.toggleMap()">
                <div>
                  <span ng-if="self.content.get('fromAddress')">
                    <strong>From: </strong>{{ self.content.get('fromAddress') }}
                  </span>
                  </br>
                  <span ng-if="self.content.get('toAddress')">
                    <strong>To: </strong>{{ self.content.get('toAddress') }}
                  </span>
                </div>
				        <svg class="rv__content__text__carret">
                  <use xlink:href="#ico-filter_map" href="#ico-filter_map"></use>
                </svg>
				        <svg class="rv__content__text__carret" ng-show="!self.showMap">
	                <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
	              </svg>
                <svg class="rv__content__text__carret" ng-show="self.showMap">
                   <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
                </svg>
          </div>
          <won-preact class="won-atom-map" component="self.WonAtomMap" props="{ locations: [self.content.get('fromLocation'), self.content.get('toLocation')] }" ng-if="self.showMap && self.content && (self.content.get('fromLocation') || self.content.get('toLocation'))"></won-preact>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.showMap = false;
      this.WonAtomMap = WonAtomMap;

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
  .module("won.owner.components.travelActionViewer", [preactModule])
  .directive("wonTravelActionViewer", genComponentConf).name;
