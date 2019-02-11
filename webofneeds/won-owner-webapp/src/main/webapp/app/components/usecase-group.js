import angular from "angular";
import ngAnimate from "angular-animate";
import labelledHrModule from "./labelled-hr.js";
import "ng-redux";
import { attach } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../won-utils.js";
import * as useCaseUtils from "../usecase-utils.js";
import { getUseCaseGroupFromRoute } from "../selectors/general-selectors.js";

import "style/_usecase-group.scss";

const serviceDependencies = [
  "$ngRedux",
  "$scope",
  "$element" /*'$routeParams' /*injections as strings here*/,
];

function genComponentConf() {
  const template = `
      <!-- HEADER -->
      <div class="ucg__header">
          <a class="cp__header__back clickable"
              ng-click="self.router__back()">
              <svg style="--local-primary:var(--won-primary-color);"
                  class="ucg__header__back__icon show-in-responsive">
                  <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
              </svg>
              <svg style="--local-primary:var(--won-primary-color);"
                  class="ucg__header__back__icon hide-in-responsive">
                  <use xlink:href="#ico36_close" href="#ico36_close"></use>
              </svg>
          </a>
          <svg class="ucg__header__icon"
              ng-if="!!self.useCaseGroup.icon">
              <use xlink:href="{{ self.useCaseGroup.icon }}" href="{{ self.useCaseGroup.icon }}"></use>
          </svg>
          <div class="ucg__header__title"
              ng-if="!!self.useCaseGroup.label">
                {{ self.useCaseGroup.label }}
          </div>
      </div>

      <!-- USE CASES -->
      <div class="ucg__main">
        <div class="ucg__main__usecase clickable"
          ng-repeat="subItem in self.useCaseGroup.subItems"
          ng-if="self.useCaseUtils.isDisplayableItem(subItem)"
          ng-click="self.startFrom(subItem)">
          <svg class="ucg__main__usecase__icon"
            ng-if="!!subItem.icon">
            <use xlink:href="{{ subItem.icon }}" href="{{ subItem.icon }}"></use>
          </svg>
          <div class="ucg__main__usecase__label"
            ng-if="!!subItem.label">
              {{ subItem.label }}
          </div>
        </div>
      </div>
    `;

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);

      window.ucg4dbg = this;
      this.useCaseUtils = useCaseUtils;

      const selectFromState = state => {
        const selectedGroup = getUseCaseGroupFromRoute(state);
        return {
          useCaseGroup: useCaseUtils.getUseCaseGroupByIdentifier(selectedGroup),
        };
      };

      // Using actionCreators like this means that every action defined there is available in the template.
      connect2Redux(selectFromState, actionCreators, [], this);
    }

    startFrom(subItem) {
      const subItemIdentifier = subItem && subItem.identifier;

      if (subItemIdentifier) {
        if (useCaseUtils.isUseCaseGroup(subItem)) {
          this.router__stateGoCurrent({
            useCaseGroup: encodeURIComponent(subItemIdentifier),
          });
        } else {
          this.router__stateGoCurrent({
            useCase: encodeURIComponent(subItemIdentifier),
          });
        }
      } else {
        console.warn("No identifier found for given usecase, ", subItem);
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
      /*scope-isolation*/
    },
    template: template,
  };
}

export default //.controller('CreateNeedController', [...serviceDependencies, CreateNeedController])
angular
  .module("won.owner.components.usecaseGroup", [ngAnimate, labelledHrModule])
  .directive("wonUsecaseGroup", genComponentConf).name;
