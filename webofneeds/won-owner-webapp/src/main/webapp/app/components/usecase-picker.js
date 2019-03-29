/**
 * Created by quasarchimaere on 03.07.2018.
 */
import angular from "angular";
import Immutable from "immutable";
import ngAnimate from "angular-animate";
import labelledHrModule from "./labelled-hr.js";

import "ng-redux";
import { attach, get } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../won-utils.js";
import {
  selectIsConnected,
  getUseCaseGroupFromRoute,
} from "../selectors/general-selectors.js";
import * as useCaseUtils from "../usecase-utils.js";
import * as accountUtils from "../account-utils.js";

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
        <div class="ucp__main__search">
          <svg class="ucp__main__search__icon clickable"
              style="--local-primary:var(--won-primary-color);"
              ng-if="self.showResetButton"
              ng-click="self.resetSearch()">
              <use xlink:href="#ico36_close" href="#ico36_close"></use>
          </svg>
          <input
              type="text"
              class="ucp__main__search__input"
              placeholder="Search for use cases"
              won-input="::self.updateSearch()" />
        </div>
        <!-- SEARCH RESULTS -->
        <div class="ucp__main__searchresult clickable"
          ng-repeat="useCase in self.searchResults"
          ng-if="self.isSearching && self.searchResults"
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
        <div class="ucp__main__noresults" ng-if="self.isSearching && !self.searchResults">
          No Results found for '{{self.textfield().value}}'.
        </div>
        <div class="ucp__main__newcustom clickable" ng-if="self.isSearching && !self.searchResults && self.customUseCase"
            ng-click="self.startFrom(self.customUseCase)">
            <svg class="ucp__main__newcustom__icon"
                ng-if="!!self.customUseCase.icon">
                <use xlink:href="{{ self.customUseCase.icon }}" href="{{ self.customUseCase.icon }}"></use>
            </svg>
            <div class="ucp__main__newcustom__label"
                ng-if="!!self.customUseCase.label">
                {{ self.customUseCase.label }}
            </div>
        </div>


        <!-- USE CASE GROUPS --> 
        <div class="ucp__main__usecase-group clickable"
          ng-repeat="ucg in self.useCaseGroups"
          ng-if="!self.isSearching && self.useCaseUtils.isDisplayableUseCaseGroup(ucg)
                 && self.useCaseUtils.countDisplayableItemsInGroup(ucg) > self.showGroupsThreshold"
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
          ng-if="!self.isSearching && self.useCaseUtils.isDisplayableUseCase(useCase)"
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

      this.useCaseUtils = useCaseUtils;
      this.searchResults = undefined;
      this.showResetButton = false;

      const selectFromState = state => {
        const showGroupsThreshold = 1; // only show groups with more than 1 use case(s) as groups

        return {
          loggedIn: accountUtils.isLoggedIn(get(state, "account")),
          showAll: getUseCaseGroupFromRoute(state) === "all",
          processingPublish: state.getIn(["process", "processingPublish"]),
          connectionHasBeenLost: !selectIsConnected(state),
          useCaseGroups: useCaseUtils.getUseCaseGroups(),
          customUseCase: useCaseUtils.getCustomUseCase(),
          showGroupsThreshold,
          ungroupedUseCases: useCaseUtils.getUnGroupedUseCases(
            showGroupsThreshold
          ),
        };
      };

      // Using actionCreators like this means that every action defined there is available in the template.
      connect2Redux(selectFromState, actionCreators, [], this);
    }

    // redirects start

    createWhatsAround() {
      if (this.processingPublish) {
        console.debug("publish in process, do not take any action");
        return;
      }

      if (this.loggedIn) {
        this.needs__whatsAround();
      } else {
        this.view__showTermsDialog(
          Immutable.fromJS({
            acceptCallback: () => {
              this.view__hideModalDialog();
              this.needs__whatsAround();
            },
            cancelCallback: () => {
              this.view__hideModalDialog();
            },
          })
        );
      }
    }

    createWhatsNew() {
      if (this.processingPublish) {
        console.debug("publish in process, do not take any action");
        return;
      }

      if (this.loggedIn) {
        this.needs__whatsNew();
      } else {
        this.view__showTermsDialog(
          Immutable.fromJS({
            acceptCallback: () => {
              this.view__hideModalDialog();
              this.needs__whatsNew();
            },
            cancelCallback: () => {
              this.view__hideModalDialog();
            },
          })
        );
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

    updateSearch() {
      this.showResetButton = true;
      const query = this.textfield().value;

      if (query && query.trim().length > 1) {
        this.isSearching = true;

        const searchResults = useCaseUtils.filterUseCasesBySearchQuery(query);

        const sortByLabelAsc = (a, b) => {
          const bValue = b && b.label && b.label.toLowerCase();
          const aValue = a && a.label && a.label.toLowerCase();

          if (aValue < bValue) return -1;
          if (aValue > bValue) return 1;
          return 0;
        };

        this.searchResults = searchResults
          ? searchResults.sort(sortByLabelAsc)
          : undefined;
      } else {
        this.searchResults = undefined;
        this.isSearching = false;
      }
    }

    resetSearch() {
      this.isSearching = false;
      this.searchResults = undefined;
      this.textfield().value = "";
      this.showResetButton = false;
    }

    // search end

    textfieldNg() {
      return angular.element(this.textfield());
    }
    textfield() {
      if (!this._searchInput) {
        this._searchInput = this.$element[0].querySelector(
          ".ucp__main__search__input"
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
