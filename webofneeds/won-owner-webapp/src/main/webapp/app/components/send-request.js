import angular from "angular";
import "ng-redux";
import postContentModule from "./post-content.js";
import chatTextFieldModule from "./chat-textfield.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";
import { getPostUriFromRoute } from "../selectors/general-selectors.js";
import { connect2Redux } from "../won-utils.js";
import { attach, getIn } from "../utils.js";
import * as needUtils from "../need-utils.js";
import { actionCreators } from "../actions/actions.js";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];

function genComponentConf() {
  let template = `
        <won-post-content post-uri="self.postUriToConnectTo"></won-post-content>
        <div class="post-info__footer" ng-if="!self.postLoading">
            <chat-textfield
                ng-if="self.showRequestField"
                placeholder="::'Message (optional)'"
                on-submit="::self.sendAdHocRequest(value, selectedPersona)"
                allow-empty-submit="::true"
                show-personas="true"
                submit-button-label="::'Ask&#160;to&#160;Chat'">
            </chat-textfield>
            <div
                class="post-info__footer__infolabel"
                ng-if="self.isInactive">
                Need is inactive, no requests allowed
            </div>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      const selectFromState = state => {
        const postUriToConnectTo = getPostUriFromRoute(state);
        const displayedPost = state.getIn(["needs", postUriToConnectTo]);

        return {
          displayedPost,
          postUriToConnectTo,
          isInactive: needUtils.isInactive(displayedPost),
          showRequestField: needUtils.isActive(displayedPost) && (needUtils.hasChatFacet(displayedPost) || needUtils.hasGroupFacet(displayedPost)),
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

    sendAdHocRequest(message, persona) {
      this.router__stateGoResetParams("connections");

      if (this.postUriToConnectTo) {
        this.connections__connectAdHoc(
          this.postUriToConnectTo,
          message,
          persona
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
  .module("won.owner.components.sendRequest", [
    chatTextFieldModule,
    postContentModule,
  ])
  .directive("wonSendRequest", genComponentConf).name;
