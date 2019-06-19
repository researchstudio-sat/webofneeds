/**
 * Created by quasarchimaere on 25.03.2019.
 */

import angular from "angular";
import postHeaderModule from "./post-header.js";
import { getIn } from "../utils.js";
import { attach } from "../cstm-ng-utils.js";
import { connect2Redux } from "../configRedux.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import { actionCreators } from "../actions/actions.js";
import ngAnimate from "angular-animate";

import "~/style/_atom-content-holds.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
      <div
        class="ach__atom"
        ng-if="self.hasHeldAtoms"
        ng-repeat="heldAtomUri in self.heldAtomUrisArray track by heldAtomUri">
        <div class="ach__atom__indicator"></div>
        <won-post-header
          class="clickable"
          ng-click="self.router__stateGoCurrent({viewAtomUri: heldAtomUri, viewConnUri: undefined})"
          atom-uri="::heldAtomUri">
        </won-post-header>
      </div>
      <div class="ach__empty"
          ng-if="!self.hasHeldAtoms">
          Not one single Atom present.
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.atomContentHolds4dbg = this;

      const selectFromState = state => {
        const atom = getIn(state, ["atoms", this.atomUri]);
        const heldAtomUris = atomUtils.getHeldAtomUris(atom);

        return {
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
  .module("won.owner.components.atomContentHolds", [
    ngAnimate,
    postHeaderModule,
  ])
  .directive("wonAtomContentHolds", genComponentConf).name;
