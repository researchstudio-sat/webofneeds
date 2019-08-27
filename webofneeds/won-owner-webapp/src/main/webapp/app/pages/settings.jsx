/** @jsx h */

/**
 * Created by ksinger on 21.08.2017.
 */
import angular from "angular";
import ngAnimate from "angular-animate";
import { get, getIn } from "../utils.js";
import { attach, classOnComponentRoot } from "../cstm-ng-utils.js";
import { actionCreators } from "../actions/actions.js";
import settingsWrapper from "../components/settings-wrapper.js";
import * as viewSelectors from "../redux/selectors/view-selectors.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import WonModalDialog from "../components/modal-dialog.jsx";

import { h } from "preact";

import "~/style/_signup.scss";

const template = (
  <container>
    <won-preact
      component="self.WonModalDialog"
      props="{}"
      ng-if="self.showModalDialog"
    />
    <won-topnav page-title="::'Settings'" />
    <won-menu ng-if="self.isLoggedIn" />
    <won-toasts />
    <won-slide-in ng-if="self.showSlideIns" />
    <main className="settings">
      <won-settings-wrapper />
    </main>
    <won-footer />
  </container>
);

const serviceDependencies = [
  "$ngRedux",
  "$scope",
  "$state" /*'$routeParams' /*injections as strings here*/,
  "$element",
];

class SettingsController {
  constructor(/* arguments <- serviceDependencies */) {
    attach(this, serviceDependencies, arguments);
    this.rememberMe = false;
    this.WonModalDialog = WonModalDialog;

    const select = state => {
      const accountState = get(state, "account");

      return {
        isLoggedIn: accountUtils.isLoggedIn(accountState),
        appTitle: getIn(state, ["config", "theme", "title"]),
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
}

export default {
  module: angular
    .module("won.owner.components.settings", [settingsWrapper, ngAnimate])
    .controller("SettingsController", [
      ...serviceDependencies,
      SettingsController,
    ]).name,
  controller: "SettingsController",
  template: template,
};
