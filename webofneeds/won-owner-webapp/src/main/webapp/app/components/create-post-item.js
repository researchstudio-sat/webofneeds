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
        <div class="cpi__item selected"
            ng-if="self.useCase">
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
        <div class="cpi__item clickable"
            ng-if="!self.useCase && listUseCase['showInList']"
            ng-repeat="listUseCase in self.useCases"
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
        <div class="cpi__item clickable"
            ng-click="self.selectUseCase()"
            ng-if="!self.useCase"
            ng-class="{'selected': self.showUseCases}">
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

        return {
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
      }
      return undefined;
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

    selectUseCase() {
      this.router__stateGoCurrent({
        connectionUri: undefined,
        postUri: undefined,
        showUseCases: true,
        useCase: undefined,
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
