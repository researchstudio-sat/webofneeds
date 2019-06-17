/** @jsx h */

/**
 * Created by ksinger on 21.08.2017.
 */
import angular from "angular";
import ngAnimate from "angular-animate";
import { getIn } from "../utils.js";
import { attach } from "../cstm-ng-utils.js";
import { actionCreators } from "../actions/actions.js";
import settingsWrapper from "../components/settings-wrapper.js";
import * as viewSelectors from "../redux/selectors/view-selectors.js";
import { h } from "preact";

import "~/style/_signup.scss";

const template = (
  <container>
    <won-modal-dialog ng-if="self.showModalDialog" />
    <won-topnav page-title="::'Settings'" />
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
];

class SettingsController {
  constructor(/* arguments <- serviceDependencies */) {
    attach(this, serviceDependencies, arguments);
    this.rememberMe = false;

    const select = state => {
      return {
        appTitle: getIn(state, ["config", "theme", "title"]),
        showModalDialog: state.getIn(["view", "showModalDialog"]),
        showSlideIns:
          viewSelectors.hasSlideIns(state) && viewSelectors.showSlideIns(state),
      };
    };
    const disconnect = this.$ngRedux.connect(select, actionCreators)(this);
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
