import angular from "angular";
import { attach } from "../../../cstm-ng-utils.js";

import "~/style/_dropdown-viewer.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
        <div class="dropdownv__header">
          <svg class="dropdownv__header__icon" ng-if="self.detail.icon">
              <use xlink:href={{self.detail.icon}} href={{self.detail.icon}}></use>
          </svg>
          <span class="dropdownv__header__label" ng-if="self.detail.label">{{self.detail.label}}</span>
        </div>
        <div class="dropdownv__content">{{ self.content }}</div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.dropdownv4dbg = this;

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
  .module("won.owner.components.dropdownViewer", [])
  .directive("wonDropdownViewer", genComponentConf).name;
