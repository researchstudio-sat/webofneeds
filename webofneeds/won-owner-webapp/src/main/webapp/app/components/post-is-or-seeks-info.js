/**
 * Created by maxstolze on 19.02.2018.
 */
import angular from "angular";

import "ng-redux";
import needMapModule from "./need-map.js";
import personViewerModule from "./details/viewer/person-viewer.js";
import descriptionViewerModule from "./details/viewer/description-viewer.js";
import locationViewerModule from "./details/viewer/location-viewer.js";
import tagsViewerModule from "./details/viewer/tags-viewer.js";
import routeViewerModule from "./details/viewer/route-viewer.js";
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

        <won-person-viewer ng-if="self.details.get('person')" content="self.details.get('person')" detail="self.getDetail('person')">
        </won-person-viewer>

        <won-description-viewer ng-if="self.details.get('description')" content="self.details.get('description')" detail="self.getDetail('description')">
        </won-description-viewer>

        <won-tags-viewer ng-if="self.details.get('tags')" content="self.details.get('tags')" detail="self.getDetail('tags')">
        </won-tags-viewer>

        <won-location-viewer ng-if="self.details.get('location')" content="self.details.get('location')" detail="self.getDetail('location')">
        </won-location-viewer>

        <won-route-viewer ng-if="self.details.get('travelAction')" content="self.details.get('travelAction')" detail="self.getDetail('route')"> <!-- TODO: rename detail to travelAction -->
        </won-route-viewer>
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
    needMapModule,
    personViewerModule,
    descriptionViewerModule,
    locationViewerModule,
    routeViewerModule,
    tagsViewerModule,
    titleViewerModule,
  ])
  .directive("wonPostIsOrSeeksInfo", genComponentConf).name;
