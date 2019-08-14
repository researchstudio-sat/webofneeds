import angular from "angular";
import { delay } from "../../../utils.js";
import { attach, DomCache } from "../../../cstm-ng-utils.js";
import wonInput from "../../../directives/input.js";

import "~/style/_petrinettransitionpicker.scss";

//value.petriNetUri && value.transitionUri

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
    <div class="petrinettransitionp__petrineturi">
      <div class="petrinettransitionp__petrineturi__label">
        PetriNetUri:
      </div>
      <div class="petrinettransitionp__petrineturi__input">
         <svg class="petrinettransitionp__petrineturi__input__icon clickable"
            style="--local-primary:var(--won-primary-color);"
            ng-if="self.showPetriNetUriResetButton"
            ng-click="self.resetPetriNetUri()">
            <use xlink:href="#ico36_close" href="#ico36_close"></use>
         </svg>
         <input
            type="text"
            class="petrinettransitionp__petrineturi__input__inner"
            won-input="::self.updatePetriNetUri()" />
      </div>
    </div>
    <div class="petrinettransitionp__transitionuri">
      <div class="petrinettransitionp__transitionuri__label">
        TransitionUri:
      </div>
      <div class="petrinettransitionp__transitionuri__input">
         <svg class="petrinettransitionp__transitionuri__input__icon clickable"
            style="--local-primary:var(--won-primary-color);"
            ng-if="self.showTransitionUriResetButton"
            ng-click="self.resetTransitionUri()">
            <use xlink:href="#ico36_close" href="#ico36_close"></use>
         </svg>
         <input
            type="text"
            class="petrinettransitionp__transitionuri__input__inner"
            won-input="::self.updateTransitionUri()" />
      </div>
    </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.petrinettransitionp4dbg = this;

      this.selectedPetriNetUri =
        this.initialValue && this.initialValue.petriNetUri;
      this.selectedTransitionNetUri =
        this.initialValue && this.initialValue.transitionUri;
      this.showPetriNetUriResetButton = false;
      this.showTransitionUriResetButton = false;

      delay(0).then(() => this.showInitialValues());
    }

    /**
     * Checks validity and uses callback method
     */
    update(petriNetUri, transitionUri) {
      if (
        petriNetUri &&
        petriNetUri.trim().length > 0 &&
        transitionUri &&
        transitionUri.trim().length > 0
      ) {
        this.onUpdate({
          value: { petriNetUri: petriNetUri, transitionUri: transitionUri },
        });
      } else {
        this.onUpdate({ value: undefined });
      }
    }

    showInitialValues() {
      this.selectedPetriNetUri =
        this.initialValue && this.initialValue.petriNetUri;
      this.selectedTransitionUri =
        this.initialValue && this.initialValue.transitionUri;

      if (
        this.initialValue &&
        this.initialValue.petriNetUri &&
        this.initialValue.petriNetUri.trim().length > 0
      ) {
        this.petriNetUriField().value = this.initialValue.petriNetUri.trim();
        this.showPetriNetUriResetButton = true;
      }
      if (
        this.initialValue &&
        this.initialValue.transitionUri &&
        this.initialValue.transitionUri.trim().length > 0
      ) {
        this.transitionUriField().value = this.initialValue.transitionUri.trim();
        this.showTransitionUriResetButton = true;
      }

      this.$scope.$apply();
    }

    updatePetriNetUri() {
      const text = this.petriNetUriField().value;

      if (text && text.trim().length > 0) {
        this.selectedPetriNetUri = text.trim();
        this.update(this.selectedPetriNetUri, this.selectedTransitionUri);
        this.showPetriNetUriResetButton = true;
      } else {
        this.resetPetriNetUri();
      }
    }

    updateTransitionUri() {
      const text = this.transitionUriField().value;

      if (text && text.trim().length > 0) {
        this.selectedTransitionUri = text.trim();
        this.update(this.selectedPetriNetUri, this.selectedTransitionUri);
        this.showTransitionUriResetButton = true;
      } else {
        this.resetTransitionUri();
      }
    }

    resetPetriNetUri() {
      this.selectedPetriNetUri = undefined;
      this.petriNetUriField().value = "";
      this.update(undefined, this.selectedTransitionUri);
      this.showPetriNetUriResetButton = false;
    }

    resetTransitionUri() {
      this.selectedTransitionUri = undefined;
      this.transitionUriField().value = "";
      this.update(this.selectedPetriNetUri, undefined);
      this.showTransitionUriResetButton = false;
    }

    petriNetUriField() {
      if (!this._petriNetUriInput) {
        this._petriNetUriInput = this.$element[0].querySelector(
          ".petrinettransitionp__petrineturi__input__inner"
        );
      }
      return this._petriNetUriInput;
    }

    transitionUriField() {
      if (!this._transitionUriInput) {
        this._transitionUriInput = this.$element[0].querySelector(
          ".petrinettransitionp__transitionuri__input__inner"
        );
      }
      return this._transitionUriInput;
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
  .module("won.owner.components.petrinettransitionPicker", [wonInput])
  .directive("wonPetrinettransitionPicker", genComponentConf).name;
