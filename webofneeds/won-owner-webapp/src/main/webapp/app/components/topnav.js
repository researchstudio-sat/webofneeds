/**
 * Created by ksinger on 20.08.2015.
 */
import angular from "angular";
import ngAnimate from "angular-animate";
import dropdownModule from "./covering-dropdown.js";
import accountMenuModule from "./account-menu.js";
import { attach, get, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../won-utils.js";
import { isLoading } from "../selectors/process-selectors.js";
import * as viewSelectors from "../selectors/view-selectors.js";

import * as accountUtils from "../redux/utils/account-utils.js";

import "~/style/_responsiveness-utils.scss";
import "~/style/_topnav.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$state", "$element"];

function genTopnavConf() {
  let template = `
        <nav class="topnav">
            <img src="skin/{{self.themeName}}/images/logo.svg" class="topnav__logo clickable" ng-click="self.router__stateGoDefault()">
            <div class="topnav__title">
              <span class="topnav__app-title hide-in-responsive" ng-click="self.router__stateGoDefault()">
                  {{ self.appTitle }}
              </span>
              <span class="topnav__divider hide-in-responsive" ng-if="self.pageTitle">
                  &mdash;
              </span>
              <span class="topnav__page-title" ng-if="self.pageTitle">
                  {{ self.pageTitle }}
              </span>
            </div>
            <div class="topnav__slideintoggle"
                ng-if="self.showSlideInIndicator"
                ng-click="self.view__toggleSlideIns()">
                <svg class="topnav__slideintoggle__icon">
                    <use xlink:href="#ico16_indicator_warning" href="#ico16_indicator_warning"></use>
                </svg>
                <svg class="topnav__slideintoggle__carret">
                    <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
                </svg>
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

      const selectFromState = state => {
        const currentRoute = getIn(state, ["router", "currentState", "name"]);
        const accountState = get(state, "account");

        return {
          themeName: getIn(state, ["config", "theme", "name"]),
          appTitle: getIn(state, ["config", "theme", "title"]),
          loggedIn: accountUtils.isLoggedIn(accountState),
          isAnonymous: accountUtils.isAnonymous(accountState),
          isSignUpView: currentRoute === "signup",
          showLoadingIndicator: isLoading(state),
          showSlideInIndicator:
            viewSelectors.hasSlideIns(state) &&
            !viewSelectors.showSlideIns(state),
        };
      };

      connect2Redux(selectFromState, actionCreators, ["self.pageTitle"], this);
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
