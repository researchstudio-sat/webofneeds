import angular from "angular";
import ngAnimate from "angular-animate";
import labelledHrModule from "./labelled-hr.js";
import "ng-redux";
import { attach, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../won-utils.js";
import { useCaseGroups } from "useCaseDefinitions";

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
              ng-click="self.router__stateGoCurrent({useCaseGroup: 'all'})">
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
          ng-repeat="useCase in self.useCaseGroup.useCases"
          ng-if="self.displayableUseCase(useCase)"
          ng-click="self.startFrom(useCase)">
          <svg class="ucg__main__usecase__icon"
            ng-if="!!useCase.icon">
            <use xlink:href="{{ useCase.icon }}" href="{{ useCase.icon }}"></use>
          </svg>
          <div class="ucg__main__usecase__label"
            ng-if="!!useCase.label">
              {{ useCase.label }}
          </div>
        </div>
      </div>
    `;

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);

      window.ucg4dbg = this;
      this.useCaseGroups = useCaseGroups;

      const selectFromState = state => {
        const selectedGroup = getIn(state, [
          "router",
          "currentParams",
          "useCaseGroup",
        ]);
        return {
          useCaseGroup: selectUseCaseGroupFrom(selectedGroup, useCaseGroups),
        };
      };

      // Using actionCreators like this means that every action defined there is available in the template.
      connect2Redux(selectFromState, actionCreators, [], this);
    }

    /**
     * return if the given useCaseGroup is displayable or not
     * needs to have at least one displayable UseCase
     * @param useCase
     * @returns {*}
     */
    displayableUseCaseGroup(useCaseGroup) {
      const useCaseGroupValid =
        useCaseGroup &&
        (useCaseGroup.label || useCaseGroup.icon) &&
        useCaseGroup.useCases;

      if (useCaseGroupValid) {
        for (const key in useCaseGroup.useCases) {
          if (this.displayableUseCase(useCaseGroup.useCases[key])) {
            return true;
          }
        }
      }
      return false;
      // TODO: error handling: no group found/group empty
    }

    /**
     * return if the given useCase is displayable or not
     * @param useCase
     * @returns {*}
     */
    displayableUseCase(useCase) {
      return useCase && useCase.identifier && (useCase.label || useCase.icon);
    }

    startFrom(selectedUseCase) {
      const selectedUseCaseIdentifier =
        selectedUseCase && selectedUseCase.identifier;

      if (selectedUseCaseIdentifier) {
        this.router__stateGoCurrent({
          useCase: encodeURIComponent(selectedUseCaseIdentifier),
        });
      } else {
        console.warn(
          "No usecase identifier found for given usecase, ",
          selectedUseCase
        );
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

function selectUseCaseGroupFrom(selectedGroup, useCaseGroups) {
  if (selectedGroup) {
    for (const groupName in useCaseGroups) {
      if (selectedGroup === useCaseGroups[groupName]["identifier"]) {
        return useCaseGroups[groupName];
      }
    }
  }
  return undefined;
}

export default //.controller('CreateNeedController', [...serviceDependencies, CreateNeedController])
angular
  .module("won.owner.components.usecaseGroup", [ngAnimate, labelledHrModule])
  .directive("wonUsecaseGroup", genComponentConf).name;
