/**
 * Created by maxstolze on 19.02.2018.
 */
import angular from "angular";

import "ng-redux";
import needMapModule from "./need-map.js";
import personDetailsModule from "./person-details.js";

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
            <h2 class="post-info__heading"
                ng-show="self.details.get('title')">
                <span ng-show="!self.searchString">Title</span>
                <span ng-show="self.searchString">Searching for</span>
            </h2>
            <p class="post-info__details"
                ng-show="self.details.get('title')">
                {{ self.details.get('title')}}
            </p>
            <h2 class="post-info__heading"
                ng-if="self.details.get('person')">
                Person Details
            </h2>
            <won-person-details 
              ng-if="self.details.get('person')"
              person="self.details.get('person')">
            </won-person-details>

           	<h2 class="post-info__heading"
                ng-show="self.details.get('description')">
                Description
            </h2>
            <p class="post-info__details--prewrap" ng-show="self.details.get('description')">{{ self.details.get('description')}}</p> <!-- no spaces or newlines within the code-tag, because it is preformatted -->

            <h2 class="post-info__heading"
                ng-show="self.details.get('tags')">
                Tags
            </h2>
            <div class="post-info__details post-info__tags"
                ng-show="self.details.get('tags')">
                    <span class="post-info__tags__tag" ng-repeat="tag in self.details.get('tags').toJS()">#{{tag}}</span>
            </div>

            <h2 class="post-info__heading" ng-if="self.details.getIn(['location','address'])">
                Location
            </h2>
            <p class="post-info__details clickable"
               ng-if="self.details.getIn(['location','address'])" ng-click="self.toggleMap()">
                {{ self.details.getIn(['location','address']) }}
				        <svg class="post-info__carret">
                  <use xlink:href="#ico-filter_map" href="#ico-filter_map"></use>
                </svg>
				        <svg class="post-info__carret" ng-show="!self.showMap">
	                <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
	              </svg>
                <svg class="post-info__carret" ng-show="self.showMap">
                   <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
                </svg>
            </p>                
            <won-need-map 
              locations="[self.details.get('location')]"
              ng-if="self.details.get('location') && self.showMap">
            </won-need-map>

            <h2 class="post-info__heading"
                ng-show="self.details.get('travelAction')">
                Route
            </h2>
            <p class="post-info__details clickable"
               ng-show="self.details.get('travelAction')"
               ng-click="self.toggleRouteMap()">

              <span ng-if="self.details.getIn(['travelAction', 'fromAddress'])">
                <strong>From: </strong>{{ self.details.getIn(['travelAction', 'fromAddress']) }}
              </span>
              </br>
              <span ng-if="self.details.getIn(['travelAction', 'toAddress'])">
              <strong>To: </strong>{{ self.details.getIn(['travelAction', 'toAddress']) }}
              </span>

              <svg class="post-info__carret">
                <use xlink:href="#ico-filter_map" href="#ico-filter_map"></use>
              </svg>
              <svg class="post-info__carret" ng-show="!self.showRouteMap">
                <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
              </svg>
              <svg class="post-info__carret" ng-show="self.showRouteMap">
                  <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
              </svg>
            </p>
            <won-need-map
              locations="self.travelLocations"
              ng-if="self.details.get('travelAction') && self.showRouteMap">
            </won-need-map>
    	`;

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);

      //TODO debug; deleteme
      window.isis4dbg = this;

      this.showMap = false;
      this.showRouteMap = false;
      this.allDetails = getAllDetails();
      const self = this;

      this.$scope.$watch("self.details.get('travelAction')", newValue => {
        self.travelLocations = newValue && [
          newValue.get("fromLocation"),
          newValue.get("toLocation"),
        ];
      });

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

      connect2Redux(selectFromState, actionCreators, [], this);
    }

    toggleMap() {
      this.showMap = !this.showMap;
    }

    toggleRouteMap() {
      this.showRouteMap = !this.showRouteMap;
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
    personDetailsModule,
  ])
  .directive("wonPostIsOrSeeksInfo", genComponentConf).name;
