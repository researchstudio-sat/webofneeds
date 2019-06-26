import angular from "angular";
import { attach } from "../../../cstm-ng-utils.js";

import "~/style/_file-viewer.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
        <div class="filev__header">
          <svg class="filev__header__icon" ng-if="self.detail.icon">
              <use xlink:href={{self.detail.icon}} href={{self.detail.icon}}></use>
          </svg>
          <span class="filev__header__label" ng-if="self.detail.label">{{self.detail.label}}</span>
        </div>
        <div class="filev__content" ng-if="self.content && self.content.size > 0">
          <div class="filev__content__item"
            ng-repeat="file in self.content.toArray()">
            <a class="filev__content__item__inner"
              ng-href="data:{{file.get('type')}};base64,{{file.get('data')}}"
              download="{{ file.get('name') }}"
              ng-if="!self.isImage(file)">
              <svg class="filev__content__item__inner__typeicon">
                <use xlink:href="#ico36_uc_transport_demand" href="#ico36_uc_transport_demand"></use>
              </svg>
              <div class="filev__content__item__inner__label">
                {{ file.get('name') }}
              </div>
            </a>
            <a class="filev__content__item__inner"
              ng-click="self.openImageInNewTab(file)"
              ng-if="self.isImage(file)">
              <img class="filev__content__item__inner__image"
                alt="{{file.get('name')}}"
                ng-src="data:{{file.get('type')}};base64,{{file.get('data')}}"/>
              <div class="filev__content__item__inner__label">
                {{ file.get('name') }}
              </div>
            </a>
          </div>
        </div>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.filev4dbg = this;

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

    isImage(file) {
      return file && /^image\//.test(file.get("type"));
    }

    openImageInNewTab(file) {
      if (file) {
        let image = new Image();
        image.src = "data:" + file.get("type") + ";base64," + file.get("data");

        let w = window.open("");
        w.document.write(image.outerHTML);
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
  .module("won.owner.components.fileViewer", [])
  .directive("wonFileViewer", genComponentConf).name;
