import React, { useState } from "react";
import PropTypes from "prop-types";
import { useDispatch, useSelector } from "react-redux";
import { actionCreators } from "../actions/actions.js";
import ReactMarkdown from "react-markdown";
import { parseRestErrorMessage } from "../won-utils.js";
import { get } from "../utils.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import won from "../won-es6";

import "~/style/_won-markdown.scss";
import WonLabelledHr from "./labelled-hr";
import { Link } from "react-router-dom";

export default function WonLoginForm({ className }) {
  const dispatch = useDispatch();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [rememberMe, setRememberMe] = useState(false);

  const accountState = useSelector(generalSelectors.getAccountState);
  const processState = useSelector(generalSelectors.getProcessState);
  const loginError = accountUtils.getLoginError(accountState);
  const isNotVerified =
    get(loginError, "code") === won.RESPONSECODE.USER_NOT_VERIFIED;
  const processingResendVerificationEmail = processUtils.isProcessingResendVerificationEmail(
    processState
  );

  function formKeyUp(event) {
    if (loginError) {
      dispatch(actionCreators.view__clearLoginError());
    }
    if (event.keyCode == 13) {
      dispatch(
        actionCreators.account__login({
          email: email,
          password: password,
          rememberMe: rememberMe,
        })
      );
    }
  }

  return (
    <won-login-form class={className ? className : ""}>
      <form
        onSubmit={e => {
          e.preventDefault();
          dispatch(
            actionCreators.account__login({
              email: email,
              password: password,
              rememberMe: rememberMe,
            })
          );
        }}
        id="loginForm"
        className="loginForm"
      >
        <input
          id="loginEmail"
          placeholder="Email address"
          value={email}
          type="email"
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
                dispatch(actionCreators.account__resendVerificationEmail(email))
              }
            >
              {"(Click to Resend Verification Email)"}
            </a>
          ) : (
            <a className="wl__errormsg__resend">{"(Resending...)"}</a>
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
        <button
          className="won-button--filled secondary"
          disabled={password === "" || email === ""}
        >
          Sign In
        </button>
        <input
          id="remember-me"
          value={rememberMe}
          onChange={event => setRememberMe(event.target.checked)}
          type="checkbox"
        />
        {" Remember me"}
      </form>
      <WonLabelledHr label="Or" />
      <div className="wl__register">
        <Link
          className="won-button--filled secondary"
          onClick={() => dispatch(actionCreators.view__hideMainMenu())}
          to="/signup"
        >
          Sign up
        </Link>
      </div>
    </won-login-form>
  );
}
WonLoginForm.propTypes = {
  className: PropTypes.string,
};
