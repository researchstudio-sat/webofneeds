/** @jsx h */

/**
 * Created by ksinger on 21.08.2017.
 */
import angular from "angular";
import ngAnimate from "angular-animate";
import { attach, get } from "../utils.js";
import { actionCreators } from "../actions/actions.js";

import signupTitleBarModule from "../components/signup-title-bar.js";
import labelledHrModule from "../components/labelled-hr.js";

import * as srefUtils from "../sref-utils.js";
import * as accountUtils from "../account-utils.js";
import * as viewSelectors from "../selectors/view-selectors.js";
import { h } from "preact";

import "style/_signup.scss";

const template = (
  <container>
    <won-modal-dialog ng-if="self.showModalDialog" />
    <header>
      <won-topnav />
    </header>
    <won-toasts />
    <won-slide-in ng-if="self.showSlideIns" />
    <main className="signup" id="signupSection">
      <won-signup-title-bar />
      <div className="signup__content">
        <div className="signup__content__form" ng-form name="registerForm">
          <input
            id="registerEmail"
            name="email"
            placeholder="Email address"
            ng-model="self.email"
            ng-class="{'ng-invalid': self.registerError}"
            required
            type="email"
            ng-keyup="self.formKeyup($event)"
          />

          <div
            className="signup__content__form__errormsg"
            ng-if="registerForm.email.$error.email"
          >
            <svg className="signup__content__form__errormsg__icon">
              <use
                xlinkHref="#ico16_indicator_warning"
                href="#ico16_indicator_warning"
              />
            </svg>
            Not a valid E-Mail address
          </div>
          <div
            className="signup__content__form__errormsg"
            ng-if="self.registerError"
          >
            <svg className="signup__content__form__errormsg__icon">
              <use
                xlinkHref="#ico16_indicator_warning"
                href="#ico16_indicator_warning"
              />
            </svg>
            {"{{self.registerError}}"}
          </div>

          <input
            name="password"
            placeholder="Password"
            ng-minlength="6"
            ng-model="self.password"
            required
            type="password"
            ng-keyup="self.formKeyup($event)"
          />

          <div
            className="signup__content__form__errormsg"
            ng-if="registerForm.password.$error.minlength"
          >
            <svg className="signup__content__form__errormsg__icon">
              <use
                xlinkHref="#ico16_indicator_warning"
                href="#ico16_indicator_warning"
              />
            </svg>
            Password too short, must be at least 6 Characters
          </div>

          <input
            name="password_repeat"
            placeholder="Repeat Password"
            ng-minlength="6"
            ng-model="self.passwordAgain"
            required
            type="password"
            compare-to="self.password"
            ng-keyup="self.formKeyup($event)"
          />

          <div
            className="signup__content__form__errormsg"
            ng-if="registerForm.password_repeat.$error.compareTo"
          >
            <svg className="signup__content__form__errormsg__icon">
              <use
                xlinkHref="#ico16_indicator_warning"
                href="#ico16_indicator_warning"
              />
            </svg>
            Password is not equal
          </div>

          <div>
            <input id="rememberMe" ng-model="self.rememberMe" type="checkbox" />
            <label htmlFor="rememberMe">remember me</label>
          </div>
          <div>
            <input
              id="acceptToS"
              ng-model="self.acceptToS"
              type="checkbox"
              required
            />
            <label htmlFor="acceptToS">
              I accept the{" "}
              <a href="{{ self.absHRef(self.$state, 'about', {'aboutSection': 'aboutTermsOfService'}) }}">
                Terms Of Service
              </a>
            </label>
          </div>
        </div>
        <button
          className="won-button--filled red"
          ng-if="self.isAnonymous"
          ng-disabled="registerForm.$invalid"
          ng-click="::self.account__transfer({email: self.email, password: self.password, privateId: self.privateId, rememberMe: self.rememberMe})"
        >
          <span>Keep Postings</span>
        </button>
        <won-labelled-hr label="or" ng-if="self.isAnonymous" />
        <button
          className="won-button--filled red"
          ng-disabled="registerForm.$invalid"
          ng-click="::self.account__register({email: self.email, password: self.password, rememberMe: self.rememberMe})"
        >
          <span ng-if="!self.isAnonymous">That’s all we need. Let’s go!</span>
          <span ng-if="self.isAnonymous">Start from Scratch</span>
        </button>
      </div>
    </main>
    <won-footer />
  </container>
);

const serviceDependencies = [
  "$ngRedux",
  "$scope",
  "$state" /*'$routeParams' /*injections as strings here*/,
];

class SignupController {
  constructor(/* arguments <- serviceDependencies */) {
    attach(this, serviceDependencies, arguments);
    this.rememberMe = false;
    this.acceptToS = false;
    Object.assign(this, srefUtils); // bind srefUtils to scope

    const select = state => {
      const accountState = get(state, "account");
      return {
        loggedIn: accountUtils.isLoggedIn(accountState),
        registerError: accountUtils.getRegisterError(accountState),
        isAnonymous: accountUtils.isAnonymous(accountState),
        privateId: accountUtils.getPrivateId(accountState),
        showModalDialog: state.getIn(["view", "showModalDialog"]),
        showSlideIns:
          viewSelectors.hasSlideIns(state) && viewSelectors.showSlideIns(state),
      };
    };
    const disconnect = this.$ngRedux.connect(select, actionCreators)(this);
    this.$scope.$on("$destroy", disconnect);
  }

  formKeyup(event) {
    if (this.registerError) {
      this.view__clearRegisterError();
    }
    if (event.keyCode == 13 && this.$scope.registerForm.$valid) {
      if (this.isAnonymous) {
        this.account__transfer({
          email: this.email,
          password: this.password,
          privateId: this.privateId,
          rememberMe: this.rememberMe,
        });
      } else {
        this.account__register({
          email: this.email,
          password: this.password,
          rememberMe: this.rememberMe,
        });
      }
    }
  }
}

export default {
  module: angular
    .module("won.owner.components.signup", [
      signupTitleBarModule,
      labelledHrModule,
      ngAnimate,
    ])
    .controller("SignupController", [...serviceDependencies, SignupController])
    .name,
  controller: "SignupController",
  template: template,
};
