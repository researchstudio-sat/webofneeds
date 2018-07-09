import angular from "angular";
import ngAnimate from "angular-animate";
import squareImageModule from "../components/square-image.js";
import { actionCreators } from "../actions/actions.js";
import { attach, getIn } from "../utils.js";
import { connect2Redux } from "../won-utils.js";

const serviceDependencies = ["$scope", "$ngRedux"];
function genComponentConf() {
  let template = `
        <!--div class="cpi__item clickable"
            ng-click="">
            <svg class="cpi__item__icon"
                title="Create a new search"
                style="--local-primary:var(--won-primary-color);">
                    <use xlink:href="#ico36_search" href="#ico36_search"></use>
            </svg>
            <div class="cpi__item__text">
                Search
            </div>
        </div-->
        <div class="cpi__item clickable"
            ng-click="self.selectUseCase()">
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

      this.SEARCH = "search";
      this.POST = "post";

      const selectFromState = state => {
        const showUseCases = getIn(state, [
          "router",
          "currentParams",
          "showUseCases",
        ]);

        return {
          showUseCases,
        };
      };
      connect2Redux(selectFromState, actionCreators, [], this);
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
