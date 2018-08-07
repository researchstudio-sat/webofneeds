/**
 * Created by ksinger on 16.09.2015.
 */
import angular from "angular";
import "angular-sanitize";
import enterModule from "../directives/enter.js";
import { dispatchEvent, attach, readAsDataURL } from "../utils.js";

function genComponentConf() {
  let template = `
        <div class="wid__dropzone--empty">
            <input type="file" accept="{{self.accepts}}" />
            <svg class="wid__dropzone__bg" style="--local-primary:#CCD2D2;">
                <use xlink:href="#illu_drag_here" href="#illu_drag_here"></use>
            </svg>
        </div>
    `;

  const serviceDependencies = [
    "$scope",
    "$element" /*injections as strings here*/,
  ];

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);

      window.idc4dbg = this;

      this.$element[0]
        .querySelector('input[type="file"]')
        .addEventListener("change", e => this.fileDropped(e.target.files[0]));
    }

    fileDropped(f) {
      if (this.accepts && new RegExp(this.accepts.replace('/','\/')).test(f.type)) {
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
        console.log("FILE TYPE NOT ACCEPTED");
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
    },
    template: template,
  };
}
export default angular
  .module("won.owner.components.fileDropzone", [enterModule])
  .directive("wonFileDropzone", genComponentConf).name;
