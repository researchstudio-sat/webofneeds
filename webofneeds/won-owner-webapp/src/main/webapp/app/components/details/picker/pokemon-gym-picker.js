import angular from "angular";
import { attach, delay } from "../../../utils.js";
import { DomCache } from "../../../cstm-ng-utils.js";

import "style/_pokemongympicker.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <label for="pgp__ex" class="pgp__label">Gym Ex:</label>
      <input
          type="checkbox"
          id="pgp__ex"
          class="pgp__ex"
          ng-model="self.pokemonGym.ex"
          ng-change="self.updateEx(self.pokemonGym.ex)"/>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.pkmGymp4dbg = this;

      this.pokemonGym = this.initialValue || {};

      delay(0).then(() => this.showInitialValues());
    }

    /**
     * Checks validity and uses callback method
     */
    update(pokemonGym) {
      if (this.detail.isValid(pokemonGym)) {
        this.onUpdate({
          value: {
            ex: pokemonGym.ex,
          },
        });
      } else {
        this.onUpdate({ value: undefined });
      }
    }

    showInitialValues() {
      this.pokemonGym = this.initialValue || {};

      this.$scope.$apply();
    }

    updateEx(ex) {
      this.pokemonGym.ex = ex;
      this.update(this.pokemonGym);
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
  .module("won.owner.components.pokemonGymPicker", [])
  .directive("pokemonGymPicker", genComponentConf).name;
