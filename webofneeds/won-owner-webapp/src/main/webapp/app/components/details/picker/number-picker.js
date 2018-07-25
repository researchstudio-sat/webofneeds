import angular from "angular";
import { attach, delay } from "../../../utils.js";
import wonInput from "../../../directives/input.js";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="numberp__input">
        <div class="numberp__input__reset clickable">
          <svg class="numberp__input__reset__icon"
            style="--local-primary:var(--won-primary-color);"
            ng-if="self.showResetButton"
            ng-click="self.resetNumber(true)">
            <use xlink:href="#ico36_close" href="#ico36_close"></use>
          </svg>
        </div>
        <input
            type="number"
            class="numberp__input__inner won-txt"
            placeholder="{{self.detail.placeholder}}"
            ng-blur="::self.updateNumber(true)"
            won-input="::self.updateNumber(false)"
            ng-class="{'numberp__input__inner--withreset' : self.showResetButton}"/>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      window.numberp4dbg = this;

      this.addedNumber = this.initialValue;
      this.showResetButton = false;

      delay(0).then(() => this.showInitialNumber());
    }

    /**
     * Checks validity and uses callback method
     */
    update(number) {
      if (number) {
        this.onUpdate({ value: number });
      } else {
        this.onUpdate({ value: undefined });
      }
    }

    showInitialNumber() {
      this.addedNumber = this.initialValue;

      if (this.initialValue) {
        this.textfield().value = this.initialValue;
        this.showResetButton = true;
      }

      this.$scope.$apply();
    }

    updateNumber(resetInput) {
      const number = this.textfield().value;

      if (number) {
        this.addedNumber = number;
        this.update(this.addedNumber);
        this.showResetButton = true;
      } else {
        this.resetNumber(resetInput);
      }
    }

    resetNumber(resetInput) {
      this.addedNumber = undefined;
      if (resetInput) {
        this.textfield().value = "";
        this.showResetButton = false;
      }
      this.update(undefined);
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
      detail: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.numberPicker", [wonInput])
  .directive("wonNumberPicker", genComponentConf).name;
