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

import * as srefUtils from "../sref-utils.js";
import * as accountUtils from "../account-utils.js";

import "~/style/_responsiveness-utils.scss";
import "~/style/_topnav.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$state", "$element"];

function genTopnavConf() {
  let template = `
        <nav class="topnav">
            <div class="topnav__inner">
                <div class="topnav__inner__left">
                    <a href="{{ self.defaultRouteHRef(self.$state) }}"
                        class="topnav__button">
                            <img src="skin/{{self.themeName}}/images/logo.svg"
                                class="topnav__button__icon">
                            <span class="topnav__app-title topnav__button__caption hide-in-responsive">
                                {{ self.appTitle }}
                            </span>
                            <!--span class="topnav__page-title" ng-if="self.pageTitle">
                                {{ self.pageTitle }}
                            </span-->
                    </a>
                    <div class="topnav__inner__left__slideintoggle"
                        ng-if="self.showSlideInIndicator"
                        ng-click="self.view__toggleSlideIns()">
                        <svg class="topnav__inner__left__slideintoggle__icon">
                            <use xlink:href="#ico16_indicator_warning" href="#ico16_indicator_warning"></use>
                        </svg>
                        <svg class="topnav__inner__left__slideintoggle__carret">
                            <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
                        </svg>
                    </div>
                </div>
                <div class="topnav__inner__center"></div>
                <div class="topnav__inner__right">
                    <ul class="topnav__list">
                        <li class="topnav__list__loading" ng-if="self.showLoadingIndicator">
                          <svg class="topnav__list__loading__spinner hspinner">
                              <use xlink:href="#ico_loading_anim" href="#ico_loading_anim"></use>
                          </svg>
                        </li>
                        <li ng-show="!self.isSignUpView && (self.isAnonymous || !self.loggedIn)">
                            <a  ui-sref="{{ self.absSRef('signup') }}"
                                class="topnav__signupbtn">
                                    Sign up
                            </a>
                        </li>
                        <li>
                            <won-account-menu>
                            </won-account-menu>
                        </li>
                    </ul>
                </div>
            </div>
        </nav>
    `;

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);
      Object.assign(this, srefUtils); // bind srefUtils to scope

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
