import angular from "angular";
import { attach, delay } from "../../../utils.js";
import { DomCache } from "../../../cstm-ng-utils.js";
import wonInput from "../../../directives/input.js";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="filep__input">
          <input
              type="file"
              class="filep__input__inner"
              placeholder="{{self.detail.placeholder}}"
              won-input="::self.updateTitle()" />
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.filep4dbg = this;

      this.addedTitle = this.initialValue;

      delay(0).then(() => this.showInitialTitle());
    }

    /**
     * Checks validity and uses callback method
     */
    update(title) {
      console.log("title: ", title);
      /*if (title && title.trim().length > 0) {
        this.onUpdate({ value: title });
      } else {
        this.onUpdate({ value: undefined });
      }*/
    }

    showInitialTitle() {
      /*this.addedTitle = this.initialValue;

      if (this.initialValue && this.initialValue.trim().length > 0) {
        this.textfield().value = this.initialValue.trim();
        this.showResetButton = true;
      }*/

      this.$scope.$apply();
    }

    updateTitle() {
      /*const text = this.textfield().value;

      if (text && text.trim().length > 0) {
        this.addedTitle = text.trim();
        this.update(this.addedTitle);
      }*/
    }

    textfieldNg() {
      return angular.element(this.textfield());
    }
    textfield() {
      if (!this._titleInput) {
        this._titleInput = this.$element[0].querySelector(
          ".filep__input__inner"
        );
      }
      return this._titleInput;
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
  .module("won.owner.components.filePicker", [wonInput])
  .directive("wonFilePicker", genComponentConf).name;
