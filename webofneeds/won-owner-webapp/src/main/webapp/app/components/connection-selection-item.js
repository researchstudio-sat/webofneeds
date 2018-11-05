/**
 * Created by ksinger on 10.04.2017.
 */

import angular from "angular";
import { attach } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import { actionCreators } from "../actions/actions.js";
import {
  selectOpenConnectionUri,
  selectNeedByConnectionUri,
} from "../selectors/selectors.js";

import connectionHeaderModule from "./connection-header.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";

import "style/_connection-selection-item-line.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
      <won-connection-header
        connection-uri="self.connectionUri"
        timestamp="self.lastUpdateTimestamp"
        ng-click="self.setOpen()"
        class="clickable">
      </won-connection-header>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      const selectFromState = state => {
        const ownNeed = selectNeedByConnectionUri(state, this.connectionUri);
        const connection =
          ownNeed && ownNeed.getIn(["connections", this.connectionUri]);

        return {
          openConnectionUri: selectOpenConnectionUri(state),
          lastUpdateTimestamp: connection && connection.get("lastUpdateDate"),
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
