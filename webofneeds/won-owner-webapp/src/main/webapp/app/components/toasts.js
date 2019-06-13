/**
 * Created by quasarchimaere on 20.11.2018.
 */
import won from "../won-es6.js";
import angular from "angular";
import ngAnimate from "angular-animate";
import { attach, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../won-utils.js";
import "angular-marked";

import "~/style/_responsiveness-utils.scss";
import "~/style/_toasts.scss";
import "~/style/_won-markdown.scss";

function genToastConf() {
  let template = `
        <div class="topnav__toasts">
            <div class="topnav__toasts__element" 
            ng-class="{ 'info' : toast.get('type') === self.WON.infoToast,
                        'warn' : toast.get('type') === self.WON.warnToast,
                        'error' : toast.get('type') === self.WON.errorToast
                      }"
            ng-repeat="toast in self.toastsArray">

                <svg class="topnav__toasts__element__icon"
                    ng-show="toast.get('type') === self.WON.infoToast"
                    style="--local-primary:#CCD2D2">
                        <use xlink:href="#ico16_indicator_info" href="#ico16_indicator_info"></use>
                </svg>

                <svg class="topnav__toasts__element__icon"
                    ng-show="toast.get('type') === self.WON.warnToast"
                    style="--local-primary:#CCD2D2">
                        <use xlink:href="#ico16_indicator_warning" href="#ico16_indicator_warning"></use>
                </svg>

                <svg class="topnav__toasts__element__icon"
                    ng-show="toast.get('type') === self.WON.errorToast"
                    style="--local-primary:#CCD2D2">
                        <use xlink:href="#ico16_indicator_error" href="#ico16_indicator_error"></use>
                </svg>

                <div class="topnav__toasts__element__text">
                    <div class="markdown" marked="toast.get('msg')"></div>
                    <p ng-show="toast.get('type') === self.WON.errorToast">
                        If the problem persists please contact
                        <a href="mailto:{{self.adminEmail}}">
                            {{self.adminEmail}}
                        </a>
                    </p>
                </div>

                <svg class="topnav__toasts__element__close clickable"
                    ng-click="self.toasts__delete(toast)"
                    style="--local-primary:var(--won-primary-color);">
                        <use xlink:href="#ico27_close" href="#ico27_close"></use>
                </svg>

            </div>
        </div>
    `;

  const serviceDependencies = [
    "$ngRedux",
    "$scope",
    "$state" /*injections as strings here*/,
  ];

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);

      const selectFromState = state => {
        return {
          adminEmail: getIn(state, ["config", "theme", "adminEmail"]),
          WON: won.WON,
          toastsArray: state.getIn(["toasts"]).toArray(),
        };
      };

      connect2Redux(selectFromState, actionCreators, [], this);
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    scope: {}, //isolate scope to allow usage within other controllers/components
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    template: template,
  };
}

export default angular
  .module("won.owner.components.toasts", ["hc.marked", ngAnimate])
  .directive("wonToasts", genToastConf).name;
