import angular from "angular";
import { attach, delay } from "../../utils.js";
import { DomCache } from "../../cstm-ng-utils.js";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="dp__input">
        <svg class="dp__input__icon clickable" 
          style="--local-primary:var(--won-primary-color);"
          ng-if="self.showResetButton"
          ng-click="self.resetDescription()">
          <use xlink:href="#ico36_close" href="#ico36_close"></use>
        </svg>
        <textarea
          won-textarea-autogrow
          class="dp__input__inner won-txt"
          ng-keyup="::self.updateDescription()"
          placeholder="Enter Description..."></textarea>
      </div> 
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.dp4dbg = this;

      this.addedDescription = this.initialDescription;
      this.showResetButton = false;

      delay(0).then(() => this.showInitialDescription());
    }

    showInitialDescription() {
      this.addedDescription = this.initialDescription;

      if (
        this.initialDescription &&
        this.initialDescription.trim().length > 0
      ) {
        this.textfield().value = this.initialDescription.trim();
        this.showResetButton = true;
      }

      this.$scope.$apply();
    }

    updateDescription() {
      const text = this.textfield().value;

      if (text && text.trim().length > 0) {
        this.addedDescription = text;
        this.onDescriptionUpdated({ description: this.addedDescription });
        this.showResetButton = true;
      } else {
        this.resetDescription();
      }
    }

    resetDescription() {
      this.addedDescription = undefined;
      this.textfield().value = "";
      this.onDescriptionUpdated({ description: undefined });
      this.showResetButton = false;
    }

    textfieldNg() {
      return this.domCache.ng(".dp__input__inner");
    }

    textfield() {
      return this.domCache.dom(".dp__input__inner");
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      onDescriptionUpdated: "&",
      initialDescription: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.descriptionPicker", [])
  .directive("wonDescriptionPicker", genComponentConf).name;
