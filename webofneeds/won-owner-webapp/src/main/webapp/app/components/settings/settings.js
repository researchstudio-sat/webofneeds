/**
 * Created by ksinger on 21.08.2017.
 */
import angular from "angular";
import ngAnimate from "angular-animate";
import { attach, getIn } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import settingsWrapper from "./settings-wrapper.js";

import * as srefUtils from "../../sref-utils.js";

import "style/_signup.scss";

const serviceDependencies = [
  "$ngRedux",
  "$scope",
  "$state" /*'$routeParams' /*injections as strings here*/,
];

class SettingsController {
  constructor(/* arguments <- serviceDependencies */) {
    attach(this, serviceDependencies, arguments);
    this.rememberMe = false;
    Object.assign(this, srefUtils); // bind srefUtils to scope

    const select = state => {
      return {
        appTitle: getIn(state, ["config", "theme", "title"]),
        showModalDialog: state.getIn(["view", "showModalDialog"]),
      };
    };
    const disconnect = this.$ngRedux.connect(select, actionCreators)(this);
    this.$scope.$on("$destroy", disconnect);
  }
}

export default angular
  .module("won.owner.components.settings", [settingsWrapper, ngAnimate])
  .controller("SettingsController", [
    ...serviceDependencies,
    SettingsController,
  ]).name;
