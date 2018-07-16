import angular from "angular";
import { attach, delay } from "../../utils.js";
import { DomCache } from "../../cstm-ng-utils.js";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="numberp__input">
         <svg class="numberp__input__icon clickable"
            style="--local-primary:var(--won-primary-color);"
            ng-if="self.showResetButton"
            ng-click="self.resetNumber()">
            <use xlink:href="#ico36_close" href="#ico36_close"></use>
          </svg>
          <input
              type="number"
              class="numberp__input__inner won-txt"
              placeholder="What? (Short title shown in lists)"
              ng-blur="::self.updateNumber()"
              ng-keyup="::self.updateNumber()"/>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.numberp4dbg = this;

      this.addedNumber = this.initialValue;
      this.showResetButton = false;

      delay(0).then(() => this.showInitialNumber());
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

    showInitialNumber() {
      this.addedNumber = this.initialValue;

      if (this.initialValue && this.initialValue.trim().length > 0) {
        this.textfield().value = this.initialValue.trim();
        this.showResetButton = true;
      }

      this.$scope.$apply();
    }

    updateNumber() {
      const text = this.textfield().value;

      if (text && text.trim().length > 0) {
        this.addedNumber = text.trim();
        this.update(this.addedNumber);
        this.showResetButton = true;
      } else {
        this.resetNumber();
      }
    }

    resetNumber() {
      this.addedNumber = undefined;
      this.textfield().value = "";
      this.update(undefined);
      this.showResetButton = false;
    }

    // textfieldNg() {
    //   return this.domCache.ng(".numberp__input__inner");
    // }

    // textfield() {
    //   return this.domCache.dom(".numberp__input__inner");
    // } // TODO: why is this done differently for number than for any other picker?

    textfieldNg() {
      return angular.element(this.textfield());
    }
    textfield() {
      if (!this._numberInput) {
        this._numberInput = this.$element[0].querySelector(
          ".numberp__input__inner"
        );
      }
      return this._numberInput;
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
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.numberPicker", [])
  .directive("wonNumberPicker", genComponentConf).name;
