/**
 * Created by quasarchimaere on 20.11.2018.
 */
import won from "../won-es6.js";
import angular from "angular";
import ngAnimate from "angular-animate";
import dropdownModule from "./covering-dropdown.js";
import { attach, delay, get, getIn, toAbsoluteURL } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux, parseRestErrorMessage } from "../won-utils.js";
import { ownerBaseUrl } from "config/default.js";
import { getVerificationTokenFromRoute } from "../selectors/general-selectors.js";
import * as viewSelectors from "../selectors/view-selectors.js";
import * as processSelectors from "../selectors/process-selectors.js";

import * as srefUtils from "../sref-utils.js";
import * as accountUtils from "../account-utils.js";

import "style/_slidein.scss";

function genSlideInConf() {
  let template = `
        <svg class="si__toggle"
            ng-click="self.view__toggleSlideIns()">
            <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
        </svg>
        <input type='text' class="si__anonymousLink" value="{{ self.anonymousLink }}" ng-if="self.inclAnonymousLinkInput"/>
        <div class="si__connectionlost" ng-if="self.showConnectionLost">
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
        <div class="si__emailverification" ng-if="self.showEmailVerification">
            <svg class="si__icon">
                <use xlink:href="#ico16_indicator_warning" href="#ico16_indicator_warning"></use>
            </svg>
            <span class="si__title" ng-if="!self.verificationToken && !self.isEmailVerified && !self.isAnonymous && !self.emailVerificationError">
                E-Mail has not been verified yet, check your Inbox.
            </span>
            <span class="si__title" ng-if="self.isProcessingVerifyEmailAddress && self.verificationToken">
                Verifying the E-Mail address
            </span>
            <span class="si__title" ng-if="!self.isProcessingVerifyEmailAddress && self.emailVerificationError">
                {{ self.parseRestErrorMessage(self.emailVerificationError) }}
            </span>
            <span class="si__title" ng-if="self.isLoggedIn && self.verificationToken && !self.isProcessingVerifyEmailAddress && self.isEmailVerified && !self.isAnonymous && !self.emailVerificationError">
                E-Mail Address verified
            </span>
            <span class="si__title" ng-if="!self.isLoggedIn && self.verificationToken && !self.isProcessingVerifyEmailAddress && !self.emailVerificationError">
                E-Mail Address verified (Please Login Now)
            </span>
            <svg class="hspinner" ng-if="self.isProcessingVerifyEmailAddress || self.isProcessingResendVerificationEmail">
                <use xlink:href="#ico_loading_anim" href="#ico_loading_anim"></use>
            </svg>
            <button
              class="si__button"
              ng-if="!self.isProcessingVerifyEmailAddress && !self.processingResendVerificationMail && ((self.isLoggedIn && !self.isEmailVerified && !self.isAnonymous && !self.emailVerificationError) || (self.verificationToken && self.emailVerificationError))"
              ng-click="self.account__resendVerificationEmail(self.email)">
                Resend Email
            </button>
            <svg class="si__close"
                ng-click="self.router__stateGoCurrent({token: undefined})"
                ng-if="!self.isProcessingVerifyEmailAddress && self.verificationToken && !self.emailVerificationError">
                <use xlink:href="#ico36_close" href="#ico36_close"></use>
            </svg>
            <svg class="si__close"
                ng-click="self.account__verifyEmailAddressSuccess()"
                ng-if="!self.isProcessingVerifyEmailAddress && self.isAlreadyVerifiedError">
                <use xlink:href="#ico36_close" href="#ico36_close"></use>
            </svg>
        </div>
        <div class="si__termsofservice" ng-if="self.showTermsOfService">
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
              ng-if="!self.isProcessingAcceptTermsOfService"
              ng-click="self.account__acceptTermsOfService()">
                Accept
            </button>
            <svg class="hspinner" ng-if="self.isProcessingAcceptTermsOfService">
                <use xlink:href="#ico_loading_anim" href="#ico_loading_anim"></use>
            </svg>
        </div>
        <div class="si__disclaimer" ng-if="self.showDisclaimer">
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
            ng-if="self.showAnonymous"
            ng-class="{
              'si__anonymous--expanded': self.isAnonymousSlideInExpanded,
              'si__anonymous--emailInput': self.showAnonymousSlideInEmailInput,
            }">
            <svg class="si__icon">
                <use xlink:href="#ico16_indicator_warning" href="#ico16_indicator_warning"></use>
            </svg>
            <span class="si__title">
                Warning: <b>You could miss out on important activity!</b>
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
              <h3>You are posting with an anonymous account. This means:</h3>
              <p>
                <ul>
                  <li>You may not notice when <b>other users want to connect.</b></li>
                  <li>You need the <b>login link</b> to access your postings later.</li>                  
                </ul>
              </p>
              <br>
              <h3>Therefore:</h3>
              <p>
                <ul>
                  <li><b><a href="{{ self.absHRef(self.$state, 'signup') }}">Consider signing up!</a></b> It will allow us to contact you if there is relevant activity.</li>
                  <li>Alternatively, we can <b>send you the login link</b> by email.</li>
                </ul>
              </p>
            </div>
            <button class="si__buttonSignup"
                ng-if="self.isAnonymousSlideInExpanded"
                ng-click="self.router__stateGoAbs('signup')">
                Sign up
            </button>
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
                ng-if="!self.isProcessingSendAnonymousLinkEmail && self.isAnonymousSlideInExpanded && self.showAnonymousSlideInEmailInput"
                ng-click="self.account__sendAnonymousLinkEmail(self.anonymousEmail, self.privateId)"
                ng-disabled="!self.isValidEmail()">
                Send link to this email
            </button>
            <svg class="hspinner" ng-if="self.isProcessingSendAnonymousLinkEmail">
                <use xlink:href="#ico_loading_anim" href="#ico_loading_anim"></use>
            </svg>
        </div>
        <div class="si__anonymoussuccess"
            ng-if="self.showAnonymousSuccess">
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
        const verificationToken = getVerificationTokenFromRoute(state);

        const accountState = get(state, "account");

        const privateId = accountUtils.getPrivateId(accountState);
        const path = "#!/connections" + `?privateId=${privateId}`;

        const anonymousLink = toAbsoluteURL(ownerBaseUrl).toString() + path;
        const isLoggedIn = accountUtils.isLoggedIn(accountState);
        const isAnonymous = accountUtils.isAnonymous(accountState);
        const isEmailVerified = accountUtils.isEmailVerified(accountState);
        const emailVerificationError = accountUtils.getEmailVerificationError(
          accountState
        );
        const isTermsOfServiceAccepted = accountUtils.isTermsOfServiceAccepted(
          accountState
        );

        const connectionHasBeenLost = getIn(state, [
          "messages",
          "lostConnection",
        ]);

        const isAnonymousSlideInExpanded = viewSelectors.isAnonymousSlideInExpanded(
          state
        );
        const showAnonymousSlideInEmailInput = viewSelectors.showAnonymousSlideInEmailInput(
          state
        );
        const anonymousLinkSent = viewSelectors.isAnonymousLinkSent(state);
        const anonymousLinkCopied = viewSelectors.isAnonymousLinkCopied(state);

        return {
          verificationToken,
          isEmailVerified,
          emailVerificationError,
          isAlreadyVerifiedError:
            get(emailVerificationError, "code") ===
            won.RESPONSECODE.TOKEN_RESEND_FAILED_ALREADY_VERIFIED,
          isProcessingVerifyEmailAddress: processSelectors.isProcessingVerifyEmailAddress(
            state
          ),
          isProcessingAcceptTermsOfService: processSelectors.isProcessingAcceptTermsOfService(
            state
          ),
          isProcessingResendVerificationEmail: processSelectors.isProcessingResendVerificationEmail(
            state
          ),
          isProcessingSendAnonymousLinkEmail: processSelectors.isProcessingSendAnonymousLinkEmail(
            state
          ),
          isTermsOfServiceAccepted,
          isLoggedIn,
          email: accountUtils.getEmail(accountState),
          isAnonymous,
          privateId,
          connectionHasBeenLost,
          reconnecting: getIn(state, ["messages", "reconnecting"]),
          isAnonymousSlideInExpanded,
          showAnonymousSlideInEmailInput,
          anonymousLinkSent,
          anonymousLinkCopied,
          anonymousLink: anonymousLink,

          showAnonymousSuccess: viewSelectors.showSlideInAnonymousSuccess(
            state
          ),
          showAnonymous: viewSelectors.showSlideInAnonymous(state),
          showDisclaimer: viewSelectors.showSlideInDisclaimer(state),
          showTermsOfService: viewSelectors.showSlideInTermsOfService(state),
          showEmailVerification: viewSelectors.showSlideInEmailVerification(
            state
          ),
          showConnectionLost: viewSelectors.showSlideInConnectionLost(state),
          inclAnonymousLinkInput:
            !connectionHasBeenLost &&
            isLoggedIn &&
            isAnonymous &&
            !anonymousLinkSent &&
            !anonymousLinkCopied &&
            isAnonymousSlideInExpanded,
        };
      };

      connect2Redux(selectFromState, actionCreators, [], this);

      this.$scope.$watch("self.verificationToken", verificationToken =>
        this.verifyEmailAddress(verificationToken)
      );
    }

    copyLinkToClipboard() {
      let tempInput = document.createElement("input");
      tempInput.style = "position: absolute; left: -1000px; top: -1000px";
      tempInput.value = this.anonymousLink;
      document.body.appendChild(tempInput);
      tempInput.select();
      document.execCommand("copy");
      document.body.removeChild(tempInput);
      this.account__copiedAnonymousLinkSuccess();
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
          !this.isProcessingVerifyEmailAddress &&
          !(
            this.isEmailVerified ||
            this.emailVerificationError ||
            this.isAnonymous
          )
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
