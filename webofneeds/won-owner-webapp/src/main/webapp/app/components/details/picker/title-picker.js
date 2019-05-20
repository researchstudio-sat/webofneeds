import angular from "angular";
import { attach, delay } from "../../../utils.js";
import { DomCache } from "../../../cstm-ng-utils.js";
import wonInput from "../../../directives/input.js";

import "~/style/_titlepicker.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="titlep__input">
         <svg class="titlep__input__icon clickable" 
            style="--local-primary:var(--won-primary-color);"
            ng-if="self.showResetButton"
            ng-click="self.resetTitle()">
            <use xlink:href="#ico36_close" href="#ico36_close"></use>
          </svg>
          <input
              type="text"
              class="titlep__input__inner"
              placeholder="{{self.detail.placeholder}}"
              won-input="::self.updateTitle()" />
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.titlep4dbg = this;

      this.addedTitle = this.initialValue;
      this.showResetButton = false;

      delay(0).then(() => this.showInitialTitle());
    }

    /**
     * Checks validity and uses callback method
     */
    update(title) {
      if (title && title.trim().length > 0) {
        this.onUpdate({ value: title });
      } else {
        this.onUpdate({ value: undefined });
      }
    }

    showInitialTitle() {
      this.addedTitle = this.initialValue;

      if (this.initialValue && this.initialValue.trim().length > 0) {
        this.textfield().value = this.initialValue.trim();
        this.showResetButton = true;
      }

      this.$scope.$apply();
    }

    updateTitle() {
      const text = this.textfield().value;

      if (text && text.trim().length > 0) {
        this.addedTitle = text.trim();
        this.update(this.addedTitle);
        this.showResetButton = true;
      } else {
        this.resetTitle();
      }
    }

    resetTitle() {
      this.addedTitle = undefined;
      this.textfield().value = "";
      this.update(undefined);
      this.showResetButton = false;
    }

    // textfieldNg() {
    //   return this.domCache.ng(".titlep__input__inner");
    // }

    // textfield() {
    //   return this.domCache.dom(".titlep__input__inner");
    // } // TODO: why is this done differently for title than for any other picker?

    textfieldNg() {
      return angular.element(this.textfield());
    }
    textfield() {
      if (!this._titleInput) {
        this._titleInput = this.$element[0].querySelector(
          ".titlep__input__inner"
        );
      }
      return this._titleInput;
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      onUpdate: "&",
      initialValue: "=",
      detail: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.titlePicker", [wonInput])
  .directive("wonTitlePicker", genComponentConf).name;
