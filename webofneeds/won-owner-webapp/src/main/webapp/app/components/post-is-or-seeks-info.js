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

import { attach } from "../utils.js";
import { getAllDetails } from "../won-utils.js";
import { connect2Redux } from "../won-utils.js";
import { actionCreators } from "../actions/actions.js";
import { selectOpenPostUri } from "../selectors.js";

//TODO can't inject $scope with the angular2-router, preventing redux-cleanup
const serviceDependencies = [
  "$ngRedux",
  "$scope",
  "$element" /*, '$routeParams' /*injections as strings here*/,
];

function genComponentConf() {
  const template = `
        <won-title-viewer ng-if="self.searchString && self.details.get('title')" content="self.details.get('title')" detail="::{ label: 'Searching for' }">
        </won-title-viewer>

        <won-title-viewer ng-if="!self.searchString && self.details.get('title')" content="self.details.get('title')" detail="::{ label: 'Title' }">
        </won-title-viewer>

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
        const postUri = selectOpenPostUri(state);
        const post = postUri && state.getIn(["needs", postUri]);
        const details = this.branch && post && post.get(this.branch);
        const searchString =
          post && this.branch === "seeks"
            ? post.get("searchString")
            : undefined; //workaround to display searchString only in seeks

        return {
          searchString,
          details,
        };
      };

      connect2Redux(selectFromState, actionCreators, ["self.branch"], this);
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
