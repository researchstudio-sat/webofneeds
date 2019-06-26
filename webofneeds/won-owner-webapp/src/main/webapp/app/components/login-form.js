/**
 * Created by ksinger on 01.09.2017.
 */
import angular from "angular";
import { get, getIn } from "../utils.js";
import { attach } from "../cstm-ng-utils.js";
import { actionCreators } from "../actions/actions.js";
import { parseRestErrorMessage } from "../won-utils.js";
import { connect2Redux } from "../configRedux.js";
import won from "../won-es6.js";
import "angular-marked";
import "~/style/_won-markdown.scss";

import * as accountUtils from "../redux/utils/account-utils.js";

function genLoginConf() {
  let template = `
        <form ng-submit="::self.account__login({email: self.email, password: self.password, rememberMe: self.rememberMe})"
            id="loginForm"
            class="loginForm"
        >
            <input
                id="loginEmail"
                placeholder="Email address"
                ng-model="self.email"
                type="email"
                required
                autofocus
                ng-keyup="self.formKeyUp($event)"/>
            <span class="wl__errormsg markdown" ng-if="self.loginError" marked="self.parseRestErrorMessage(self.loginError)">
            </span>
            <a class="wl__errormsg__resend"
                 ng-if="self.isNotVerified && !self.processingResendVerificationEmail"
                 ng-click="self.account__resendVerificationEmail(self.email)">(Click to Resend Verification Email)</a>
            <a class="wl__errormsg__resend"
                 ng-if="self.isNotVerified && self.processingResendVerificationEmail">(Resending...)</a>
            <input
                id="loginPassword"
                placeholder="Password"
                ng-model="self.password"
                type="password"
                required
                ng-keyup="self.formKeyUp($event)"/>

            <button
                class="won-button--filled lighterblue"
                ng-disabled="loginForm.$invalid">
                    Sign In
            </button>
            <input
                id="remember-me"
                ng-model="self.rememberMe"
                type="checkbox"/> Remember me
        </form>
        <div class="wl__register">
            No account yet?
            <a class="clickable" ng-click="self.view__hideMainMenu() && self.router__stateGo('signup')">
                Sign up
            </a>
        </div>`;

  const serviceDependencies = [
    "$ngRedux",
    "$scope",
    "$element",
    "$state" /*'$routeParams' /*injections as strings here*/,
  ];

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);
      this.parseRestErrorMessage = parseRestErrorMessage;
      window.lic4dbg = this;

      this.email = "";
      this.password = "";
      this.rememberMe = false;

      const login = state => {
        const accountState = get(state, "account");
        const loginError = accountUtils.getLoginError(accountState);
        const isNotVerified =
          get(loginError, "code") === won.RESPONSECODE.USER_NOT_VERIFIED;

        return {
          loggedIn: accountUtils.isLoggedIn(accountState),
          loginError,
          processingResendVerificationEmail: getIn(state, [
            "process",
            "processingResendVerificationEmail",
          ]),
          isNotVerified,
        };
      };

      connect2Redux(login, actionCreators, [], this);
    }

    formKeyUp(event) {
      if (this.loginError) {
        this.view__clearLoginError();
      }
      if (event.keyCode == 13) {
        this.account__login({
          email: this.email,
          password: this.password,
          rememberMe: this.rememberMe,
        });
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
  .module("won.owner.components.loginForm", ["hc.marked"])
  .directive("wonLoginForm", genLoginConf).name;
