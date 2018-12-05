/**
 * Created by ksinger on 21.08.2017.
 */
import angular from "angular";
import ngAnimate from "angular-animate";
import { attach, getIn } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";

import signupTitleBarModule from "../signup-title-bar.js";
import labelledHrModule from "../labelled-hr.js";

import * as srefUtils from "../../sref-utils.js";

import "style/_signup.scss";

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
      const privateId = getIn(state, ["router", "currentParams", "privateId"]);

      return {
        loggedIn: state.getIn(["account", "loggedIn"]),
        registerError: state.getIn(["account", "registerError"]),
        isPrivateIdUser: !!privateId,
        privateId,
        showModalDialog: state.getIn(["view", "showModalDialog"]),
      };
    };
    const disconnect = this.$ngRedux.connect(select, actionCreators)(this);
    this.$scope.$on("$destroy", disconnect);
  }

  formKeyup(event) {
    this.view__clearRegisterError();
    if (event.keyCode == 13 && this.$scope.registerForm.$valid) {
      if (this.isPrivateIdUser) {
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

export default angular
  .module("won.owner.components.signup", [
    signupTitleBarModule,
    labelledHrModule,
    ngAnimate,
  ])
  .controller("SignupController", [...serviceDependencies, SignupController])
  .name;
