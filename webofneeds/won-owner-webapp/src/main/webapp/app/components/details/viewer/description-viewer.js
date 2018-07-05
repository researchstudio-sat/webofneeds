import angular from "angular";
import { attach } from "../../../utils.js";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
        <div class="dv__header">
          <svg class="dv__header__icon" ng-if="self.detail.icon">
              <use xlink:href={{self.detail.icon}} href={{self.detail.icon}}></use>
          </svg>
          <span ng-if="self.detail.label">{{self.detail.label}}</span>
        </div>
        <div class="dv__content">{{ self.content }}</div>  <!-- no spaces or newlines within the code-tag, because it is preformatted -->
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.dv4dbg = this;
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
  .module("won.owner.components.descriptionViewer", [])
  .directive("wonDescriptionViewer", genComponentConf).name;
