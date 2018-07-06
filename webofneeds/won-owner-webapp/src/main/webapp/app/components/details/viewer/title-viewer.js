import angular from "angular";
import { attach } from "../../../utils.js";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
        <div class="titlev__header">
          <svg class="titlev__header__icon" ng-if="self.detail.icon">
              <use xlink:href={{self.detail.icon}} href={{self.detail.icon}}></use>
          </svg>
          <span ng-if="self.detail.label">{{self.detail.label}}</span>
        </div>
        <div class="titlev__content">{{ self.content }}</div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.titlev4dbg = this;
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
  .module("won.owner.components.titleViewer", [])
  .directive("wonTitleViewer", genComponentConf).name;
