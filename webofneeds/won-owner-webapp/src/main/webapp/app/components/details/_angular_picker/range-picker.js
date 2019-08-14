import angular from "angular";
import { delay, isValidNumber } from "../../../utils.js";
import { attach } from "../../../cstm-ng-utils.js";
import wonInput from "../../../directives/input.js";

import "~/style/_rangepicker.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="rangep__input">
          <label class="rangep__input__label">
            {{self.detail.minLabel}}
          </label>
          <div class="rangep__input__reset clickable">
            <svg class="rangep__input__reset__icon"
              style="--local-primary:var(--won-primary-color);"
              ng-if="self.showMinResetButton"
              ng-click="self.resetMinNumber(true)">
              <use xlink:href="#ico36_close" href="#ico36_close"></use>
            </svg>
          </div>
          <input
              type="number"
              class="rangep__input__min"
              placeholder="{{self.detail.minPlaceholder}}"
              ng-blur="::self.updateMinNumber(true)"
              won-input="::self.updateMinNumber(false)"
              ng-class="{'rangep__input__min--withreset' : self.showMinResetButton}"/>
      </div>
      <div class="rangep__input">
          <label class="rangep__input__label">
            {{self.detail.maxLabel}}
          </label>
          <div class="rangep__input__reset clickable">
            <svg class="rangep__input__reset__icon"
              style="--local-primary:var(--won-primary-color);"
              ng-if="self.showMaxResetButton"
              ng-click="self.resetMaxNumber(true)">
              <use xlink:href="#ico36_close" href="#ico36_close"></use>
            </svg>
          </div>
          <input
              type="number"
              class="rangep__input__max"
              placeholder="{{self.detail.maxPlaceholder}}"
              ng-blur="::self.updateMaxNumber(true)"
              won-input="::self.updateMaxNumber(false)"
              ng-class="{'rangep__input__max--withreset' : self.showMaxResetButton}"/>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      window.rangep4dbg = this;

      this.addedMinNumber = this.initialValue && this.initialValue.min;
      this.addedMaxNumber = this.initialValue && this.initialValue.max;
      this.showMinResetButton = false;
      this.showMaxResetButton = false;

      delay(0).then(() => this.showInitialRange());
    }

    /**
     * Checks validity and uses callback method
     */
    updateMin(number) {
      const parsedNum = Number.parseFloat(number);
      if (isValidNumber(parsedNum)) {
        this.onUpdate({
          value: { min: parsedNum, max: this.addedMaxNumber },
        });
      } else {
        if (isValidNumber(this.addedMaxNumber)) {
          this.onUpdate({
            value: { min: number, max: this.addedMaxNumber },
          });
        } else {
          this.onUpdate({
            value: undefined,
          });
        }
      }
    }
    updateMax(number) {
      const parsedNum = Number.parseFloat(number);
      if (isValidNumber(parsedNum)) {
        this.onUpdate({
          value: { min: this.addedMinNumber, max: parsedNum },
        });
      } else {
        if (isValidNumber(this.addedMinNumber)) {
          this.onUpdate({
            value: { min: this.addedMinNumber, max: number },
          });
        } else {
          this.onUpdate({
            value: undefined,
          });
        }
      }
    }

    showInitialRange() {
      if (this.initialValue) {
        const min = Number.parseFloat(this.initialValue.min);
        const max = Number.parseFloat(this.initialValue.max);

        this.addedMinNumber = min;
        this.addedMaxNumber = max;

        if (isValidNumber(min)) {
          this.minTextfield().value = min;
          this.showMinResetButton = true;
        }
        if (isValidNumber(max)) {
          this.maxTextfield().value = max;
          this.showMaxResetButton = true;
        }
      }

      this.$scope.$apply();
    }

    updateMinNumber(resetInput) {
      const number = Number.parseFloat(this.minTextfield().value);

      if (isValidNumber(number)) {
        this.addedMinNumber = number;
        this.updateMin(this.addedMinNumber);
        this.showMinResetButton = true;
      } else {
        this.resetMinNumber(resetInput);
      }
    }

    updateMaxNumber(resetInput) {
      const number = Number.parseFloat(this.maxTextfield().value);

      if (isValidNumber(number)) {
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
  .module("won.owner.components.rangePicker", [wonInput])
  .directive("wonRangePicker", genComponentConf).name;
