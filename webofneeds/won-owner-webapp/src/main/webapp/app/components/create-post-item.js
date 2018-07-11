import angular from "angular";
import ngAnimate from "angular-animate";
import squareImageModule from "../components/square-image.js";
import { actionCreators } from "../actions/actions.js";
import { attach, getIn } from "../utils.js";
import { useCases } from "useCaseDefinitions";
import { connect2Redux } from "../won-utils.js";

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
              'selected': self.showUseCases,
              'cpi__item--withcolspan': !self.evenUseCaseListSize,
            }">
            <svg class="cpi__item__icon"
                title="Create a new post"
                style="--local-primary:var(--won-primary-color);">
                    <use xlink:href="#ico36_plus" href="#ico36_plus"></use>
            </svg>
            <div class="cpi__item__text">
                Add interest or suggestion
            </div>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      this.useCases = useCases;
      window.cpitem4dbg = this;

      const selectFromState = state => {
        const showUseCases = getIn(state, [
          "router",
          "currentParams",
          "showUseCases",
        ]);

        const useCaseString = getIn(state, [
          "router",
          "currentParams",
          "useCase",
        ]);

        const listUseCases = this.getListUseCases();

        return {
          listUseCases,
          evenUseCaseListSize:
            listUseCases && Object.keys(listUseCases).length % 2 == 0,
          showUseCases: !!showUseCases,
          useCase: useCaseString && this.getUseCase(useCaseString),
        };
      };
      connect2Redux(selectFromState, actionCreators, [], this);
    }

    getUseCase(useCaseString) {
      if (useCaseString) {
        for (const useCaseName in useCases) {
          if (useCaseString === useCases[useCaseName]["identifier"]) {
            return useCases[useCaseName];
          }
        }
        if (useCaseString === "search") {
          return useCaseString;
        }
      }
      return undefined;
    }

    getListUseCases() {
      let listUseCases = {};

      for (const useCaseKey in this.useCases) {
        if (this.useCases[useCaseKey]["showInList"]) {
          listUseCases[useCaseKey] = this.useCases[useCaseKey];
        }
      }
      return listUseCases;
    }

    startFrom(selectedUseCase) {
      const selectedUseCaseIdentifier =
        selectedUseCase && selectedUseCase.identifier;

      if (selectedUseCaseIdentifier) {
        this.router__stateGoCurrent({
          connectionUri: undefined,
          postUri: undefined,
          showUseCases: undefined,
          useCase: encodeURIComponent(selectedUseCaseIdentifier),
        });
      } else {
        console.log(
          "No usecase identifier found for given usecase, ",
          selectedUseCase
        );
      }
    }

    showAvailableUseCases() {
      this.router__stateGoCurrent({
        connectionUri: undefined,
        postUri: undefined,
        showUseCases: true,
        useCase: undefined,
      });
    }

    showPureSearch() {
      // TODO: this link is still broken
      this.router__stateGoCurrent({
        connectionUri: undefined,
        postUri: undefined,
        showUseCases: undefined,
        useCase: "search",
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
