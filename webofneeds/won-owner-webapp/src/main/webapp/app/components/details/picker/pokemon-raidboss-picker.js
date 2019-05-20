import angular from "angular";
import { attach, delay } from "../../../utils.js";
import { DomCache } from "../../../cstm-ng-utils.js";
import datetimePickerModule from "./datetime-picker.js";
import titlePickerModule from "./title-picker.js";

import "/style/_pokemonraidbosspicker.scss";

const serviceDependencies = ["$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="prbp__level">
          <input id="prbp__level__1" type="radio" name="prbp__level" class="prbp__level__option" value="1" ng-checked="self.isLevelChecked(1)"/>
          <label for="prbp__level__1" ng-click="self.updateLevel(1)">{{ self.detail.getLevelLabel(1) }}</label>
          <input id="prbp__level__2" type="radio" name="prbp__level" class="prbp__level__option" value="2" ng-checked="self.isLevelChecked(2)"/>
          <label for="prbp__level__2" ng-click="self.updateLevel(2)">{{ self.detail.getLevelLabel(2) }}</label>
          <input id="prbp__level__3" type="radio" name="prbp__level" class="prbp__level__option" value="3" ng-checked="self.isLevelChecked(3)"/>
          <label for="prbp__level__3" ng-click="self.updateLevel(3)">{{ self.detail.getLevelLabel(3) }}</label>
          <input id="prbp__level__4" type="radio" name="prbp__level" class="prbp__level__option" value="4" ng-checked="self.isLevelChecked(4)"/>
          <label for="prbp__level__4" ng-click="self.updateLevel(4)">{{ self.detail.getLevelLabel(4) }}</label>
          <input id="prbp__level__5" type="radio" name="prbp__level" class="prbp__level__option" value="5" ng-checked="self.isLevelChecked(5)"/>
          <label for="prbp__level__5" ng-click="self.updateLevel(5)">{{ self.detail.getLevelLabel(5) }}</label>
      </div>
      <label for="prbp__hatched">Hatched</label>
      <input
          type="checkbox"
          id="prbp__hatched"
          class="prbp__hatched"
          ng-model="self.pokemonRaidBoss.hatched"
          ng-change="self.updateHatched(self.pokemonRaidBoss.hatched)"/>
      <label class="prbp__label" ng-class="{'prbp__label--disabled': self.pokemonRaidBoss.hatched}">Hatches at</label>
      <won-datetime-picker
          ng-class="{'prbp__hatches--disabled': self.pokemonRaidBoss.hatched}"
          class="prbp__hatches"
          initial-value="self.initialValue && self.initialValue.hatches"
          on-update="self.updateHatches(value)"
          detail="self.detail && self.detail.hatches">
      </won-datetime-picker>
      <label class="prbp__label">Expires at</label>
      <won-datetime-picker
          class="prbp__expires"
          initial-value="self.initialValue && self.initialValue.expires"
          on-update="self.updateExpires(value)"
          detail="self.detail && self.detail.expires">
      </won-datetime-picker>
      <label class="prbp__label"
          ng-class="{'prbp__label--disabled': !self.pokemonRaidBoss.hatched}">Pokemon</label>
      <won-title-picker
          ng-class="{'prbp__pokemon--disabled': !self.pokemonRaidBoss.hatched}"
          class="prbp__pokemon"
          initial-value="self.pokemonFilter"
          on-update="self.updatePokemonFilter(value)"
          detail="self.detail && self.detail.filterDetail">
      </won-title-picker>
      <div class="prbp__pokemonlist" ng-class="{'prbp__pokemonlist--disabled': !self.pokemonRaidBoss.hatched}">
        <div class="prbp__pokemonlist__pokemon"
          ng-repeat="pokemon in self.detail.pokemonList | filter:self.filterPokemon(self.pokemonFilter, self.pokemonRaidBoss.id, self.pokemonRaidBoss.form)"
          ng-class="{
            'prbp__pokemonlist__pokemon--selected': self.pokemonRaidBoss.id == pokemon.id && (!self.pokemonRaidBoss.form || self.pokemonRaidBoss.form == pokemon.form)
          }"
          ng-click="self.updatePokemon(pokemon.id, pokemon.form)">
          <img class="prbp__pokemonlist__pokemon__image" src="{{pokemon.imageUrl}}"/>
          <div class="prbp__pokemonlist__pokemon__id">
            #{{pokemon.id}}
          </div>
          <div class="prbp__pokemonlist__pokemon__name">
            {{pokemon.name}}
            <span class="prbp__pokemonlist__pokemon__name__form" ng-if="pokemon.form">({{ pokemon.form }})</span>
          </div>
        </div>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      window.pkmRaidBossp4dbg = this;

      this.pokemonRaidBoss = this.initialValue || { level: 1 };

      this.pokemonFilter = undefined;

      delay(0).then(() => this.showInitialValues());
    }

    /**
     * Checks validity and uses callback method
     */
    update(pokemonRaidBoss) {
      if (this.detail.isValid(pokemonRaidBoss)) {
        this.onUpdate({
          value: {
            id: pokemonRaidBoss.hatched ? pokemonRaidBoss.id : undefined,
            form: pokemonRaidBoss.hatched ? pokemonRaidBoss.form : undefined,
            level: pokemonRaidBoss.level,
            hatched: pokemonRaidBoss.hatched,
            hatches: !pokemonRaidBoss.hatched
              ? pokemonRaidBoss.hatches
              : undefined,
            expires: pokemonRaidBoss.expires,
          },
        });
      } else {
        this.onUpdate({ value: undefined });
      }
    }

    filterPokemon(pokemonFilter, selectedId, selectedForm) {
      return pokemon => {
        if (pokemonFilter) {
          const filterArray =
            pokemonFilter &&
            pokemonFilter
              .trim()
              .toLowerCase()
              .split(" ");
          if (filterArray && filterArray.length > 0) {
            for (const idx in filterArray) {
              if (pokemon.id == filterArray[idx]) return true;
              if ("#" + pokemon.id === filterArray[idx]) return true;
              if (
                pokemon.form &&
                pokemon.form.toLowerCase().includes(filterArray[idx])
              )
                return true;
              if (pokemon.name.toLowerCase().includes(filterArray[idx]))
                return true;
            }
          }
        }

        if (selectedId) {
          if (
            (!selectedForm && selectedId == pokemon.id && !pokemon.form) ||
            (selectedForm &&
              selectedId == pokemon.id &&
              selectedForm === pokemon.form)
          ) {
            return true;
          }
          return false;
        }

        return true;
      };
    }

    updateHatched(hatched) {
      this.pokemonRaidBoss.hatched = hatched;
      this.update(this.pokemonRaidBoss);
    }

    updateHatches(datetime) {
      this.pokemonRaidBoss.hatches = datetime;
      this.update(this.pokemonRaidBoss);
    }

    updateExpires(datetime) {
      this.pokemonRaidBoss.expires = datetime;
      this.update(this.pokemonRaidBoss);
    }

    updateLevel(level) {
      this.pokemonRaidBoss.level = level;
      this.update(this.pokemonRaidBoss);
    }

    isLevelChecked(option) {
      return this.pokemonRaidBoss && this.pokemonRaidBoss.level === option;
    }

    updatePokemonFilter(filter) {
      this.pokemonFilter = filter && filter.trim();
    }

    updatePokemon(id, form) {
      if (this.pokemonRaidBoss.id == id && this.pokemonRaidBoss.form === form) {
        this.pokemonRaidBoss.id = undefined;
        this.pokemonRaidBoss.form = undefined;
      } else {
        this.pokemonRaidBoss.id = id;
        this.pokemonRaidBoss.form = form;
      }

      console.debug(
        "Selected Pokemon:",
        this.pokemonRaidBoss,
        "based on (",
        id,
        ",",
        form,
        ")"
      );

      this.update(this.pokemonRaidBoss);
    }

    showInitialValues() {
      this.pokemonRaidBoss = this.initialValue || { level: 1 };
      this.$scope.$apply();
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
  .module("won.owner.components.pokemonRaidbossPicker", [
    datetimePickerModule,
    titlePickerModule,
  ])
  .directive("pokemonRaidbossPicker", genComponentConf).name;
