import angular from "angular";
import "ng-redux";
import { actionCreators } from "../../../actions/actions.js";
import { attach, get } from "../../../utils.js";
import { connect2Redux } from "../../../won-utils.js";

import "/style/_pokemon-gym-viewer.scss";
import "/style/_won-markdown.scss";

const serviceDependencies = ["$scope", "$ngRedux", "$element"];
function genComponentConf() {
  let template = `
        <div class="pgv__header">
          <svg class="pgv__header__icon" ng-if="self.detail.icon">
              <use xlink:href={{self.detail.icon}} href={{self.detail.icon}}></use>
          </svg>
          <span class="pgv__header__label" ng-if="self.detail.label">{{self.detail.label}}</span>
        </div>
        <div class="pgv__content">
          <div class="pgv__content__ex" ng-if="self.ex">This is an Ex Gym!</div>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.pgv4dbg = this;

      const selectFromState = () => {
        const ex = get(this.content, "ex");

        return {
          ex,
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.content", "self.detail"],
        this
      );
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      content: "=",
      detail: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.pokemonGymViewer", [])
  .directive("pokemonGymViewer", genComponentConf).name;
