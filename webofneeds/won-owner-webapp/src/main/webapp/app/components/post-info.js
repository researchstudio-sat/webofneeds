/**
 * Created by ksinger on 10.05.2016.
 */

import angular from "angular";
import WonAtomHeaderBig from "./atom-header-big.jsx";
import WonAtomContent from "./atom-content.jsx";
import WonAtomMenu from "./atom-menu.jsx";
import ChatTextfield from "./chat-textfield.jsx";
import { get, getIn } from "../utils.js";
import { connect2Redux } from "../configRedux.js";
import * as viewUtils from "../redux/utils/view-utils.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import { actionCreators } from "../actions/actions.js";
import { attach, classOnComponentRoot } from "../cstm-ng-utils.js";

import { getUseCaseIcon, getUseCaseLabel } from "../usecase-utils.js";

import "~/style/_post-info.scss";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as processSelectors from "../redux/selectors/process-selectors.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import Immutable from "immutable";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
        <won-preact class="atomHeaderBig" component="self.WonAtomHeaderBig" props="{atomUri: self.atomUri}"></won-preact>
        <won-preact class="atomMenu" component="self.WonAtomMenu" props="{atomUri: self.atomUri}"></won-preact>
        <won-preact class="atomContent" component="self.WonAtomContent" props="{atomUri: self.atomUri}"></won-preact>
        <div class="post-info__footer" ng-if="self.showFooter">
            <!-- AdHoc Request Field -->
            <won-preact
                class="chatTextfield"
                component="self.ChatTextfield"
                props="{
                    placeholder: 'Message (optional)',
                    allowEmptySubmit: true,
                    showPersonas: true,
                    submitButtonLabel: 'Ask&#160;to&#160;Chat',
                    onSubmit: self.sendAdHocRequest                    
                }"
                ng-if="self.showAdHocRequestField"
            ></won-preact>
            <!-- TODO refactor the old ng reference to the react one on-submit="::self.sendAdHocRequest(value, selectedPersona)" -->
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
      window.pi4dbg = this;
      this.WonAtomHeaderBig = WonAtomHeaderBig;
      this.WonAtomMenu = WonAtomMenu;
      this.WonAtomContent = WonAtomContent;
      this.ChatTextfield = ChatTextfield;

      const selectFromState = state => {
        const atom = getIn(state, ["atoms", this.atomUri]);

        const isOwned = generalSelectors.isAtomOwned(state, this.atomUri);

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
        const visibleTab = viewUtils.getVisibleTabByAtomUri(
          viewState,
          this.atomUri
        );

        const atomLoading =
          !atom || processSelectors.isAtomLoading(state, this.atomUri);

        const holderUri = atomUtils.getHeldByUri(atom);

        return {
          loggedIn: accountUtils.isLoggedIn(get(state, "account")),
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
          addHolderUri: showEnabledUseCases && holderUri,
          holderUri,
        };
      };
      connect2Redux(selectFromState, actionCreators, ["self.atomUri"], this);

      classOnComponentRoot("won-is-loading", () => this.atomLoading, this);
    }

    selectUseCase(ucIdentifier) {
      this.router__stateGo("create", {
        useCase: ucIdentifier,
        useCaseGroup: undefined,
        connectionUri: undefined,
        fromAtomUri: this.atomUri,
        viewConnUri: undefined,
        mode: "CONNECT",
        holderUri: this.addHolderUri ? this.holderUri : undefined,
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
    template: template,
    scope: {
      atomUri: "=",
    },
  };
}

export default angular
  .module("won.owner.components.postInfo", [])
  .directive("wonPostInfo", genComponentConf).name;
