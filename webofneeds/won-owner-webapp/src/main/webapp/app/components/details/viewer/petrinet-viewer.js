import angular from "angular";
import { attach } from "../../../utils.js";
import petrinetStateModule from "../../petrinet-state.js";

import "~/style/_petrinet-viewer.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
        <div class="petrinetv__header">
          <svg class="petrinetv__header__icon" ng-if="self.detail.icon">
              <use xlink:href={{self.detail.icon}} href={{self.detail.icon}}></use>
          </svg>
          <span class="petrinetv__header__label" ng-if="self.detail.label">{{self.detail.label}}</span>
        </div>
        <div class="petrinetv__content">
          <won-petrinet-state
            class="petrinetv__content__state"
            ng-if="self.content && self.content.get('processURI')"
            process-uri="self.content.get('processURI')">
          </won-petrinet-state>
          <a class="petrinetv__content__download" ng-show="self.content"
            ng-href="data:{{self.content.get('type')}};base64,{{self.content.get('data')}}"
            download="{{ self.content.get('name') }}">
            <svg class="petrinetv__content__download__typeicon">
              <use xlink:href="#ico36_uc_transport_demand" href="#ico36_uc_transport_demand"></use>
            </svg>
            <div class="petrinetv__content__download__label clickable">
              Download '{{ self.content.get('name') }}'
            </div>
          </a>
        </div>
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
  .module("won.owner.components.petrinetViewer", [petrinetStateModule])
  .directive("wonPetrinetViewer", genComponentConf).name;
