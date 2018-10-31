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
import { actionCreators } from "../actions/actions.js";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];

function genComponentConf() {
  let template = `
        <won-post-content post-uri="self.postUriToConnectTo"></won-post-content>
        <div class="post-info__footer" ng-if="!self.isLoading()">
            <won-feedback-grid ng-if="self.connection && !self.connection.get('isRated')" connection-uri="self.connectionUri"></won-feedback-grid>

            <chat-textfield-simple
                placeholder="::'Message (optional)'"
                on-submit="::self.sendRequest(value)"
                allow-empty-submit="::true"
                show-personas="::true"
                submit-button-label="::'Ask to Chat'"
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
        const ownNeed =
          connectionUri && getOwnedNeedByConnectionUri(state, connectionUri);
        const connection =
          ownNeed && ownNeed.getIn(["connections", connectionUri]);
        const postUriToConnectTo = !connection
          ? getPostUriFromRoute(state)
          : connection && connection.get("remoteNeedUri");

        const displayedPost = state.getIn(["needs", postUriToConnectTo]);

        return {
          connection,
          connectionUri,
          ownNeed,
          displayedPost,
          postUriToConnectTo,
        };
      };
      connect2Redux(selectFromState, actionCreators, [], this);

      classOnComponentRoot("won-is-loading", () => this.isLoading(), this);
    }

    isLoading() {
      return !this.displayedPost || this.displayedPost.get("isLoading");
    }

    sendRequest(message) {
      const isOwnNeedWhatsX =
        this.ownNeed &&
        (this.ownNeed.get("isWhatsAround") || this.ownNeed.get("isWhatsNew"));

      if (!this.connection || isOwnNeedWhatsX) {
        this.router__stateGoResetParams("connections");

        if (isOwnNeedWhatsX) {
          //Close the connection if there was a present connection for a whatsaround need
          this.connections__close(this.connectionUri);
        }

        if (this.postUriToConnectTo) {
          this.connections__connectAdHoc(this.postUriToConnectTo, message);
        }

        //this.router__stateGoCurrent({connectionUri: null, sendAdHocRequest: null});
      } else {
        this.needs__connect(
          this.ownNeed.get("uri"),
          this.connectionUri,
          this.ownNeed
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
