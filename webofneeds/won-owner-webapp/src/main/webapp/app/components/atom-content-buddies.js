/**
 * Created by quasarchimaere on 25.03.2019.
 */

import angular from "angular";
import inviewModule from "angular-inview";
import labelledHrModule from "./labelled-hr.js";
import suggestPostPickerModule from "./details/picker/suggestpost-picker.js";
import { get, getIn } from "../utils.js";
import { attach } from "../cstm-ng-utils.js";
import won from "../won-es6.js";
import { connect2Redux } from "../configRedux.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as connectionSelectors from "../redux/selectors/connection-selectors.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import { actionCreators } from "../actions/actions.js";
import preactModule from "./preact-module.js";
import WonAtomCard from "./atom-card.jsx";
import ngAnimate from "angular-animate";

import "~/style/_atom-content-buddies.scss";

const CONNECTION_READ_TIMEOUT = 1500;

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
      <div
          class="acb__buddy"
          ng-if="!self.isOwned && self.hasBuddies"
          ng-repeat="memberUri in self.buddiesArray track by memberUri">
          <won-preact class="clickable" component="self.WonAtomCard" props="{ atomUri: memberUri, currentLocation: self.currentLocation, showSuggestions: false, showPersona: false }"></won-preact>
          <div class="acb__buddy__actions"></div>
      </div>
      <div class="acb__buddy"
          ng-if="self.isOwned && self.hasBuddyConnections && conn.get('state') !== self.won.WON.Closed"
          ng-repeat="conn in self.buddyConnectionsArray"
          in-view="conn.get('unread') && $inview && self.markAsRead(conn)"
          ng-class="{'won-unread': conn.get('unread')}">
          <won-preact class="clickable" component="self.WonAtomCard" props="{ atomUri: conn.get('targetAtomUri'), currentLocation: self.currentLocation, showSuggestions: false, showPersona: false }"></won-preact>
          <div class="acb__buddy__actions">
              <div
                class="acb__buddy__actions__button red won-button--filled"
                ng-click="self.openRequest(conn)"
                ng-if="conn.get('state') === self.won.WON.RequestReceived">
                  Accept
              </div>
              <div
                class="acb__buddy__actions__button red won-button--outlined thin"
                ng-click="self.rejectConnection(conn)"
                ng-if="conn.get('state') === self.won.WON.RequestReceived">
                  Reject
              </div>
              <div
                class="acb__buddy__actions__button red won-button--filled"
                ng-click="self.requestBuddy(conn)"
                ng-if="conn.get('state') === self.won.WON.Suggested">
                  Request
              </div>
              <div
                class="acb__buddy__actions__button red won-button--outlined thin"
                ng-disabled="true"
                ng-if="conn.get('state') === self.won.WON.RequestSent">
                  Waiting for Accept...
              </div>
              <div
                class="acb__buddy__actions__button red won-button--outlined thin"
                ng-click="self.closeConnection(conn)"
                ng-if="conn.get('state') === self.won.WON.Suggested || conn.get('state') === self.won.WON.Connected">
                  Remove
              </div>
          </div>
      </div>
      <div class="acb__empty"
          ng-if="!self.buddies && !self.hasBuddyConnections">
          No Buddies present.
      </div>
      <won-labelled-hr label="::'Request'" class="acb__labelledhr" ng-if="self.isOwned" arrow="self.suggestAtomExpanded? 'up' : 'down'" ng-click="self.suggestAtomExpanded = !self.suggestAtomExpanded"></won-labelled-hr>
      <won-suggestpost-picker
          ng-if="self.isOwned && self.suggestAtomExpanded"
          initial-value="undefined"
          on-update="self.requestBuddy(value)"
          detail="::{placeholder: 'Insert AtomUri to invite'}"
          excluded-uris="self.excludedFromRequestUris"
          allowed-sockets="::[self.won.BUDDY.BuddySocketCompacted]"
          excluded-text="::'Requesting yourself or someone who is already your Buddy is not allowed'"
          not-allowed-socket-text="::'Request does not work on atoms without the Buddy Socket'"
          no-suggestions-text="::'No known Personas available'"
      ></won-suggestpost-picker>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.won = won;
      this.suggestAtomExpanded = false;
      window.atomContentBuddies4dbg = this;
      this.WonAtomCard = WonAtomCard;

      const selectFromState = state => {
        const atom = getIn(state, ["atoms", this.atomUri]);
        const isOwned = generalSelectors.isAtomOwned(state, this.atomUri);

        const hasBuddySocket = atomUtils.hasBuddySocket(atom);

        const buddies = hasBuddySocket && get(atom, "buddies");

        const buddyConnections =
          isOwned &&
          hasBuddySocket &&
          connectionSelectors.getBuddyConnectionsByAtomUri(
            state,
            this.atomUri,
            true,
            true
          );

        let excludedFromRequestUris = [this.atomUri];

        if (buddyConnections) {
          buddyConnections.map(conn =>
            excludedFromRequestUris.push(get(conn, "targetAtomUri"))
          );
        }

        return {
          atom,
          isOwned,
          hasBuddySocket,
          hasBuddies: buddies && buddies.size > 0,
          hasBuddyConnections: buddyConnections && buddyConnections.size > 0,
          buddyConnectionsArray: buddyConnections && buddyConnections.toArray(),
          excludedFromRequestUris,
          buddiesArray: buddies && buddies.toArray(),
        };
      };
      connect2Redux(selectFromState, actionCreators, ["self.atomUri"], this);
    }

    closeConnection(conn) {
      if (!conn) {
        return;
      }

      const payload = {
        caption: "Persona",
        text: "Remove Buddy?",
        buttons: [
          {
            caption: "Yes",
            callback: () => {
              const connUri = conn.get("uri");

              if (conn.get("unread")) {
                this.connections__markAsRead({
                  connectionUri: connUri,
                  atomUri: this.atomUri,
                });
              }

              this.connections__close(connUri);
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

    rejectConnection(conn) {
      if (!conn) {
        return;
      }

      const payload = {
        caption: "Persona",
        text: "Reject Buddy Request?",
        buttons: [
          {
            caption: "Yes",
            callback: () => {
              const connUri = conn.get("uri");

              if (conn.get("unread")) {
                this.connections__markAsRead({
                  connectionUri: connUri,
                  atomUri: this.atomUri,
                });
              }

              this.connections__close(connUri);
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

      const connUri = get(conn, "uri");
      const targetAtomUri = get(conn, "targetAtomUri");

      if (conn.get("unread")) {
        this.connections__markAsRead({
          connectionUri: connUri,
          atomUri: this.atomUri,
        });
      }
      this.atoms__connect(this.atomUri, connUri, targetAtomUri, message);
    }

    requestBuddy(targetAtomUri, message = "") {
      if (!this.isOwned || !this.hasBuddySocket) {
        console.warn("Trying to request a non-owned or non buddySocket atom");
        return;
      }

      const payload = {
        caption: "Persona",
        text: "Send Buddy Request?",
        buttons: [
          {
            caption: "Yes",
            callback: () => {
              this.atoms__connect(
                this.atomUri,
                undefined,
                targetAtomUri,
                message,
                won.BUDDY.BuddySocketCompacted,
                won.BUDDY.BuddySocketCompacted
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
  .module("won.owner.components.atomContentBuddies", [
    ngAnimate,
    labelledHrModule,
    preactModule,
    suggestPostPickerModule,
    inviewModule.name,
  ])
  .directive("wonAtomContentBuddies", genComponentConf).name;
