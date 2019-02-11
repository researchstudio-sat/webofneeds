import angular from "angular";
import ngAnimate from "angular-animate";
import labelledHrModule from "./labelled-hr.js";
import "ng-redux";
import { attach } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../won-utils.js";
import * as useCaseDefinitions from "useCaseDefinitions";
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
          ng-repeat="element in self.useCaseGroup.useCases"
          ng-if="self.useCaseDefinitions.isDisplayableUseCase(element)"
          ng-click="self.startFrom(element)">
          <svg class="ucg__main__usecase__icon"
            ng-if="!!element.icon">
            <use xlink:href="{{ element.icon }}" href="{{ element.icon }}"></use>
          </svg>
          <div class="ucg__main__usecase__label"
            ng-if="!!element.label">
              {{ element.label }}
          </div>
        </div>
      </div>
    `;

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);

      window.ucg4dbg = this;
      this.useCaseDefinitions = useCaseDefinitions;

      const selectFromState = state => {
        const selectedGroup = getUseCaseGroupFromRoute(state);
        return {
          useCaseGroup: useCaseDefinitions.getUseCaseGroupByIdentifier(
            selectedGroup
          ),
        };
      };

      // Using actionCreators like this means that every action defined there is available in the template.
      connect2Redux(selectFromState, actionCreators, [], this);
    }

    startFrom(element) {
      const elementIdentifier = element && element.identifier;

      if (elementIdentifier) {
        if (useCaseDefinitions.isUseCaseGroup(element)) {
          this.router__stateGoCurrent({
            useCaseGroup: encodeURIComponent(elementIdentifier),
          });
        } else {
          this.router__stateGoCurrent({
            useCase: encodeURIComponent(elementIdentifier),
          });
        }
      } else {
        console.warn("No identifier found for given usecase, ", element);
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
