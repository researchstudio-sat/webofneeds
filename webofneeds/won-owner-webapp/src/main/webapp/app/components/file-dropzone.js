/**
 * Created by ksinger on 16.09.2015.
 */
import angular from "angular";
import enterModule from "../directives/enter.js";
import { dispatchEvent, readAsDataURL } from "../utils.js";
import { attach } from "../cstm-ng-utils.js";
import globToRegexp from "glob-to-regexp";

import "~/style/_file-dropzone.scss";

function genComponentConf() {
  let template = `
        <svg class="wid__dropzone__bg">
            <use xlink:href="{{self.getIcon()}}" href="{{self.getIcon()}}"></use>
        </svg>
        <input type="file" accept="{{self.accepts}}" ng-multiple="::self.multiSelect"/>
    `;

  const serviceDependencies = [
    "$scope",
    "$element" /*injections as strings here*/,
  ];

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);

      window.idc4dbg = this;

      const fileInput = this.$element[0].querySelector("input");

      fileInput.addEventListener("change", () => {
        for (const file of fileInput.files) {
          this.fileDropped(file);
        }
      });

      this.$element[0].addEventListener("dragenter", e =>
        this.handleDragOver(e)
      );

      this.$element[0].addEventListener("dragover", e =>
        this.handleDragOver(e)
      );

      this.$element[0].addEventListener("dragleave", () =>
        this.handleDragStop()
      );

      this.$element[0].addEventListener("drop", e => this.handleDrop(e));

      this.$element[0].addEventListener("click", e => {
        if (e.target == e.currentTarget) {
          this.openFilePicker();
        }
      });
    }

    getIcon() {
      if (this.invalid) {
        return "#ico36_close";
      } else {
        return "#illu_drag_here";
      }
    }

    handleDragOver(event) {
      event.preventDefault();
      event.stopPropagation();
      const items = event.dataTransfer.items;

      if (!this.multiSelect && items.length > 1) {
        this.$element[0].classList.add("invalid");
        this.$element[0].classList.remove("valid");
        this.invalid = true;
        event.dataTransfer.dropEffect = "none";
        this.$scope.$digest();
        return;
      }

      for (const item of items) {
        if (item.kind !== "file" || !this.fileIsValid(item)) {
          //show error state
          this.$element[0].classList.add("invalid");
          this.$element[0].classList.remove("valid");
          this.invalid = true;
          event.dataTransfer.dropEffect = "none";
          this.$scope.$digest();
          return;
        }
      }

      this.$element[0].classList.add("valid");
      this.$element[0].classList.remove("invalid");
      event.dataTransfer.dropEffect = "copy";
      this.invalid = false;
      this.$scope.$digest();
    }

    handleDragStop() {
      this.invalid = false;
      this.$element[0].classList.remove("valid", "invalid");
      this.$scope.$digest();
    }

    handleDrop(event) {
      event.preventDefault();
      event.stopPropagation();
      this.handleDragStop();

      const items = event.dataTransfer.items;
      for (const item of items) {
        if (item.kind !== "file" || !this.fileIsValid(item)) {
          return;
        }
      }

      for (const item of items) {
        this.fileDropped(item.getAsFile());
      }
    }

    openFilePicker() {
      const fileInput = this.$element[0].querySelector("input");
      fileInput.value = "";
      fileInput.click();
    }

    fileIsValid(f) {
      return !this.accepts || globToRegexp(this.accepts).test(f.type);
    }

    fileDropped(f) {
      if (this.fileIsValid(f)) {
        readAsDataURL(f)
          .then(dataUrl => {
            this.$scope.$digest(); // so the preview is displayed

            const b64data = dataUrl.split("base64,")[1];
            const fileData = {
              name: f.name,
              type: f.type,
              data: b64data,
            };
            return fileData;
          })
          .then(imageData => {
            this.onImagePicked({ image: imageData });
            dispatchEvent(this.$element[0], "image-picked", imageData);
          });
      } else {
        console.warn("FILE TYPE NOT ACCEPTED");
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
      onImagePicked: "&",
      accepts: "=",
      multiSelect: "=",
    },
    template: template,
  };
}
export default angular
  .module("won.owner.components.fileDropzone", [enterModule])
  .directive("ngMultiple", function() {
    return {
      restrict: "A",
      scope: {
        ngMultiple: "=",
      },
      link: function(scope, element) {
        scope.$watch("ngMultiple", function(newValue) {
          if (newValue) {
            element.attr("multiple", "multiple");
          } else {
            element.removeAttr("multiple");
          }
        });
      },
    };
  })
  .directive("wonFileDropzone", genComponentConf).name;
