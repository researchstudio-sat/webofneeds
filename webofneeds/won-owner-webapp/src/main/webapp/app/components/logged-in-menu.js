/**
 * Created by ksinger on 20.08.2015.
 */
import angular from "angular";
import { attach, get } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../won-utils.js";

import * as accountUtils from "../account-utils.js";

function genComponentConf() {
  let template = `
    <span class="dd__userlabel show-in-responsive" ng-if="self.loggedIn" title="{{ self.getEmail() }}">{{ self.getEmail() }}</span>
    <hr class="show-in-responsive"/>
    <a ng-if="self.isAnonymous"
        class="won-button--outlined thin red show-in-responsive"
        ng-click="self.view__hideMainMenu() && self.router__stateGo('signup')">
        Sign up
    </a>
    <a class="won-button--outlined thin red"
        ng-click="self.view__hideMainMenu() && self.router__stateGo('settings')">
        <span>Account Settings</span>
    </a>
    <hr/>
    <button
        class="won-button--filled lighterblue"
        style="width:100%"
        ng-click="::self.account__logout()">
        <span>Sign out</span>
    </button>
    `;

  const serviceDependencies = [
    "$ngRedux",
    "$scope",
    "$state" /*'$routeParams' /*injections as strings here*/,
  ];

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);

      window.lopu4dbg = this;

      this.email = "";
      this.password = "";

      const logout = state => {
        const accountState = get(state, "account");

        return {
          loggedIn: accountUtils.isLoggedIn(accountState),
          email: accountUtils.getEmail(accountState),
          isAnonymous: accountUtils.isAnonymous(accountState),
        };
      };

      connect2Redux(logout, actionCreators, [], this);
    }

    getEmail() {
      if (this.isAnonymous) {
        return "Anonymous";
      } else {
        return this.email;
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
  .module("won.owner.components.logout", [])
  .directive("wonLoggedInMenu", genComponentConf).name;
