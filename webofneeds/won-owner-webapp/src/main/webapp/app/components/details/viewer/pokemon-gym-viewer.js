import angular from "angular";
import "ng-redux";
import { actionCreators } from "../../../actions/actions.js";
import { attach, get } from "../../../utils.js";
import { connect2Redux } from "../../../won-utils.js";
import * as generalSelectors from "../../../selectors/general-selectors.js";
import atomMapModule from "../../atom-map.js";
import "angular-marked";

import "style/_pokemon-gym-viewer.scss";
import "style/_won-markdown.scss";

const serviceDependencies = ["$scope", "$ngRedux", "$element"];
function genComponentConf() {
  let template = `
        <div class="pgv__header">
          <svg class="pgv__header__icon" ng-if="self.detail.icon">
              <use xlink:href={{self.detail.icon}} href={{self.detail.icon}}></use>
          </svg>
          <span class="pgv__header__label" ng-if="self.detail.label">{{self.detail.label}}</span>
        </div>
        <div class="pgv__content">
          <div class="pgv__content__label">Location:</div>
          <div class="pgv__content__name" ng-click="self.toggleMap()">
            <div class="pgv__content__name__label">{{ self.name }}<span class="pgv__content__name__label__ex" ng-if="self.ex">(Ex Gym)</span></div>
            <svg class="pgv__content__name__carret">
              <use xlink:href="#ico-filter_map" href="#ico-filter_map"></use>
            </svg>
            <svg class="pgv__content__name__carret" ng-show="!self.showMap">
              <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
            </svg>
            <svg class="pgv__content__name__carret" ng-show="self.showMap">
               <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
            </svg>
          </div>
          <!-- TODO Location -->
          <won-atom-map
            locations="[self.location]"
            current-location="self.currentLocation"
            ng-if="self.location && self.showMap">
          </won-atom-map>
          <div class="pgv__content__label pgv__content__label--info" ng-if="self.info" ng-click="self.toggleAdditionalInfo()">
            Additional Information
            <svg class="pgv__content__label__carret" ng-show="!self.showAdditionalInfo">
              <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
            </svg>
            <svg class="pgv__content__label__carret" ng-show="self.showAdditionalInfo">
               <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
            </svg>
          </div>
          <div class="pgv__content__info markdown" marked="self.info" ng-if="self.info && self.showAdditionalInfo"></div>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.paypalpaymentv4dbg = this;
      this.showMap = false;
      this.showAdditionalInfo = false;

      const selectFromState = state => {
        const currentLocation = generalSelectors.getCurrentLocation(state);

        const ex = get(this.content, "ex");
        const name = get(this.content, "name");
        const info = get(this.content, "info");
        const location = get(this.content, "location");

        return {
          currentLocation,
          name,
          info,
          location,
          ex,
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.content", "self.detail"],
        this
      );
    }

    toggleMap() {
      this.showMap = !this.showMap;
    }
    toggleAdditionalInfo() {
      this.showAdditionalInfo = !this.showAdditionalInfo;
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
  .module("won.owner.components.pokemonGymViewer", [atomMapModule, "hc.marked"])
  .directive("pokemonGymViewer", genComponentConf).name;
