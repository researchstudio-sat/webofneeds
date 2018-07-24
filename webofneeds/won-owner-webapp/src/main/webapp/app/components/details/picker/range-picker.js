import angular from "angular";
import { attach, delay } from "../../../utils.js";
import wonInput from "../../../directives/input.js";

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
      if (number) {
        this.onUpdate({
          value: { min: number, max: this.addedMaxNumber },
        });
      } else {
        if (this.addedMaxNumber) {
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
      if (number) {
        this.onUpdate({
          value: { min: this.addedMinNumber, max: number },
        });
      } else {
        if (this.addedMinNumber) {
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
      this.addedMinNumber = this.initialValue && this.initialValue.min;
      this.addedMaxNumber = this.initialValue && this.initialValue.max;

      if (this.initialValue) {
        if (this.initialValue.min) {
          this.minTextfield().value = this.initialValue.min;
          this.showMinResetButton = true;
        }
        if (this.initialValue.max) {
          this.maxTextfield().value = this.initialValue.max;
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
  .module("won.owner.components.rangePicker", [wonInput])
  .directive("wonRangePicker", genComponentConf).name;
