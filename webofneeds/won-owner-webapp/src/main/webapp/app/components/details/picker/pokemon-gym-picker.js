import angular from "angular";
import { attach, delay } from "../../../utils.js";
import { DomCache } from "../../../cstm-ng-utils.js";
import wonInput from "../../../directives/input.js";
import "angular-marked";

import "style/_pokemongympicker.scss";
import "style/_won-markdown.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="pgp__input">
      GYM PICKA!!! TODO: REMOVE THIS LINE!!!
        <svg class="pgp__input__icon clickable"
          style="--local-primary:var(--won-primary-color);"
          ng-if="self.showResetButton"
          ng-click="self.resetDescription()">
          <use xlink:href="#ico36_close" href="#ico36_close"></use>
        </svg>
        <textarea
          won-textarea-autogrow
          class="pgp__input__inner"
          won-input="::self.updateDescription()"
          placeholder="{{self.detail.placeholder}}"></textarea>
      </div>
      <div class="pgp__preview__header">Preview</div>
      <div ng-if="self.addedDescription" class="pgp__preview__content markdown" marked="self.addedDescription"></div>
      <div ng-if="!self.addedDescription" class="pgp__preview__content--empty>Add Content to see instant preview</div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.pkmGymp4dbg = this;

      this.addedDescription = this.initialValue;
      this.showResetButton = false;

      delay(0).then(() => this.showInitialDescription());
    }

    /**
     * Checks validity and uses callback method
     */
    update(description) {
      if (description && description.trim().length > 0) {
        this.onUpdate({ value: description });
      } else {
        this.onUpdate({ value: undefined });
      }
    }

    showInitialDescription() {
      this.addedDescription = this.initialValue;

      if (this.initialValue && this.initialValue.trim().length > 0) {
        this.textfield().value = this.initialValue.trim();
        this.showResetButton = true;
      }

      this.$scope.$apply();
    }

    updateDescription() {
      const text = this.textfield().value;

      if (text && text.trim().length > 0) {
        this.addedDescription = text;
        this.update(this.addedDescription);
        this.showResetButton = true;
      } else {
        this.resetDescription();
      }
    }

    resetDescription() {
      this.addedDescription = undefined;
      this.textfield().value = "";
      this.update(undefined);
      this.showResetButton = false;
    }

    textfieldNg() {
      return this.domCache.ng(".pgp__input__inner");
    }

    textfield() {
      return this.domCache.dom(".pgp__input__inner");
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
  .module("won.owner.components.pokemonGymPicker", [wonInput, "hc.marked"])
  .directive("pokemonGymPicker", genComponentConf).name;
