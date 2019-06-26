import angular from "angular";
import { attach } from "../../../cstm-ng-utils.js";

import "~/style/_range-viewer.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
        <div class="rangev__header">
          <svg class="rangev__header__icon" ng-if="self.detail.icon">
              <use xlink:href={{self.detail.icon}} href={{self.detail.icon}}></use>
          </svg>
          <span class="rangev__header__label" ng-if="self.detail.label">{{self.detail.label}}</span>
        </div>
        <div class="rangev__content">{{ self.detail.generateHumanReadable({value: self.content.toJS(), includeLabel: false}) }}</div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      window.rangev4dbg = this;

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
  .module("won.owner.components.rangeViewerModule", [])
  .directive("wonRangeViewer", genComponentConf).name;
