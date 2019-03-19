import angular from "angular";
import Immutable from "immutable";
import "ng-redux";
import postContentModule from "./post-content.js";
import postMenuModule from "./post-menu.js";
import chatTextFieldModule from "./chat-textfield.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";
import { getPostUriFromRoute } from "../selectors/general-selectors.js";
import { connect2Redux } from "../won-utils.js";
import { attach, get, getIn } from "../utils.js";
import * as needUtils from "../need-utils.js";
import { actionCreators } from "../actions/actions.js";
import { getUseCaseLabel, getUseCaseIcon } from "../usecase-utils.js";
import * as accountUtils from "../account-utils.js";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];

function genComponentConf() {
  let template = `
        <won-post-menu post-uri="self.postUriToConnectTo"></won-post-menu>
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
            <!-- Reaction Use Cases -->
            <won-labelled-hr label="::'Or'" class="pm__footer__labelledhr"  ng-if="self.hasReactionUseCases"></won-labelled-hr>
            <button class="won-button--filled red post-info__footer__button" style="margin: 0rem 0rem .3rem 0rem;
                    ng-if="self.hasReactionUseCases"
                    ng-repeat="ucIdentifier in self.reactionUseCasesArray"
                    ng-click="self.selectUseCase(ucIdentifier)">
                    <svg class="won-button-icon" style="--local-primary:white;" ng-if="self.getUseCaseIcon(ucIdentifier)">
                        <use xlink:href="{{ self.getUseCaseIcon(ucIdentifier) }}" href="{{ self.getUseCaseIcon(ucIdentifier) }}"></use>
                    </svg>
                    <span>{{ self.getUseCaseLabel(ucIdentifier) }}</span>
            </button>
            <!-- Enabled Use Cases -->
            <won-labelled-hr label="::'Or'" class="pm__footer__labelledhr"  ng-if="self.hasEnabledUseCases"></won-labelled-hr>
            <button class="won-button--filled red post-info__footer__button" style="margin: 0rem 0rem .3rem 0rem;"
                    ng-if="self.hasEnabledUseCases"
                    ng-repeat="ucIdentifier in self.enabledUseCasesArray"
                    ng-click="self.selectUseCase(ucIdentifier)">
                    <svg class="won-button-icon" style="--local-primary:white;" ng-if="self.getUseCaseIcon(ucIdentifier)">
                        <use xlink:href="{{ self.getUseCaseIcon(ucIdentifier) }}" href="{{ self.getUseCaseIcon(ucIdentifier) }}"></use>
                    </svg>
                    <span>{{ self.getUseCaseLabel(ucIdentifier) }}</span>
            </button>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      const selectFromState = state => {
        const postUriToConnectTo = getPostUriFromRoute(state);
        const displayedPost = state.getIn(["needs", postUriToConnectTo]);

        const post = state.getIn(["needs", postUriToConnectTo]);
        const reactionUseCases =
          post &&
          !needUtils.isOwned(post) &&
          getIn(post, ["matchedUseCase", "reactionUseCases"]);
        const hasReactionUseCases =
          reactionUseCases && reactionUseCases.size > 0;

        const enabledUseCases =
          post &&
          needUtils.isOwned(post) &&
          getIn(post, ["matchedUseCase", "enabledUseCases"]);
        const hasEnabledUseCases = enabledUseCases && enabledUseCases.size > 0;

        return {
          loggedIn: accountUtils.isLoggedIn(get(state, "account")),
          displayedPost,
          postUriToConnectTo,
          isInactive: needUtils.isInactive(displayedPost),
          hasReactionUseCases,
          reactionUseCasesArray: reactionUseCases && reactionUseCases.toArray(),
          hasEnabledUseCases,
          enabledUseCasesArray: enabledUseCases && enabledUseCases.toArray(),
          showRequestField:
            needUtils.isActive(displayedPost) &&
            (needUtils.hasChatFacet(displayedPost) ||
              needUtils.hasGroupFacet(displayedPost)),
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

    selectUseCase(ucIdentifier) {
      this.router__stateGo("connections", {
        useCase: ucIdentifier,
        useCaseGroup: undefined,
        postUri: undefined,
        fromNeedUri: this.postUriToConnectTo,
        viewNeedUri: undefined,
        viewConnUri: undefined,
        mode: "CONNECT",
      });
    }

    sendAdHocRequest(message, persona) {
      const tempPostUriToConnectTo = this.postUriToConnectTo;

      if (this.loggedIn) {
        this.router__stateGoResetParams("connections");

        if (tempPostUriToConnectTo) {
          this.connections__connectAdHoc(
            tempPostUriToConnectTo,
            message,
            persona
          );
        }
      } else {
        this.view__showTermsDialog(
          Immutable.fromJS({
            acceptCallback: () => {
              this.view__hideModalDialog();
              this.router__stateGoResetParams("connections");

              if (tempPostUriToConnectTo) {
                this.connections__connectAdHoc(
                  tempPostUriToConnectTo,
                  message,
                  persona
                );
              }
            },
            cancelCallback: () => {
              this.view__hideModalDialog();
            },
          })
        );
      }
    }

    getUseCaseIcon(ucIdentifier) {
      return getUseCaseIcon(ucIdentifier);
    }

    getUseCaseLabel(ucIdentifier) {
      return getUseCaseLabel(ucIdentifier);
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
    postMenuModule,
    postContentModule,
  ])
  .directive("wonSendRequest", genComponentConf).name;
