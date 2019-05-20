import angular from "angular";
import {
  attach,
  delay,
  isValidDate,
  toLocalISODateString,
  get,
} from "../../../utils.js";
import { DomCache } from "../../../cstm-ng-utils.js";

import "~/style/_datetimepicker.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <!-- TODO: add some indication that both date and time need to be specified -->
      <button class="datetimep__button won-button--filled red"
        ng-click="self.currentDatetime()">
          Now
      </button>

      <!-- DATE -->
      <div class="datetimep__input">
          <input
              type="date"
              class="datetimep__input__inner"
              ng-attr-id="{{::self.getUniqueDateId()}}"
              placeholder="{{self.detail.placeholder}}"
              ng-model="self.date_value"
              ng-change="::self.updateDatetime()"/>
      </div>
          
      <!-- TIME -->
      <div class="datetimep__input">
          <svg class="datetimep__input__icon clickable"
              style="--local-primary:var(--won-primary-color);"
              ng-if="self.showResetButton"
              ng-click="self.resetDatetime()">
                  <use xlink:href="#ico36_close" href="#ico36_close"></use>
          </svg>
          <input
              type="time"
              class="datetimep__input__inner"
              ng-attr-id="{{::self.getUniqueTimeId()}}"
              placeholder="{{self.detail.placeholder}}"
              ng-model="self.time_value"
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

      const currentDate = toLocalISODateString(new Date());
      // format: [-]yyyy-MM-DDThh:mm:ss[Z|[+|-]hh:mm]

      this.fallbackDate = currentDate.split("T")[0];
      this.fallbackTime = "12:00";

      this.showResetButton = false;

      delay(0).then(() => this.showInitialDatetime());
    }

    getUniqueDateId() {
      return "datetime-date-" + this.$scope.$id;
    }

    getUniqueTimeId() {
      return "datetime-time-" + this.$scope.$id;
    }

    /**
     * Checks validity and uses callback method
     */
    update(datetimeString) {
      const d = new Date(datetimeString);
      if (isValidDate(d)) {
        this.onUpdate({ value: d });
      } else {
        this.onUpdate({ value: undefined });
      }
    }

    showInitialDatetime() {
      this.setDatetime(this.initialValue);
      this.$scope.$apply();
    }

    updateDatetime() {
      let date = this.datefield().value;
      let time = this.timefield().value;

      // check if either time or date are set
      if (
        (date && date.trim().length > 0) ||
        (time && time.trim().length > 0)
      ) {
        // add fallback date
        if (!date) {
          date = this.fallbackDate;
          this.datefield().value = this.fallbackDate;
        }
        // add fallback time
        if (!time) {
          time = this.fallbackTime;
          this.timefield().value = this.fallbackTime;
        }

        this.addedDatetime = date.trim() + "T" + time.trim();
        this.update(this.addedDatetime);
        this.showResetButton = true;
      } else {
        this.resetDatetime();
      }
    }

    setDatetime(datetime) {
      if (isValidDate(datetime)) {
        const datetimeString = toLocalISODateString(datetime);
        const croppedDatetimeString = get(
          // only select up till minutes; drop seconds, ms and timezone
          // (we'll generate the local timezone anyway)
          datetimeString &&
            datetimeString.match(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/),
          0 // first match if any
        );
        if (croppedDatetimeString) {
          // checking already happens with isValidDate,
          // so valid date/time can be assumed here
          const splitDateTimeString = croppedDatetimeString.split("T");
          this.addedDatetime = croppedDatetimeString;
          this.datefield().value = splitDateTimeString[0];
          this.timefield().value = splitDateTimeString[1];
          this.showResetButton = true;

          return true;
        }
        return false;
      }
      return false;
    }

    currentDatetime() {
      if (this.setDatetime(new Date())) {
        this.update(this.addedDatetime);
      }
    }

    resetDatetime() {
      this.addedDatetime = undefined;
      this.datefield().value = "";
      this.timefield().value = "";
      this.update(undefined);
      this.showResetButton = false;
    }

    datefieldNg() {
      return angular.element(this.datefield());
    }
    datefield() {
      if (!this._dateInput) {
        this._dateInput = this.$element[0].querySelector(
          "#" + this.getUniqueDateId()
        );
      }
      return this._dateInput;
    }

    timefieldNg() {
      return angular.element(this.timefield());
    }
    timefield() {
      if (!this._timeInput) {
        this._timeInput = this.$element[0].querySelector(
          "#" + this.getUniqueTimeId()
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
  .module("won.owner.components.datetimePicker", [])
  .directive("wonDatetimePicker", genComponentConf).name;
