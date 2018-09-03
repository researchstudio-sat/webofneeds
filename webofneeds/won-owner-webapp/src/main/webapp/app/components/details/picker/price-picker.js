import angular from "angular";
import { attach, delay } from "../../../utils.js";
import wonInput from "../../../directives/input.js";

import "style/_pricepicker.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="pricep__input">
        <div class="pricep__input__reset clickable">
          <svg class="pricep__input__reset__icon"
            style="--local-primary:var(--won-primary-color);"
            ng-if="self.showResetButton"
            ng-click="self.resetNumber(true)">
            <use xlink:href="#ico36_close" href="#ico36_close"></use>
          </svg>
        </div>
        <input
            type="number"
            class="pricep__input__inner"
            placeholder="{{self.detail.placeholder}}"
            ng-blur="::self.updateNumber(true)"
            won-input="::self.updateNumber(false)"
            ng-class="{'pricep__input__inner--withreset' : self.showResetButton}"/>
        <select
            class="pricep__input__currency"
            ng-model="self.selectedCurrency"
            won-input="::self.updateCurrency()">
            <option ng-repeat="currency in self.detail.currency" value="{{currency.value}}" selected>{{currency.label}}</option>
        </select>
        <select
            class="pricep__input__unitCode"
            ng-model="self.selectedUnitCode"
            won-input="::self.updateUnitCode()">
            <option ng-repeat="unitCode in self.detail.unitCode" value="{{unitCode.value}}" selected>{{unitCode.label}}</option>
        </select>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      window.pricep4dbg = this;

      this.addedNumber = this.initialValue && this.initialValue.amount;
      this.selectedCurrency = this.initialValue && this.initialValue.currency;
      this.selectedUnitCode = this.initialValue && this.initialValue.unitCode;

      if (!this.selectedCurrency) {
        this.selectedCurrency = "EUR";
      }

      if (!this.selectedUnitCode) {
        this.selectedUnitCode = "";
      }

      this.showResetButton = false;

      delay(0).then(() => this.showInitialNumber());
    }

    /**
     * Checks validity and uses callback method
     */
    update(number, currency, unitCode) {
      if (number && currency) {
        this.onUpdate({
          value: {
            amount: number,
            currency: currency,
            unitCode: unitCode !== "" ? unitCode : undefined,
          },
        });
      } else {
        this.onUpdate({ value: undefined });
      }
    }

    showInitialNumber() {
      this.addedNumber = this.initialValue && this.initialValue.amount;
      this.selectedCurrency =
        (this.initialValue && this.initialValue.currency) || "EUR";
      this.selectedUnitCode =
        (this.initialValue && this.initialValue.unitCode) || "";

      if (this.initialValue && this.initialValue.amount) {
        this.amount().value = this.initialValue.amount;
        this.showResetButton = true;
      }

      this.$scope.$apply();
    }

    currency() {
      if (!this._currency) {
        this._currency = this.$element[0].querySelector(
          ".pricep__input__currency"
        );
      }
      return this._currency;
    }

    unitCode() {
      if (!this._unitCode) {
        this._unitCode = this.$element[0].querySelector(
          ".pricep__input__unitCode"
        );
      }
      return this._unitCode;
    }

    updateNumber(resetInput) {
      const number = this.amount().value;

      if (number) {
        this.addedNumber = number;
        this.update(
          this.addedNumber,
          this.selectedCurrency,
          this.selectedUnitCode
        );
        this.showResetButton = true;
      } else {
        this.resetNumber(resetInput);
      }
    }

    updateCurrency() {
      this.selectedCurrency = this.currency().value;
      if (this.selectedCurrency) {
        this.update(
          this.addedNumber,
          this.selectedCurrency,
          this.selectedUnitCode
        );
      } else {
        this.update(undefined);
      }
    }

    updateUnitCode() {
      this.selectedUnitCode = this.unitCode().value;

      if (this.selectedUnitCode) {
        this.update(
          this.addedNumber,
          this.selectedCurrency,
          this.selectedUnitCode
        );
      } else {
        this.update(undefined);
      }
    }

    resetNumber(resetInput) {
      this.addedNumber = undefined;
      this.selectedCurrency = "EUR";
      this.selectedUnitCode = "";

      if (resetInput) {
        this.amount().value = "";
        this.currency().value = "EUR";
        this.unitCode().value = "";
        this.showResetButton = false;
      }
      this.update(undefined);
    }

    amount() {
      if (!this._amountInput) {
        this._amountInput = this.$element[0].querySelector(
          ".pricep__input__inner"
        );
      }
      return this._amountInput;
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
  .module("won.owner.components.pricePicker", [wonInput])
  .directive("wonPricePicker", genComponentConf).name;
