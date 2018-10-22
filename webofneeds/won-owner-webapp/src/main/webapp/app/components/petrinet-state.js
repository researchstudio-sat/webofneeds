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
          <div class="ps__active__header">
            Marked Places
          </div>
          <div class="ps__active__markedPlace"
            ng-if="self.hasMarkedPlaces"
            ng-repeat="markedPlace in self.markedPlacesArray">
              {{ self.getSimpleLabel(markedPlace) }}
          </div>
          <div class="ps__active__noMarkedPlace" ng-if="!self.hasMarkedPlaces">
            No Marked Places in PetriNet
          </div>
          <div class="ps__active__header">
            Enabled Transitions
          </div>
          <div class="ps__active__enabledTransition"
            ng-if="self.hasEnabledTransitions"
            ng-repeat="enabledTransition in self.enabledTransitionsArray">
              <div class="ps__active__enabledTransition__label">
                {{ self.getSimpleLabel(enabledTransition) }}
              </div>
              <!-- The button is labelled 'send' at the moment because we jsut send the transition but not claim it right away -->
              <button class="ps__active__enabledTransition__button won-button--filled thin red"
                ng-disabled="self.multiSelectType"
                ng-click="self.sendClaim(enabledTransition)">
                  Claim
              </button>
          </div>
          <div class="ps__active__noEnabledTransition" ng-if="!self.hasEnabledTransitions">
            No Enabled Transitions in PetriNet
          </div>
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
          connection.getIn(["petriNetData", "data", this.processUri]);

        const isLoading = false; //TODO: Implement Loading in state first
        const markedPlaces = process && process.get("markedPlaces");
        const enabledTransitions = process && process.get("enabledTransitions");

        const markedPlacesSize = markedPlaces ? markedPlaces.size : 0;
        const enabledTransitionsSize = enabledTransitions
          ? enabledTransitions.size
          : 0;

        return {
          connectionUri: connectionUri,
          multiSelectType: connection && connection.get("multiSelectType"),
          process: process,
          hasEnabledTransitions: enabledTransitionsSize > 0,
          hasMarkedPlaces: markedPlacesSize > 0,
          enabledTransitionsArray:
            enabledTransitions && enabledTransitions.toArray(),
          markedPlacesArray: markedPlaces && markedPlaces.toArray(),
          isLoading: isLoading,
        };
      };

      connect2Redux(selectFromState, actionCreators, ["self.processUri"], this);
    }

    getSimpleLabel(str) {
      if (str) {
        const indexOfLastSharp = str.lastIndexOf("#");

        if (indexOfLastSharp != -1 && indexOfLastSharp + 1 < str.length) {
          return str.substr(indexOfLastSharp + 1);
        }
      }
      return str;
    }

    //TODO: IMPL ADHOC CLAIM
    sendClaim(transitionUri) {
      if (transitionUri && this.processUri && this.connectionUri) {
        console.log(
          "send transition 'claim' ",
          transitionUri,
          " for processUri: ",
          this.processUri
        );

        this.connections__sendChatMessage(
          undefined,
          new Map().set("petriNetTransition", {
            petriNetUri: this.processUri,
            transitionUri: transitionUri,
          }),
          undefined,
          this.connectionUri,
          false
        );
      }
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
