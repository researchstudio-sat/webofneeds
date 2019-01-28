import won from "../won-es6.js";
import angular from "angular";
import groupAdministrationHeaderModule from "./group-administration-header.js";
import postHeaderModule from "./post-header.js";
import labelledHrModule from "./labelled-hr.js";
import { connect2Redux } from "../won-utils.js";
import { attach, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { getGroupPostAdminUriFromRoute } from "../selectors/general-selectors.js";
import { getGroupChatConnectionsByNeedUri } from "../selectors/connection-selectors.js";
import submitButtonModule from "./submit-button.js";

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
            <div class="ga__content__member"
                ng-if="self.hasGroupChatConnections"
                ng-repeat="conn in self.groupChatConnectionsArray">
                <won-post-header
                  class="clickable"
                  ng-click="self.router__stateGoCurrent({viewNeedUri: conn.get('remoteNeedUri')})"
                  need-uri="::conn.get('remoteNeedUri')"
                  hide-image="::false">
                </won-post-header>
                <div class="ga__content__member__actions">
                    <div
                      class="ga__content__member__actions__button red won-button--outlined thin"
                      ng-click="self.openRequest(conn.get('uri'))"
                      ng-if="conn.get('state') === self.won.WON.RequestReceived">
                        Accept
                    </div>
                    <div
                      class="ga__content__member__actions__button red won-button--outlined thin"
                      ng-click="self.closeConnection(conn.get('uri'))"
                      ng-if="conn.get('state') === self.won.WON.RequestReceived">
                        Reject
                    </div>
                    <div
                      class="ga__content__member__actions__button red won-button--outlined thin"
                      ng-click="self.sendRequest(conn.get('uri'), conn.get('remoteNeedUri'))"
                      ng-if="conn.get('state') === self.won.WON.Suggested">
                        Request
                    </div>
                    <div
                      class="ga__content__member__actions__button red won-button--outlined thin"
                      ng-disabled="true"
                      ng-if="conn.get('state') === self.won.WON.RequestSent">
                        Waiting for Accept...
                    </div>
                    <div
                      class="ga__content__member__actions__button red won-button--outlined thin"
                      ng-click="self.closeConnection(conn.get('uri'))"
                      ng-if="conn.get('state') === self.won.WON.Suggested || conn.get('state') === self.won.WON.Connected">
                        Remove
                    </div>
                </div>
            </div>
        </div>
        <div class="ga__footer">
            <won-submit-button
                class="ga__footer__button"
                is-valid="::true"
                on-submit="self.joinGroup(persona)"
                show-personas="::true"
                label="::'Join&#160;Group'">
            </won-submit-button>
        </div>
    `;

  class Controller {
    constructor(/* arguments = dependency injections */) {
      attach(this, serviceDependencies, arguments);
      window.pgm4dbg = this;
      this.won = won;

      const selectFromState = state => {
        const groupPostAdminUri = getGroupPostAdminUriFromRoute(state);
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
        };
      };

      connect2Redux(selectFromState, actionCreators, [], this);
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

    joinGroup(selectedPersona) {
      if (this.groupPostAdminUri) {
        this.connections__connectAdHoc(
          this.groupPostAdminUri,
          "",
          selectedPersona
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
    scope: {},
    template: template,
  };
}

export default angular
  .module("won.owner.components.groupAdministration", [
    groupAdministrationHeaderModule,
    postHeaderModule,
    labelledHrModule,
    submitButtonModule,
  ])
  .directive("wonGroupAdministration", genComponentConf).name;
