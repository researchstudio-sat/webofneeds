/**
 * Created by quasarchimaere on 25.03.2019.
 */

import angular from "angular";
import inviewModule from "angular-inview";
import atomCardModule from "./atom-card.js";
import { attach } from "../cstm-ng-utils.js";
import { connect2Redux } from "../configRedux.js";
import { actionCreators } from "../actions/actions.js";
import ngAnimate from "angular-animate";

import "~/style/_atom-content-suggestions.scss";
import * as connectionSelectors from "../redux/selectors/connection-selectors";
import won from "../won-es6";
import { get } from "../utils.js";

const CONNECTION_READ_TIMEOUT = 1500;

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
      <div 
        class="acs__atom"
        ng-repeat="suggestion in self.suggestionsArray"
        ng-if="self.hasSuggestions"
        in-view="suggestion.get('unread') && $inview && self.markAsRead(suggestion)"
        ng-class="{'won-unread': suggestion.get('unread')}">
          <won-atom-card
              class="clickable"
              atom-uri="::suggestion.get('targetAtomUri')"
              current-location="self.currentLocation"
              ng-click="self.viewSuggestion(suggestion)"
              show-suggestions="::false"
              show-persona="::true"
              disable-default-atom-interaction="::true"
          ></won-atom-card>
          <div class="acs__atom__actions">
              <div
                  class="acs__atom__actions__button red won-button--filled"
                  ng-click="self.sendRequest(suggestion)">
                  Request
              </div>
              <div
                  class="acs__atom__actions__button red won-button--outlined thin"
                  ng-click="self.closeConnection(suggestion)">
                  Remove
              </div>
          </div>
      </div>
      <div class="acs__empty"
          ng-if="!self.hasSuggestions">
          No Suggestions for this Atom.
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.atomContentSuggestions4dbg = this;

      const selectFromState = state => {
        const suggestions = connectionSelectors.getSuggestedConnectionsByAtomUri(
          state,
          this.atomUri
        );

        return {
          hasSuggestions: suggestions && suggestions.size > 0,
          suggestionsArray: suggestions && suggestions.toArray(),
        };
      };
      connect2Redux(selectFromState, actionCreators, ["self.atomUri"], this);
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

    viewSuggestion(conn) {
      if (!conn) {
        return;
      }

      const connUri = conn.get("uri");

      if (conn.get("unread")) {
        this.connections__markAsRead({
          connectionUri: connUri,
          atomUri: this.atomUri,
        });
      }

      this.router__stateGoCurrent({
        viewConnUri: connUri,
      });
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

      this.connections__rate(connUri, won.WONCON.binaryRatingGood);
      this.atoms__connect(this.atomUri, connUri, targetAtomUri, message);
      this.router__stateGo("connections", {
        connectionUri: connUri,
        viewConnUri: undefined,
      });
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
  .module("won.owner.components.atomContentSuggestions", [
    ngAnimate,
    atomCardModule,
    inviewModule.name,
  ])
  .directive("wonAtomContentSuggestions", genComponentConf).name;
