import React from "react";
import PropTypes from "prop-types";
import won from "../won-es6";
import { connect } from "react-redux";
import { actionCreators } from "../actions/actions.js";
import { getVerificationTokenFromRoute } from "../redux/selectors/general-selectors.js";
import { get, getIn, toAbsoluteURL } from "../utils.js";
import { parseRestErrorMessage } from "../won-utils.js";
import { ownerBaseUrl } from "~/config/default.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import * as viewSelectors from "../redux/selectors/view-selectors.js";
import * as processSelectors from "../redux/selectors/process-selectors.js";

import "~/style/_slidein.scss";
import ico16_indicator_warning from "~/images/won-icons/ico16_indicator_warning.svg";
import ico_loading_anim from "~/images/won-icons/ico_loading_anim.svg";
import ico16_indicator_info from "~/images/won-icons/ico16_indicator_info.svg";
import ico36_close from "~/images/won-icons/ico36_close.svg";

const mapStateToProps = state => {
  const verificationToken = getVerificationTokenFromRoute(state);

  const accountState = get(state, "account");

  const privateId = accountUtils.getPrivateId(accountState);
  const path = "#!/inventory" + `?privateId=${privateId}`;

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

  const connectionHasBeenLost = getIn(state, ["messages", "lostConnection"]);

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
    showAnonymousSlideInEmailInput,
    anonymousLinkSent,
    anonymousLinkCopied,
    anonymousLink: anonymousLink,

    showAnonymousSuccess: viewSelectors.showSlideInAnonymousSuccess(state),
    showAnonymous: viewSelectors.showSlideInAnonymous(state),
    showDisclaimer: viewSelectors.showSlideInDisclaimer(state),
    showTermsOfService: viewSelectors.showSlideInTermsOfService(state),
    showEmailVerification: viewSelectors.showSlideInEmailVerification(state),
    showConnectionLost: viewSelectors.showSlideInConnectionLost(state),
    inclAnonymousLinkInput:
      !connectionHasBeenLost &&
      isLoggedIn &&
      isAnonymous &&
      !anonymousLinkSent &&
      !anonymousLinkCopied,
  };
};

const mapDispatchToProps = dispatch => {
  return {
    accountCopiedAnonymousLinkSuccess: () => {
      dispatch(actionCreators.account__copiedAnonymousLinkSuccess());
    },
    accountVerifyEmailAddress: verificationToken => {
      dispatch(actionCreators.account__verifyEmailAddress(verificationToken));
    },
    accountResendVerificationEmail: email => {
      dispatch(actionCreators.account__resendVerificationEmail(email));
    },
    accountSendAnonymousLinkEmail: (email, privateId) => {
      dispatch(
        actionCreators.account__sendAnonymousLinkEmail(email, privateId)
      );
    },
    accountVerifyEmailAddressSuccess: () => {
      dispatch(actionCreators.account__verifyEmailAddressSuccess());
    },
    accountAcceptDisclaimer: () => {
      dispatch(actionCreators.account__acceptDisclaimer());
    },
    accountAcceptTermsOfService: () => {
      dispatch(actionCreators.account__acceptTermsOfService());
    },
    reconnectStart: () => {
      dispatch(actionCreators.reconnect__start());
    },
    routerGoCurrent: props => {
      dispatch(actionCreators.router__stateGoCurrent(props));
    },
    routerGo: (path, props) => {
      dispatch(actionCreators.router__stateGo(path, props));
    },
    routerGoAbs: (path, props) => {
      dispatch(actionCreators.router__stateGoAbs(path, props));
    },
    hideAnonymousSlideIn: () => {
      dispatch(actionCreators.view__anonymousSlideIn__hide());
    },
    showEmailInput: () => {
      dispatch(actionCreators.view__anonymousSlideIn__showEmailInput());
    },
  };
};

