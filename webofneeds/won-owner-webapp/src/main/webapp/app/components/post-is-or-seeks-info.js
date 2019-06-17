/**
 * Created by maxstolze on 19.02.2018.
 */
import angular from "angular";

import "ng-redux";

import { attach } from "../utils.js";
import * as useCaseUtils from "../usecase-utils.js";
import { connect2Redux } from "../configRedux.js";
import { actionCreators } from "../actions/actions.js";

import "~/style/_post-is-or-seeks-info.scss";
import { title } from "../../config/details/basic.js";

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
          ng-if="detail.identifier && detail.viewerComponent && self.getDetailContent(detail.identifier)"
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

      this.allDetails = useCaseUtils.getAllDetails();

      const selectFromState = state => {
        const post = this.postUri && state.getIn(["atoms", this.postUri]);
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
      if (this.branch == "content" && key == title.identifier) {
        return undefined;
      } else {
        return key && this.details && this.details.get(key);
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
      branch: "=",
      postUri: "=",
    },
    template: template,
  };
}

export default //.controller('CreateAtomController', [...serviceDependencies, CreateAtomController])
angular
  .module("won.owner.components.postIsOrSeeksInfo", [])
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
