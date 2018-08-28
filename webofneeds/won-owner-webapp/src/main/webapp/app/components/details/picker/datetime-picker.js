import angular from "angular";
import { attach, delay } from "../../../utils.js";
import { DomCache } from "../../../cstm-ng-utils.js";

import "style/_datetimepicker.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="datetimep__input">
         <svg class="datetimep__input__icon clickable"
            style="--local-primary:var(--won-primary-color);"
            ng-if="self.showResetButton"
            ng-click="self.resetDatetime()">
            <use xlink:href="#ico36_close" href="#ico36_close"></use>
          </svg>
          <input
              type="datetime-local"
              class="datetimep__input__inner"
              placeholder="{{self.detail.placeholder}}"
              ng-model="self.value"
              ng-change="::self.updateDatetime()"
              ng-class="{'datetimep__input__inner--withreset' : self.showResetButton}"/>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.datetimep4dbg = this;

      this.addedDatetime = this.initialValue;
      this.showResetButton = false;

      delay(0).then(() => this.showInitialDatetime());
    }

    /**
     * Checks validity and uses callback method
     */
    update(datetime) {
      const d = new Date(datetime);
      if (datetime) {
        // this.onUpdate({ value: d }); // TODO return raw date
        this.onUpdate({ value: d.toISOString() });
      } else {
        this.onUpdate({ value: undefined });
      }
    }

    showInitialDatetime() {
      this.addedDatetime = this.initialValue;

      if (this.initialValue && this.initialValue.trim().length > 0) {
        this.textfield().value = this.initialValue.trim();
        this.showResetButton = true;
      }

      this.$scope.$apply();
    }

    updateDatetime() {
      const text = this.textfield().value;

      if (text && text.trim().length > 0) {
        this.addedDatetime = text.trim();
        this.update(this.addedDatetime);
        this.showResetButton = true;
      } else {
        this.resetDatetime();
      }
    }

    resetDatetime() {
      this.addedDatetime = undefined;
      this.textfield().value = "";
      this.update(undefined);
      this.showResetButton = false;
    }

    // textfieldNg() {
    //   return this.domCache.ng(".datetimep__input__inner");
    // }

    // textfield() {
    //   return this.domCache.dom(".datetimep__input__inner");
    // } // TODO: why is this done differently for number than for any other picker?

    textfieldNg() {
      return angular.element(this.textfield());
    }
    textfield() {
      if (!this._datetimeInput) {
        this._datetimeInput = this.$element[0].querySelector(
          ".datetimep__input__inner"
        );
      }
      return this._datetimeInput;
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
  .module("won.owner.components.datetimePicker", [])
  .directive("wonDatetimePicker", genComponentConf).name;
