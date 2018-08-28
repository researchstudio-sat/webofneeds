import angular from "angular";
import ngAnimate from "angular-animate";
import labelledHrModule from "./labelled-hr.js";
import "ng-redux";
import { attach, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../won-utils.js";
import { useCaseGroups } from "useCaseDefinitions";

import "style/_usecase-group-content.scss";

const serviceDependencies = [
  "$ngRedux",
  "$scope",
  "$element" /*'$routeParams' /*injections as strings here*/,
];

function genComponentConf() {
  const template = `
  <div class="ucgc__usecasegroup"
    ng-if="self.displayableUseCaseGroup(self.useCaseGroup)">
      <!-- HEADER -->
      <div class="ucgc__usecasegroup__header">
        <svg class="ucgc__usecasegroup__header__icon"
          ng-if="!!self.useCaseGroup.icon">
          <use xlink:href="{{ self.useCaseGroup.icon }}" href="{{ self.useCaseGroup.icon }}"></use>
        </svg>
        <div class="ucgc__usecasegroup__header__label"
          ng-if="!!self.useCaseGroup.label">
            {{ self.useCaseGroup.label }}
        </div>
      </div>
      <!-- USE CASES -->
      <div class="ucgc__usecasegroup__usecases">
        <div class="ucgc__usecasegroup__usecases__usecase clickable"
          ng-repeat="useCase in self.useCaseGroup.useCases"
          ng-if="self.displayableUseCase(useCase)"
          ng-click="self.startFrom(useCase)">
          <svg class="ucgc__usecasegroup__usecases__usecase__icon"
            ng-if="!!useCase.icon">
            <use xlink:href="{{ useCase.icon }}" href="{{ useCase.icon }}"></use>
          </svg>
          <div class="ucgc__usecasegroup__usecases__usecase__label"
            ng-if="!!useCase.label">
              {{ useCase.label }}
          </div>
        </div>
      </div>
  </div>
    `;

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);

      window.ucgc4dbg = this;
      this.useCaseGroups = useCaseGroups;
      // this.showUseCaseGroupHeaders = this.showUseCaseGroups();
      // this.showUseCaseGroups = false;

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
        console.log(
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
  .module("won.owner.components.usecaseGroupContent", [
    ngAnimate,
    labelledHrModule,
  ])
  .directive("wonUsecaseGroupContent", genComponentConf).name;
