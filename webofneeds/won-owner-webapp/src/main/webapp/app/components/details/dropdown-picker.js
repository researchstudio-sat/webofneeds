import angular from "angular";
import { attach, delay } from "../../utils.js";
import { DomCache } from "../../cstm-ng-utils.js";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="dropdownp__input">
         <select
            type="text"
            class="dropdownp__input__inner"
            ng-selected="::self.updateDropdown()">
            <option value="1">option 1</option>
            <option value="2">option 2</option>
            <option value="3">option 3</option>
            <option value="4">option 4</option>
            <option value="5">option 5</option>
         </select>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.dropdownp4dbg = this;

      delay(0).then(() => this.showInitialValues());
    }

    /**
     * Checks validity and uses callback method
     */
    update(selectedValue) {
      this.onUpdate({ value: selectedValue });
    }

    showInitialValues() {
      //TODO: IMPLEMENT THE INITIAL VALUES
      this.$scope.$apply();
    }

    updateDropdown() {
      this.update(this.select().value);
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
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.dropdownPicker", [])
  .directive("wonDropdownPicker", genComponentConf).name;
