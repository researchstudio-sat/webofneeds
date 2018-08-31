/**
 * Created by quasarchimaere on 03.07.2018.
 */
import angular from "angular";
import ngAnimate from "angular-animate";
import labelledHrModule from "./labelled-hr.js";

import "ng-redux";
import { attach, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../won-utils.js";
import { selectIsConnected } from "../selectors.js";
import { useCases, useCaseGroups } from "useCaseDefinitions";

import "style/_usecase-picker.scss";

const serviceDependencies = [
  "$ngRedux",
  "$scope",
  "$element" /*'$routeParams' /*injections as strings here*/,
];

function genComponentConf() {
  const template = `
        <!-- HEADER -->
        <div class="ucp__header" ng-if="self.useCaseGroup === 'all'">
            <a class="ucp__header__back clickable"
                ng-click="self.router__stateGoCurrent({useCase: undefined, useCaseGroup: undefined})">
                <svg style="--local-primary:var(--won-primary-color);"
                    class="ucp__header__back__icon show-in-responsive">
                    <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                </svg>
                <svg style="--local-primary:var(--won-primary-color);"
                    class="ucp__header__back__icon hide-in-responsive">
                    <use xlink:href="#ico36_close" href="#ico36_close"></use>
                </svg>
            </a>
            <span class="ucp__header__title">What do you have or want?</span>
        </div>

        <!-- WHAT'S AROUND -->
        <div class="ucp__createx">
            <button class="ucp__createx__button--pending won-button--filled red"
                    ng-if="self.pendingPublishing"
                    ng-disabled="self.pendingPublishing">
                <span>Finding out what's going on&hellip;</span>
            </button>

            <button class="won-button--filled red ucp__createx__button"
                    ng-if="!self.pendingPublishing"
                    ng-click="self.createWhatsAround()"
                    ng-disabled="self.pendingPublishing">
                <svg class="won-button-icon" style="--local-primary:white;">
                    <use xlink:href="#ico36_location_current" href="#ico36_location_current"></use>
                </svg>
                <span>What's in your Area?</span>
            </button>
            <button class="won-button--filled red ucp__createx__button"
                    ng-if="!self.pendingPublishing"
                    ng-click="self.createWhatsNew()"
                    ng-disabled="self.pendingPublishing">
                <span>What's new?</span>
            </button>

            <won-labelled-hr label="::'Or'" class="ucp__createx__labelledhr"></won-labelled-hr>
        </div>

        <div class="ucp__main">

        <!-- TODO: SEARCH FIELD -->
        <!-- TODO: SEARCH RESULTS -->
        <!-- USE CASE GROUPS - TODO: only show while not searching --> 
        <div class="ucp__main__usecase-group clickable"
          ng-repeat="useCaseGroup in self.useCaseGroups"
          ng-if="self.showGroups && self.displayableUseCaseGroup(useCaseGroup)"
          ng-click="self.viewUseCaseGroup(useCaseGroup)">
              <svg class="ucp__main__usecase-group__icon"
                ng-if="!!useCaseGroup.icon">
                <use xlink:href="{{ useCaseGroup.icon }}" href="{{ useCaseGroup.icon }}"></use>
              </svg>
              <div class="ucp__main__usecase-group__label"
                ng-if="!!useCaseGroup.label">
                  {{ useCaseGroup.label }}
              </div>
        </div>
        <!-- USE CASES WITHOUT GROUPS - TODO: only show while not searching --> 
        <!--
        <div class="ucp__main__usecase-group clickable"
          ng-repeat="useCase in self.ungroupedUseCases"
          ng-if="self.displayableUseCase(useCase)"
          ng-click="self.startFrom(useCase)">
              <svg class="ucp__main__usecase-group__icon"
                ng-if="!!useCase.icon">
                <use xlink:href="{{ useCase.icon }}" href="{{ useCase.icon }}"></use>
              </svg>
              <div class="ucp__main__usecase-group__label"
                ng-if="!!useCase.label">
                  {{ useCase.label }}
              </div>
        </div>
        -->

        </div>
    `;

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);
      window.ucp4dbg = this;

      this.useCaseGroups = useCaseGroups;
      this.showGroups = this.countDisplayableUseCaseGroups() > 0; // TODO: change this once single use cases can be displayed here

      const selectFromState = state => {
        const useCaseGroup = getIn(state, [
          "router",
          "currentParams",
          "useCaseGroup",
        ]);

        return {
          useCaseGroup,
          pendingPublishing: state.get("creatingWhatsX"),
          connectionHasBeenLost: !selectIsConnected(state),
        };
      };

      this.ungroupedUseCases = this.getUseCasesNotInGroups(useCases);

      // Using actionCreators like this means that every action defined there is available in the template.
      connect2Redux(selectFromState, actionCreators, [], this);
    }

    // redirects start

    createWhatsAround() {
      if (!this.pendingPublishing) {
        this.needs__whatsAround();
      }
    }

    createWhatsNew() {
      if (!this.pendingPublishing) {
        this.needs__whatsNew();
      }
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

    viewUseCaseGroup(selectedUseCaseGroup) {
      const selectedGroupIdentifier =
        selectedUseCaseGroup && selectedUseCaseGroup.identifier;

      if (selectedGroupIdentifier) {
        this.router__stateGoCurrent({
          useCaseGroup: encodeURIComponent(selectedGroupIdentifier),
        });
      } else {
        console.log(
          "No identifier found for given usecase group, ",
          selectedUseCaseGroup
        );
      }
    }

    // redirects end

    /**
     * return the amount of displayable useCases in a useCaseGroup
     * @param useCaseGroup
     * @return {*}
     */
    countDisplayableUseCasesInGroup(useCaseGroup) {
      let countUseCases = 0;

      for (const key in useCaseGroup.useCases) {
        if (this.displayableUseCase(useCaseGroup.useCases[key])) {
          countUseCases++;
        }
      }
      return countUseCases;
    }

    /**
     * return the amount of displayable useCaseGroups
     * @returns {*}
     */
    countDisplayableUseCaseGroups() {
      let countDisplayedUseCaseGroups = 0;

      for (const key in this.useCaseGroups) {
        if (this.displayableUseCaseGroup(this.useCaseGroups[key])) {
          countDisplayedUseCaseGroups++;
        }
      }
      return countDisplayedUseCaseGroups;
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

    getUseCasesNotInGroups(allUseCases) {
      for (const group in this.useCaseGroups) {
        // if (this.countDisplayableUseCasesInGroup(group) < 2) {
        //   // should probably be in a different function
        //   continue;
        // }
        for (const useCase in group.useCases) {
          console.log("not displaying: " + useCase);
          delete allUseCases[useCase];
        }
      }
      return allUseCases;
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
  .module("won.owner.components.usecasePicker", [ngAnimate, labelledHrModule])
  .directive("wonUsecasePicker", genComponentConf).name;
