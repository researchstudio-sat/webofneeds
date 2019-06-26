import angular from "angular";
import { delay } from "../../../utils.js";
import { attach, DomCache } from "../../../cstm-ng-utils.js";
import dropzoneModule from "../../file-dropzone.js";

import "~/style/_imagepicker.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <won-file-dropzone on-image-picked="::self.updateImages(image)" accepts="::self.detail.accepts" multi-select="::self.detail.multiSelect" ng-if="self.detail.multiSelect || !self.addedImages || self.addedImages.length == 0">
      </won-file-dropzone>
      <div class="imagep__header" ng-if="self.addedImages && self.addedImages.length > 0">
        {{ self.getUploadedHeader() }}
      </div>
      <div class="imagep__preview" ng-if="self.addedImages && self.addedImages.length > 0">
        <div class="imagep__preview__item"
          ng-repeat="image in self.addedImages"
          ng-if="self.isImage(image)"
          ng-class="{
            'imagep__preview__item--default': image.default,
          }">
          <div class="imagep__preview__item__label" ng-click="self.setImageAsDefault(image)">
            {{ image.name }}
          </div>
          <svg
            class="imagep__preview__item__remove"
            ng-click="self.removeImage(image)">
            <use xlink:href="#ico36_close" href="#ico36_close"></use>
          </svg>
          <img class="imagep__preview__item__image"
            ng-click="self.setImageAsDefault(image)"
            alt="{{image.name}}"
            ng-src="data:{{image.type}};base64,{{image.data}}"/>
          <div class="imagep__preview__item__default" ng-click="self.setImageAsDefault(image)">
            <span ng-if="image.default">Default Image</span>
            <span class="imagep__preview__item__default__set" ng-if="!image.default">Click to set Image as Default</span>
          </div>
        </div>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.imagep4dbg = this;

      this.addedImages = this.initialValue;

      delay(0).then(() => this.showInitialImages());
    }

    /**
     * Checks validity and uses callback method
     */
    update(data) {
      if (data && data.length > 0) {
        this.onUpdate({ value: data });
      } else {
        this.onUpdate({ value: undefined });
      }
    }

    showInitialImages() {
      this.addedImages = this.initialValue;
      this.$scope.$apply();
    }

    updateImages(image) {
      if (!this.addedImages) {
        this.addedImages = [];
      }
      if (this.isImage(image)) {
        if (this.addedImages.length == 0) {
          image.default = true;
        }
        this.addedImages.push(image);
        this.update(this.addedImages);
      }
      this.$scope.$apply();
    }

    setImageAsDefault(imageToSetAsDefault) {
      if (imageToSetAsDefault) {
        this.addedImages = this.addedImages.map(image => {
          image.default = image === imageToSetAsDefault;
          return image;
        });
        this.update(this.addedImages);
      }
    }

    removeImage(imageToRemove) {
      if (!this.addedImages) {
        this.addedImages = [];
      }
      this.addedImages = this.addedImages.filter(
        image => imageToRemove !== image
      );

      const hasDefaultImage = !!this.addedImages.find(image => image.default);

      if (!hasDefaultImage && this.addedImages.length > 0) {
        this.addedImages[0].default = true;
      }

      this.update(this.addedImages);
    }

    isImage(image) {
      return image && /^image\//.test(image.type);
    }

    getUploadedHeader() {
      return this.addedImages && this.addedImages.length > 1
        ? "Chosen Images:"
        : "Chosen Image:";
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      onUpdate: "&",
      initialValue: "=",
      detail: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.imagePicker", [dropzoneModule])
  .directive("wonImagePicker", genComponentConf).name;
