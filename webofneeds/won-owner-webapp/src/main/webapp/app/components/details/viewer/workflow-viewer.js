import angular from "angular";
import { attach, delay } from "../../../utils.js";
import BpmnViewer from "bpmn-js";

import "style/_workflow-viewer.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
        <div class="workflowv__header">
          <svg class="workflowv__header__icon" ng-if="self.detail.icon">
              <use xlink:href={{self.detail.icon}} href={{self.detail.icon}}></use>
          </svg>
          <span class="workflowv__header__label" ng-if="self.detail.label">{{self.detail.label}}</span>
        </div>
        <div class="workflowv__content" ng-show="self.content">
          <div class="workflowv__content__label clickable" ng-if="!self.parseError" ng-click="self.fitDiagramToViewport()">
            {{ self.content.get('name') }} (Click to Center Diagram)
          </div>
          <div class="workflowv__content__label" ng-if="self.parseError">
            {{ self.content.get('name') }} - unable to display WorkFlow Diagram!
          </div>
          <div class="workflowv__content__diagram" ng-attr-id="{{self.getUniqueDiagramId()}}"></div>
        </div>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.workflowv4dbg = this;

      this.$scope.$watch("self.content", (newContent, prevContent) =>
        this.updatedContent(newContent, prevContent)
      );
      this.$scope.$watch("self.details", (newDetails, prevDetails) =>
        this.updatedDetails(newDetails, prevDetails)
      );

      delay(0).then(() => this.loadDiagramFromContent());
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
        this.loadDiagramFromContent();
      }
    }

    getUniqueDiagramId() {
      return "diagram-" + this.$scope.$id;
    }

    initializeBpmnViewer() {
      console.log("init bpmnviewer for element: ", this.getUniqueDiagramId());
      this.bpmnViewer = new BpmnViewer({
        container: "#" + this.getUniqueDiagramId(),
      });

      if (this.bpmnViewer) {
        console.log("Init BpmnViewer Successful");
      } else {
        console.log("Init BpmnViewer Failed");
      }
    }

    loadDiagramFromContent() {
      if (this.content && this.content.get("data")) {
        console.log("content contains data");
        if (!this.bpmnViewer) {
          console.log(
            "bpmnViewer is not initialized... starting initialization"
          );
          this.initializeBpmnViewer();
        }
        console.log("calling importXML for the data in the content");
        this.bpmnViewer.importXML(atob(this.content.get("data")), err => {
          if (err) {
            console.error(
              "Workflow was invalid and could not be parsed, reason:",
              err
            );
            this.parseError = true;
          } else {
            console.log("Workflow rendered");
            this.fitDiagramToViewport();
            this.parseError = false;
          }
        });
      } else {
        console.log("content did not contain data");
      }
    }

    fitDiagramToViewport() {
      console.log("Fit Diagram To Viewport");
      const scale = this.bpmnViewer.get("canvas").zoom("fit-viewport", "auto");
      console.log("fitDiagram Scale: ", scale);
      if (isNaN(scale) || scale == 0) {
        console.log("scale was NaN or 0 zoom to level 0.2");
        this.bpmnViewer.get("canvas").zoom(0.2, "auto");
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
  .module("won.owner.components.workflowViewer", [])
  .directive("wonWorkflowViewer", genComponentConf).name;
