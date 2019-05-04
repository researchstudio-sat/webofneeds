/**
 * Created by ksinger on 10.04.2017.
 */

import angular from "angular";
import { attach, getIn, get } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import { actionCreators } from "../actions/actions.js";
import {
  getConnectionUriFromRoute,
  getOwnedAtomByConnectionUri,
} from "../selectors/general-selectors.js";

import connectionHeaderModule from "./connection-header.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";

import "style/_connection-selection-item-line.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
      <won-connection-header
        connection-uri="::self.connectionUri"
        ng-click="self.setOpen()"
        class="clickable">
      </won-connection-header>
      <button
        class="csi__closebutton red won-button--outlined thin"
        ng-click="self.closeConnection()"
        ng-if="self.targetAtomFailedToLoad">
          Close
      </button>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      const selectFromState = state => {
        const ownedAtom = getOwnedAtomByConnectionUri(
          state,
          this.connectionUri
        );
        const connection = getIn(ownedAtom, [
          "connections",
          this.connectionUri,
        ]);
        const targetAtomUri = get(connection, "targetAtomUri");
        return {
          openConnectionUri: getConnectionUriFromRoute(state),
          lastUpdateTimestamp: get(connection, "lastUpdateDate"),
          targetAtomFailedToLoad:
            targetAtomUri &&
            getIn(state, ["process", "atoms", targetAtomUri, "failedToLoad"]),
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.connectionUri"],
        this
      );

      classOnComponentRoot("selected", () => this.isOpen(), this);
    }
    isOpen() {
      return this.openConnectionUri === this.connectionUri;
    }

    closeConnection() {
      this.connections__close(this.connectionUri);
      this.router__stateGoCurrent({
        useCase: undefined,
        connectionUri: undefined,
      });
    }

    setOpen() {
      this.onSelectedConnection({ connectionUri: this.connectionUri }); //trigger callback with scope-object
      //TODO either publish a dom-event as well; or directly call the route-change
    }
  }
  Controller.$inject = serviceDependencies;
  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      connectionUri: "=",
      /*
             * Usage:
             *  on-selected-connection="myCallback(connectionUri)"
             */
      onSelectedConnection: "&",
    },
    template: template,
  };
}
export default angular
  .module("won.owner.components.connectionSelectionItem", [
    connectionHeaderModule,
  ])
  .directive("wonConnectionSelectionItem", genComponentConf).name;
