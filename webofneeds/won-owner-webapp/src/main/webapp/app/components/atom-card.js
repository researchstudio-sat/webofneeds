import angular from "angular";
import "ng-redux";
import { actionCreators } from "../actions/actions.js";
import { get, getIn } from "../utils.js";
import { attach } from "../cstm-ng-utils.js";
import { connect2Redux } from "../configRedux.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";
import preactModule from "./preact-module.js";
import WonSkeletonCard from "./cards/skeleton-card.jsx";
import WonPersonaCard from "./cards/persona-card.jsx";
import WonOtherCard from "./cards/other-card.jsx";

import "~/style/_atom-card.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
    <!-- ATOM SKELETON VIEW-->
    <won-preact class="won-skeleton-card" component="self.WonSkeletonCard" props="{atomUri: self.atomUri, showSuggestions: self.showSuggestions, showPersona: self.showPersona}" ng-if="self.isSkeleton"></won-preact>
    
    <!-- PERSONA VIEW -->
    <won-preact class="won-persona-card" component="self.WonPersonaCard" props="{atomUri: self.atomUri, disableDefaultAtomInteraction: self.disableDefaultAtomInteraction}" ng-if="self.isPersona"></won-preact>
    
    <!-- OTHER POSTS VIEW -->
    <won-preact class="won-other-card" component="self.WonOtherCard"  props="{atomUri: self.atomUri, showSuggestions: self.showSuggestions, showPersona: self.showPersona, disableDefaultAtomInteraction: self.disableDefaultAtomInteraction, currentLocation: self.currentLocation}" ng-if="self.isOtherAtom"></won-preact>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      this.WonSkeletonCard = WonSkeletonCard;
      this.WonPersonaCard = WonPersonaCard;
      this.WonOtherCard = WonOtherCard;

      const selectFromState = state => {
        const atom = getIn(state, ["atoms", this.atomUri]);
        const isPersona = atomUtils.isPersona(atom);
        const process = get(state, "process");
        const isSkeleton =
          !(
            processUtils.isAtomLoaded(process, this.atomUri) &&
            !get(atom, "isBeingCreated")
          ) ||
          get(atom, "isBeingCreated") ||
          processUtils.hasAtomFailedToLoad(process, this.atomUri) ||
          processUtils.isAtomLoading(process, this.atomUri) ||
          processUtils.isAtomToLoad(process, this.atomUri);

        if (isSkeleton) {
          return {
            isPersona: false,
            isOtherAtom: false,
            isSkeleton: true,
          };
        } else if (isPersona) {
          return {
            isPersona: true,
            isOtherAtom: false,
            isSkeleton: false,
          };
        } else {
          return {
            isPersona: false,
            isOtherAtom: true,
            isSkeleton: false,
          };
        }
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        [
          "self.atomUri",
          "self.currentLocation",
          "self.showSuggestions",
          "self.showPersona",
          "self.disableDefaultAtomInteraction",
        ],
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
      atomUri: "=",
      currentLocation: "=",
      showSuggestions: "=",
      showPersona: "=",
      disableDefaultAtomInteraction: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.atomCard", [preactModule])
  .directive("wonAtomCard", genComponentConf).name;
