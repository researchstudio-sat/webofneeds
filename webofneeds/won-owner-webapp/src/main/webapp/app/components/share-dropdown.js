/**
 * Created by ksinger on 30.03.2017.
 */

import angular from "angular";
import ngAnimate from "angular-animate";
import { attach } from "../utils.js";
import shareModule from "./post-share-link.js";

import "style/_share-dropdown.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
            <svg class="sdd__icon__small"
                ng-if="self.postLoading || self.postFailedToLoad"
                style="--local-primary:var(--won-skeleton-color);">
                    <use xlink:href="#ico16_share" href="#ico16_share"></use>
            </svg>
            <svg class="sdd__icon__small clickable"
                ng-if="!self.postLoading && !self.postFailedToLoad"
                style="--local-primary:var(--won-secondary-color);"
                ng-click="self.contextMenuOpen = true">
                    <use xlink:href="#ico16_share" href="#ico16_share"></use>
            </svg>
            <div class="sdd__sharemenu" ng-show="self.contextMenuOpen">
                <div class="sdd__sharemenu__content" >
                    <div class="topline">
                        <svg class="sdd__icon__small__sharemenu clickable"
                            ng-click="self.contextMenuOpen = false"
                            style="--local-primary:black;">
                            <use xlink:href="#ico16_share" href="#ico16_share"></use>
                        </svg>
                    </div>
                    <won-post-share-link post-uri="self.atomUri"></won-post-share-link>
                </div>
            </div>
        `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      const callback = event => {
        const clickedElement = event.target;
        //hide MainMenu if click was outside of the component and menu was open
        if (
          this.contextMenuOpen &&
          !this.$element[0].contains(clickedElement)
        ) {
          this.contextMenuOpen = false;
          this.$scope.$apply();
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
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      atomUri: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.shareDropdown", [ngAnimate, shareModule])
  .directive("wonShareDropdown", genComponentConf).name;
