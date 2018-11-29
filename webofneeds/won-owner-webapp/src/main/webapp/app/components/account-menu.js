/**
 * Created by ksinger on 01.09.2017.
 */
import angular from "angular";
import { attach } from "../utils.js";
import { ellipsizeString } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../won-utils.js";

import dropdownModule from "./covering-dropdown.js";
import loginFormModule from "./login-form.js";
import loggedInMenuModule from "./logged-in-menu.js";

import * as srefUtils from "../sref-utils.js";
import { isPrivateUser } from "../selectors/general-selectors.js";

import "style/_login.scss";

function genLogoutConf() {
  let template = `
        <won-dropdown class="dd-right-aligned-header">
            <won-dd-header class="topnav__button">


                <span class="topnav__button__caption hide-in-responsive">
                    {{self.loggedIn? self.getEmail() : "Sign In"}}
                </span>

                <svg class="topnav__carret" style="--local-primary:var(--won-primary-color);">
                    <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
                </svg>

                <svg class="topnav__button__icon" style="--local-primary:var(--won-primary-color);" ng-if="!self.isPrivateIdUser">
                    <use xlink:href="#ico36_person" href="#ico36_person"></use>
                </svg>
                <svg class="topnav__button__icon" style="--local-primary:var(--won-primary-color);" ng-if="self.isPrivateIdUser">
                    <use xlink:href="#ico36_person_anon" href="#ico36_person_anon"></use>
                </svg>

            </won-dd-header>
            <won-dd-menu>


                <won-logged-in-menu
                    class="am__menu--loggedin"
                    ng-show="self.loggedIn">
                </won-logged-in-menu>


                <won-login-form
                    class="am__menu--loggedout"
                    ng-show="!self.loggedIn">
                </won-login-form>


            </won-dd-menu>
        </won-dropdown>
    `;

  const serviceDependencies = [
    "$state",
    "$ngRedux",
    "$scope" /*'$routeParams' /*injections as strings here*/,
  ];

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);
      Object.assign(this, srefUtils);

      window.lopu4dbg = this;

      this.email = "";
      this.password = "";
      this.maxEmailLength = 16;

      const logout = state => ({
        loggedIn: state.getIn(["account", "loggedIn"]),
        email: state.getIn(["account", "email"]),
        isPrivateIdUser: isPrivateUser(state),
      });

      connect2Redux(logout, actionCreators, [], this);
    }

    getEmail() {
      if (this.isPrivateIdUser) {
        return "Anonymous";
      } else {
        return ellipsizeString(this.email, this.maxEmailLength);
      }
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: { open: "=" },
    template: template,
  };
}

export default angular
  .module("won.owner.components.accountMenu", [
    dropdownModule,
    loginFormModule,
    loggedInMenuModule,
  ])
  .directive("wonAccountMenu", genLogoutConf).name;
