import angular from "angular";
import { attach } from "../../../utils.js";

import "style/_instrument-viewer.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
        <div class="iv__header">
          <svg class="iv__header__icon" ng-if="self.detail.icon">
              <use xlink:href={{self.detail.icon}} href={{self.detail.icon}}></use>
          </svg>
          <span class="iv__header__label" ng-if="self.detail.label">{{self.detail.label}}</span>
        </div>
        <div class="iv__content">
          <div class="iv__content__label" ng-if="self.content.get('instrument')">
             Instrument
          </div>
          <div class="iv__content__value" ng-if="self.content.get('instrument')">
            {{self.content.get('instrument')}}
          </div>
          <div class="iv__content__label" ng-if="self.content.get('genre')">
            Genre
          </div>
          <div class="iv__content__value" ng-if="self.content.get('genre')">
            {{self.content.get('genre')}}
          </div>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      //TODO: debug; deleteme
      window.musician4dbg = this;

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
  .module("won.owner.components.instrumentViewerModule", [])
  .directive("wonInstrumentViewer", genComponentConf).name;
