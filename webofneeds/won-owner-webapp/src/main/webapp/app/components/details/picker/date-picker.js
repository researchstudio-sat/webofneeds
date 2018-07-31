import angular from "angular";
import { attach, delay } from "../../../utils.js";
import { DomCache } from "../../../cstm-ng-utils.js";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="datep__input">
         <svg class="datep__input__icon clickable"
            style="--local-primary:var(--won-primary-color);"
            ng-if="self.showResetButton"
            ng-click="self.resetDate()">
            <use xlink:href="#ico36_close" href="#ico36_close"></use>
          </svg>
          <input
              type="date"
              class="datep__input__inner won-txt"
              placeholder="{{self.detail.placeholder}}"
              ng-model="self.value"
              ng-change="::self.updateDate()"
              ng-class="{'datep__input__inner--withreset' : self.showResetButton}"/>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.datep4dbg = this;

      this.addedDate = this.initialValue;
      this.showResetButton = false;

      delay(0).then(() => this.showInitialDate());
    }

    /**
     * Checks validity and uses callback method
     */
    update(date) {
      if (date) {
        this.onUpdate({ value: date });
      } else {
        this.onUpdate({ value: undefined });
      }
    }

    showInitialDate() {
      this.addedDate = this.initialValue;

      if (this.initialValue && this.initialValue.trim().length > 0) {
        this.textfield().value = this.initialValue.trim();
        this.showResetButton = true;
      }

      this.$scope.$apply();
    }

    updateDate() {
      const text = this.textfield().value;

      if (text && text.trim().length > 0) {
        this.addedDate = text.trim();
        this.update(this.addedDate);
        this.showResetButton = true;
      } else {
        this.resetDate();
      }
    }

    resetDate() {
      this.addedDate = undefined;
      this.textfield().value = "";
      this.update(undefined);
      this.showResetButton = false;
    }

    // textfieldNg() {
    //   return this.domCache.ng(".datep__input__inner");
    // }

    // textfield() {
    //   return this.domCache.dom(".datep__input__inner");
    // } // TODO: why is this done differently for number than for any other picker?

    textfieldNg() {
      return angular.element(this.textfield());
    }
    textfield() {
      if (!this._dateInput) {
        this._dateInput = this.$element[0].querySelector(
          ".datep__input__inner"
        );
      }
      return this._dateInput;
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
  .module("won.owner.components.datePicker", [])
  .directive("wonDatePicker", genComponentConf).name;
