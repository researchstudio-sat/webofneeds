import angular from "angular";
import { delay } from "../../../utils.js";
import { attach, DomCache } from "../../../cstm-ng-utils.js";

import "~/style/_dropdownpicker.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="dropdownp__input">
         <select
            type="text"
            class="dropdownp__input__inner"
            ng-model="self.selectedValue"
            ng-change="::self.updateDropdown()">
            <option value="">{{ self.detail.placeholder }}</option>
            <option ng-repeat="option in self.detail.options" value="{{option.value}}">{{option.label}}</option>
         </select>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.dropdownp4dbg = this;

      delay(0).then(() => this.showInitialValue());
    }

    /**
     * Checks validity and uses callback method
     */
    update(selectedValue) {
      this.onUpdate({ value: selectedValue });
    }

    showInitialValue() {
      //TODO: IMPLEMENT THE INITIAL VALUES
      this.selectedValue = this.initialValue;
      this.$scope.$apply();
    }

    updateDropdown() {
      this.selectedValue = this.select().value;
      if (this.selectedValue && this.selectedValue.length !== "") {
        this.update(this.selectedValue);
      } else {
        this.update(undefined);
      }
    }

    selectNg() {
      return angular.element(this.select());
    }
    select() {
      if (!this._select) {
        this._select = this.$element[0].querySelector(
          ".dropdownp__input__inner"
        );
      }
      return this._select;
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
  .module("won.owner.components.dropdownPicker", [])
  .directive("wonDropdownPicker", genComponentConf).name;
