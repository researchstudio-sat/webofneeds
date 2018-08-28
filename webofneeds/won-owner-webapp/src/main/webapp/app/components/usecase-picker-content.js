/**
 * Created by quasarchimaere on 03.07.2018.
 */
import angular from "angular";
import ngAnimate from "angular-animate";
import labelledHrModule from "./labelled-hr.js";
import "ng-redux";
import { attach } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../won-utils.js";
import { selectIsConnected } from "../selectors.js";
import { useCaseGroups } from "useCaseDefinitions";

import "style/_usecase-picker-content.scss";

const serviceDependencies = [
  "$ngRedux",
  "$scope",
  "$element" /*'$routeParams' /*injections as strings here*/,
];

function genComponentConf() {
  const template = `
      <div class="ucpc__createx won-pending-publishing" >
          <button class="won-button--filled red ucpc__createx__button"
                  ng-if="self.pendingPublishing"
                  ng-disabled="self.pendingPublishing">
              <span>Finding out what's going on&hellip;</span>
          </button>
      </div>
      <button class="won-button--filled red ucpc__createx__button"
              ng-if="!self.pendingPublishing"
              ng-click="self.createWhatsAround()"
              ng-disabled="self.pendingPublishing">
          <svg class="won-button-icon" style="--local-primary:white;">
              <use xlink:href="#ico36_location_current" href="#ico36_location_current"></use>
          </svg>
          <span>What's in your Area?</span>
      </button>
      <button class="won-button--filled red ucpc__createx__button"
              ng-if="!self.pendingPublishing"
              ng-click="self.createWhatsNew()"
              ng-disabled="self.pendingPublishing">
          <span>What's new?</span>
      </button>
      <won-labelled-hr label="::'Or'" class="ucpc__labelledhr"></won-labelled-hr>
      <div class="ucpc__usecasegroups"
        ng-repeat="useCaseGroup in self.useCaseGroups"
        ng-if="self.displayableUseCaseGroup(useCaseGroup)">
          <div class="ucpc__usecasegroups__group"
            ng-if="self.showUseCaseGroupHeaders"
            ng-click="self.startFrom(useCaseGroup)">
            <svg class="ucpc__usecasegroups__group__icon"
              ng-if="!!useCaseGroup.icon">
              <use xlink:href="{{ useCaseGroup.icon }}" href="{{ useCaseGroup.icon }}"></use>
            </svg>
            <div class="ucpc__usecasegroups__group__label"
              ng-if="!!useCaseGroup.label">
                {{ useCaseGroup.label }}
            </div>
          </div>
      </div>
    `;

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);

      window.ucpc4dbg = this;
      this.useCaseGroups = useCaseGroups;
      this.showUseCaseGroupHeaders = this.showUseCaseGroups();

      const selectFromState = state => {
        return {
          pendingPublishing: state.get("creatingWhatsX"),
          connectionHasBeenLost: !selectIsConnected(state),
        };
      };

      // Using actionCreators like this means that every action defined there is available in the template.
      connect2Redux(selectFromState, actionCreators, [], this);
    }

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

    startFrom(selectedUseCaseGroup) {
      const selectedGroupIdentifier =
        selectedUseCaseGroup && selectedUseCaseGroup.identifier;

      if (selectedGroupIdentifier) {
        this.router__stateGoCurrent({
          showUseCases: undefined,
          useCaseGroup: encodeURIComponent(selectedGroupIdentifier),
        });
      } else {
        console.log(
          "No identifier found for given usecase group, ",
          selectedUseCaseGroup
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
  .module("won.owner.components.usecasePickerContent", [
    ngAnimate,
    labelledHrModule,
  ])
  .directive("wonUsecasePickerContent", genComponentConf).name;
