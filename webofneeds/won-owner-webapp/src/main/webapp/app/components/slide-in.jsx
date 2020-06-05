import React, { useState } from "react";
import won from "../won-es6";
import { useSelector, useDispatch } from "react-redux";
import { actionCreators } from "../actions/actions.js";
import {
  get,
  getIn,
  toAbsoluteURL,
  getQueryParams,
  generateLink,
} from "../utils.js";
import { parseRestErrorMessage } from "../won-utils.js";
import { ownerBaseUrl } from "~/config/default.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import * as viewSelectors from "../redux/selectors/view-selectors.js";
import * as processSelectors from "../redux/selectors/process-selectors.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";

import "~/style/_slidein.scss";
import ico16_indicator_warning from "~/images/won-icons/ico16_indicator_warning.svg";
import ico_loading_anim from "~/images/won-icons/ico_loading_anim.svg";
import ico16_indicator_info from "~/images/won-icons/ico16_indicator_info.svg";
import ico36_close from "~/images/won-icons/ico36_close.svg";
import { Link, useHistory } from "react-router-dom";

export default function WonSlideIn() {
  const dispatch = useDispatch();
  const history = useHistory();
  const [state, setState] = useState({
    anonymousEmail: "",
    emailValidationMessage: "",
  });

  const { token } = getQueryParams(history.location);
  const verificationToken = token;

  const accountState = useSelector(generalSelectors.getAccountState);

  const privateId = accountUtils.getPrivateId(accountState);
  const path = "#!/inventory" + `?privateId=${privateId}`;
  const anonymousLink = toAbsoluteURL(ownerBaseUrl).toString() + path;
  const isLoggedIn = accountUtils.isLoggedIn(accountState);
  const isAnonymous = accountUtils.isAnonymous(accountState);
  const isEmailVerified = accountUtils.isEmailVerified(accountState);
  const emailVerificationError = accountUtils.getEmailVerificationError(
    accountState
  );
  const connectionHasBeenLost = useSelector(state =>
    getIn(state, ["messages", "lostConnection"])
  );
  const showAnonymousSlideInEmailInput = useSelector(
    viewSelectors.showAnonymousSlideInEmailInput
  );
  const anonymousLinkSent = useSelector(viewSelectors.isAnonymousLinkSent);
  const anonymousLinkCopied = useSelector(viewSelectors.isAnonymousLinkCopied);

  const isAlreadyVerifiedError =
    get(emailVerificationError, "code") ===
    won.RESPONSECODE.TOKEN_RESEND_FAILED_ALREADY_VERIFIED;
  const isProcessingVerifyEmailAddress = useSelector(
    processSelectors.isProcessingVerifyEmailAddress
  );
  const isProcessingAcceptTermsOfService = useSelector(
    processSelectors.isProcessingAcceptTermsOfService
  );
  const isProcessingResendVerificationEmail = useSelector(
    processSelectors.isProcessingResendVerificationEmail
  );
  const isProcessingSendAnonymousLinkEmail = useSelector(
    processSelectors.isProcessingSendAnonymousLinkEmail
  );
  const email = accountUtils.getEmail(accountState);
  const reconnecting = useSelector(state =>
    getIn(state, ["messages", "reconnecting"])
  );
  const showAnonymousSuccess = useSelector(
    viewSelectors.showSlideInAnonymousSuccess
  );
  const showAnonymous = useSelector(viewSelectors.showSlideInAnonymous);
  const showDisclaimer = useSelector(viewSelectors.showSlideInDisclaimer);
  const showTermsOfService = useSelector(
    viewSelectors.showSlideInTermsOfService
  );
  const showEmailVerification = useSelector(
    viewSelectors.showSlideInEmailVerification(history)
  );
  const showConnectionLost = useSelector(
    viewSelectors.showSlideInConnectionLost
  );

  function copyLinkToClipboard() {
    let tempInput = document.createElement("input");
    tempInput.style = "position: absolute; left: -1000px; top: -1000px";
    tempInput.value = anonymousLink;
    document.body.appendChild(tempInput);
    tempInput.select();
    document.execCommand("copy");
    document.body.removeChild(tempInput);
    dispatch(actionCreators.account__copiedAnonymousLinkSuccess());
  }

  function setAnonymousEmail(event) {
    const email = event.target.value;
    const emailValidationMessage = event.target.validationMessage;
    setState({
      anonymousEmail: email,
      emailValidationMessage: emailValidationMessage || "",
    });
  }

  function isValidEmail() {
    return (
      state.anonymousEmail &&
      state.anonymousEmail.length > 0 &&
      state.emailValidationMessage === ""
    ); //Simple Email Validation
  }

  return (
    <won-slide-in>
      {showConnectionLost && (
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
          {connectionHasBeenLost &&
            !reconnecting && (
              <button
                onClick={() => dispatch(actionCreators.reconnect__start())}
                className="si__button"
              >
                Reconnect
              </button>
            )}
          {reconnecting && (
            <svg className="hspinner">
              <use xlinkHref={ico_loading_anim} href={ico_loading_anim} />
            </svg>
          )}
        </div>
      )}
      {showEmailVerification && (
        <div className="si__emailverification">
          <svg className="si__icon">
            <use
              xlinkHref={ico16_indicator_warning}
              href={ico16_indicator_warning}
            />
          </svg>
          {!verificationToken &&
            !isEmailVerified &&
            !isAnonymous &&
            !emailVerificationError && (
              <span className="si__title">
                E-Mail has not been verified yet, check your Inbox.
              </span>
            )}
          {isProcessingVerifyEmailAddress &&
            verificationToken && (
              <span className="si__title">Verifying the E-Mail address</span>
            )}
          {!isProcessingVerifyEmailAddress &&
            !!emailVerificationError && (
              <span className="si__title">
                {parseRestErrorMessage(emailVerificationError)}
              </span>
            )}
          {isLoggedIn &&
            verificationToken &&
            !isProcessingVerifyEmailAddress &&
            isEmailVerified &&
            !isAnonymous &&
            !emailVerificationError && (
              <span className="si__title">E-Mail Address verified</span>
            )}
          {!isLoggedIn &&
            verificationToken &&
            !isProcessingVerifyEmailAddress &&
            !emailVerificationError && (
              <span className="si__title">
                E-Mail Address verified (Please Login Now)
              </span>
            )}
          {(isProcessingVerifyEmailAddress ||
            isProcessingResendVerificationEmail) && (
            <svg className="hspinner">
              <use xlinkHref={ico_loading_anim} href={ico_loading_anim} />
            </svg>
          )}
          {!isProcessingVerifyEmailAddress &&
            !isProcessingResendVerificationEmail &&
            ((isLoggedIn &&
              !isEmailVerified &&
              !isAnonymous &&
              !emailVerificationError) ||
              (verificationToken && !!emailVerificationError)) && (
              <button
                className="si__button"
                onClick={() =>
                  dispatch(
                    actionCreators.account__resendVerificationEmail(email)
                  )
                }
              >
                Resend Email
              </button>
            )}
          {!isProcessingVerifyEmailAddress &&
            verificationToken &&
            !emailVerificationError && (
              <svg
                className="si__close"
                onClick={() =>
                  history.replace(
                    generateLink(history.location, {
                      token: undefined,
                    })
                  )
                }
              >
                <use xlinkHref={ico36_close} href={ico36_close} />
              </svg>
            )}
          {!isProcessingVerifyEmailAddress &&
            isAlreadyVerifiedError && (
              <svg
                className="si__close"
                onClick={() =>
                  dispatch(actionCreators.account__verifyEmailAddressSuccess())
                }
              >
                <use xlinkHref={ico36_close} href={ico36_close} />
              </svg>
            )}
        </div>
      )}
      {showTermsOfService && (
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
          {isProcessingAcceptTermsOfService ? (
            <svg className="hspinner">
              <use xlinkHref={ico_loading_anim} href={ico_loading_anim} />
            </svg>
          ) : (
            <button
              className="si__button"
              onClick={() =>
                dispatch(actionCreators.account__acceptTermsOfService())
              }
            >
              Accept
            </button>
          )}
        </div>
      )}
      {showDisclaimer && (
        <div className="si__disclaimer">
          <svg className="si__icon">
            <use xlinkHref={ico16_indicator_info} href={ico16_indicator_info} />
          </svg>
          <div className="si__title">
            This is the demonstrator of an ongoing research project.
          </div>
          <div className="si__text">
            Please keep in mind:
            <ul>
              <li> Your posts are public.</li>
              <li> Your user account is not publicly linked to your posts.</li>
              <li> The connections of your posts are public.</li>
              <li>
                {" "}
                The messages you exchange with others are private, but stored in
                clear text on our servers.
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
            onClick={() => dispatch(actionCreators.account__acceptDisclaimer())}
            className="si__bottomButton"
          >
            {"Ok, I'll keep that in mind"}
          </button>
        </div>
      )}
      {showAnonymous && (
        <div
          className={
            "si__anonymous " +
            (showAnonymousSlideInEmailInput
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
            onClick={() =>
              dispatch(actionCreators.view__anonymousSlideIn__hide())
            }
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
                  <Link className="clickable" to="/signup">
                    Consider signing up!
                  </Link>
                </b>{" "}
                It will allow us to contact you if there is relevant activity.
              </li>
              <li>
                Alternatively, we can <b>send you the login link</b> by email.
              </li>
            </ul>
          </div>
          <Link className=" si__buttonSignup" to="/signup">
            Sign up
          </Link>
          <button className="si__buttonCopy" onClick={copyLinkToClipboard}>
            Copy login link to clipboard
          </button>
          <button
            className="si__buttonEmail"
            onClick={() =>
              dispatch(actionCreators.view__anonymousSlideIn__showEmailInput())
            }
          >
            Email login link ...
          </button>
          {showAnonymousSlideInEmailInput && (
            <input
              className="si__emailInput"
              type="email"
              value={state.anonymousEmail}
              placeholder="Type your email"
              onChange={setAnonymousEmail}
            />
          )}
          {!isProcessingSendAnonymousLinkEmail &&
            showAnonymousSlideInEmailInput && (
              <button
                className="si__buttonSend"
                onClick={() =>
                  dispatch(
                    actionCreators.account__sendAnonymousLinkEmail(
                      state.anonymousEmail,
                      privateId
                    )
                  )
                }
                disabled={!isValidEmail()}
              >
                Send link to this email
              </button>
            )}
          {isProcessingSendAnonymousLinkEmail && (
            <svg className="hspinner">
              <use xlinkHref={ico_loading_anim} href={ico_loading_anim} />
            </svg>
          )}
        </div>
      )}
      {showAnonymousSuccess && (
        <div className="si__anonymoussuccess">
          <svg className="si__icon">
            <use xlinkHref={ico16_indicator_info} href={ico16_indicator_info} />
          </svg>
          {anonymousLinkSent && (
            <div className="si__title">
              {"Link sent to " + state.anonymousEmail + "."}
            </div>
          )}
          {anonymousLinkCopied && (
            <div className="si__title">Link copied to clipboard.</div>
          )}
          <svg
            className="si__close"
            onClick={() =>
              dispatch(actionCreators.view__anonymousSlideIn__hide())
            }
          >
            <use xlinkHref={ico36_close} href={ico36_close} />
          </svg>
        </div>
      )}
    </won-slide-in>
  );
}
