/**
 * Created by quasarchimaere on 25.03.2019.
 */

import angular from "angular";
import { getIn } from "../utils.js";
import { attach } from "../cstm-ng-utils.js";
import { connect2Redux } from "../configRedux.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import { actionCreators } from "../actions/actions.js";
import ngAnimate from "angular-animate";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import "~/style/_atom-content-holds.scss";
import preactModule from "./preact-module.js";
import WonAtomCard from "./atom-card.jsx";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
      <won-preact class="ach__atom" component="self.WonAtomCard"
                        props="{ atomUri: heldAtomUri, currentLocation: self.currentLocation, showSuggestions: self.isOwned, showPersona: false }"
                        ng-repeat="heldAtomUri in self.heldAtomUrisArray track by heldAtomUri"
                        ng-if="self.hasHeldAtoms"></won-preact>
      
      <div class="ach__createatom"
          ng-if="self.isOwned"
          ng-click="self.router__stateGo('create', {holderUri: self.atomUri})"
        >
          <svg class="ach__createatom__icon" title="Create a new post">
            <use xlink:href="#ico36_plus" href="#ico36_plus" />
          </svg>
          <span class="ach__createatom__label">New</span>
      </div>
      <div class="ach__empty"
          ng-if="!self.isOwned && !self.hasHeldAtoms">
          Not one single Atom present.
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.atomContentHolds4dbg = this;
      this.WonAtomCard = WonAtomCard;

      const selectFromState = state => {
        const atom = getIn(state, ["atoms", this.atomUri]);
        const heldAtomUris = atomUtils.getHeldAtomUris(atom);

        return {
          isOwned: generalSelectors.isAtomOwned(state, this.atomUri),
          hasHeldAtoms: atomUtils.hasHeldAtoms(atom),
          heldAtomUrisArray: heldAtomUris && heldAtomUris.toArray(),
        };
      };
      connect2Redux(selectFromState, actionCreators, ["self.atomUri"], this);
    }
  }

  Controller.$inject = serviceDependencies;
  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    template: template,
    scope: {
      atomUri: "=",
    },
  };
}

export default angular
  .module("won.owner.components.atomContentHolds", [ngAnimate, preactModule])
  .directive("wonAtomContentHolds", genComponentConf).name;
