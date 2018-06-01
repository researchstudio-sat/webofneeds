/**
 * Created by ksinger on 10.04.2017.
 */

import won from "../won-es6.js";
import angular from "angular";
import { labels } from "../won-label-utils.js";
import { attach } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import { actionCreators } from "../actions/actions.js";
import {
  selectOpenConnectionUri,
  selectNeedByConnectionUri,
} from "../selectors.js";

import connectionHeaderModule from "./connection-header.js";
import connectionStateModule from "./connection-state.js";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
      <won-connection-state connection-uri="self.connectionUri" ng-if="self.connection.get('state') === self.WON.Suggested">
      </won-connection-state>
      <won-connection-header
        connection-uri="self.connectionUri"
        timestamp="self.lastUpdateTimestamp"
        ng-click="self.setOpen()"
        class="clickable">
      </won-post-header>

      <div class="conn__unreadCount" ng-if="self.connection.get('state') === self.WON.Connected">
        {{ self.unreadMessagesCount }}
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.labels = labels;
      //this.settingsOpen = false;

      const selectFromState = state => {
        const ownNeed = selectNeedByConnectionUri(state, this.connectionUri);
        const connection =
          ownNeed && ownNeed.getIn(["connections", this.connectionUri]);

        const allMessages = connection && connection.get("messages");
        const unreadMessages =
          allMessages && allMessages.filter(msg => msg.get("unread"));

        return {
          WON: won.WON,
          ownNeed,
          connection,
          connectionUri: connection && connection.get("uri"),
          openConnectionUri: selectOpenConnectionUri(state),
          lastUpdateTimestamp: connection && connection.get("lastUpdateDate"),
          unreadMessagesCount:
            unreadMessages && unreadMessages.size > 0
              ? unreadMessages.size
              : undefined,
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.connectionUri"],
        this
      );

      this.$scope.$watch(
        () => this.isOpen(),
        newVal => {
          if (newVal) {
            this.$element[0].classList.add("selected");
          } else {
            this.$element[0].classList.remove("selected");
          }
        }
      );
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
    connectionStateModule,
  ])
  .directive("wonConnectionSelectionItem", genComponentConf).name;
