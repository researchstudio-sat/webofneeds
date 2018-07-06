/**
 * Created by ksinger on 20.08.2015.
 */
import won from "../won-es6.js";
import angular from "angular";
import ngAnimate from "angular-animate";
//import loginComponent from './login.js';
//import logoutComponent from './logout.js';
import dropdownModule from "./covering-dropdown.js";
import accountMenuModule from "./account-menu.js";
import modalDialogModule from "./modal-dialog.js";
import { attach, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../won-utils.js";
import { selectNeedByConnectionUri } from "../selectors.js";

import * as srefUtils from "../sref-utils.js";

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
                ng-click="self.reconnect()"
                class="si__button">
                    Reconnect
            </button>

            <img src="images/spinner/on_red.gif"
                alt="Reconnecting&hellip;"
                ng-show="self.reconnecting"
                class="hspinner"/>
        </div>
        <div class="slide-in" ng-class="{'visible': !self.acceptedDisclaimer}">
            <svg class="si__icon" style="--local-primary:white;">
                <use xlink:href="#ico16_indicator_warning" href="#ico16_indicator_info"></use>
            </svg>
            <div class="si__text">
                This is the demonstrator of an ongoing research project. Please keep in mind:
                <ul>
                	<li> Your posts are public. Do not post anything you are not comfortable with everyone seeing. </li>
			<li> Your user account is not publicly linked to your posts, to give you some privacy.</li>
                	<li> The connections of your posts (i.e. the other posts you see, and their status) are public.</li>
                	<li> Your conversations are private, but stored in clear text on our servers. Do not write anything you are not comfortable with writing on a postcard.</li>
                </ul>     
                <a target="_blank"
                   href="{{ self.absHRef(self.$state, 'about', {'#': 'privacyPolicy'}) }}">
                   See Privacy Policy.
                </a>
                <br />
                We track your session with a cookie, and log you in automatically with another one, if you want that. We use an analytics tool (on our own servers) to improve the application, which also identifies you with a cookie.
                <a target="_blank"
                   href="/piwik/index.php?module=CoreAdminHome&action=optOut&language=en">
                   Suppress tracking.
                </a>
	  		</div>
            <button
                ng-click="self.acceptDisclaimer()"
                class="si__button">
                    Ok, I'll keep it in mind
            </button>
        </div>
        <won-modal-dialog ng-if="self.showModalDialog"></won-modal-dialog>

        <nav class="topnav" ng-class="{'hide-in-responsive': !self.isPostView && self.connectionOrPostDetailOpen}">
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
                    <p ng-show="!toast.get('unsafeHtmlEnabled')">
                        {{toast.get('msg')}}
                    </p>
                    <p ng-show="toast.get('unsafeHtmlEnabled')"
                        ng-bind-html="toast.get('msg')">
                    </p>
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
    "$sanitize",
    "$state" /*injections as strings here*/,
  ];

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);
      Object.assign(this, srefUtils); // bind srefUtils to scope

      window.tnc4dbg = this;

      const selectFromState = state => {
        const currentRoute = getIn(state, ["router", "currentState", "name"]);
        const showCreateView = getIn(state, [
          "router",
          "currentParams",
          "showCreateView",
        ]);
        const selectedPostUri = decodeURIComponent(
          getIn(state, ["router", "currentParams", "postUri"])
        );
        const selectedPost =
          selectedPostUri && state.getIn(["needs", selectedPostUri]);
        const selectedConnectionUri = decodeURIComponent(
          getIn(state, ["router", "currentParams", "connectionUri"])
        );
        const privateId = getIn(state, [
          "router",
          "currentParams",
          "privateId",
        ]);
        const need =
          selectedConnectionUri &&
          selectNeedByConnectionUri(state, selectedConnectionUri);
        const selectedConnection =
          need && need.getIn(["connections", selectedConnectionUri]);

        return {
          themeName: getIn(state, ["config", "theme", "name"]),
          appTitle: getIn(state, ["config", "theme", "title"]),
          adminEmail: getIn(state, ["config", "theme", "adminEmail"]),
          WON: won.WON,
          loggedIn: state.getIn(["user", "loggedIn"]),
          acceptedDisclaimer: state.getIn(["user", "acceptedDisclaimer"]),
          email: state.getIn(["user", "email"]),
          isPrivateIdUser: !!privateId,
          connectionOrPostDetailOpen:
            selectedConnection || selectedPost || showCreateView,
          toastsArray: state.getIn(["toasts"]).toArray(),
          connectionHasBeenLost: state.getIn(["messages", "lostConnection"]), // name chosen to avoid name-clash with the action-creator
          reconnecting: state.getIn(["messages", "reconnecting"]),
          showModalDialog: state.get("showModalDialog"),
          isSignUpView: currentRoute === "signup",
          isPostView: currentRoute === "post",
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
    "ngSanitize",
    //loginComponent,
    //logoutComponent,
    dropdownModule,
    accountMenuModule,
    modalDialogModule,
    ngAnimate,
  ])
  .directive("wonTopnav", genTopnavConf).name;
