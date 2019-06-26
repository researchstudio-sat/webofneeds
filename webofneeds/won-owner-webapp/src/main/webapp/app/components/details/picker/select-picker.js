import angular from "angular";
import { attach } from "../../../cstm-ng-utils.js";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="selectp__input">
        <label ng-repeat="option in self.detail.options"
          ng-click="self.updateSelects()"
          class="selectp__input__inner">
          <input
            class="selectp__input__inner__select"
            type="{{ self.getSelectType() }}"
            name="{{ self.getInputName() }}"
            value="{{option.value}}"
            ng-checked="self.isChecked(option)"/>
          {{ option.label }}
        </label>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.selectp4dbg = this;
    }

    getSelectType() {
      return this.detail && this.detail.multiSelect ? "checkbox" : "radio";
    }

    /**
     * Checks validity and uses callback method
     */
    update(selectedValues) {
      // check if there are tags
      if (selectedValues && selectedValues.length > 0) {
        this.onUpdate({ value: selectedValues });
      } else {
        this.onUpdate({ value: undefined });
      }
    }

    isChecked(option) {
      if (this.initialValue) {
        for (const key in this.initialValue) {
          if (this.initialValue[key] === option.value) {
            return true;
          }
        }
      }
      return false;
    }

    updateSelects() {
      let selectedValues = [];
      this.selectedFields().forEach(selected =>
        selectedValues.push(selected.value)
      );
      this.update(selectedValues);
    }

    selectedFields() {
      return this.$element[0].querySelectorAll(
        "input[name='" + this.getInputName() + "']:checked"
      );
    }

    getInputName() {
      /*TODO: Implement a sort of hashcode to prepend to the returned name to add the
      possibility of using the same identifier in is and seeks for these pickers*/
      return this.detail.identifier;
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
  .module("won.owner.components.selectPicker", [])
  .directive("wonSelectPicker", genComponentConf).name;
