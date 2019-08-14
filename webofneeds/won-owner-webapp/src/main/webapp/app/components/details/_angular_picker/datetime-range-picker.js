import angular from "angular";
import { attach, DomCache } from "../../../cstm-ng-utils.js";

import datetimePickerModule from "./datetime-picker.js";

//import "~/style/_datetimepickerrange.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      From
      <won-datetime-picker
          initial-value="self.initialValue && self.initialValue.fromDatetime"
          on-update="self.updateFromDatetime(value)"
          detail="self.detail && self.detail.fromDatetime"
      ></won-datetime-picker>
      To
      <won-datetime-picker
          initial-value="self.initialValue && self.initialValue.toDatetime"
          on-update="self.updateToDatetime(value)"
          detail="self.detail && self.detail.toDatetime"
      ></won-datetime-picker>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.datetimerangep4dbg = this;

      this.datetimeRange = this.initialValue || {};
    }

    /**
     * Checks validity and uses callback method
     */
    update(dtrange) {
      // TODO: think about adding sanity checking (from < to)
      if (this.isNotEmpty(dtrange)) {
        this.onUpdate({ value: dtrange });
      } else {
        this.onUpdate({ value: undefined });
      }
    }

    updateFromDatetime(datetime) {
      this.datetimeRange.fromDatetime = datetime;
      this.update(this.datetimeRange);
    }

    updateToDatetime(datetime) {
      this.datetimeRange.toDatetime = datetime;
      this.update(this.datetimeRange);
    }

    // checks whether both dates are undefined
    isNotEmpty(range) {
      return range && (range.fromDatetime || range.toDatetime);
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
  .module("won.owner.components.datetimeRangePicker", [datetimePickerModule])
  .directive("wonDatetimeRangePicker", genComponentConf).name;
