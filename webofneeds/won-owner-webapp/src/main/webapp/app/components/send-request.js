import angular from "angular";
import Immutable from "immutable";
import "ng-redux";
import postContentModule from "./post-content.js";
import postMenuModule from "./post-menu.js";
import chatTextFieldModule from "./chat-textfield.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as processSelectors from "../redux/selectors/process-selectors.js";
import { connect2Redux } from "../configRedux.js";
import { attach, get, getIn } from "../utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import { actionCreators } from "../actions/actions.js";
import { getUseCaseLabel, getUseCaseIcon } from "../usecase-utils.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import * as viewUtils from "../redux/utils/view-utils.js";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];

function genComponentConf() {
  let template = `
        <won-post-menu post-uri="self.atomUri"></won-post-menu>
        <won-post-content post-uri="self.atomUri"></won-post-content>
        <div class="post-info__footer" ng-if="self.showFooter">
            <!-- AdHoc Request Field -->
            <chat-textfield
                ng-if="self.showAdHocRequestField"
                placeholder="::'Message (optional)'"
                on-submit="::self.sendAdHocRequest(value, selectedPersona)"
                allow-empty-submit="::true"
                show-personas="true"
                submit-button-label="::'Ask&#160;to&#160;Chat'">

            </chat-textfield>
            <!-- Reaction Use Cases -->
            <button class="won-button--filled red post-info__footer__button"
                    ng-if="self.showReactionUseCases"
                    ng-repeat="ucIdentifier in self.reactionUseCasesArray"
                    ng-click="self.selectUseCase(ucIdentifier)">
                    <svg class="won-button-icon" style="--local-primary:white;" ng-if="self.getUseCaseIcon(ucIdentifier)">
                        <use xlink:href="{{ self.getUseCaseIcon(ucIdentifier) }}" href="{{ self.getUseCaseIcon(ucIdentifier) }}"></use>
                    </svg>
                    <span>{{ self.getUseCaseLabel(ucIdentifier) }}</span>
            </button>
            <!-- Enabled Use Cases -->
            <button class="won-button--filled red post-info__footer__button"
                    ng-if="self.showEnabledUseCases"
                    ng-repeat="ucIdentifier in self.enabledUseCasesArray"
                    ng-click="self.selectUseCase(ucIdentifier)">
                    <svg class="won-button-icon" style="--local-primary:white;" ng-if="self.getUseCaseIcon(ucIdentifier)">
                        <use xlink:href="{{ self.getUseCaseIcon(ucIdentifier) }}" href="{{ self.getUseCaseIcon(ucIdentifier) }}"></use>
                    </svg>
                    <span>{{ self.getUseCaseLabel(ucIdentifier) }}</span>
            </button>
            <div
                class="post-info__footer__infolabel"
                ng-if="self.isInactive">
                Atom is inactive, no requests allowed
            </div>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      const selectFromState = state => {
        const atomUri = generalSelectors.getPostUriFromRoute(state);
        const atom = getIn(state, ["atoms", atomUri]);

        const isOwned = generalSelectors.isAtomOwned(state, atomUri);

        const isConnectible = atomUtils.isConnectible(atom);
        const hasReactionUseCases = atomUtils.hasReactionUseCases(atom);
        const hasEnabledUseCases = atomUtils.hasEnabledUseCases(atom);

        const showEnabledUseCases =
          isConnectible && isOwned && hasEnabledUseCases;
        const showReactionUseCases =
          isConnectible && !isOwned && hasReactionUseCases;

        const showAdHocRequestField =
          !isOwned &&
          isConnectible &&
          !showEnabledUseCases &&
          !showReactionUseCases;

        const viewState = get(state, "view");
        const visibleTab = viewUtils.getVisibleTabByAtomUri(viewState, atomUri);

        const atomLoading =
          !atom || processSelectors.isAtomLoading(state, atomUri);
        return {
          loggedIn: accountUtils.isLoggedIn(get(state, "account")),
          atomUri,
          isInactive: atomUtils.isInactive(atom),
          showAdHocRequestField,
          showEnabledUseCases,
          showReactionUseCases,
          reactionUseCasesArray:
            showReactionUseCases &&
            atomUtils.getReactionUseCases(atom).toArray(),
          enabledUseCasesArray:
            showEnabledUseCases && atomUtils.getEnabledUseCases(atom).toArray(),
          atomLoading,
          showFooter:
            !atomLoading &&
            visibleTab === "DETAIL" &&
            (showEnabledUseCases ||
              showReactionUseCases ||
              showAdHocRequestField),
        };
      };
      connect2Redux(selectFromState, actionCreators, [], this);

      classOnComponentRoot("won-is-loading", () => this.atomLoading, this);
    }

    selectUseCase(ucIdentifier) {
      this.router__stateGo("create", {
        useCase: ucIdentifier,
        useCaseGroup: undefined,
        connectionUri: undefined,
        fromAtomUri: this.atomUri,
        viewAtomUri: undefined,
        viewConnUri: undefined,
        mode: "CONNECT",
      });
    }

    sendAdHocRequest(message, persona) {
      const _atomUri = this.atomUri;

      if (this.loggedIn) {
        this.router__stateGoResetParams("connections");

        if (_atomUri) {
          this.connections__connectAdHoc(_atomUri, message, persona);
        }
      } else {
        this.view__showTermsDialog(
          Immutable.fromJS({
            acceptCallback: () => {
              this.view__hideModalDialog();
              this.router__stateGoResetParams("connections");

              if (_atomUri) {
                this.connections__connectAdHoc(_atomUri, message, persona);
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
