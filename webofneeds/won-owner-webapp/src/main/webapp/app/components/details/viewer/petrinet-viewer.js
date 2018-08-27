import angular from "angular";
import { attach } from "../../../utils.js";

import "style/_petrinet-viewer.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
        <div class="petrinetv__header">
          <svg class="petrinetv__header__icon" ng-if="self.detail.icon">
              <use xlink:href={{self.detail.icon}} href={{self.detail.icon}}></use>
          </svg>
          <span class="petrinetv__header__label" ng-if="self.detail.label">{{self.detail.label}}</span>
        </div>
        <a class="petrinetv__content" ng-show="self.content"
          ng-href="data:{{self.content.get('type')}};base64,{{self.content.get('data')}}"
          download="{{ self.content.get('name') }}">
          <div class="petrinetv__content__label clickable">
            {{ self.content.get('name') }}
          </div>
          <svg class="petrinetv__content__typeicon">
            <use xlink:href="#ico36_uc_transport_demand" href="#ico36_uc_transport_demand"></use>
          </svg>
        </a>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.petrinetv4dbg = this;

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
        console.log("updating content");
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
  .module("won.owner.components.petrinetViewer", [])
  .directive("wonPetrinetViewer", genComponentConf).name;
