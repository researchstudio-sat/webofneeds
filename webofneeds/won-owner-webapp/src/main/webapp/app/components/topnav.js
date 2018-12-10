/**
 * Created by ksinger on 20.08.2015.
 */
import angular from "angular";
import ngAnimate from "angular-animate";
import dropdownModule from "./covering-dropdown.js";
import accountMenuModule from "./account-menu.js";
import { attach, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../won-utils.js";

import * as srefUtils from "../sref-utils.js";

import "style/_responsiveness-utils.scss";
import "style/_topnav.scss";

function genTopnavConf() {
  let template = `
        <nav class="topnav">
            <div class="topnav__inner">
                <div class="topnav__inner__left">
                    <a href="{{ self.defaultRouteHRef(self.$state) }}"
                        class="topnav__button">
                            <img src="skin/{{self.themeName}}/images/logo.svg"
                                class="topnav__button__icon">
                            <span class="topnav__page-title topnav__button__caption hide-in-responsive">
                                {{ self.appTitle }}
                            </span>
                    </a>
                </div>
                <div class="topnav__inner__center"></div>
                <div class="topnav__inner__right">
                    <ul class="topnav__list">
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

  const serviceDependencies = [
    "$ngRedux",
    "$scope",
    "$state" /*injections as strings here*/,
  ];

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);
      Object.assign(this, srefUtils); // bind srefUtils to scope

      window.tnc4dbg = this;

      const selectFromState = state => {
        const currentRoute = getIn(state, ["router", "currentState", "name"]);

        return {
          themeName: getIn(state, ["config", "theme", "name"]),
          appTitle: getIn(state, ["config", "theme", "title"]),
          loggedIn: state.getIn(["account", "loggedIn"]),
          isAnonymous: state.getIn(["account", "isAnonymous"]),
          isSignUpView: currentRoute === "signup",
        };
      };

      connect2Redux(selectFromState, actionCreators, [], this);
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    scope: {}, //isolate scope to allow usage within other controllers/components
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
