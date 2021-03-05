import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { get, getIn } from "../utils.js";

import "~/style/_modal-dialog.scss";
import WonLabelledHr from "~/app/components/labelled-hr";
import * as accontUtils from "~/app/redux/utils/account-utils";
import * as generalSelectors from "~/app/redux/selectors/general-selectors";
import { actionCreators } from "~/app/actions/actions";
import * as wonUtils from "~/app/won-utils";
import ReactMarkdown from "react-markdown";
import { parseRestErrorMessage } from "~/app/won-utils";
import * as accountUtils from "~/app/redux/utils/account-utils";
import won from "~/app/won-es6";
import * as processUtils from "~/app/redux/utils/process-utils";

export default function WonModalDialog() {
  const dispatch = useDispatch();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [rememberMe, setRememberMe] = useState(false);

  const modalDialog = useSelector(state =>
    getIn(state, ["view", "modalDialog"])
  );
  const modalDialogCaption = get(modalDialog, "caption");
  const modalDialogText = get(modalDialog, "text");
  const modalDialogButtons = get(modalDialog, "buttons") || [];
  const showLoggedOutDialog = get(modalDialog, "showLoggedOutDialog");

  const accountState = useSelector(generalSelectors.getAccountState);
  const processState = useSelector(generalSelectors.getProcessState);
  const loginError = accountUtils.getLoginError(accountState);
  const isNotVerified =
    get(loginError, "code") === won.RESPONSECODE.USER_NOT_VERIFIED;
  const processingResendVerificationEmail = processUtils.isProcessingResendVerificationEmail(
    processState
  );
  const isLoggedIn = accontUtils.isLoggedIn(accountState);
  const isAnonymous = accontUtils.isAnonymous(accountState);
  const loggedInEmail = accontUtils.getEmail(accountState);

  function formKeyUp(event) {
    if (loginError) {
      dispatch(actionCreators.view__clearLoginError());
    }

    if (event.keyCode === 13) {
      login(event);
    }
  }

  useEffect(
    () => {
      if (isLoggedIn && loggedInEmail !== email) {
        setEmail(loggedInEmail);
      }
    },
    [isLoggedIn, loggedInEmail]
  );

  function login(event) {
    event.preventDefault();
    console.debug("Login submit");

    dispatch(
      actionCreators.account__login(
        {
          email: email,
          password: password,
          rememberMe: rememberMe,
        },
        get(modalDialog, "afterLoginCallback")
      )
    );
  }

  function loginAnon() {
    dispatch(
      actionCreators.account__login({
        privateId: accontUtils.getPrivateId(accountState),
      })
    );
  }

  function registerAnonAccount() {
    const privateId = wonUtils.generatePrivateId();
    dispatch(
      actionCreators.account__register(
        { privateId },
        get(modalDialog, "afterLoginCallback")
      )
    );
  }

  const closeDialog = () => dispatch(actionCreators.view__hideModalDialog());

  return (
    <won-modal-dialog>
      <div className="md__dialog">
        {showLoggedOutDialog ? (
          <React.Fragment>
            <div className="md__dialog__header">
              <span className="md__dialog__header__caption">
                {isLoggedIn
                  ? "Session Expired, Login to continue"
                  : "You are currently not logged in"}
              </span>
            </div>
            <div className="md__dialog__content">
              {isLoggedIn && isAnonymous ? (
                <span className="md__dialog__content__text">
                  Your state shows that you used to be logged in with an
                  Anonymous Account, but lost your session. Click the button
                  below to sign in again.
                </span>
              ) : (
                <won-login-form>
                  <form onSubmit={login} id="loginForm" className="loginForm">
                    <input
                      id="loginEmail"
                      placeholder="Email address"
                      value={email}
                      type="email"
                      disabled={isLoggedIn}
                      required
                      autoFocus
                      onKeyUp={formKeyUp}
                      onChange={event => setEmail(event.target.value)}
                    />
                    {!!loginError && (
                      <ReactMarkdown
                        className="wl__errormsg markdown"
                        source={parseRestErrorMessage(loginError)}
                      />
                    )}
                    {isNotVerified &&
                      (!processingResendVerificationEmail ? (
                        <a
                          className="wl__errormsg__resend"
                          onClick={() =>
                            dispatch(
                              actionCreators.account__resendVerificationEmail(
                                email
                              )
                            )
                          }
                        >
                          {"(Click to Resend Verification Email)"}
                        </a>
                      ) : (
                        <a className="wl__errormsg__resend">
                          {"(Resending...)"}
                        </a>
                      ))}
                    <input
                      id="loginPassword"
                      placeholder="Password"
                      value={password}
                      type="password"
                      required
                      onKeyUp={formKeyUp}
                      onChange={event => setPassword(event.target.value)}
                    />
                    {email.length > 0 &&
                      password.length > 0 && (
                        <label>
                          <input
                            id="remember-me"
                            value={rememberMe}
                            onChange={event =>
                              setRememberMe(event.target.checked)
                            }
                            type="checkbox"
                          />
                          Remember me
                        </label>
                      )}
                    <button
                      className="won-button--filled secondary"
                      disabled={password === "" || email === ""}
                    >
                      Sign In
                    </button>
                  </form>
                </won-login-form>
              )}

              {!isLoggedIn ? (
                <React.Fragment>
                  <WonLabelledHr label="or" />
                  <span className="md__dialog__content__text">
                    {
                      "If you don't have an Account yet, you do not have to register right away."
                    }
                    <br />
                    <br />
                    {"Simple click 'Yes', to accept the "}
                    <a
                      target="_blank"
                      rel="noopener noreferrer"
                      href="#!/about?aboutSection=aboutTermsOfService"
                    >
                      Terms Of Service(ToS)
                    </a>
                    {
                      " and anonymous account will be created for you. Clicking 'No' will just cancel the action. You can link this Account to an E-Mail address later."
                    }
                  </span>
                </React.Fragment>
              ) : isAnonymous ? (
                <div className="md__dialog__footer md__dialog__footer--column">
                  <button
                    className={"won-button--filled secondary"}
                    onClick={loginAnon}
                  >
                    <span>Re-Login Anonymous Account</span>
                  </button>
                </div>
              ) : (
                undefined
              )}
            </div>
            {!isLoggedIn ? (
              <div className="md__dialog__footer md__dialog__footer--column">
                <button
                  className={"won-button--filled secondary"}
                  onClick={registerAnonAccount}
                >
                  <span>Yes, I accept ToS</span>
                </button>
                <button
                  className={"won-button--filled secondary"}
                  onClick={closeDialog}
                >
                  <span>No, cancel</span>
                </button>
              </div>
            ) : (
              undefined
            )}
          </React.Fragment>
        ) : (
          <React.Fragment>
            <div className="md__dialog__header">
              <span className="md__dialog__header__caption">
                {modalDialogCaption}
              </span>
            </div>
            <div className="md__dialog__content">
              <span className="md__dialog__content__text">
                {modalDialogText}
              </span>
            </div>
            <div
              className={
                "md__dialog__footer " +
                (modalDialogButtons.size > 2
                  ? " md__dialog__footer--row"
                  : " md__dialog__footer--column")
              }
            >
              {modalDialogButtons.map((button, index) => (
                <button
                  key={get(button, "caption") + "-" + index}
                  className={"won-button--filled secondary"}
                  onClick={get(button, "callback")}
                >
                  <span>{get(button, "caption")}</span>
                </button>
              ))}
            </div>
          </React.Fragment>
        )}
      </div>
    </won-modal-dialog>
  );
}
