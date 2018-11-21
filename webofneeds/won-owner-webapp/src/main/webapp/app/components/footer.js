/**
 * Created by quasarchimaere on 19.11.2018.
 */
import won from "../won-es6.js";
import angular from "angular";
import Immutable from "immutable";
import { attach, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../won-utils.js";

import * as srefUtils from "../sref-utils.js";

import "style/_footer.scss";

function genTopnavConf() {
  let template = `
      <div class="footer">
        <!-- TODO: find or create logos that are stylable -->
        <!--<img src="skin/{{self.themeName}}/images/logo.svg" class="footer__logo">
        <div class="footer__appTitle">
            {{ self.appTitle }}
        </div>
        <div class="footer__tagLine">Web Of Needs</div>-->
        <div class="footer__links">
            <a class="footer__links__link" href="{{ self.absHRef(self.$state, 'about', {'aboutSection': undefined}) }}">About</a>
            <span class="footer__links__divider">|</span>
            <a class="footer__links__link" href="{{ self.absHRef(self.$state, 'about', {'aboutSection': 'aboutPrivacyPolicy'}) }}">Privacy</a>
            <span class="footer__links__divider">|</span>
            <a class="footer__links__link" href="{{ self.absHRef(self.$state, 'about', {'aboutSection': 'aboutFaq'}) }}">FAQ</a>
            <span class="footer__links__divider">|</span>
            <a class="footer__links__link" href="{{ self.absHRef(self.$state, 'about', {'aboutSection': 'aboutTermsOfService'}) }}">Terms Of Service</a>
            <span class="footer__links__divider">|</span>
            <span class="footer__links__link" ng-click="self.toggleDebugMode()">{{ self.getDebugModeLabel() }}</span>
            <span class="footer__links__divider">|</span>
            <span class="footer__links__link" ng-click="self.toggleRdfDisplay()">{{self.shouldShowRdf? "Hide raw RDF data" : "Show raw RDF data"}}</span>
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
      Object.assign(this, srefUtils); // bind srefUtils to scope

      window.footer4dbg = this;

      const selectFromState = state => {
        return {
          themeName: getIn(state, ["config", "theme", "name"]),
          appTitle: getIn(state, ["config", "theme", "title"]),
          shouldShowRdf: state.get("showRdf"),
        };
      };

      connect2Redux(selectFromState, actionCreators, [], this);
    }

    toggleDebugMode() {
      won.debugmode = !won.debugmode;
      const text = won.debugmode ? "Debugmode On" : "Debugmode Off";
      this.toasts__push(Immutable.fromJS({ text }));
    }

    getDebugModeLabel() {
      return won.debugmode ? "Turn Off Debugmode" : "Turn On Debugmode";
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
  .module("won.owner.components.footer", [])
  .directive("wonFooter", genTopnavConf).name;
