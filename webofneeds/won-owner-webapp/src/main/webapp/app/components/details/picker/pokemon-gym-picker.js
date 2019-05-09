import angular from "angular";
import { attach, delay } from "../../../utils.js";
import { DomCache } from "../../../cstm-ng-utils.js";
import locationPickerModule from "./location-picker.js";
import descriptionPickerModule from "./description-picker.js";
import titlePickerModule from "./title-picker.js";

import "style/_pokemongympicker.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <label class="pgp__label">Location:</label>
      <won-location-picker
          class="pgp__location"
          initial-value="self.pokemonGym.location"
          on-update="self.updateLocation(value)"
          detail="self.detail && self.detail.locationDetail">
      </won-location-picker>
      <label class="pgp__label">Name:</label>
      <won-title-picker
          class="pgp__name"
          initial-value="self.pokemonGym.name"
          on-update="self.updateName(value)"
          detail="self.detail && self.detail.nameDetail">
      </won-title-picker>
      <label for="pgp__ex" class="pgp__label">Gym Ex:</label>
      <input
          type="checkbox"
          id="pgp__ex"
          class="pgp__ex"
          ng-model="self.pokemonGym.ex"
          ng-change="self.updateEx(self.pokemonGym.ex)"/>
      <label class="pgp__label">Additional Info:</label>
      <won-description-picker
          class="pgp__info"
          initial-value="self.pokemonGym.info"
          on-update="self.updateInfo(value)"
          detail="self.detail && self.detail.infoDetail">
      </won-description-picker>
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
        console.debug("gym: ", pokemonGym);
        //TODO IMPL (include an isValid of some sorts)
        this.onUpdate({
          value: {
            name: pokemonGym.name,
            location: pokemonGym.location,
            info: pokemonGym.info,
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

    updateLocation(location) {
      this.pokemonGym.location = location;
      this.update(this.pokemonGym);
    }

    updateInfo(info) {
      this.pokemonGym.info = info;
      this.update(this.pokemonGym);
    }

    updateName(name) {
      this.pokemonGym.name = name;
      this.update(this.pokemonGym);
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
  .module("won.owner.components.pokemonGymPicker", [
    titlePickerModule,
    locationPickerModule,
    descriptionPickerModule,
  ])
  .directive("pokemonGymPicker", genComponentConf).name;
