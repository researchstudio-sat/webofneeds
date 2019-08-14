import angular from "angular";
import { delay } from "../../../utils.js";
import { attach, DomCache } from "../../../cstm-ng-utils.js";
import dropzoneModule from "../../file-dropzone.js";
import BpmnViewer from "bpmn-js";

import "~/style/_workflowpicker.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <won-file-dropzone on-image-picked="::self.updateWorkflow(image)" accepts="::self.detail.accepts" multi-select="::false" ng-if="!self.addedWorkflow">
      </won-file-dropzone>
      <div class="workflowp__preview" ng-show="self.addedWorkflow">
        <div class="workflowp__preview__label clickable" ng-click="self.fitDiagramToViewport()">
          {{ self.addedWorkflow.name }} (Click to Center Diagram)
        </div>
        <svg
          class="workflowp__preview__remove"
          ng-click="self.removeWorkflow()">
          <use xlink:href="#ico36_close" href="#ico36_close"></use>
        </svg>
        <div class="workflowp__preview__diagram clickable" ng-attr-id="{{self.getUniqueDiagramId()}}" ng-click="self.fitDiagramToViewport()"></div>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.workflowp4dbg = this;

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

      if (!this.bpmnViewer) {
        this.initializeBpmnViewer();
      }
      if (this.addedWorkflow && this.addedWorkflow.data) {
        this.bpmnViewer.importXML(atob(this.addedWorkflow.data), err => {
          if (err) {
            console.error(
              "Workflow was invalid and could not be parsed, reason:",
              err
            );
            this.removeWorkflow();
            this.$scope.$apply();
          } else {
            console.debug("Workflow rendered");
            this.fitDiagramToViewport();
            this.$scope.$apply();
          }
        });
      } else {
        this.$scope.$apply();
      }
    }

    updateWorkflow(file) {
      console.debug("called updateWorkflow: ", file);
      this.addedWorkflow = file;
      this.update(this.addedWorkflow);

      if (!this.bpmnViewer) {
        this.initializeBpmnViewer();
      }
      if (this.addedWorkflow && this.addedWorkflow.data) {
        this.bpmnViewer.importXML(atob(this.addedWorkflow.data), err => {
          if (err) {
            console.error(
              "Workflow was invalid and could not be parsed, reason:",
              err
            );
            this.removeWorkflow();
            this.$scope.$apply();
          } else {
            console.debug("Workflow rendered");
            this.$scope.$apply();

            const self = this;
            setTimeout(() => {
              self.fitDiagramToViewport();
            }, 0);
          }
        });
      } else {
        this.$scope.$apply();
      }
    }

    removeWorkflow() {
      this.addedWorkflow = undefined;
      this.update(this.addedWorkflow);
    }

    initializeBpmnViewer() {
      console.debug("init bpmnviewer for element: ", this.getUniqueDiagramId());
      this.bpmnViewer = new BpmnViewer({
        container: "#" + this.getUniqueDiagramId(),
      });

      if (this.bpmnViewer) {
        console.debug("Init BpmnViewer Successful");
      } else {
        console.debug("Init BpmnViewer Failed");
      }
    }

    fitDiagramToViewport() {
      console.debug("Fit Diagram To Viewport");
      const scale = this.bpmnViewer.get("canvas").zoom("fit-viewport", "auto");
      console.debug("fitDiagram Scale: ", scale);
      if (isNaN(scale) || scale == 0) {
        console.debug("scale was NaN or 0 zoom to level 0.2");
        this.bpmnViewer.get("canvas").zoom(0.2, "auto");
      }
    }

    getUniqueDiagramId() {
      return "diagram-" + this.$scope.$id;
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
  .module("won.owner.components.workflowPicker", [dropzoneModule])
  .directive("wonWorkflowPicker", genComponentConf).name;
