/**
 * Component for rendering the connection state as an svg
 * Created by fsuda on 10.04.2017.
 */
import angular from "angular";
import "ng-redux";
import { actionCreators } from "../actions/actions.js";

import { generateSimpleTransitionLabel, getIn } from "../utils.js";
import { attach } from "../cstm-ng-utils.js";
import {
  getOwnedAtomByConnectionUri,
  getConnectionUriFromRoute,
} from "../redux/selectors/general-selectors.js";
import { connect2Redux } from "../configRedux.js";

import "~/style/_petrinet-state.scss";

const serviceDependencies = ["$ngRedux", "$scope"];
function genComponentConf() {
  let template = `
        <div class="ps__active" ng-if="self.process && (self.petriNetDataLoaded || !self.petriNetDataLoading)">
          <div class="ps__active__header">
            Marked Places
          </div>
          <div class="ps__active__markedPlace"
            ng-if="self.hasMarkedPlaces"
            ng-repeat="markedPlace in self.markedPlacesArray">
              {{ self.generateSimpleTransitionLabel(markedPlace) }}
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
                {{ self.generateSimpleTransitionLabel(enabledTransition) }}
              </div>
              <!-- The button is labelled 'send' at the moment because we jsut send the transition but not claim it right away -->
              <button class="ps__active__enabledTransition__button won-button--filled thin red"
                ng-disabled="self.multiSelectType || self.petriNetDataDirty"
                ng-click="self.sendClaim(enabledTransition)">
                  Claim
              </button>
          </div>
          <div class="ps__active__noEnabledTransition" ng-if="!self.hasEnabledTransitions">
            No Enabled Transitions in PetriNet
          </div>
        </div>
        <div class="ps__inactive" ng-if="!self.process && !self.petriNetDataLoading && self.petriNetDataLoaded">
            This PetriNet, is not active (yet).
        </div>
        <div class="ps__loading" ng-if="self.petriNetDataLoaded && (self.petriNetDataLoading || self.petriNetDataDirty)">
            <svg class="ps__loading__spinner">
              <use xlink:href="#ico_loading_anim" href="#ico_loading_anim"></use>
            </svg>
            <div class="ps__loading__label">The PetriNet-State, is currently being calculated</div>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.generateSimpleTransitionLabel = generateSimpleTransitionLabel;

      const selectFromState = state => {
        const connectionUri = getConnectionUriFromRoute(state); //TODO: create selector that returns the correct connectionUri without looking up the open one
        const atom =
          connectionUri && getOwnedAtomByConnectionUri(state, connectionUri);
        const connection = atom && atom.getIn(["connections", connectionUri]);

        const petriNetData = connection && connection.get("petriNetData");

        const process =
          this.processUri && petriNetData && petriNetData.get(this.processUri);

        const petriNetDataLoading =
          connection &&
          getIn(state, [
            "process",
            "connections",
            connectionUri,
            "petriNetData",
            "loading",
          ]);
        const petriNetDataLoaded =
          petriNetData &&
          getIn(state, [
            "process",
            "connections",
            connectionUri,
            "petriNetData",
            "loaded",
          ]);
        const petriNetDataDirty =
          petriNetData &&
          getIn(state, [
            "process",
            "connections",
            connectionUri,
            "petriNetData",
            "dirty",
          ]);
        const markedPlaces = process && process.get("markedPlaces");
        const enabledTransitions = process && process.get("enabledTransitions");

        const markedPlacesSize = markedPlaces ? markedPlaces.size : 0;
        const enabledTransitionsSize = enabledTransitions
          ? enabledTransitions.size
          : 0;

        return {
          connectionUri: connectionUri,
          multiSelectType: connection && connection.get("multiSelectType"),
          petriNetData: petriNetData,
          process: process,
          hasEnabledTransitions: enabledTransitionsSize > 0,
          hasMarkedPlaces: markedPlacesSize > 0,
          enabledTransitionsArray:
            enabledTransitions && enabledTransitions.toArray(),
          markedPlacesArray: markedPlaces && markedPlaces.toArray(),
          petriNetDataDirty: petriNetDataDirty,
          petriNetDataLoading: petriNetDataLoading,
          petriNetDataLoaded: petriNetDataLoaded,
        };
      };

      connect2Redux(selectFromState, actionCreators, ["self.processUri"], this);
    }

    sendClaim(transitionUri) {
      if (transitionUri && this.processUri && this.connectionUri) {
        console.debug(
          "send transition 'claim' ",
          transitionUri,
          " for processUri: ",
          this.processUri
        );

        this.connections__sendChatMessageClaimOnSuccess(
          undefined,
          new Map().set("petriNetTransition", {
            petriNetUri: this.processUri,
            transitionUri: transitionUri,
          }),
          this.connectionUri
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
