/**
 * Created by maxstolze on 19.02.2018.
 */
import angular from "angular";

import "ng-redux";
// TODO: these should be replaced by importing defintions from config
import personViewerModule from "./details/viewer/person-viewer.js";
import descriptionViewerModule from "./details/viewer/description-viewer.js";
import locationViewerModule from "./details/viewer/location-viewer.js";
import tagsViewerModule from "./details/viewer/tags-viewer.js";
import travelActionViewerModule from "./details/viewer/travel-action-viewer.js";
import titleViewerModule from "./details/viewer/title-viewer.js";
import numberViewerModule from "./details/viewer/number-viewer.js";
import datetimeViewerModule from "./details/viewer/datetime-viewer.js";
import dropdownViewerModule from "./details/viewer/dropdown-viewer.js";
import selectViewerModule from "./details/viewer/select-viewer.js";
import rangeViewerModule from "./details/viewer/range-viewer.js";
import fileViewerModule from "./details/viewer/file-viewer.js";
import workflowViewerModule from "./details/viewer/workflow-viewer.js";
import petrinetViewerModule from "./details/viewer/petrinet-viewer.js";

import { attach } from "../utils.js";
import { getAllDetails } from "../won-utils.js";
import { connect2Redux } from "../won-utils.js";
import { actionCreators } from "../actions/actions.js";

import "style/_post-is-or-seeks-info.scss";

//TODO can't inject $scope with the angular2-router, preventing redux-cleanup
const serviceDependencies = [
  "$ngRedux",
  "$scope",
  "$element" /*, '$routeParams' /*injections as strings here*/,
];

function genComponentConf() {
  const template = `
      <!-- COMPONENT -->
        <div class="pis__component"
          ng-repeat="detail in self.allDetails"
          ng-if="detail.identifier && self.getDetailContent(detail.identifier)"
          detail-viewer-element="{{detail.viewerComponent}}"
          detail="detail"
          content="self.getDetailContent(detail.identifier)">
        </div>
    	`;

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);

      //TODO debug; deleteme
      window.isis4dbg = this;

      this.allDetails = getAllDetails();

      const selectFromState = state => {
        const post = this.postUri && state.getIn(["needs", this.postUri]);
        const details = this.branch && post && post.get(this.branch);

        return {
          details,
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.branch", "self.postUri"],
        this
      );
    }

    getDetail(key) {
      const detail = this.allDetails && this.allDetails[key];
      if (!detail) {
        console.error(
          "Could not find detail with key: ",
          key,
          " in:  ",
          this.allDetails
        );
      }
      return detail;
    }

    getDetailContent(key) {
      return key && this.details && this.details.get(key);
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      branch: "=",
      postUri: "=",
    },
    template: template,
  };
}

export default //.controller('CreateNeedController', [...serviceDependencies, CreateNeedController])
angular
  .module("won.owner.components.postIsOrSeeksInfo", [
    personViewerModule,
    descriptionViewerModule,
    locationViewerModule,
    travelActionViewerModule,
    tagsViewerModule,
    titleViewerModule,
    numberViewerModule,
    dropdownViewerModule,
    datetimeViewerModule,
    selectViewerModule,
    rangeViewerModule,
    fileViewerModule,
    workflowViewerModule,
    petrinetViewerModule,
  ])
  .directive("detailViewerElement", [
    "$compile",
    function($compile) {
      return {
        restrict: "A",
        scope: {
          content: "=",
          detail: "=",
        },
        link: function(scope, element, attrs) {
          const customTag = attrs.detailViewerElement;
          if (!customTag) return;

          const customElem = angular.element(
            `<${customTag} detail="detail" content="content"></${customTag}>`
          );

          element.append($compile(customElem)(scope));
        },
      };
    },
  ])
  .directive("wonPostIsOrSeeksInfo", genComponentConf).name;
