import won from "../won-es6.js";
import angular from "angular";
import groupAdministrationHeaderModule from "./group-administration-header.js";
import connectionHeaderModule from "./connection-header.js";
import labelledHrModule from "./labelled-hr.js";
import { connect2Redux } from "../won-utils.js";
import { attach, getIn, delay } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { getGroupChatPostUriFromRoute } from "../selectors/general-selectors.js";
import { getGroupChatConnectionsByNeedUri } from "../selectors/connection-selectors.js";

import "style/_group-administration.scss";
import "style/_rdflink.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];

function genComponentConf() {
  let template = `
        <div class="ga__header">
            <a class="ga__header__back clickable"
               ng-click="self.router__stateGoCurrent({groupPostAdminUri : undefined})">
                <svg style="--local-primary:var(--won-primary-color);"
                     class="ga__header__back__icon clickable">
                    <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                </svg>
            </a>
            <won-group-administration-header
              need-uri="self.groupPostAdminUri">
            </won-group-administration-header>
            <!-- todo impl and include groupchat context-dropdown -->
        </div>
        <div class="ga__content">
            <div class="ga__content__participant"
                ng-if="self.hasGroupChatConnections"
                ng-repeat="conn in self.groupChatConnectionsArray">
                <won-connection-header
                    connection-uri="conn.get('uri')"
                    hide-image="::false">
                </won-connection-header>
                <div class="ga__content__participant__actions">
                    <div
                      class="ga__content__participant__actions__button red won-button--outlined thin"
                      ng-click="self.openRequest(conn.get('uri'))"
                      ng-if="conn.get('state') === self.won.WON.RequestReceived">
                        Accept
                    </div>
                    <div
                      class="ga__content__participant__actions__button red won-button--outlined thin"
                      ng-click="self.closeConnection(conn.get('uri'))"
                      ng-if="conn.get('state') === self.won.WON.RequestReceived">
                        Reject
                    </div>
                    <div
                      class="ga__content__participant__actions__button red won-button--outlined thin"
                      ng-click="self.sendRequest(conn.get('uri'), conn.get('remoteNeedUri'))"
                      ng-if="conn.get('state') === self.won.WON.Suggested">
                        Request
                    </div>
                    <div
                      class="ga__content__participant__actions__button red won-button--outlined thin"
                      ng-disabled="true"
                      ng-if="conn.get('state') === self.won.WON.RequestSent">
                        Waiting for Accept...
                    </div>
                    <div
                      class="ga__content__participant__actions__button red won-button--outlined thin"
                      ng-click="self.closeConnection(conn.get('uri'))"
                      ng-if="conn.get('state') === self.won.WON.Suggested || conn.get('state') === self.won.WON.Connected">
                        Remove
                    </div>
                </div>
            </div>
        </div>
    `;

  class Controller {
    constructor(/* arguments = dependency injections */) {
      attach(this, serviceDependencies, arguments);
      window.pgm4dbg = this;
      this.won = won;

      this.scrollContainer().addEventListener("scroll", e => this.onScroll(e));

      const selectFromState = state => {
        const groupPostAdminUri = getGroupChatPostUriFromRoute(state);
        const groupChatPost = getIn(state, ["needs", groupPostAdminUri]);
        const groupChatConnections = getGroupChatConnectionsByNeedUri(
          state,
          groupPostAdminUri
        );

        return {
          groupPostAdminUri,
          groupChatPost,
          groupChatConnections,
          hasGroupChatConnections:
            groupChatConnections && groupChatConnections.size > 0,
          groupChatConnectionsArray:
            groupChatConnections && groupChatConnections.toArray(),
          /*isSentRequest:
            connection && connection.get("state") === won.WON.RequestSent,
          isReceivedRequest:
            connection && connection.get("state") === won.WON.RequestReceived,
          isConnected:
            connection && connection.get("state") === won.WON.Connected,
          isSuggested:
            connection && connection.get("state") === won.WON.Suggested,*/
        };
      };

      connect2Redux(selectFromState, actionCreators, [], this);

      this._snapBottom = true; //Don't snap to bottom immediately, because this scrolls the whole page... somehow?

      this.$scope.$watch(
        () => this.groupChatConnections && this.groupChatConnections.size, // trigger if there's messages added (or removed)
        () =>
          delay(0).then(() =>
            // scroll to bottom directly after rendering, if snapped
            this.updateScrollposition()
          )
      );
    }

    snapToBottom() {
      this._snapBottom = true;
      this.scrollToBottom();
    }
    unsnapFromBottom() {
      this._snapBottom = false;
    }
    updateScrollposition() {
      if (this._snapBottom) {
        this.scrollToBottom();
      }
    }
    scrollToBottom() {
      this._programmaticallyScrolling = true;

      this.scrollContainer().scrollTop = this.scrollContainer().scrollHeight;
    }
    onScroll() {
      if (!this._programmaticallyScrolling) {
        //only unsnap if the user scrolled themselves
        this.unsnapFromBottom();
      }

      const sc = this.scrollContainer();
      const isAtBottom = sc.scrollTop + sc.offsetHeight >= sc.scrollHeight;
      if (isAtBottom) {
        this.snapToBottom();
      }

      this._programmaticallyScrolling = false;
    }
    scrollContainer() {
      if (!this._scrollContainer) {
        this._scrollContainer = this.$element[0].querySelector(".ga__content");
      }
      return this._scrollContainer;
    }

    openRequest(connUri, message) {
      this.connections__open(connUri, message);
    }

    sendRequest(connUri, remoteNeedUri, message) {
      this.connections__rate(connUri, won.WON.binaryRatingGood);
      this.needs__connect(
        this.groupPostAdminUri,
        connUri,
        remoteNeedUri,
        message
      );
    }

    closeConnection(connUri, rateBad = false) {
      rateBad && this.connections__rate(connUri, won.WON.binaryRatingBad);
      this.connections__close(connUri);
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {},
    template: template,
  };
}

export default angular
  .module("won.owner.components.groupAdministration", [
    groupAdministrationHeaderModule,
    connectionHeaderModule,
    labelledHrModule,
  ])
  .directive("wonGroupAdministration", genComponentConf).name;
