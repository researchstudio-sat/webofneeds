import angular from "angular";
import ngAnimate from "angular-animate";
import squareImageModule from "../components/square-image.js";
import { actionCreators } from "../actions/actions.js";
import { attach } from "../utils.js";
import * as useCaseUtils from "../usecase-utils.js";
import { connect2Redux } from "../won-utils.js";
import {
  getUseCaseFromRoute,
  getUseCaseGroupFromRoute,
} from "../selectors/general-selectors.js";

import "~/style/_create-post-item.scss";

const serviceDependencies = ["$scope", "$ngRedux"];
function genComponentConf() {
  let template = `
        
        <!-- SHOW USECASE THAT'S BEING CREATED RIGHT NOW - NOT SEARCH -->
        <div class="cpi__item selected cpi__item--withcolspan"
            ng-if="self.useCase && self.useCase !== 'search'">
            <svg class="cpi__item__icon"
                title="{{self.useCase['label']}}"
                ng-if="self.useCase['icon']"
                style="--local-primary:var(--won-primary-color);">
                    <use xlink:href="{{self.useCase['icon']}}" href="{{self.useCase['icon']}}"></use>
            </svg>
            <div class="cpi__item__text">
                {{ self.useCase['label'] }}
            </div>
        </div>

        <!-- SHOW USECASE THAT'S BEING CREATED RIGHT NOW - SEARCH -->
        <div class="cpi__item selected cpi__item--withcolspan"
            ng-if="self.useCase && self.useCase === 'search'">
            <svg class="cpi__item__icon"
                title="Search"
                style="--local-primary:var(--won-primary-color);">
                    <use xlink:href="#ico36_search" href="#ico36_search"></use>
            </svg>
            <div class="cpi__item__text">
                Search
            </div>
        </div>

        <!-- SHOW PURE SEARCH -->
        <div 
          class="cpi__item clickable"
          ng-click="self.showPureSearch()"
          ng-if="!self.useCase">
            <svg 
              class="cpi__item__icon"
              title="Search"
              style="--local-primary:var(--won-primary-color);">
                <use xlink:href="#ico36_search" href="#ico36_search"></use>
            </svg>
            <div class="cpi__item__text">
                Search
            </div>
        </div>

        <!-- SHOW STICKY USE CASES -->
        <div class="cpi__item clickable"
            ng-if="!self.useCase"
            ng-repeat="listUseCase in self.listUseCases"
            ng-click="self.startFrom(listUseCase)">
            <svg class="cpi__item__icon"
                title="{{listUseCase['label']}}"
                ng-if="listUseCase['icon']"
                style="--local-primary:var(--won-primary-color);">
                    <use xlink:href="{{listUseCase['icon']}}" href="{{listUseCase['icon']}}"></use>
            </svg>
            <div class="cpi__item__text">
                {{ listUseCase['label'] }}
            </div>
        </div>

        <!-- SHOW USECASE OVERVIEW -->
        <div class="cpi__item clickable"
            ng-click="self.showAvailableUseCases()"
            ng-if="!self.useCase"
            ng-class="{
              'selected': !!self.useCaseGroup,
              'cpi__item--withcolspan': !self.evenUseCaseListSize,
            }">
            <svg class="cpi__item__icon"
                title="Create a new post"
                style="--local-primary:var(--won-primary-color);">
                    <use xlink:href="#ico36_plus" href="#ico36_plus"></use>
            </svg>
            <div class="cpi__item__text">
                New
            </div>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      window.cpitem4dbg = this;

      const selectFromState = state => {
        const useCaseGroup = getUseCaseGroupFromRoute(state);
        const useCaseString = getUseCaseFromRoute(state);

        const listUseCases = useCaseUtils.getListUseCases();
        const useCase = useCaseUtils.getUseCase(useCaseString);

        return {
          listUseCases,
          evenUseCaseListSize:
            listUseCases && Object.keys(listUseCases).length % 2 == 0,
          useCaseGroup,
          useCase: useCaseString !== "search" ? useCase : "search",
        };
      };
      connect2Redux(selectFromState, actionCreators, [], this);
    }

    startFrom(selectedUseCase) {
      const selectedUseCaseIdentifier =
        selectedUseCase && selectedUseCase.identifier;

      if (selectedUseCaseIdentifier) {
        this.router__stateGo("create", {
          useCase: encodeURIComponent(selectedUseCaseIdentifier),
          useCaseGroup: undefined,
          fromAtomUri: undefined,
          mode: undefined,
        });
      } else {
        console.error(
          "No usecase identifier found for given usecase, ",
          selectedUseCase
        );
      }
    }

    showAvailableUseCases() {
      this.router__stateGo("create", {
        useCase: undefined,
        useCaseGroup: "all",
        fromAtomUri: undefined,
        mode: undefined,
      });
    }

    showPureSearch() {
      // TODO: this link is still broken
      this.router__stateGo("create", {
        useCase: "search",
        useCaseGroup: undefined,
        fromAtomUri: undefined,
        mode: undefined,
      });
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {},
    template: template,
  };
}

export default angular
  .module("won.owner.components.createPostItem", [squareImageModule, ngAnimate])
  .directive("wonCreatePostItem", genComponentConf).name;
