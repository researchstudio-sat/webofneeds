/**
 * Created by quasarchimaere on 03.07.2018.
 */
import angular from "angular";
import ngAnimate from "angular-animate";

import "ng-redux";
import { attach } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../won-utils.js";
import { selectIsConnected } from "../selectors.js";
import { useCaseGroups } from "useCaseDefinitions";

const serviceDependencies = [
  "$ngRedux",
  "$scope",
  "$element" /*'$routeParams' /*injections as strings here*/,
];

function genComponentConf() {
  const template = `
        <div class="ucp__header">
            <a class="ucp__header__back clickable"
                ng-click="self.router__stateGoCurrent({showCreateView: undefined, useCase: undefined})">
                <svg style="--local-primary:var(--won-primary-color);"
                    class="ucp__header__back__icon">
                    <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                </svg>
            </a>
            <span class="ucp__header__title">What would you like to post?</span>
        </div>
        <div class="ucp__content">
          <div class="ucp__content__usecasegroup"
            ng-repeat="useCaseGroup in self.useCaseGroups"
            ng-if="self.displayableUseCaseGroup(useCaseGroup)">
              <div class="ucp__content__usecasegroup__header"
                ng-if="self.showUseCaseGroupHeaders">
                <svg class="ucp__content__usecasegroup__header__icon"
                  ng-if="!!useCaseGroup.icon">
                  <use xlink:href="{{ useCaseGroup.icon }}" href="{{ useCaseGroup.icon }}"></use>
                </svg>
                <div class="ucp__content__usecasegroup__header__label"
                  ng-if="!!useCaseGroup.label">
                    {{ useCaseGroup.label }}
                </div>
              </div>
              <div class="ucp__content__usecasegroup__usecases">
                <div class="ucp__content__usecasegroup__usecases__usecase clickable"
                  ng-repeat="useCase in useCaseGroup.useCases"
                  ng-if="self.displayableUseCase(useCase)"
                  ng-click="self.startFrom(useCase)">
                  <svg class="ucp__content__usecasegroup__usecases__usecase__icon"
                    ng-if="!!useCase.icon">
                    <use xlink:href="{{ useCase.icon }}" href="{{ useCase.icon }}"></use>
                  </svg>
                  <div class="ucp__content__usecasegroup__usecases__usecase__label"
                    ng-if="!!useCase.label">
                      {{ useCase.label }}
                  </div>
                </div>
              </div>
          </div>
        </div>
    `;

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);

      window.ucp4dbg = this;
      this.useCaseGroups = useCaseGroups;
      this.showUseCaseGroupHeaders = this.showUseCaseGroups();

      const selectFromState = state => {
        return {
          connectionHasBeenLost: !selectIsConnected(state),
        };
      };

      // Using actionCreators like this means that every action defined there is available in the template.
      connect2Redux(selectFromState, actionCreators, [], this);
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

    /**
     * Only display the headers of the useCaseGroups if there are multiple displayable useCaseGroups
     * @returns {boolean}
     */
    showUseCaseGroups() {
      let countDisplayedUseCaseGroups = 0;

      for (const key in this.useCaseGroups) {
        if (
          this.displayableUseCaseGroup(this.useCaseGroups[key]) &&
          ++countDisplayedUseCaseGroups > 1
        ) {
          return true;
        }
      }
      return false;
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
  .module("won.owner.components.usecasePicker", [ngAnimate])
  .directive("wonUsecasePicker", genComponentConf).name;
