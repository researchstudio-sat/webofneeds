/**
 * Created by quasarchimaere on 20.11.2018.
 */
import won from "../won-es6.js";
import angular from "angular";
import ngAnimate from "angular-animate";
import dropdownModule from "./covering-dropdown.js";
import { attach, delay, getIn, toAbsoluteURL } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux, parseRestErrorMessage } from "../won-utils.js";
import { ownerBaseUrl } from "config";

import * as srefUtils from "../sref-utils.js";

import "style/_slidein.scss";

function genSlideInConf() {
  let template = `
        <input type='text' class="si__anonymousLink" value="{{ self.anonymousLink }}" ng-if="!self.connectionHasBeenLost && self.loggedIn && self.privateId && !self.anonymousLinkSent && !self.anonymousLinkCopied && self.isAnonymousSlideInExpanded"/>
        <div class="si__connectionlost" ng-class="{'visible': self.connectionHasBeenLost}">
            <svg class="si__icon">
                <use xlink:href="#ico16_indicator_warning" href="#ico16_indicator_warning"></use>
            </svg>
            <span class="si__title">
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
        <div class="si__emailverification" ng-class="{'visible': !self.connectionHasBeenLost && (self.verificationToken || (self.loggedIn && !self.emailVerified))}">
            <svg class="si__icon">
                <use xlink:href="#ico16_indicator_warning" href="#ico16_indicator_warning"></use>
            </svg>
            <span class="si__title" ng-if="!self.verificationToken && !self.emailVerified && !self.emailVerificationError">
                E-Mail has not been verified yet, check your Inbox.
            </span>
            <span class="si__title" ng-if="self.processingVerifyEmailAddress && self.verificationToken">
                Verifying the E-Mail address
            </span>
            <span class="si__title" ng-if="!self.processingVerifyEmailAddress && self.emailVerificationError">
                {{ self.parseRestErrorMessage(self.emailVerificationError) }}
            </span>
            <span class="si__title" ng-if="self.loggedIn && self.verificationToken && !self.processingVerifyEmailAddress && self.emailVerified && !self.emailVerificationError">
                E-Mail Address verified
            </span>
            <span class="si__title" ng-if="!self.loggedIn && self.verificationToken && !self.processingVerifyEmailAddress && !self.emailVerificationError">
                E-Mail Address verified (Please Login Now)
            </span>
            <svg class="hspinner" ng-if="self.processingVerifyEmailAddress || self.processingResendVerificationEmail">
                <use xlink:href="#ico_loading_anim" href="#ico_loading_anim"></use>
            </svg>
            <button
              class="si__button"
              ng-if="!self.processingVerifyEmailAddress && !self.processingResendVerificationMail && ((self.loggedIn && !self.emailVerified && !self.emailVerificationError) || (self.verificationToken && self.emailVerificationError))"
              ng-click="self.account__resendVerificationEmail(self.email)">
                Resend Email
            </button>
            <svg class="si__close"
                ng-click="self.router__stateGoCurrent({token: undefined})"
                ng-if="!self.processingVerifyEmailAddress && self.verificationToken && !self.emailVerificationError">
                <use xlink:href="#ico36_close" href="#ico36_close"></use>
            </svg>
            <svg class="si__close"
                ng-click="self.account__verifyEmailAddressSuccess()"
                ng-if="!self.processingVerifyEmailAddress && self.isAlreadyVerifiedError">
                <use xlink:href="#ico36_close" href="#ico36_close"></use>
            </svg>
        </div>
        <div class="si__termsofservice" ng-class="{'visible': self.loggedIn && !self.connectionHasBeenLost && !self.acceptedTermsOfService}">
            <svg class="si__icon">
                <use xlink:href="#ico16_indicator_warning" href="#ico16_indicator_warning"></use>
            </svg>
            <span class="si__title">
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
        <div class="si__disclaimer" ng-class="{'visible': !self.connectionHasBeenLost && !self.acceptedDisclaimer}">
            <svg class="si__icon">
                <use xlink:href="#ico16_indicator_warning" href="#ico16_indicator_info"></use>
            </svg>
            <div class="si__title">
                This is the demonstrator of an ongoing research project.
            </div>
            <div class="si__text">
                Please keep in mind:
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
                class="si__bottomButton">
                    Ok, I'll keep that in mind
            </button>
        </div>
        <div class="si__anonymous"
            ng-class="{
              'visible': !self.connectionHasBeenLost && self.loggedIn && self.privateId && !self.anonymousLinkSent && !self.anonymousLinkCopied,
              'si__anonymous--expanded': self.isAnonymousSlideInExpanded,
              'si__anonymous--emailInput': self.showAnonymousSlideInEmailInput,
            }">
            <svg class="si__icon">
                <use xlink:href="#ico16_indicator_warning" href="#ico16_indicator_warning"></use>
            </svg>
            <span class="si__title">
                Anonymous Account Warning!
            </span>
            <svg class="si__carret"
                ng-click="self.view__anonymousSlideIn__expand()"
                ng-if="!self.isAnonymousSlideInExpanded">
                <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
            </svg>
            <svg class="si__carret"
                ng-click="self.view__anonymousSlideIn__collapse()"
                ng-if="self.isAnonymousSlideInExpanded">
                <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
            </svg>
            <div class="si__text" ng-if="self.isAnonymousSlideInExpanded">
              You are using an 'anonymous account'. This means:
              <ul>
                <li>When <b>others want to connect</b> with you, <b>you don't see them</b> unless you are on this page</li>
                <li>You will <b>lose your content</b> unless you remember the <b>login link</b></li>
              </ul>
              
              Therefore, please:
              <ul>
                <li>Consider <b>signing up</b>! You will be able to keep the content you have already created.</li>
                <li>Alternatively, save the <b>login link</b> or let us email it to you</li>
              </ul>
            </div>
            <button class="si__buttonCopy"
                ng-if="self.isAnonymousSlideInExpanded"
                ng-click="self.copyLinkToClipboard()">
                Copy login link to clipboard
            </button>
            <button class="si__buttonEmail"
                ng-if="self.isAnonymousSlideInExpanded"
                ng-click="self.view__anonymousSlideIn__showEmailInput()">
                Email login link ...
            </button>
            <input class="si__emailInput"
              ng-if="self.isAnonymousSlideInExpanded && self.showAnonymousSlideInEmailInput"
              type="email"
              ng-model="self.anonymousEmail"
              placeholder="Type your email"/>
            <button class="si__buttonSend"
                ng-if="!self.processingSendAnonymousLinkEmail && self.isAnonymousSlideInExpanded && self.showAnonymousSlideInEmailInput"
                ng-click="self.account__sendAnonymousLinkEmail(self.anonymousEmail, self.privateId)"
                ng-disabled="!self.isValidEmail()">
                Send link to this email
            </button>
            <svg class="hspinner" ng-if="self.processingSendAnonymousLinkEmail">
                <use xlink:href="#ico_loading_anim" href="#ico_loading_anim"></use>
            </svg>
        </div>
        <div class="si__anonymoussuccess"
            ng-class="{
              'visible': !self.connectionHasBeenLost && self.loggedIn && (self.anonymousLinkSent || self.anonymousLinkCopied),
            }">
            <svg class="si__icon">
                <use xlink:href="#ico16_indicator_info" href="#ico16_indicator_info"></use>
            </svg>
            <div class="si__title" ng-if="self.anonymousLinkSent">
                Link sent to {{ self.anonymousEmail }}.
            </div>
            <div class="si__title" ng-if="self.anonymousLinkCopied">
                Link copied to clipboard.
            </div>
            <svg class="si__close"
                ng-click="self.view__anonymousSlideIn__hide()">
                <use xlink:href="#ico36_close" href="#ico36_close"></use>
            </svg>
        </div>
    `;

  const serviceDependencies = [
    "$ngRedux",
    "$scope",
    "$state" /*injections as strings here*/,
    "$element",
  ];

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);
      Object.assign(this, srefUtils); // bind srefUtils to scope
      this.parseRestErrorMessage = parseRestErrorMessage;

      this.anonymousEmail = undefined;

      const selectFromState = state => {
        const verificationToken = getIn(state, [
          "router",
          "currentParams",
          "token",
        ]);

        const privateId = getIn(state, [
          "router",
          "currentParams",
          "privateId",
        ]);

        const path = "#!/connections" + `?privateId=${this.privateId}`;
        const anonymousLink = toAbsoluteURL(ownerBaseUrl).toString() + path;

        return {
          privateId,
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
          processingResendVerificationEmail: getIn(state, [
            "process",
            "processingResendVerificationEmail",
          ]),
          processingSendAnonymousLinkEmail: getIn(state, [
            "process",
            "processingSendAnonymousLinkEmail",
          ]),
          acceptedTermsOfService: getIn(state, [
            "account",
            "acceptedTermsOfService",
          ]),
          loggedIn: getIn(state, ["account", "loggedIn"]),
          email: getIn(state, ["account", "email"]),
          connectionHasBeenLost: getIn(state, ["messages", "lostConnection"]), // name chosen to avoid name-clash with the action-creator
          reconnecting: getIn(state, ["messages", "reconnecting"]),
          isAlreadyVerifiedError:
            getIn(state, ["account", "emailVerificationError", "code"]) ==
            won.RESPONSECODE.TOKEN_RESEND_FAILED_ALREADY_VERIFIED,
          isAnonymousSlideInExpanded: getIn(state, [
            "view",
            "anonymousSlideIn",
            "expanded",
          ]),
          showAnonymousSlideInEmailInput: getIn(state, [
            "view",
            "anonymousSlideIn",
            "showEmailInput",
          ]),
          anonymousLinkSent: getIn(state, [
            "view",
            "anonymousSlideIn",
            "linkSent",
          ]),
          anonymousLinkCopied: getIn(state, [
            "view",
            "anonymousSlideIn",
            "linkCopied",
          ]),
          anonymousLink: anonymousLink,
        };
      };

      connect2Redux(selectFromState, actionCreators, [], this);

      this.$scope.$watch("self.verificationToken", verificationToken =>
        this.verifyEmailAddress(verificationToken)
      );
    }

    copyLinkToClipboard() {
      const linkEl = this.getAnonymousLinkField();
      if (linkEl) {
        linkEl.focus();
        linkEl.setSelectionRange(0, linkEl.value.length);
        if (!document.execCommand("copy")) {
          window.prompt("Copy to clipboard: Ctrl+C", linkEl.value);
        } else {
          linkEl.setSelectionRange(0, 0);
          linkEl.blur();
          this.account__copiedAnonymousLinkSuccess();
        }
      }
    }

    isValidEmail() {
      return (
        this.getEmailField().value &&
        this.getEmailField().value.length > 0 &&
        this.getEmailField().validationMessage === ""
      ); //Simple Email Validation
    }

    getAnonymousLinkField() {
      if (!this._anonymousLinkField) {
        this._anonymousLinkField = this.$element[0].querySelector(
          ".si__anonymousLink"
        );
      }
      return this._anonymousLinkField;
    }

    getEmailField() {
      if (!this._emailField) {
        this._emailField = this.$element[0].querySelector(".si__emailInput");
      }
      return this._emailField;
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
