import angular from "angular";
import { attach, delay } from "../../../utils.js";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="rangep__input">
         <svg class="rangep__input__icon clickable"
            style="--local-primary:var(--won-primary-color);"
            ng-if="self.showMinResetButton"
            ng-click="self.resetMinNumber(true)">
            <use xlink:href="#ico36_close" href="#ico36_close"></use>
          </svg>
          <label class="rangep__input__label">
            {{self.detail.minLabel}}
          </label>
          <input
              type="number"
              class="rangep__input__min won-txt"
              placeholder="{{self.detail.minPlaceholder}}"
              ng-blur="::self.updateMinNumber(true)"
              ng-keyup="::self.updateMinNumber(false)"/>
      </div>
      <div class="rangep__input">
         <svg class="rangep__input__icon clickable"
            style="--local-primary:var(--won-primary-color);"
            ng-if="self.showMaxResetButton"
            ng-click="self.resetMaxNumber(true)">
            <use xlink:href="#ico36_close" href="#ico36_close"></use>
          </svg>
          <label class="rangep__input__label">
            {{self.detail.maxLabel}}
          </label>
          <input
              type="number"
              class="rangep__input__max won-txt"
              placeholder="{{self.detail.maxPlaceholder}}"
              ng-blur="::self.updateMaxNumber(true)"
              ng-keyup="::self.updateMaxNumber(false)"/>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      window.rangep4dbg = this;

      this.addedMinNumber = this.initialValue && this.initialValue.minValue;
      this.addedMaxNumber = this.initialValue && this.initialValue.maxValue;
      this.showMinResetButton = false;
      this.showMaxResetButton = false;

      delay(0).then(() => this.showInitialRange());
    }

    /**
     * Checks validity and uses callback method
     */
    updateMin(number) {
      if (number) {
        this.onUpdate({
          value: { minValue: number, maxValue: this.addedMaxNumber },
        });
      } else {
        this.onUpdate({
          value: { minValue: undefined, maxValue: this.addedMaxNumber },
        });
      }
    }
    updateMax(number) {
      if (number) {
        this.onUpdate({
          value: { minValue: this.addedMinNumber, maxValue: number },
        });
      } else {
        this.onUpdate({
          value: { minValue: this.addedMinNumber, maxValue: undefined },
        });
      }
    }

    showInitialRange() {
      this.addedMinNumber = this.initialValue && this.initialValue.minValue;
      this.addedMaxNumber = this.initialValue && this.initialValue.maxValue;

      if (this.initialValue) {
        if (this.initialValue.minValue) {
          this.minTextfield().value = this.initialValue.minValue;
          this.showMinResetButton = true;
        }
        if (this.initialValue.maxValue) {
          this.maxTextfield().value = this.initialValue.maxValue;
          this.showMaxResetButton = true;
        }
      }

      this.$scope.$apply();
    }

    updateMinNumber(resetInput) {
      const number = this.minTextfield().value;

      if (number) {
        this.addedMinNumber = number;
        this.updateMin(this.addedMinNumber);
        this.showMinResetButton = true;
      } else {
        this.resetMinNumber(resetInput);
      }
    }

    updateMaxNumber(resetInput) {
      const number = this.maxTextfield().value;

      if (number) {
        this.addedMaxNumber = number;
        this.updateMax(this.addedMaxNumber);
        this.showMaxResetButton = true;
      } else {
        this.resetMaxNumber(resetInput);
      }
    }

    resetMinNumber(resetInput) {
      this.addedMinNumber = undefined;
      if (resetInput) {
        this.minTextfield().value = "";
        this.showMinResetButton = false;
      }
      this.updateMin(undefined);
    }

    resetMaxNumber(resetInput) {
      this.addedMaxNumber = undefined;
      if (resetInput) {
        this.maxTextfield().value = "";
        this.showMaxResetButton = false;
      }
      this.updateMax(undefined);
    }

    minTextfield() {
      if (!this._minNumberInput) {
        this._minNumberInput = this.$element[0].querySelector(
          ".rangep__input__min"
        );
      }
      return this._minNumberInput;
    }

    maxTextfield() {
      if (!this._maxNumberInput) {
        this._maxNumberInput = this.$element[0].querySelector(
          ".rangep__input__max"
        );
      }
      return this._maxNumberInput;
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
  .module("won.owner.components.rangePicker", [])
  .directive("wonRangePicker", genComponentConf).name;
