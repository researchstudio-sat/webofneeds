/**
 * Created by ksinger on 01.09.2017.
 */
import angular from "angular";
import { attach, delay } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import {
  connect2Redux,
  getLoginErrorMessage,
  resendEmailVerification,
} from "../won-utils.js";
import won from "../won-es6.js";

import * as srefUtils from "../sref-utils.js";

function genLoginConf() {
  let template = `
        <form ng-submit="::self.account__login({email: self.email, password: self.password, rememberMe: self.rememberMe}, {redirectToFeed: false})"
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
            <span class="wl__errormsg" ng-if="self.loginError">
                {{self.getLoginErrorMessage(self.loginError)}}
                <a class="wl__errormsg__resend"
                 ng-if="!self.clickedResend && self.isNotVerified"
                 ng-click="self.resendEmailVerification()">(Click to Resend Verification Email)</a>
            </span>
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
            <a href="{{ self.absHRef(self.$state, 'signup') }}" ng-click="self.hideMainMenuDisplay()">
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
      Object.assign(this, srefUtils); // bind srefUtils to scope
      this.getLoginErrorMessage = getLoginErrorMessage;
      window.lic4dbg = this;

      this.email = "";
      this.password = "";
      this.rememberMe = false;
      this.clickedResend = false;

      const login = state => ({
        loggedIn: state.getIn(["account", "loggedIn"]),
        loginError: state.getIn(["account", "loginError"]),
        isNotVerified:
          state.getIn(["account", "loginError", "code"]) ==
          won.RESPONSECODE.USER_NOT_VERIFIED,
      });

      connect2Redux(login, actionCreators, [], this);
    }

    formKeyUp(event) {
      this.typedAtLoginCredentials();
      if (event.keyCode == 13) {
        this.account__login(
          {
            email: this.email,
            password: this.password,
            rememberMe: this.rememberMe,
          },
          {
            redirectToFeed: false,
          }
        );
      }
    }

    resendEmailVerification() {
      this.clickedResend = true;
      resendEmailVerification(this.email); //TODO: Implement error cases and success response

      delay(2000).then(() => {
        this.clickedResend = false;
      });
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
  .module("won.owner.components.loginForm", [])
  .directive("wonLoginForm", genLoginConf).name;
