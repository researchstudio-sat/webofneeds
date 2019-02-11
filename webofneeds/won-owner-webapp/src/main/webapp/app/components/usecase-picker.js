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
import {
  selectIsConnected,
  getUseCaseGroupFromRoute,
} from "../selectors/general-selectors.js";
import * as useCaseDefinitions from "useCaseDefinitions";

import "style/_usecase-picker.scss";

const serviceDependencies = [
  "$ngRedux",
  "$scope",
  "$element" /*'$routeParams' /*injections as strings here*/,
];

function genComponentConf() {
  const template = `
        <!-- HEADER -->
        <div class="ucp__header" ng-if="self.showAll">
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
            <span class="ucp__header__title">Pick one!</span>
        </div>

        <!-- WHAT'S AROUND -->
        <div class="ucp__createx">
            <button class="ucp__createx__button--pending won-button--filled red"
                    ng-if="self.processingPublish"
                    ng-disabled="self.processingPublish">
                <span>Finding out what's going on&hellip;</span>
            </button>

            <button class="won-button--filled red ucp__createx__button"
                    ng-if="!self.processingPublish"
                    ng-click="self.createWhatsAround()"
                    ng-disabled="self.processingPublish">
                <svg class="won-button-icon" style="--local-primary:white;">
                    <use xlink:href="#ico36_location_current" href="#ico36_location_current"></use>
                </svg>
                <span>What's in your Area?</span>
            </button>
            <button class="won-button--filled red ucp__createx__button"
                    ng-if="!self.processingPublish"
                    ng-click="self.createWhatsNew()"
                    ng-disabled="self.processingPublish">
                <span>What's new?</span>
            </button>

            <won-labelled-hr label="::'Or'" class="ucp__createx__labelledhr"></won-labelled-hr>
        </div>

        <div class="ucp__main">

        <!-- SEARCH FIELD -->
        <input
            type="text"
            class="ucp__main__search"
            placeholder="Search for use cases"
            won-input="::self.updateSearch()" />

        <!-- SEARCH RESULTS -->
        <div class="ucp__main__searchresult clickable"
          ng-repeat="useCase in self.searchResults"
          ng-click="self.startFrom(useCase)">
            <svg class="ucp__main__searchresult__icon"
                ng-if="!!useCase.icon">
                <use xlink:href="{{ useCase.icon }}" href="{{ useCase.icon }}"></use>
              </svg>
              <div class="ucp__main__searchresult__label"
                ng-if="!!useCase.label">
                  {{ useCase.label }}
              </div>
        </div>


        <!-- USE CASE GROUPS --> 
        <div class="ucp__main__usecase-group clickable"
          ng-repeat="ucg in self.useCaseGroups"
          ng-if="!self.isSearching && self.useCaseDefinitions.isDisplayableUseCaseGroup(ucg)
                 && self.useCaseDefinitions.countDisplayableUseCasesInGroup(ucg) > self.showGroupsThreshold"
          ng-click="self.viewUseCaseGroup(ucg)">
              <svg class="ucp__main__usecase-group__icon"
                ng-if="!!ucg.icon">
                <use xlink:href="{{ ucg.icon }}" href="{{ ucg.icon }}"></use>
              </svg>
              <div class="ucp__main__usecase-group__label"
                ng-if="!!ucg.label">
                  {{ ucg.label }}
              </div>
        </div>
        <!-- USE CASES WITHOUT GROUPS --> 
        <div class="ucp__main__usecase-group clickable"
          ng-repeat="useCase in self.ungroupedUseCases"
          ng-if="!self.isSearching && self.useCaseDefinitions.isDisplayableUseCase(useCase)"
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

        </div>
    `;

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);
      window.ucp4dbg = this;

      this.useCaseDefinitions = useCaseDefinitions;
      const showGroupsThreshold = 1; // only show groups with more than 1 use case(s) as groups
      this.searchResults = undefined;

      const selectFromState = state => {
        return {
          showAll: getUseCaseGroupFromRoute(state) === "all",
          processingPublish: state.getIn(["process", "processingPublish"]),
          connectionHasBeenLost: !selectIsConnected(state),
          useCaseGroups: useCaseDefinitions.getUseCaseGroups(),
          showGroupsThreshold,
          ungroupedUseCases: useCaseDefinitions.getUnGroupedUseCases(
            showGroupsThreshold
          ),
        };
      };

      // Using actionCreators like this means that every action defined there is available in the template.
      connect2Redux(selectFromState, actionCreators, [], this);
    }

    // redirects start

    createWhatsAround() {
      if (!this.processingPublish) {
        this.needs__whatsAround();
      }
    }

    createWhatsNew() {
      if (!this.processingPublish) {
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
        console.warn(
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
        console.warn(
          "No identifier found for given usecase group, ",
          selectedUseCaseGroup
        );
      }
    }

    // redirects end

    // search start

    // TODO: deal with use cases that are in more than one group - they might show up repeatedly
    // TODO: group search results by use case groups - only showing groups with results
    updateSearch() {
      const query = this.textfield().value;
      let results = new Map();

      if (query && query.trim().length > 1) {
        this.isSearching = true;

        for (const key in this.useCaseGroups) {
          const group = Object.values(this.useCaseGroups[key].useCases);

          for (const useCase of group) {
            if (this.searchFunction(useCase, query)) {
              results.set(useCase.identifier, useCase);
            }
          }
        }

        if (results.size === 0) {
          this.searchResults = undefined;
          this.isSearching = false;
        }

        this.searchResults = Array.from(results.values());
      } else {
        this.searchResults = undefined;
        this.isSearching = false;
      }
    }

    searchFunction(useCase, searchString) {
      // don't treat use cases that can't be displayed as results
      if (!useCaseDefinitions.isDisplayableUseCase(useCase)) {
        return false;
      }
      // check for searchString in use case label and draft
      const useCaseLabel = useCase.label
        ? JSON.stringify(useCase.label).toLowerCase()
        : "";
      const useCaseDraft = useCase.draft
        ? JSON.stringify(useCase.draft).toLowerCase()
        : "";

      const useCaseString = useCaseLabel.concat(useCaseDraft);
      const queries = searchString.toLowerCase().split(" ");

      for (let query of queries) {
        if (useCaseString.includes(query)) {
          return true;
        }
      }

      return false;
    }

    // search end

    textfieldNg() {
      return angular.element(this.textfield());
    }
    textfield() {
      if (!this._searchInput) {
        this._searchInput = this.$element[0].querySelector(
          ".ucp__main__search"
        );
      }
      return this._searchInput;
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
