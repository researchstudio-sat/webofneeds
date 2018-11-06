import angular from "angular";
import { attach, delay, isValidNumber } from "../../../utils.js";
import wonInput from "../../../directives/input.js";

import suggestpostPickerModule from "./suggestpost-picker.js";

import "style/_paypalpaymentpicker.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="paypalpaymentp__input">
        <div class="paypalpaymentp__input__reset clickable">
          <svg class="paypalpaymentp__input__reset__icon"
            style="--local-primary:var(--won-primary-color);"
            ng-if="self.showAmountResetButton"
            ng-click="self.resetNumber(true)">
            <use xlink:href="#ico36_close" href="#ico36_close"></use>
          </svg>
        </div>
        <input
            type="number"
            class="paypalpaymentp__input__amount"
            placeholder="{{self.detail.amountPlaceholder}}"
            ng-blur="::self.updateNumber(true)"
            won-input="::self.updateNumber(false)"
            ng-class="{'paypalpaymentp__input__amount--withreset' : self.showAmountResetButton}"/>
        <select
            class="paypalpaymentp__input__currency"
            ng-model="self.selectedCurrency"
            ng-disabled="self.detail.currency.length <= 1"
            won-input="::self.updateCurrency()">
            <option ng-repeat="currency in self.detail.currency" value="{{currency.value}}">{{currency.label}}</option>
        </select>
      </div>
      <div class="paypalpaymentp__textinput">
        <input
            type="email"
            class="paypalpaymentp__textinput__receiver"
            placeholder="{{self.detail.receiverPlaceholder}}"
            ng-blur="::self.updateReceiver(true)"
            won-input="::self.updateReceiver(false)"
            ng-class="{'paypalpaymentp__textinput__receiver--withreset' : self.showReceiverResetButton}"/>
        <div class="paypalpaymentp__textinput__reset clickable">
          <svg class="paypalpaymentp__textinput__reset__icon"
            style="--local-primary:var(--won-primary-color);"
            ng-if="self.showReceiverResetButton"
            ng-click="self.resetReceiver(true)">
            <use xlink:href="#ico36_close" href="#ico36_close"></use>
          </svg>
        </div>
      </div>
      <div class="paypalpaymentp__textinput">
        <input
            type="text"
            class="paypalpaymentp__textinput__secret"
            placeholder="{{self.detail.secretPlaceholder}}"
            ng-blur="::self.updateSecret(true)"
            won-input="::self.updateSecret(false)"
            ng-class="{'paypalpaymentp__textinput__secret--withreset' : self.showSecretResetButton}"/>
        <div class="paypalpaymentp__textinput__reset clickable">
          <svg class="paypalpaymentp__textinput__reset__icon"
            style="--local-primary:var(--won-primary-color);"
            ng-if="self.showSecretResetButton"
            ng-click="self.resetSecret(true)">
            <use xlink:href="#ico36_close" href="#ico36_close"></use>
          </svg>
        </div>
      </div>
      <won-suggestpost-picker
          initial-value="self.initialValue && self.initialValue.costumerUri"
          on-update="self.updateCostumerUri(value)"
          detail="self.detail && self.detail.suggestPost"
      ></won-suggestpost-picker>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      window.paypalpaymentp4dbg = this;

      this.addedNumber = this.initialValue && this.initialValue.amount;
      this.selectedCurrency = this.initialValue && this.initialValue.currency;
      this.costumerUri = this.initialValue && this.initialValue.costumerUri;
      this.secret = this.initialValue && this.initialValue.secret;
      this.receiver = this.initialValue && this.initialValue.receiver;

      if (!this.selectedCurrency) {
        this.selectedCurrency = "EUR";
      }

      this.showAmountResetButton = false;
      this.showReceiverResetButton = false;
      this.showSecretResetButton = false;

      delay(0).then(() => this.showInitialValues());
    }

    /**
     * Checks validity and uses callback method
     */
    update(number, currency, costumerUri, secret, receiver) {
      const parsedNumber = Number.parseFloat(number);
      if (
        isValidNumber(number) &&
        currency &&
        costumerUri &&
        secret &&
        receiver
      ) {
        this.onUpdate({
          value: {
            amount: parsedNumber,
            currency: currency,
            costumerUri: costumerUri,
            secret: secret,
            receiver: receiver,
          },
        });
      } else {
        this.onUpdate({ value: undefined });
      }
    }

    getDefaultCurrency() {
      let defaultCurrency;

      this.detail &&
        this.detail.currency.forEach(curr => {
          if (curr.default) defaultCurrency = curr.value;
        });

      return defaultCurrency;
    }

    showInitialValues() {
      this.addedNumber = this.initialValue && this.initialValue.amount;
      this.costumerUri = this.initialValue && this.initialValue.costumerUri;
      this.secret = this.initialValue && this.initialValue.secret;
      this.receiver = this.initialValue && this.initialValue.receiver;
      this.selectedCurrency =
        (this.initialValue && this.initialValue.currency) ||
        this.getDefaultCurrency();
      if (this.initialValue && this.initialValue.amount) {
        this.amount().value = this.initialValue.amount;
        this.showAmountResetButton = true;
      }
      if (this.initialValue && this.initialValue.secret) {
        this.secretInput().value = this.initialValue.secret;
        this.showSecretResetButton = true;
      }
      if (this.initialValue && this.initialValue.receiver) {
        this.receiverInput().value = this.initialValue.receiver;
        this.showReceiverResetButton = true;
      }

      this.$scope.$apply();
    }

    updateNumber(resetInput) {
      const number = Number.parseFloat(this.amount().value);

      if (isValidNumber(number)) {
        this.addedNumber = number;
        this.update(
          this.addedNumber,
          this.selectedCurrency,
          this.costumerUri,
          this.secret,
          this.receiver
        );
        this.showAmountResetButton = true;
      } else {
        this.resetNumber(resetInput);
      }
    }

    updateCostumerUri(costumerUri) {
      this.costumerUri = costumerUri;
      this.update(
        this.addedNumber,
        this.selectedCurrency,
        this.costumerUri,
        this.secret,
        this.receiver
      );
    }

    updateSecret() {
      this.secret = this.secretInput().value;

      if (this.secret) {
        this.showSecretResetButton = true;
      }
      this.update(
        this.addedNumber,
        this.selectedCurrency,
        this.costumerUri,
        this.secret,
        this.receiver
      );
    }

    updateReceiver() {
      this.receiver = this.receiverInput().value;

      if (this.receiver) {
        this.showReceiverResetButton = true;
      }
      this.update(
        this.addedNumber,
        this.selectedCurrency,
        this.costumerUri,
        this.secret,
        this.receiver
      );
    }

    updateCurrency() {
      this.selectedCurrency = this.currency().value;
      if (this.selectedCurrency) {
        this.update(
          this.addedNumber,
          this.selectedCurrency,
          this.costumerUri,
          this.secret,
          this.receiver
        );
      } else {
        this.update(
          this.addedNumber,
          this.selectedCurrency,
          this.costumerUri,
          this.secret,
          this.receiver
        );
      }
    }

    resetNumber(resetInput) {
      this.addedNumber = undefined;
      this.selectedCurrency = this.getDefaultCurrency();

      if (resetInput) {
        this.amount().value = "";
        this.currency().value = this.selectedCurrency;
        this.showAmountResetButton = false;
      }
      this.update(
        this.addedNumber,
        this.selectedCurrency,
        this.costumerUri,
        this.secret,
        this.receiver
      );
    }

    resetSecret(resetInput) {
      this.secret = undefined;

      if (resetInput) {
        this.secretInput().value = "";
        this.showSecretResetButton = false;
      }
      this.update(
        this.addedNumber,
        this.selectedCurrency,
        this.costumerUri,
        this.secret,
        this.receiver
      );
    }

    resetReceiver(resetInput) {
      this.receiver = undefined;

      if (resetInput) {
        this.receiverInput().value = "";
        this.showReceiverResetButton = false;
      }
      this.update(
        this.addedNumber,
        this.selectedCurrency,
        this.costumerUri,
        this.secret,
        this.receiver
      );
    }

    amount() {
      if (!this._amountInput) {
        this._amountInput = this.$element[0].querySelector(
          ".paypalpaymentp__input__amount"
        );
      }
      return this._amountInput;
    }

    currency() {
      if (!this._currency) {
        this._currency = this.$element[0].querySelector(
          ".paypalpaymentp__input__currency"
        );
      }
      return this._currency;
    }

    receiverInput() {
      if (!this._receiver) {
        this._receiver = this.$element[0].querySelector(
          ".paypalpaymentp__textinput__receiver"
        );
      }
      return this._receiver;
    }

    secretInput() {
      if (!this._secret) {
        this._secret = this.$element[0].querySelector(
          ".paypalpaymentp__textinput__secret"
        );
      }
      return this._secret;
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
  .module("won.owner.components.paypalPaymentPicker", [
    wonInput,
    suggestpostPickerModule,
  ])
  .directive("wonPaypalPaymentPicker", genComponentConf).name;
