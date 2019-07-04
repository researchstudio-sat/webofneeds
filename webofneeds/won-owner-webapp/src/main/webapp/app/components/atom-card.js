import angular from "angular";
import "ng-redux";
import skeletonCardModule from "./cards/skeleton-card.js";
import otherCardModule from "./cards/other-card.js";
import personaCardModule from "./cards/persona-card.js";
import { actionCreators } from "../actions/actions.js";
import { getIn, get } from "../utils.js";
import { attach } from "../cstm-ng-utils.js";
import { connect2Redux } from "../configRedux.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";

import "~/style/_atom-card.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
    <!-- ATOM SKELETON VIEW-->
    <won-skeleton-card
        ng-if="self.isSkeleton"
        atom-uri="self.atomUri"
        show-suggestions="::self.showSuggestions"
        show-persona="self.showPersona">
    </won-skeleton-card>
    
    <!-- PERSONA VIEW -->
    <won-persona-card
        ng-if="self.isPersona"
        atom-uri="self.atomUri"
        disable-default-atom-interaction="self.disableDefaultAtomInteraction">
    </won-persona-card>
    
    <!-- OTHER POSTS VIEW -->
    <won-other-card
        ng-if="self.isOtherAtom"
        atom-uri="self.atomUri"
        current-location="self.currentLocation"
        show-persona="self.showPersona"
        show-suggestions="::self.showSuggestions"
        disable-default-atom-interaction="self.disableDefaultAtomInteraction">
    </won-other-card>    
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

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
  .module("won.owner.components.atomCard", [
    skeletonCardModule,
    personaCardModule,
    otherCardModule,
  ])
  .directive("wonAtomCard", genComponentConf).name;
