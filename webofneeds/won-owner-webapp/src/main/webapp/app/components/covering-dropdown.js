/**
 * Created by ksinger on 24.08.2017.
 */

import angular from "angular";
import { actionCreators } from "../actions/actions.js";
import { attach } from "../utils.js";
import { connect2Redux } from "../won-utils.js";

import "style/_covering-dropdown.scss";

const serviceDependencies = ["$scope", "$ngRedux", "$element"];
function genComponentConf() {
  let template = `
      <div
        ng-transclude="header"
        class="dd__open-button clickable"
        ng-class="{ 'dd--closed' : !self.showMainMenu }"
        ng-click="self.view__showMainMenuDisplay()"
      >
      </div>
      <div class="dd__dropdown" ng-show="self.showMainMenu">
        <div
          ng-transclude="header"
          class="dd__close-button clickable"
          ng-class="{ 'dd--open' : self.showMainMenu }"
          ng-click="self.hideMainMenuDisplay()"
        >
        </div>
        <div
          class="dd__menu"
          ng-transclude="menu"
         >
         </div>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      const selectFromState = state => ({
        showMainMenu: state.getIn(["view", "showMainMenu"]),
      });

      connect2Redux(selectFromState, actionCreators, [], this);

      const callback = event => {
        const clickedElement = event.target;
        //hide MainMenu if click was outside of the component and menu was open
        if (this.showMainMenu && !this.$element[0].contains(clickedElement)) {
          this.hideMainMenuDisplay();
        }
      };

      this.$scope.$on("$destroy", () => {
        window.document.removeEventListener("click", callback);
      });

      window.document.addEventListener("click", callback);
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    transclude: {
      header: "wonDdHeader",
      menu: "wonDdMenu",
    },

    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    // //scope: { }, // not isolated on purpose to allow using parent's scope
    scope: {},
    template: template,
  };
}

export default angular
  .module("won.owner.components.coveringDropdown", [])
  .directive("wonDropdown", genComponentConf).name;
