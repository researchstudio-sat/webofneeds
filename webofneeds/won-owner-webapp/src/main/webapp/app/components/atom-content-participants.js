/**
 * Created by quasarchimaere on 25.03.2019.
 */

import angular from "angular";
import inviewModule from "angular-inview";
import labelledHrModule from "./labelled-hr.js";
import postHeaderModule from "./post-header.js";
import suggestPostPickerModule from "./details/picker/suggestpost-picker.js";
import { getIn, get } from "../utils.js";
import { attach } from "../cstm-ng-utils.js";
import won from "../won-es6.js";
import { connect2Redux } from "../configRedux.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as connectionSelectors from "../redux/selectors/connection-selectors.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import { actionCreators } from "../actions/actions.js";
import ngAnimate from "angular-animate";

import "~/style/_atom-content-participants.scss";

const CONNECTION_READ_TIMEOUT = 1500;

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
      <div
          class="acp__participant"
          ng-if="!self.isOwned && self.groupMembers"
          ng-repeat="memberUri in self.groupMembersArray track by memberUri">
          <won-atom-card
              class="clickable"
              atom-uri="::memberUri"
              current-location="self.currentLocation"
              show-suggestions="::false"
              show-persona="::true"
          ></won-atom-card>
          <div class="acp__participant__actions"></div>
      </div>
      <div class="acp__participant"
          ng-if="self.isOwned && self.hasGroupChatConnections && conn.get('state') !== self.won.WON.Closed"
          ng-repeat="conn in self.groupChatConnectionsArray"
          in-view="conn.get('unread') && $inview && self.markAsRead(conn)"
          ng-class="{'won-unread': conn.get('unread')}">
          <won-atom-card
              class="clickable"
              atom-uri="::conn.get('targetAtomUri')"
              current-location="self.currentLocation"
              show-suggestions="::false"
              show-persona="::true"
          ></won-atom-card>
          <div class="acp__participant__actions">
              <div
                class="acp__participant__actions__button red won-button--outlined thin"
                ng-click="self.openRequest(conn)"
                ng-if="conn.get('state') === self.won.WON.RequestReceived">
                  Accept
              </div>
              <div
                class="acp__participant__actions__button red won-button--outlined thin"
                ng-click="self.closeConnection(conn)"
                ng-if="conn.get('state') === self.won.WON.RequestReceived">
                  Reject
              </div>
              <div
                class="acp__participant__actions__button red won-button--outlined thin"
                ng-click="self.sendRequest(conn)"
                ng-if="conn.get('state') === self.won.WON.Suggested">
                  Request
              </div>
              <div
                class="acp__participant__actions__button red won-button--outlined thin"
                ng-disabled="true"
                ng-if="conn.get('state') === self.won.WON.RequestSent">
                  Waiting for Accept...
              </div>
              <div
                class="acp__participant__actions__button red won-button--outlined thin"
                ng-click="self.closeConnection(conn)"
                ng-if="conn.get('state') === self.won.WON.Suggested || conn.get('state') === self.won.WON.Connected">
                  Remove
              </div>
          </div>
      </div>
      <div class="acp__empty"
          ng-if="(!self.isOwned && !self.groupMembers) || (self.isOwned && !self.hasGroupChatConnections)">
          No Groupmembers present.
      </div>
      <won-labelled-hr label="::'Invite'" class="acp__labelledhr" ng-if="self.isOwned" arrow="self.suggestAtomExpanded? 'up' : 'down'" ng-click="self.suggestAtomExpanded = !self.suggestAtomExpanded"></won-labelled-hr>
      <won-suggestpost-picker
          ng-if="self.isOwned && self.suggestAtomExpanded"
          initial-value="undefined"
          on-update="self.inviteParticipant(value)"
          detail="::{placeholder: 'Insert AtomUri to invite'}"
          excluded-uris="self.excludedFromInviteUris"
          allowed-sockets="::[self.won.CHAT.ChatSocketCompacted, self.won.GROUP.GroupSocketCompacted]"
          excluded-text="::'Invitation does not work for atoms that are already part of the Group, or the group itself'"
          not-allowed-socket-text="::'Invitation does not work on atoms without Group or Chat Socket'"
          no-suggestions-text="::'No Participants available to invite'"
      ></won-suggestpost-picker>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.won = won;
      this.suggestAtomExpanded = false;
      window.postcontentparticipants4dbg = this;

      const selectFromState = state => {
        const post = getIn(state, ["atoms", this.atomUri]);
        const isOwned = generalSelectors.isAtomOwned(state, this.atomUri);

        const hasGroupSocket = atomUtils.hasGroupSocket(post);

        const groupMembers = hasGroupSocket && get(post, "groupMembers");
        const groupChatConnections =
          isOwned &&
          hasGroupSocket &&
          connectionSelectors.getGroupChatConnectionsByAtomUri(
            state,
            this.atomUri
          );

        let excludedFromInviteUris = [this.atomUri];

        if (groupChatConnections) {
          groupChatConnections
            .filter(conn => !connectionUtils.isClosed(conn))
            .map(conn =>
              excludedFromInviteUris.push(get(conn, "targetAtomUri"))
            );
        }

        return {
          post,
          isOwned,
          hasGroupSocket,
          groupMembers: groupMembers && groupMembers.size > 0,
          hasGroupChatConnections:
            groupChatConnections && groupChatConnections.size > 0,
          groupChatConnectionsArray:
            groupChatConnections && groupChatConnections.toArray(),
          excludedFromInviteUris,
          groupMembersArray: groupMembers && groupMembers.toArray(),
        };
      };
      connect2Redux(selectFromState, actionCreators, ["self.atomUri"], this);
    }

    closeConnection(conn, rateBad = false) {
      if (!conn) {
        return;
      }

      const connUri = conn.get("uri");

      if (rateBad) {
        this.connections__rate(connUri, won.WONCON.binaryRatingBad);
      }

      if (conn.get("unread")) {
        this.connections__markAsRead({
          connectionUri: connUri,
          atomUri: this.atomUri,
        });
      }

      this.connections__close(connUri);
    }

    openRequest(conn, message = "") {
      if (!conn) {
        return;
      }

      const connUri = get(conn, "uri");

      if (conn.get("unread")) {
        this.connections__markAsRead({
          connectionUri: connUri,
          atomUri: this.atomUri,
        });
      }

      this.connections__open(connUri, message);
    }

    sendRequest(conn, message = "") {
      if (!conn) {
        return;
      }

      const payload = {
        caption: "Group",
        text: "Add as Participant?",
        buttons: [
          {
            caption: "Yes",
            callback: () => {
              const connUri = get(conn, "uri");
              const targetAtomUri = get(conn, "targetAtomUri");

              if (conn.get("unread")) {
                this.connections__markAsRead({
                  connectionUri: connUri,
                  atomUri: this.atomUri,
                });
              }

              this.connections__rate(connUri, won.WONCON.binaryRatingGood);
              this.atoms__connect(
                this.atomUri,
                connUri,
                targetAtomUri,
                message
              );
              this.view__hideModalDialog();
            },
          },
          {
            caption: "No",
            callback: () => {
              this.view__hideModalDialog();
            },
          },
        ],
      };
      this.view__showModalDialog(payload);
    }

    inviteParticipant(atomUri, message = "") {
      if (!this.isOwned || !this.hasGroupSocket) {
        console.warn("Trying to invite to a non-owned or non groupSocket atom");
        return;
      }
      this.atoms__connect(this.atomUri, undefined, atomUri, message);
    }

    markAsRead(conn) {
      if (conn && conn.get("unread")) {
        const payload = {
          connectionUri: conn.get("uri"),
          atomUri: this.atomUri,
        };

        const tmp_connections__markAsRead = this.connections__markAsRead;

        setTimeout(function() {
          tmp_connections__markAsRead(payload);
        }, CONNECTION_READ_TIMEOUT);
      }
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
  .module("won.owner.components.atomContentParticipants", [
    ngAnimate,
    labelledHrModule,
    postHeaderModule,
    suggestPostPickerModule,
    inviewModule.name,
  ])
  .directive("wonAtomContentParticipants", genComponentConf).name;
