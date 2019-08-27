/** @jsx h */

/**
 * Created by ksinger on 21.08.2017.
 */
import angular from "angular";
import ngAnimate from "angular-animate";
import { get } from "../utils.js";
import { attach, classOnComponentRoot } from "../cstm-ng-utils.js";
import { actionCreators } from "../actions/actions.js";
import WonLabelledHr from "../components/labelled-hr.jsx";
import WonModalDialog from "../components/modal-dialog.jsx";
import WonToasts from "../components/toasts.jsx";
import WonMenu from "../components/menu.jsx";
import WonFooter from "../components/footer.jsx";

import * as accountUtils from "../redux/utils/account-utils.js";
import * as viewSelectors from "../redux/selectors/view-selectors.js";
import { h } from "preact";
import "~/style/_signup.scss";

const template = (
  <container>
    <won-preact
      className="modalDialog"
      component="self.WonModalDialog"
      props="{}"
      ng-if="self.showModalDialog"
    />
    <won-topnav page-title="::'Sign Up'" />
    <won-preact
      className="menu"
      component="self.WonMenu"
      props="{}"
      ng-if="self.isLoggedIn"
    />
    <won-preact className="toasts" component="self.WonToasts" props="{}" />
    <won-slide-in ng-if="self.showSlideIns" />
    <main className="signup" id="signupSection">
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
              <a
                className="clickable"
                ng-click="self.router__stateGo('about', {'aboutSection': 'aboutTermsOfService'})"
              >
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
        <won-preact
          className="labelledHr"
          component="self.WonLabelledHr"
          props="{label: 'or'}"
          ng-if="self.isAnonymous"
        />
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
    <won-preact className="footer" component="self.WonFooter" props="{}" />
  </container>
);

const serviceDependencies = [
  "$ngRedux",
  "$scope",
  "$state" /*'$routeParams' /*injections as strings here*/,
  "$element",
];

class SignupController {
  constructor(/* arguments <- serviceDependencies */) {
    attach(this, serviceDependencies, arguments);
    this.rememberMe = false;
    this.acceptToS = false;
    this.WonLabelledHr = WonLabelledHr;
    this.WonModalDialog = WonModalDialog;
    this.WonToasts = WonToasts;
    this.WonMenu = WonMenu;
    this.WonFooter = WonFooter;

    const select = state => {
      const accountState = get(state, "account");
      return {
        isLoggedIn: accountUtils.isLoggedIn(accountState),
        registerError: accountUtils.getRegisterError(accountState),
        isAnonymous: accountUtils.isAnonymous(accountState),
        privateId: accountUtils.getPrivateId(accountState),
        showModalDialog: state.getIn(["view", "showModalDialog"]),
        showSlideIns:
          viewSelectors.hasSlideIns(state) &&
          viewSelectors.isSlideInsVisible(state),
      };
    };
    const disconnect = this.$ngRedux.connect(select, actionCreators)(this);
    classOnComponentRoot("won-signed-out", () => !this.isLoggedIn, this);
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
    .module("won.owner.components.signup", [ngAnimate])
    .controller("SignupController", [...serviceDependencies, SignupController])
    .name,
  controller: "SignupController",
  template: template,
};
