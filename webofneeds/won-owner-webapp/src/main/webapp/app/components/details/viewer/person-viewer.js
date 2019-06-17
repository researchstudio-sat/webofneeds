import angular from "angular";
import { attach } from "../../../cstm-ng-utils.js";

import "~/style/_person-viewer.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
        <div class="pv__header">
          <svg class="pv__header__icon" ng-if="self.detail.icon">
              <use xlink:href={{self.detail.icon}} href={{self.detail.icon}}></use>
          </svg>
          <span class="pv__header__label" ng-if="self.detail.label">{{self.detail.label}}</span>
        </div>
        <div class="pv__content">
          <div class="pv__content__label" ng-if="self.content.get('title')">
            Title
          </div>
          <div class="pv__content__value" ng-if="self.content.get('title')">
            {{self.content.get('title')}}
          </div>

          <div class="pv__content__label" ng-if="self.content.get('name')">
            Name
          </div>
          <div class="pv__content__value" ng-if="self.content.get('name')">
            {{self.content.get('name')}}
          </div>

          <div class="pv__content__label" ng-if="self.content.get('position')">
            Position
          </div>
          <div class="pv__content__value" ng-if="self.content.get('position')">
            {{self.content.get('position')}}
          </div>

          <div class="pv__content__label" ng-if="self.content.get('company')">
            Company
          </div>
          <div class="pv__content__value" ng-if="self.content.get('company')">
            {{self.content.get('company')}}
          </div>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      //TODO: debug; deleteme
      window.person4dbg = this;

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
  .module("won.owner.components.personViewerModule", [])
  .directive("wonPersonViewer", genComponentConf).name;
