import angular from "angular";
import { attach, delay } from "../../../utils.js";
import { DomCache } from "../../../cstm-ng-utils.js";
import dropzoneModule from "../../file-dropzone.js";

import "~/style/_petrinetpicker.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <won-file-dropzone on-image-picked="::self.updateWorkflow(image)" accepts="::self.detail.accepts" multi-select="::false" ng-if="!self.addedWorkflow">
      </won-file-dropzone>
      <div class="petrinetp__preview" ng-show="self.addedWorkflow">
        <div class="petrinetp__preview__label clickable">
          {{ self.addedWorkflow.name }}
        </div>
        <svg
          class="petrinetp__preview__remove"
          ng-click="self.removeWorkflow()">
          <use xlink:href="#ico36_close" href="#ico36_close"></use>
        </svg>
        <svg class="petrinetp__preview__typeicon">
          <use xlink:href="#ico36_uc_transport_demand" href="#ico36_uc_transport_demand"></use>
        </svg>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.petrinetp4dbg = this;

      this.addedWorkflow = this.initialValue;

      delay(0).then(() => this.showInitialWorkflow());
    }

    /**
     * Checks validity and uses callback method
     */
    update(data) {
      if (data) {
        this.onUpdate({ value: data });
      } else {
        this.onUpdate({ value: undefined });
      }
    }

    showInitialWorkflow() {
      this.addedWorkflow = this.initialValue;
      this.$scope.$apply();
    }

    updateWorkflow(file) {
      this.addedWorkflow = file;
      this.update(this.addedWorkflow);
      this.$scope.$apply();
    }

    removeWorkflow() {
      this.addedWorkflow = undefined;
      this.update(this.addedWorkflow);
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
  .module("won.owner.components.petrinetpicker", [dropzoneModule])
  .directive("wonPetrinetPicker", genComponentConf).name;
