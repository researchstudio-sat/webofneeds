import angular from "angular";
import { attach, delay } from "../../../utils.js";
import wonInput from "../../../directives/input.js";

import "style/_pricerangepicker.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="pricerangep__input">
          <label class="pricerangep__input__label">
            {{self.detail.minLabel}}
          </label>
          <div class="pricerangep__input__reset clickable">
            <svg class="pricerangep__input__reset__icon"
              style="--local-primary:var(--won-primary-color);"
              ng-if="self.showMinResetButton"
              ng-click="self.resetMinNumber(true)">
              <use xlink:href="#ico36_close" href="#ico36_close"></use>
            </svg>
          </div>
          <input
              type="number"
              class="pricerangep__input__min"
              placeholder="{{self.detail.minPlaceholder}}"
              ng-blur="::self.updateMinNumber(true)"
              won-input="::self.updateMinNumber(false)"
              ng-class="{'rangep__input__min--withreset' : self.showMinResetButton}"/>
      </div>
      <div class="pricerangep__input">
          <label class="pricerangep__input__label">
            {{self.detail.maxLabel}}
          </label>
          <div class="pricerangep__input__reset clickable">
            <svg class="pricerangep__input__reset__icon"
              style="--local-primary:var(--won-primary-color);"
              ng-if="self.showMaxResetButton"
              ng-click="self.resetMaxNumber(true)">
              <use xlink:href="#ico36_close" href="#ico36_close"></use>
            </svg>
          </div>
          <input
              type="number"
              class="pricerangep__input__max"
              placeholder="{{self.detail.maxPlaceholder}}"
              ng-blur="::self.updateMaxNumber(true)"
              won-input="::self.updateMaxNumber(false)"
              ng-class="{'rangep__input__max--withreset' : self.showMaxResetButton}"/>
      </div>
      <select
            class="pricerangep__input__currency"
            ng-model="self.selectedCurrency"
            ng-disabled="self.detail.currency.length <= 1"
            won-input="::self.updateCurrency()">
            <option ng-repeat="currency in self.detail.currency" value="{{currency.value}}">{{currency.label}}</option>
      </select>
      <select
            class="pricerangep__input__unitCode"
            ng-model="self.selectedUnitCode"
            ng-if="!self.totalUnitCodeOnly()"
            ng-disabled="self.detail.unitCode.length <= 1"
            won-input="::self.updateUnitCode()">
            <option ng-repeat="unitCode in self.detail.unitCode" value="{{unitCode.value}}">{{unitCode.label}}</option>
      </select>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      window.rangep4dbg = this;

      this.addedMinNumber = this.initialValue && this.initialValue.min;
      this.addedMaxNumber = this.initialValue && this.initialValue.max;
      this.selectedCurrency = this.initialValue && this.initialValue.currency;
      this.selectedUnitCode = this.initialValue && this.initialValue.unitCode;

      if (!this.selectedCurrency) {
        this.selectedCurrency = "EUR";
      }

      if (!this.selectedUnitCode) {
        this.selectedUnitCode = "";
      }

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

    /**
     * If there is no unitCode present in the given detail other than the "" blank/total unit code then we do not show any dropdown picker
     * @returns {boolean}
     */
    totalUnitCodeOnly() {
      const unitCode = this.detail && this.detail.unitCode;
      return unitCode && unitCode.length == 1 && unitCode[0].value == "";
    }

    getDefaultCurrency() {
      let defaultCurrency;

      this.detail &&
        this.detail.currency.forEach(curr => {
          if (curr.default) defaultCurrency = curr.value;
        });

      return defaultCurrency;
    }
    getDefaultUnitCode() {
      let defaultUnitCode;

      this.detail &&
        this.detail.unitCode.forEach(uc => {
          if (uc.default) defaultUnitCode = uc.value;
        });

      return defaultUnitCode;
    }

    showInitialRange() {
      this.addedMinNumber = this.initialValue && this.initialValue.min;
      this.addedMaxNumber = this.initialValue && this.initialValue.max;
      this.selectedCurrency =
        (this.initialValue && this.initialValue.currency) ||
        this.getDefaultCurrency();
      this.selectedUnitCode =
        (this.initialValue && this.initialValue.unitCode) ||
        this.getDefaultUnitCode();

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
          ".pricerangep__input__min"
        );
      }
      return this._minNumberInput;
    }

    maxTextfield() {
      if (!this._maxNumberInput) {
        this._maxNumberInput = this.$element[0].querySelector(
          ".pricerangep__input__max"
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
  .module("won.owner.components.priceRangePicker", [wonInput])
  .directive("wonPriceRangePicker", genComponentConf).name;
