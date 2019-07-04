import angular from "angular";
import { attach } from "../../../cstm-ng-utils.js";

import "~/style/_select-viewer.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
        <div class="selectv__header">
          <svg class="selectv__header__icon" ng-if="self.detail.icon">
              <use xlink:href={{self.detail.icon}} href={{self.detail.icon}}></use>
          </svg>
          <span class="selectv__header__label" ng-if="self.detail.label">{{self.detail.label}}</span>
        </div>
        <div class="selectv__content">
          <label ng-repeat="option in self.detail.options"
            class="selectv__input__inner">
            <input
              class="selectv__input__inner__select"
              type="{{ self.getSelectType() }}"
              name="{{ self.getInputName() }}"
              value="{{option.value}}"
              disabled="true"
              ng-checked="self.isChecked(option)"/>
            {{ option.label }}
          </label>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.selectv4dbg = this;

      this.$scope.$watch("self.content", (newContent, prevContent) =>
        this.updatedContent(newContent, prevContent)
      );
      this.$scope.$watch("self.details", (newDetails, prevDetails) =>
        this.updatedDetails(newDetails, prevDetails)
      );
    }

    getSelectType() {
      return this.detail && this.detail.multiSelect ? "checkbox" : "radio";
    }

    isChecked(option) {
      return this.content && !!this.content.find(elem => elem === option.value);
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

    getInputName() {
      /*TODO: Implement a sort of hashcode to prepend to the returned name to add the
       possibility of using the same identifier in is and seeks for these viewers*/
      return this.detail.identifier;
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
  .module("won.owner.components.selectViewer", [])
  .directive("wonSelectViewer", genComponentConf).name;
