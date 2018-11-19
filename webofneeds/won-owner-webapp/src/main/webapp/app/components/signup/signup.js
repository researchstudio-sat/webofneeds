/**
 * Created by ksinger on 21.08.2017.
 */
import angular from "angular";
import { attach, getIn } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";

import signupTitleBarModule from "../signup-title-bar.js";
import labelledHrModule from "../labelled-hr.js";

import topNavModule from "../topnav.js";

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

      const themeName = getIn(state, ["config", "theme", "name"]);

      return {
        //focusSignup: state.getIn(['router', 'currentParams', 'focusSignup']) === "true",
        themeName,
        appTitle: getIn(state, ["config", "theme", "title"]),
        loggedIn: state.getIn(["user", "loggedIn"]),
        registerError: state.getIn(["user", "registerError"]),
        isPrivateIdUser: !!privateId,
        privateId,
      };
    };
    const disconnect = this.$ngRedux.connect(select, actionCreators)(this);
    this.$scope.$on("$destroy", disconnect);
  }

  formKeyup(event) {
    this.registerReset();
    if (event.keyCode == 13 && this.passwordAgain === this.password) {
      if (this.isPrivateIdUser) {
        this.transfer({
          email: this.email,
          password: this.password,
          privateId: this.privateId,
          rememberMe: this.rememberMe,
        });
      } else {
        this.register({
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
    //overviewTitleBarModule,
    //accordionModule,
    signupTitleBarModule,
    topNavModule,
    labelledHrModule,
    //flexGridModule,
    //compareToModule,
  ])
  .controller("SignupController", [...serviceDependencies, SignupController])
  .name;
