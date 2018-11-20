/**
 * Created by ksinger on 20.08.2015.
 */
import won from "../won-es6.js";
import angular from "angular";
import ngAnimate from "angular-animate";
import dropdownModule from "./covering-dropdown.js";
import accountMenuModule from "./account-menu.js";
import modalDialogModule from "./modal-dialog.js";
import { attach, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../won-utils.js";
import "angular-marked";

import * as srefUtils from "../sref-utils.js";

import "style/_responsiveness-utils.scss";
import "style/_slidein.scss";
import "style/_topnav.scss";

function genTopnavConf() {
  let template = `
        <div class="slide-in" ng-class="{'visible': self.connectionHasBeenLost}">
            <svg class="si__icon" style="--local-primary:white;">
                <use xlink:href="#ico16_indicator_warning" href="#ico16_indicator_warning"></use>
            </svg>
            <span class="si__text">
                Lost connection &ndash; make sure your internet-connection
                is working, then click &ldquo;reconnect&rdquo;.
            </span>
            <button
                ng-show="self.connectionHasBeenLost && !self.reconnecting"
                ng-click="self.reconnect__start()"
                class="si__button">
                    Reconnect
            </button>

            <svg class="hspinner" ng-show="self.reconnecting">
                <use xlink:href="#ico_loading_anim" href="#ico_loading_anim"></use>
            </svg>
        </div>
        <div class="slide-in" ng-class="{'visible': !self.acceptedDisclaimer}">
            <svg class="si__icon" style="--local-primary:white;">
                <use xlink:href="#ico16_indicator_warning" href="#ico16_indicator_info"></use>
            </svg>
            <div class="si__smalltext">
                This is the demonstrator of an ongoing research project. Please keep in mind:
                <ul>
                	<li> Your posts are public. </li>
					        <li> Your user account is not publicly linked to your posts.</li>
                	<li> The connections of your posts are public.</li>
                	<li> The messages you exchange with others are private, but stored in clear text on our servers. </li>
                </ul>     
                <a target="_blank"
                   href="{{ self.absHRef(self.$state, 'about', {'aboutSection': 'aboutPrivacyPolicy'}) }}">
                   See Privacy Policy.
                </a>
                <br />
                We use cookies to track your session using a self-hosted analytics tool.
                <a target="_blank"
                   href="/piwik/index.php?module=CoreAdminHome&action=optOut&language=en">
                   Suppress tracking.
                </a>
	  		    </div>
            <button
                ng-click="self.acceptDisclaimer()"
                class="si__smallbutton">
                    Ok, I'll keep that in mind
            </button>
        </div>
        <won-modal-dialog ng-if="self.showModalDialog"></won-modal-dialog>

        <nav class="topnav">
            <div class="topnav__inner">
                <div class="topnav__inner__left">
                    <a href="{{ self.defaultRouteHRef(self.$state) }}"
                        class="topnav__button">
                            <img src="skin/{{self.themeName}}/images/logo.svg"
                                class="topnav__button__icon">
                            <span class="topnav__page-title topnav__button__caption hide-in-responsive">
                                {{ self.appTitle }}
                            </span>
                    </a>
                </div>
                <div class="topnav__inner__center"></div>
                <div class="topnav__inner__right">
                    <ul class="topnav__list">
                        <li ng-show="!self.isSignUpView && (self.isPrivateIdUser || !self.loggedIn)">
                            <a  ui-sref="{{ self.absSRef('signup') }}"
                                class="topnav__signupbtn">
                                    Sign up
                            </a>
                        </li>
                        <li>
                            <won-account-menu>
                            </won-account-menu>
                        </li>
                    </ul>
                </div>
            </div>
        </nav>

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
                    <div marked="toast.get('msg')"></div>
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
      Object.assign(this, srefUtils); // bind srefUtils to scope

      window.tnc4dbg = this;

      const selectFromState = state => {
        const currentRoute = getIn(state, ["router", "currentState", "name"]);
        const privateId = getIn(state, [
          "router",
          "currentParams",
          "privateId",
        ]);

        return {
          themeName: getIn(state, ["config", "theme", "name"]),
          appTitle: getIn(state, ["config", "theme", "title"]),
          adminEmail: getIn(state, ["config", "theme", "adminEmail"]),
          WON: won.WON,
          loggedIn: state.getIn(["user", "loggedIn"]),
          acceptedDisclaimer: state.getIn(["user", "acceptedDisclaimer"]),
          email: state.getIn(["user", "email"]),
          isPrivateIdUser: !!privateId,
          toastsArray: state.getIn(["toasts"]).toArray(),
          connectionHasBeenLost: state.getIn(["messages", "lostConnection"]), // name chosen to avoid name-clash with the action-creator
          reconnecting: state.getIn(["messages", "reconnecting"]),
          showModalDialog: state.get("showModalDialog"),
          isSignUpView: currentRoute === "signup",
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
  .module("won.owner.components.topnav", [
    "hc.marked",
    //loginComponent,
    //logoutComponent,
    dropdownModule,
    accountMenuModule,
    modalDialogModule,
    ngAnimate,
  ])
  .directive("wonTopnav", genTopnavConf).name;
