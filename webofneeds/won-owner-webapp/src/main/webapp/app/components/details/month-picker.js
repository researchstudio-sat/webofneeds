import angular from "angular";
import { attach, delay } from "../../utils.js";
import { DomCache } from "../../cstm-ng-utils.js";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="monthp__input">
         <svg class="monthp__input__icon clickable"
            style="--local-primary:var(--won-primary-color);"
            ng-if="self.showResetButton"
            ng-click="self.resetDatetime()">
            <use xlink:href="#ico36_close" href="#ico36_close"></use>
          </svg>
          <input
              type="month"
              class="monthp__input__inner won-txt"
              placeholder="{{self.detail.placeholder}}"
              ng-model="self.value"
              ng-change="::self.updateMonth()"/>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.monthp4dbg = this;

      this.addedMonth = this.initialValue;
      this.showResetButton = false;

      delay(0).then(() => this.showInitialMonth());
    }

    /**
     * Checks validity and uses callback method
     */
    update(month) {
      if (month) {
        this.onUpdate({ value: month });
      } else {
        this.onUpdate({ value: undefined });
      }
    }

    showInitialMonth() {
      this.addedMonth = this.initialValue;

      if (this.initialValue && this.initialValue.trim().length > 0) {
        this.textfield().value = this.initialValue.trim();
        this.showResetButton = true;
      }

      this.$scope.$apply();
    }

    updateMonth() {
      const text = this.textfield().value;

      if (text && text.trim().length > 0) {
        this.addedMonth = text.trim();
        this.update(this.addedMonth);
        this.showResetButton = true;
      } else {
        this.resetMonth();
      }
    }

    resetMonth() {
      this.addedMonth = undefined;
      this.textfield().value = "";
      this.update(undefined);
      this.showResetButton = false;
    }

    // textfieldNg() {
    //   return this.domCache.ng(".monthp__input__inner");
    // }

    // textfield() {
    //   return this.domCache.dom(".monthp__input__inner");
    // } // TODO: why is this done differently for number than for any other picker?

    textfieldNg() {
      return angular.element(this.textfield());
    }
    textfield() {
      if (!this._monthInput) {
        this._monthInput = this.$element[0].querySelector(
          ".monthp__input__inner"
        );
      }
      return this._monthInput;
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
  .module("won.owner.components.monthPicker", [])
  .directive("wonMonthPicker", genComponentConf).name;
