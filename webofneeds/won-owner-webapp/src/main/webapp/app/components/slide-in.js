/**
 * Created by quasarchimaere on 20.11.2018.
 */
import angular from "angular";
import ngAnimate from "angular-animate";
import dropdownModule from "./covering-dropdown.js";
import { attach, delay, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import {
  connect2Redux,
  resendEmailVerification,
  parseRestErrorMessage,
} from "../won-utils.js";

import * as srefUtils from "../sref-utils.js";

import "style/_slidein.scss";

function genSlideInConf() {
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
        <div class="slide-in" ng-class="{'visible': !self.connectionHasBeenLost && self.verificationToken}">
            <svg class="si__icon" style="--local-primary:white;">
                <use xlink:href="#ico16_indicator_warning" href="#ico16_indicator_warning"></use>
            </svg>
            <span class="si__text" ng-if="self.processingVerifyEmailAddress">
                Verifying the E-Mail address
            </span>
            <span class="si__text" ng-if="!self.processingVerifyEmailAddress && self.emailVerified && !self.emailVerificationError">
                E-Mail Address verified
            </span>
            <span class="si__text" ng-if="!self.processingVerifyEmailAddress && self.emailVerificationError">
                {{ self.parseRestErrorMessage(self.emailVerificationError) }}
            </span>
            <button
              class="si__button"
              ng-disabled="self.clickedResend"
              ng-if="self.loggedIn && !self.processingVerifyEmailAddress && self.emailVerificationError"
              ng-click="self.resendEmailVerification()">
                Resend E-Mail
            </button>
            <svg class="si__close"
                style="--local-primary:white;"
                ng-click="self.router__stateGoCurrent({token: undefined})"
                ng-if="!self.processingVerifyEmailAddress && !self.emailVerificationError">
                <use xlink:href="#ico36_close" href="#ico36_close"></use>
            </svg>
            <svg class="hspinner" ng-if="self.processingVerifyEmailAddress">
                <use xlink:href="#ico_loading_anim" href="#ico_loading_anim"></use>
            </svg>
        </div>
        <div class="slide-in" ng-class="{'visible': self.loggedIn && !self.connectionHasBeenLost && !self.emailVerified}">
            <svg class="si__icon" style="--local-primary:white;">
                <use xlink:href="#ico16_indicator_warning" href="#ico16_indicator_warning"></use>
            </svg>
            <span class="si__text" ng-if="!self.clickedResend">
                E-Mail has not been verified yet, check your Inbox.
            </span>
            <span class="si__text" ng-if="self.clickedResend">
                E-Mail has been resent to {{ self.email }}, check your Inbox.
            </span>
            <button
              class="si__button"
              ng-disabled="self.clickedResend"
              ng-click="self.resendEmailVerification()">
                Resend E-Mail
            </button>
        </div>
        <div class="slide-in" ng-class="{'visible': self.loggedIn && !self.connectionHasBeenLost && !self.acceptedTermsOfService}">
            <svg class="si__icon" style="--local-primary:white;">
                <use xlink:href="#ico16_indicator_warning" href="#ico16_indicator_warning"></use>
            </svg>
            <span class="si__text">
                You have not accepted the
                <a target="_blank"
                   href="{{ self.absHRef(self.$state, 'about', {'aboutSection': 'aboutTermsOfService'}) }}">
                   Terms Of Service
                 </a> yet.
            </span>
            <button
              class="si__button"
              ng-if="!self.processingAcceptTermsOfService"
              ng-click="self.account__acceptTermsOfService()">
                Accept
            </button>
            <svg class="hspinner" ng-if="self.processingAcceptTermsOfService">
                <use xlink:href="#ico_loading_anim" href="#ico_loading_anim"></use>
            </svg>
        </div>
        <div class="slide-in" ng-class="{'visible': !self.connectionHasBeenLost && !self.acceptedDisclaimer}">
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
                ng-click="self.account__acceptDisclaimer()"
                class="si__smallbutton">
                    Ok, I'll keep that in mind
            </button>
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
      this.clickedResend = false;
      this.parseRestErrorMessage = parseRestErrorMessage;

      const selectFromState = state => {
        const verificationToken = getIn(state, [
          "router",
          "currentParams",
          "token",
        ]);

        return {
          verificationToken,
          acceptedDisclaimer: getIn(state, ["account", "acceptedDisclaimer"]),
          emailVerified: getIn(state, ["account", "emailVerified"]),
          emailVerificationError: getIn(state, [
            "account",
            "emailVerificationError",
          ]),
          processingVerifyEmailAddress: getIn(state, [
            "process",
            "processingVerifyEmailAddress",
          ]),
          processingAcceptTermsOfService: getIn(state, [
            "process",
            "processingAcceptTermsOfService",
          ]),
          acceptedTermsOfService: getIn(state, [
            "account",
            "acceptedTermsOfService",
          ]),
          loggedIn: getIn(state, ["account", "loggedIn"]),
          email: getIn(state, ["account", "email"]),
          connectionHasBeenLost: getIn(state, ["messages", "lostConnection"]), // name chosen to avoid name-clash with the action-creator
          reconnecting: getIn(state, ["messages", "reconnecting"]),
        };
      };

      connect2Redux(selectFromState, actionCreators, [], this);

      this.$scope.$watch("self.verificationToken", verificationToken =>
        this.verifyEmailAddress(verificationToken)
      );
    }

    resendEmailVerification() {
      this.clickedResend = true;
      resendEmailVerification(this.email); //TODO: Implement error cases and success response

      delay(2000).then(() => {
        this.clickedResend = false;
      });
    }

    verifyEmailAddress(verificationToken) {
      delay(0).then(() => {
        if (
          verificationToken &&
          !this.processingVerifyEmailAddress &&
          !(this.emailVerified || this.emailVerificationError)
        ) {
          this.account__verifyEmailAddress(verificationToken);
        }
      });
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
  .module("won.owner.components.slideIn", [dropdownModule, ngAnimate])
  .directive("wonSlideIn", genSlideInConf).name;
