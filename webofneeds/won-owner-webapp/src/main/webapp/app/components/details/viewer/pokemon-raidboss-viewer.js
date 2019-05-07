import angular from "angular";
import { attach, get } from "../../../utils.js";
import "ng-redux";
import { actionCreators } from "../../../actions/actions.js";
import { connect2Redux } from "../../../won-utils.js";
import { relativeTime } from "../../../won-label-utils.js";
import { selectLastUpdateTime } from "../../../selectors/general-selectors.js";
import "style/_pokemon-raidboss-viewer.scss";

const serviceDependencies = ["$scope", "$ngRedux", "$element"];
function genComponentConf() {
  let template = `
        <div class="prbv__header">
          <svg class="prbv__header__icon" ng-if="self.detail.icon">
              <use xlink:href={{self.detail.icon}} href={{self.detail.icon}}></use>
          </svg>
          <span class="prbv__header__label" ng-if="self.detail.label">{{self.detail.label}}</span>
        </div>
        <div class="prbv__content">
          <div class="prbv__content__level"
            ng-class="{
              'prbv__content__level--normal': self.level == 1 || self.level == 2,
              'prbv__content__level--rare': self.level == 3 || self.level == 4,
              'prbv__content__level--legendary': self.level == 5,
            }">
            {{ self.levelLabel }}
          </div>
          <div class="prbv__content__pokemon" ng-if="self.hatched && self.pokemon">
            <img class="prbv__content__pokemon__image" src="{{self.pokemon.imageUrl}}"/>
            <div class="prbv__content__pokemon__id">#{{self.pokemon.id}}</div>
            <div class="prbv__content__pokemon__name">{{self.pokemon.name}}</div>
          </div>
          <div class="prbv__content__pokemon" ng-if="!self.hatched || !self.pokemon">
            <img class="prbv__content__pokemon__image prbv__content__pokemon__image--unhatched" src="{{self.detail.pokemonList[0].imageUrl}}"/>
            <div class="prbv__content__pokemon__id">?</div>
            <div class="prbv__content__pokemon__name" ng-if="!self.shouldHaveHatched">Hatches {{ self.friendlyHatchesTime }}</div>
            <div class="prbv__content__pokemon__name" ng-if="self.shouldHaveHatched">Should have hatched {{ self.friendlyHatchesTime }}</div>
          </div>
          <div class="prbv__content__expires prbv__content__expires--expired" ng-if="self.hasExpired">
            Has expired {{ self.friendlyExpiresTime }}
          </div>
          <div class="prbv__content__expires" ng-if="!self.hasExpired">
            Expires {{ self.friendlyExpiresTime }}
          </div>
          <!--div class="prbv__content__label">
            Hatches:
          </div>
          <div class="prbv__content__hatches">
            {{ self.hatches }}
          </div>
          <div class="prbv__content__label">
            Hatched:
          </div>
          <div class="prbv__content__hatched">
            {{ self.hatched }}
          </div-->
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.pkmv4dbg = this;

      const selectFromState = state => {
        const id = get(this.content, "id");
        const level = get(this.content, "level");
        const hatches = get(this.content, "hatches");
        const expires = get(this.content, "expires");
        const hatched = get(this.content, "hatched");

        return {
          id,
          pokemon:
            id &&
            this.detail &&
            this.detail.findPokemonById &&
            this.detail.findPokemonById(id),
          level,
          levelLabel:
            level &&
            this.detail &&
            this.detail.getLevelLabel &&
            this.detail.getLevelLabel(level),
          hatches,
          expires,
          hatched,
          shouldHaveHatched: hatches && selectLastUpdateTime(state) > hatches,
          hasExpired: expires && selectLastUpdateTime(state) > expires,
          friendlyHatchesTime:
            hatches && relativeTime(selectLastUpdateTime(state), hatches),
          friendlyExpiresTime:
            expires && relativeTime(selectLastUpdateTime(state), expires),
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.content", "self.detail"],
        this
      );
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      content: "=",
      detail: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.pokemonRaidbossViewer", [])
  .directive("pokemonRaidbossViewer", genComponentConf).name;
