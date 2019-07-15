/**
 * Created by ksinger on 20.08.2015.
 */
import angular from "angular";
import ngAnimate from "angular-animate";
import dropdownModule from "./covering-dropdown.js";
import accountMenuModule from "./account-menu.js";
import { get, getIn } from "../utils.js";
import { attach } from "../cstm-ng-utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../configRedux.js";
import { isLoading } from "../redux/selectors/process-selectors.js";
import * as connectionSelectors from "../redux/selectors/connection-selectors";
import * as accountUtils from "../redux/utils/account-utils.js";
import { delay } from "../utils.js";
import * as viewSelectors from "../redux/selectors/view-selectors.js";

import "~/style/_responsiveness-utils.scss";
import "~/style/_topnav.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$state", "$element"];

function genTopnavConf() {
  let template = `
        <nav class="topnav">
            <img src="skin/{{self.themeName}}/images/logo.svg" class="topnav__logo clickable hide-in-responsive" ng-click="self.router__stateGoDefault()">
            <img src="skin/{{self.themeName}}/images/logo.svg" class="topnav__logo clickable show-in-responsive" ng-click="self.view__toggleMenu()">
            <div class="topnav__title">
              <span class="topnav__app-title hide-in-responsive" ng-click="self.router__stateGoDefault()">
                  {{ self.appTitle }}
              </span>
              <span class="topnav__divider hide-in-responsive" ng-if="!self.showMenu && self.pageTitle">
                  &mdash;
              </span>
              <span class="topnav__page-title" ng-if="!self.showMenu && self.pageTitle">
                  {{ self.pageTitle }}
              </span>
              <span class="topnav__page-title" ng-if="self.showMenu">
                  Menu
              </span>
            </div>
            <div class="topnav__loading" ng-if="self.showLoadingIndicator">
                <svg class="topnav__loading__spinner hspinner">
                    <use xlink:href="#ico_loading_anim" href="#ico_loading_anim"></use>
                </svg>
            </div>
            <button ng-click="self.router__stateGo('signup')" class="topnav__signupbtn won-button--filled red" ng-if="!self.isSignUpView && (self.isAnonymous || !self.loggedIn)">
                Sign up
            </button>
            <won-account-menu>
            </won-account-menu>
        </nav>
    `;

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);

      window.tnc4dbg = this;
      window.tnc4dbg.connectionSelectors = connectionSelectors;

      const selectFromState = state => {
        const currentRoute = getIn(state, ["router", "currentState", "name"]);
        const accountState = get(state, "account");

        return {
          showMenu: viewSelectors.showMenu(state),
          themeName: getIn(state, ["config", "theme", "name"]),
          appTitle: getIn(state, ["config", "theme", "title"]),
          loggedIn: accountUtils.isLoggedIn(accountState),
          isAnonymous: accountUtils.isAnonymous(accountState),
          isSignUpView: currentRoute === "signup",
          showLoadingIndicator: isLoading(state),
          connectionsToCrawl: connectionSelectors.getChatConnectionsToCrawl(
            state
          ),
        };
      };

      connect2Redux(selectFromState, actionCreators, ["self.pageTitle"], this);

      this.$scope.$watch("self.connectionsToCrawl", connectionsToCrawl =>
        this.ensureUnreadMessagesAreLoaded(connectionsToCrawl)
      );
    }

    ensureUnreadMessagesAreLoaded(connectionsToCrawl) {
      delay(0).then(() => {
        const MESSAGECOUNT = 10;
        console.debug(
          "connectionsToCrawl: ",
          connectionsToCrawl,
          " Size: ",
          connectionsToCrawl.size
        );
        connectionsToCrawl.map(conn => {
          const messages = conn.get("messages");
          const messageCount = messages ? messages.size : 0;

          if (messageCount == 0) {
            this.connections__showLatestMessages(conn.get("uri"), MESSAGECOUNT);
          } else {
            const receivedMessages = messages.filter(
              msg => !msg.get("outgoingMessage")
            );
            const receivedMessagesReadPresent = receivedMessages.find(
              msg => !msg.get("unread")
            );

            if (!receivedMessagesReadPresent) {
              this.connections__showMoreMessages(conn.get("uri"), MESSAGECOUNT);
            }
          }
        });
      });
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    scope: {
      pageTitle: "=",
    }, //isolate scope to allow usage within other controllers/components
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    template: template,
  };
}

export default angular
  .module("won.owner.components.topnav", [
    dropdownModule,
    accountMenuModule,
    ngAnimate,
  ])
  .directive("wonTopnav", genTopnavConf).name;
