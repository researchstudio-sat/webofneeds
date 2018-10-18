/**
 * Component for rendering the connection state as an svg
 * Created by fsuda on 10.04.2017.
 */
import angular from "angular";
import "ng-redux";
import { actionCreators } from "../actions/actions.js";

import { attach } from "../utils.js";
import {
  selectNeedByConnectionUri,
  selectOpenConnectionUri,
} from "../selectors.js";
import { connect2Redux } from "../won-utils.js";

import "style/_petrinet-state.scss";

const serviceDependencies = ["$ngRedux", "$scope"];
function genComponentConf() {
  let template = `
        <div class="ps__active" ng-if="self.process && !self.isLoading">
            {{ self.process.toJS() }}
        </div>
        <div class="ps__inactive" ng-if="!self.process && !self.isLoading">
            This PetriNet, is not active (yet).
        </div>
        <div class="ps__loading" ng-if="!self.process && self.isLoading">
            The PetriNet-State, is currently loading
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      const selectFromState = state => {
        const connectionUri = selectOpenConnectionUri(state); //TODO: create selector that returns the correct connectionUri without looking up the open one
        const need =
          connectionUri && selectNeedByConnectionUri(state, connectionUri);
        const connection = need && need.getIn(["connections", connectionUri]);

        const process =
          this.processUri &&
          connection &&
          connection.getIn(["petriNetData", this.processUri]);

        const isLoading = false; //TODO: Implement Loading in state first

        return {
          process: process,
          isLoading: isLoading,
        };
      };

      connect2Redux(selectFromState, actionCreators, ["self.processUri"], this);
    }
  }
  Controller.$inject = serviceDependencies;
  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      processUri: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.petrinetState", [])
  .directive("wonPetrinetState", genComponentConf).name;
