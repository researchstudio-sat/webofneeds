/**
 * Created by quasarchimaere on 25.03.2019.
 */

import angular from "angular";
import inviewModule from "angular-inview";
import labelledHrModule from "./labelled-hr.js";
import postHeaderModule from "./post-header.js";
import suggestPostPickerModule from "./details/picker/suggestpost-picker.js";
import { attach, getIn, get } from "../utils.js";
import won from "../won-es6.js";
import { connect2Redux } from "../configRedux.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as connectionSelectors from "../redux/selectors/connection-selectors.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import { actionCreators } from "../actions/actions.js";
import ngAnimate from "angular-animate";

import "~/style/_atom-content-buddies.scss";

const CONNECTION_READ_TIMEOUT = 1500;

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
      <div
          class="ac-buddies__buddy"
          ng-if="!self.isOwned && self.hasBuddies"
          ng-repeat="memberUri in self.buddiesArray track by memberUri">
          <div class="ac-buddies__buddy__indicator"></div>
          <won-post-header
            class="clickable"
            ng-click="self.router__stateGoCurrent({viewAtomUri: memberUri, viewConnUri: undefined})"
            atom-uri="::memberUri">
          </won-post-header>
          <div class="ac-buddies__buddy__actions"></div>
      </div>
      <div class="ac-buddies__buddy"
          ng-if="self.isOwned && self.hasBuddyConnections && conn.get('state') !== self.won.WON.Closed"
          ng-repeat="conn in self.buddyConnectionsArray"
          in-view="conn.get('unread') && $inview && self.markAsRead(conn)"
          ng-class="{'won-unread': conn.get('unread')}">
          <div class="ac-buddies__buddy__indicator"></div>
          <won-post-header
            class="clickable"
            ng-click="self.router__stateGoCurrent({viewAtomUri: conn.get('targetAtomUri'), viewConnUri: undefined})"
            atom-uri="::conn.get('targetAtomUri')">
          </won-post-header>
          <div class="ac-buddies__buddy__actions">
              <div
                class="ac-buddies__buddy__actions__button red won-button--outlined thin"
                ng-click="self.openRequest(conn)"
                ng-if="conn.get('state') === self.won.WON.RequestReceived">
                  Accept
              </div>
              <div
                class="ac-buddies__buddy__actions__button red won-button--outlined thin"
                ng-click="self.closeConnection(conn)"
                ng-if="conn.get('state') === self.won.WON.RequestReceived">
                  Reject
              </div>
              <div
                class="ac-buddies__buddy__actions__button red won-button--outlined thin"
                ng-click="self.sendRequest(conn)"
                ng-if="conn.get('state') === self.won.WON.Suggested">
                  Request
              </div>
              <div
                class="ac-buddies__buddy__actions__button red won-button--outlined thin"
                ng-disabled="true"
                ng-if="conn.get('state') === self.won.WON.RequestSent">
                  Waiting for Accept...
              </div>
              <div
                class="ac-buddies__buddy__actions__button red won-button--outlined thin"
                ng-click="self.closeConnection(conn)"
                ng-if="conn.get('state') === self.won.WON.Suggested || conn.get('state') === self.won.WON.Connected">
                  Remove
              </div>
          </div>
      </div>
      <div class="ac-buddies__empty"
          ng-if="(!self.isOwned && !self.buddies) || (self.isOwned && !self.hasBuddyConnections)">
          No Buddies present.
      </div>
      <won-labelled-hr label="::'Request'" class="ac-buddies__labelledhr" ng-if="self.isOwned"></won-labelled-hr>
      <won-suggestpost-picker
          ng-if="self.isOwned"
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
      window.atomContentBuddies4dbg = this;

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
      this.atoms__connect(
        this.atomUri,
        undefined,
        targetAtomUri,
        message,
        won.BUDDY.BuddySocketCompacted,
        won.BUDDY.BuddySocketCompacted
      );
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
    postHeaderModule,
    suggestPostPickerModule,
    inviewModule.name,
  ])
  .directive("wonAtomContentBuddies", genComponentConf).name;
