import angular from "angular";
import { attach } from "../../../utils.js";
import "angular-marked";

import "style/_description-viewer.scss";
import "style/_won-markdown.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
        <div class="dv__header">
          <svg class="dv__header__icon" ng-if="self.detail.icon">
              <use xlink:href={{self.detail.icon}} href={{self.detail.icon}}></use>
          </svg>
          <span class="dv__header__label" ng-if="self.detail.label">{{self.detail.label}}</span>
        </div>
        <div class="markdown" marked="self.content"></div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.dv4dbg = this;

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
  .module("won.owner.components.descriptionViewer", ["hc.marked"])
  .directive("wonDescriptionViewer", genComponentConf).name;
