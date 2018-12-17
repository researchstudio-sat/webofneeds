import angular from "angular";
import "ng-redux";
import feedbackGridModule from "./feedback-grid.js";
import postContentModule from "./post-content.js";
import chatTextFieldSimpleModule from "./chat-textfield-simple.js";
import connectionContextDropdownModule from "./connection-context-dropdown.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";
import {
  getPostUriFromRoute,
  getOwnedNeedByConnectionUri,
} from "../selectors/general-selectors.js";
import { connect2Redux } from "../won-utils.js";
import { attach, getIn } from "../utils.js";
import { isWhatsAroundNeed, isWhatsNewNeed } from "../need-utils.js";
import { actionCreators } from "../actions/actions.js";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];

function genComponentConf() {
  let template = `
        <won-post-content post-uri="self.postUriToConnectTo"></won-post-content>
        <div class="post-info__footer" ng-if="!self.postLoading">
            <won-feedback-grid ng-if="self.connection && !self.connection.get('isRated')" connection-uri="self.connectionUri"></won-feedback-grid>

            <chat-textfield-simple
                placeholder="::'Message (optional)'"
                on-submit="::self.sendRequest(value, selectedPersona)"
                allow-empty-submit="::true"
                show-personas="true"
                submit-button-label="::'Ask&#160;to&#160;Chat'"
                ng-if="!self.connection || self.connection.get('isRated')"
            >
            </chat-textfield-simple>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      const selectFromState = state => {
        const connectionUri = decodeURIComponent(
          getIn(state, ["router", "currentParams", "connectionUri"])
        );
        const ownedNeed =
          connectionUri && getOwnedNeedByConnectionUri(state, connectionUri);
        const connection =
          ownedNeed && ownedNeed.getIn(["connections", connectionUri]);
        const postUriToConnectTo = !connection
          ? getPostUriFromRoute(state)
          : connection && connection.get("remoteNeedUri");

        const displayedPost = state.getIn(["needs", postUriToConnectTo]);

        return {
          connection,
          connectionUri,
          ownedNeed,
          displayedPost,
          postUriToConnectTo,
          postLoading:
            !displayedPost ||
            getIn(state, [
              "process",
              "needs",
              displayedPost.get("uri"),
              "loading",
            ]),
        };
      };
      connect2Redux(selectFromState, actionCreators, [], this);

      classOnComponentRoot("won-is-loading", () => this.postLoading, this);
    }

    sendRequest(message, persona) {
      const isOwnedNeedWhatsX =
        this.ownedNeed &&
        (isWhatsAroundNeed(this.ownedNeed) || isWhatsNewNeed(this.ownedNeed));

      if (!this.connection || isOwnedNeedWhatsX) {
        this.router__stateGoResetParams("connections");

        if (isOwnedNeedWhatsX) {
          //Close the connection if there was a present connection for a whatsaround need
          this.connections__close(this.connectionUri);
        }

        if (this.postUriToConnectTo) {
          this.connections__connectAdHoc(
            this.postUriToConnectTo,
            message,
            persona
          );
        }

        //this.router__stateGoCurrent({connectionUri: null, sendAdHocRequest: null});
      } else {
        this.needs__connect(
          this.ownedNeed.get("uri"),
          this.connectionUri,
          this.ownedNeed
            .getIn(["connections", this.connectionUri])
            .get("remoteNeedUri"),
          message
        );
        this.router__stateGoCurrent({ connectionUri: this.connectionUri });
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
  .module("won.owner.components.sendRequest", [
    feedbackGridModule,
    chatTextFieldSimpleModule,
    connectionContextDropdownModule,
    postContentModule,
  ])
  .directive("wonSendRequest", genComponentConf).name;