class WonSlideIn extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      anonymousEmail: "",
      emailValidationMessage: "",
    };
    this.copyLinkToClipboard = this.copyLinkToClipboard.bind(this);
    this.setAnonymousEmail = this.setAnonymousEmail.bind(this);
  }

  render() {
    return (
      <won-slide-in>
        {this.props.showConnectionLost && (
          <div className="si__connectionlost">
            <svg className="si__icon">
              <use
                xlinkHref={ico16_indicator_warning}
                href={ico16_indicator_warning}
              />
            </svg>
            <span className="si__title">
              Lost connection &ndash; make sure your internet-connection is
              working, then click &ldquo;reconnect&rdquo;.
            </span>
            {this.props.connectionHasBeenLost &&
              !this.props.reconnecting && (
                <button
                  onClick={this.props.reconnectStart}
                  className="si__button"
                >
                  Reconnect
                </button>
              )}
            {this.props.reconnecting && (
              <svg className="hspinner">
                <use xlinkHref={ico_loading_anim} href={ico_loading_anim} />
              </svg>
            )}
          </div>
        )}
        {this.props.showEmailVerification && (
          <div className="si__emailverification">
            <svg className="si__icon">
              <use
                xlinkHref={ico16_indicator_warning}
                href={ico16_indicator_warning}
              />
            </svg>
            {!this.props.verificationToken &&
              !this.props.isEmailVerified &&
              !this.props.isAnonymous &&
              !this.props.emailVerificationError && (
                <span className="si__title">
                  E-Mail has not been verified yet, check your Inbox.
                </span>
              )}
            {this.props.isProcessingVerifyEmailAddress &&
              this.props.verificationToken && (
                <span className="si__title">Verifying the E-Mail address</span>
              )}
            {!this.props.isProcessingVerifyEmailAddress &&
              !!this.props.emailVerificationError && (
                <span className="si__title">
                  {parseRestErrorMessage(this.props.emailVerificationError)}
                </span>
              )}
            {this.props.isLoggedIn &&
              this.props.verificationToken &&
              !this.props.isProcessingVerifyEmailAddress &&
              this.props.isEmailVerified &&
              !this.props.isAnonymous &&
              !this.props.emailVerificationError && (
                <span className="si__title">E-Mail Address verified</span>
              )}
            {!this.props.isLoggedIn &&
              this.props.verificationToken &&
              !this.props.isProcessingVerifyEmailAddress &&
              !this.props.emailVerificationError && (
                <span className="si__title">
                  E-Mail Address verified (Please Login Now)
                </span>
              )}
            {(this.props.isProcessingVerifyEmailAddress ||
              this.props.isProcessingResendVerificationEmail) && (
              <svg className="hspinner">
                <use xlinkHref={ico_loading_anim} href={ico_loading_anim} />
              </svg>
            )}
            {!this.props.isProcessingVerifyEmailAddress &&
              !this.props.isProcessingResendVerificationEmail &&
              ((this.props.isLoggedIn &&
                !this.props.isEmailVerified &&
                !this.props.isAnonymous &&
                !this.props.emailVerificationError) ||
                (this.props.verificationToken &&
                  !!this.props.emailVerificationError)) && (
                <button
                  className="si__button"
                  onClick={() =>
                    this.props.accountResendVerificationEmail(this.props.email)
                  }
                >
                  Resend Email
                </button>
              )}
            {!this.props.isProcessingVerifyEmailAddress &&
              this.props.verificationToken &&
              !this.props.emailVerificationError && (
                <svg
                  className="si__close"
                  onClick={() =>
                    this.props.routerGoCurrent({ token: undefined })
                  }
                >
                  <use xlinkHref={ico36_close} href={ico36_close} />
                </svg>
              )}
            {!this.props.isProcessingVerifyEmailAddress &&
              this.props.isAlreadyVerifiedError && (
                <svg
                  className="si__close"
                  onClick={this.props.accountVerifyEmailAddressSuccess}
                >
                  <use xlinkHref={ico36_close} href={ico36_close} />
                </svg>
              )}
          </div>
        )}
        {this.props.showTermsOfService && (
          <div className="si__termsofservice">
            <svg className="si__icon">
              <use
                xlinkHref={ico16_indicator_warning}
                href={ico16_indicator_warning}
              />
            </svg>
            <span className="si__title">
              {"You have not accepted the "}
              <a
                target="_blank"
                rel="noopener noreferrer"
                href="#!/about?aboutSection=aboutTermsOfService"
              >
                {"Terms Of Service"}
              </a>
              {" yet."}
            </span>
            {this.props.isProcessingAcceptTermsOfService ? (
              <svg className="hspinner">
                <use xlinkHref={ico_loading_anim} href={ico_loading_anim} />
              </svg>
            ) : (
              <button
                className="si__button"
                onClick={this.props.accountAcceptTermsOfService}
              >
                Accept
              </button>
            )}
          </div>
        )}
        {this.props.showDisclaimer && (
          <div className="si__disclaimer">
            <svg className="si__icon">
              <use
                xlinkHref={ico16_indicator_info}
                href={ico16_indicator_info}
              />
            </svg>
            <div className="si__title">
              This is the demonstrator of an ongoing research project.
            </div>
            <div className="si__text">
              Please keep in mind:
              <ul>
                <li> Your posts are public.</li>
                <li>
                  {" "}
                  Your user account is not publicly linked to your posts.
                </li>
                <li> The connections of your posts are public.</li>
                <li>
                  {" "}
                  The messages you exchange with others are private, but stored
                  in clear text on our servers.
                </li>
              </ul>
              <a
                target="_blank"
                rel="noopener noreferrer"
                href="#!/about?aboutSection=aboutPrivacyPolicy"
              >
                See Privacy Policy.
              </a>
              <br />
              We use cookies to track your session using a self-hosted analytics
              tool.
              <a
                target="_blank"
                rel="noopener noreferrer"
                href="/piwik/index.php?module=CoreAdminHome&action=optOut&language=en"
              >
                Suppress tracking.
              </a>
            </div>
            <button
              onClick={this.props.accountAcceptDisclaimer}
              className="si__bottomButton"
            >
              {"Ok, I'll keep that in mind"}
            </button>
          </div>
        )}
        {this.props.showAnonymous && (
          <div
            className={
              "si__anonymous " +
              (this.props.showAnonymousSlideInEmailInput
                ? " si__anonymous--emailInput "
                : "")
            }
          >
            <svg className="si__icon">
              <use
                xlinkHref={ico16_indicator_warning}
                href={ico16_indicator_warning}
              />
            </svg>
            <span className="si__title">
              Warning: <b>You could miss out on important activity!</b>
            </span>
            <svg
              className="si__close"
              onClick={this.props.hideAnonymousSlideIn}
            >
              <use xlinkHref={ico36_close} href={ico36_close} />
            </svg>
            <div className="si__text">
              <h3>You are posting with an anonymous account. This means:</h3>
              <ul>
                <li>
                  You may not notice when <b>other users want to connect.</b>
                </li>
                <li>
                  You need the <b>login link</b> to access your postings later.
                </li>
              </ul>
              <br />
              <h3>Therefore:</h3>
              <ul>
                <li>
                  <b>
                    <a
                      className="clickable"
                      onClick={() => this.props.routerGo("signup")}
                    >
                      Consider signing up!
                    </a>
                  </b>{" "}
                  It will allow us to contact you if there is relevant activity.
                </li>
                <li>
                  Alternatively, we can <b>send you the login link</b> by email.
                </li>
              </ul>
            </div>
            <button
              className=" si__buttonSignup"
              onClick={() => this.props.routerGoAbs("signup")}
            >
              Sign up
            </button>
            <button
              className="si__buttonCopy"
              onClick={this.copyLinkToClipboard}
            >
              Copy login link to clipboard
            </button>
            <button
              className="si__buttonEmail"
              onClick={this.props.showEmailInput}
            >
              Email login link ...
            </button>
            {this.props.showAnonymousSlideInEmailInput && (
              <input
                className="si__emailInput"
                type="email"
                value={this.state.anonymousEmail}
                placeholder="Type your email"
                onChange={this.setAnonymousEmail}
              />
            )}
            {!this.props.isProcessingSendAnonymousLinkEmail &&
              this.props.showAnonymousSlideInEmailInput && (
                <button
                  className="si__buttonSend"
                  onClick={() =>
                    this.props.accountSendAnonymousLinkEmail(
                      this.state.anonymousEmail,
                      this.props.privateId
                    )
                  }
                  disabled={!this.isValidEmail()}
                >
                  Send link to this email
                </button>
              )}
            {this.props.isProcessingSendAnonymousLinkEmail && (
              <svg className="hspinner">
                <use xlinkHref={ico_loading_anim} href={ico_loading_anim} />
              </svg>
            )}
          </div>
        )}
        {this.props.showAnonymousSuccess && (
          <div className="si__anonymoussuccess">
            <svg className="si__icon">
              <use
                xlinkHref={ico16_indicator_info}
                href={ico16_indicator_info}
              />
            </svg>
            {this.props.anonymousLinkSent && (
              <div className="si__title">
                {"Link sent to " + this.state.anonymousEmail + "."}
              </div>
            )}
            {this.props.anonymousLinkCopied && (
              <div className="si__title">Link copied to clipboard.</div>
            )}
            <svg
              className="si__close"
              onClick={this.props.hideAnonymousSlideIn}
            >
              <use xlinkHref={ico36_close} href={ico36_close} />
            </svg>
          </div>
        )}
      </won-slide-in>
    );
  }

  copyLinkToClipboard() {
    let tempInput = document.createElement("input");
    tempInput.style = "position: absolute; left: -1000px; top: -1000px";
    tempInput.value = this.props.anonymousLink;
    document.body.appendChild(tempInput);
    tempInput.select();
    document.execCommand("copy");
    document.body.removeChild(tempInput);
    this.props.accountCopiedAnonymousLinkSuccess();
  }

  setAnonymousEmail(event) {
    const email = event.target.value;
    const emailValidationMessage = event.target.validationMessage;
    this.setState({
      anonymousEmail: email,
      emailValidationMessage: emailValidationMessage || "",
    });
  }

  isValidEmail() {
    return (
      this.state.anonymousEmail &&
      this.state.anonymousEmail.length > 0 &&
      this.state.emailValidationMessage === ""
    ); //Simple Email Validation
  }
}
WonSlideIn.propTypes = {
  verificationToken: PropTypes.string,
  isEmailVerified: PropTypes.bool,
  emailVerificationError: PropTypes.string,
  isAlreadyVerifiedError: PropTypes.bool,
  isProcessingVerifyEmailAddress: PropTypes.bool,
  isProcessingAcceptTermsOfService: PropTypes.bool,
  isProcessingResendVerificationEmail: PropTypes.bool,
  isProcessingSendAnonymousLinkEmail: PropTypes.bool,
  isTermsOfServiceAccepted: PropTypes.bool,
  isLoggedIn: PropTypes.bool,
  email: PropTypes.string,
  isAnonymous: PropTypes.bool,
  privateId: PropTypes.string,
  connectionHasBeenLost: PropTypes.bool,
  reconnecting: PropTypes.bool,
  showAnonymousSlideInEmailInput: PropTypes.bool,
  anonymousLinkSent: PropTypes.bool,
  anonymousLinkCopied: PropTypes.bool,
  anonymousLink: PropTypes.string,
  showAnonymousSuccess: PropTypes.bool,
  showAnonymous: PropTypes.bool,
  showDisclaimer: PropTypes.bool,
  showTermsOfService: PropTypes.bool,
  showEmailVerification: PropTypes.bool,
  showConnectionLost: PropTypes.bool,
  inclAnonymousLinkInput: PropTypes.bool,

  accountCopiedAnonymousLinkSuccess: PropTypes.func,
  accountVerifyEmailAddress: PropTypes.func,
  accountResendVerificationEmail: PropTypes.func,
  accountSendAnonymousLinkEmail: PropTypes.func,
  accountVerifyEmailAddressSuccess: PropTypes.func,
  accountAcceptTermsOfService: PropTypes.func,
  accountAcceptDisclaimer: PropTypes.func,
  reconnectStart: PropTypes.func,
  routerGoCurrent: PropTypes.func,
  routerGo: PropTypes.func,
  routerGoAbs: PropTypes.func,
  hideAnonymousSlideIn: PropTypes.func,
  showEmailInput: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonSlideIn);
