import angular from "angular";
import { attach } from "../../../utils.js";

import "~/style/_image-viewer.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
        <div class="imagev__header">
          <svg class="imagev__header__icon" ng-if="self.detail.icon">
              <use xlink:href={{self.detail.icon}} href={{self.detail.icon}}></use>
          </svg>
          <span class="imagev__header__label" ng-if="self.detail.label">{{self.detail.label}}</span>
        </div>
        <div class="imagev__content" ng-if="self.content && self.content.size > 0">
          <div class="imagev__content__selected">
            <img class="imagev__content__selected__image"
              ng-click="self.openImageInNewTab(self.getSelectedImage())"
              alt="{{self.getSelectedImage().get('name')}}"
              ng-src="data:{{self.getSelectedImage().get('type')}};base64,{{self.getSelectedImage().get('data')}}"/>
          </div>
          <div class="imagev__content__thumbnails" ng-if="self.content.size > 1">
            <div class="imagev__content__thumbnails__thumbnail"
              ng-repeat="file in self.content.toArray()"
              ng-if="self.isImage(file)"
              ng-class="{
                'imagev__content__thumbnails__thumbnail--selected': self.selectedIndex == $index,
              }">
              <img class="imagev__content__thumbnails__thumbnail__image" ng-click="self.changeSelectedIndex($index)"
                alt="{{file.get('name')}}"
                ng-src="data:{{file.get('type')}};base64,{{file.get('data')}}"/>
            </div>
          </div>
        </div>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.imagev4dbg = this;
      this.selectedIndex = 0;
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

    changeSelectedIndex(index) {
      this.selectedIndex = index;
    }

    getSelectedImage() {
      return this.content && this.content.toArray()[this.selectedIndex];
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
  .module("won.owner.components.imageViewer", [])
  .directive("wonImageViewer", genComponentConf).name;
