import angular from "angular";
import { attach } from "../../../utils.js";

import "~/style/_datetime-viewer.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
        <div class="datetimev__header">
          <svg class="datetimev__header__icon" ng-if="self.detail.icon">
              <use xlink:href={{self.detail.icon}} href={{self.detail.icon}}></use>
          </svg>
          <span class="datetimev__header__label" ng-if="self.detail.label">{{self.detail.label}}</span>
        </div>
        <div class="datetimev__content">{{ self.content && self.content.toLocaleString() }}</div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.datetimev4dbg = this;

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
  .module("won.owner.components.datetimeViewer", [])
  .directive("wonDatetimeViewer", genComponentConf).name;
