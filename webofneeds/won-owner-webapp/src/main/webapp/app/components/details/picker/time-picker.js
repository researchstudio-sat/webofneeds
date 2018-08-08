import angular from "angular";
import { attach, delay } from "../../../utils.js";
import { DomCache } from "../../../cstm-ng-utils.js";

import "style/_timepicker.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="timep__input">
         <svg class="timep__input__icon clickable"
            style="--local-primary:var(--won-primary-color);"
            ng-if="self.showResetButton"
            ng-click="self.resetTime()">
            <use xlink:href="#ico36_close" href="#ico36_close"></use>
          </svg>
          <input
              type="time"
              class="timep__input__inner"
              placeholder="{{self.detail.placeholder}}"
              ng-model="self.value"
              ng-change="::self.updateTime()"
              ng-class="{'timep__input__inner--withreset' : self.showResetButton}"/>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.timep4dbg = this;

      this.addedTime = this.initialValue;
      this.showResetButton = false;

      delay(0).then(() => this.showInitialTime());
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

    showInitialTime() {
      this.addedTime = this.initialValue;

      if (this.initialValue && this.initialValue.trim().length > 0) {
        this.textfield().value = this.initialValue.trim();
        this.showResetButton = true;
      }

      this.$scope.$apply();
    }

    updateTime() {
      const text = this.textfield().value;

      if (text && text.trim().length > 0) {
        this.addedTime = text.trim();
        this.update(this.addedTime);
        this.showResetButton = true;
      } else {
        this.resetTime();
      }
    }

    resetTime() {
      this.addedTime = undefined;
      this.textfield().value = "";
      this.update(undefined);
      this.showResetButton = false;
    }

    // textfieldNg() {
    //   return this.domCache.ng(".timep__input__inner");
    // }

    // textfield() {
    //   return this.domCache.dom(".timep__input__inner");
    // } // TODO: why is this done differently for number than for any other picker?

    textfieldNg() {
      return angular.element(this.textfield());
    }
    textfield() {
      if (!this._timeInput) {
        this._timeInput = this.$element[0].querySelector(
          ".timep__input__inner"
        );
      }
      return this._timeInput;
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
  .module("won.owner.components.timePicker", [])
  .directive("wonTimePicker", genComponentConf).name;
