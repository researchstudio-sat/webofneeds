import React, { useState, useEffect } from "react";
import { useSelector, useDispatch } from "react-redux";
import * as accountUtils from "../../redux/utils/account-utils.js";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";

import "~/style/_signup.scss";
import ico16_indicator_warning from "~/images/won-icons/ico16_indicator_warning.svg";
import { actionCreators } from "../../actions/actions";
import { useHistory } from "react-router-dom";
import WonGenericPage from "~/app/pages/genericPage";
import { getQueryParams } from "../../utils.js";

const MINPW_LENGTH = 6;

export default function PageForgotPassword() {
  const dispatch = useDispatch();

  const accountState = useSelector(generalSelectors.getAccountState);
  const resetPasswordError = accountUtils.getResetPasswordError(accountState);

  const history = useHistory();
  const qParams = getQueryParams(history.location);

  const [state, setState] = useState({
    email: qParams.email ? qParams.email : "",
    recoveryKey: "",
    newPassword: "",
    error: resetPasswordError
      ? {
          msg: resetPasswordError.get("msg"),
          code: resetPasswordError.get("code"),
        }
      : undefined,
  });

  useEffect(
    () => {
      if (
        resetPasswordError &&
        resetPasswordError.get("msg") &&
        resetPasswordError.get("code")
      ) {
        setState({
          ...state,
          error: {
            msg: resetPasswordError.get("msg"),
            code: resetPasswordError.get("code"),
          },
        });
      }
    },
    [resetPasswordError, accountState]
  );

  function resetPassword() {
    if (!state.error) {
      dispatch(
        actionCreators.account__resetPassword({
          email: state.email,
          newPassword: state.newPassword,
          recoveryKey: state.recoveryKey,
        })
      );
    }
  }

  function isValid() {
    if (state.error) {
      return false;
    }
    if (state.email.length < 1) {
      return false;
    }
    if (state.newPassword.length < MINPW_LENGTH) {
      return false;
    }
    return isValidRecoveryKey(state.recoveryKey);
  }

  function isValidRecoveryKey(key) {
    //TODO check key structure!
    if (key.length < 1) {
      return false;
    }
    return true;
  }

  function changeEmail(event) {
    setState({
      ...state,
      email: event.target.value,
      validEmail: event.target.validity.valid,
      error: state.error
        ? state.error.code === 8400 || state.error.code === 1400
          ? undefined
          : state.error
        : undefined,
    });
  }

  function changeNewPassword(event) {
    setState({
      ...state,
      newPassword: event.target.value,
      error: state.error
        ? state.error.code === 8401
          ? undefined
          : state.error
        : undefined,
    });
  }

  function changeRecoveryKey(event) {
    setState({
      ...state,
      recoveryKey: event.target.value,
      error: state.error
        ? state.error.code === 8402
          ? undefined
          : state.error
        : undefined,
    });
  }

  return (
    <WonGenericPage pageTitle="Forgot Password">
      <main className="signup" id="signupSection">
        <div className="signup__content">
          <div className="signup__content__form" name="registerForm">
            <input
              id="registerEmail"
              name="email"
              placeholder="Email address"
              required
              type="email"
              onChange={changeEmail}
              value={state.email}
            />
            {state.error &&
              state.error.msg &&
              (state.error.code === 8400 || state.error.code === 1400) && (
                <div className="signup__content__form__errormsg">
                  <svg className="signup__content__form__errormsg__icon">
                    <use
                      xlinkHref={ico16_indicator_warning}
                      href={ico16_indicator_warning}
                    />
                  </svg>
                  <span className="signup__content__form__errormsg__label">
                    {state.error.msg}
                  </span>
                </div>
              )}
            <input
              name="password"
              placeholder="New Password"
              required
              type="password"
              onChange={changeNewPassword}
              value={state.newPassword}
            />
            {!state.error &&
              state.newPassword.length > 0 &&
              state.newPassword.length < MINPW_LENGTH && (
                <div className="signup__content__form__errormsg">
                  <svg className="signup__content__form__errormsg__icon">
                    <use
                      xlinkHref={ico16_indicator_warning}
                      href={ico16_indicator_warning}
                    />
                  </svg>
                  <span className="signup__content__form__errormsg__label">{`Password too short, must be at least ${MINPW_LENGTH} Characters`}</span>
                </div>
              )}
            {state.error &&
              state.error.msg &&
              state.error.code === 8401 && (
                <div className="signup__content__form__errormsg">
                  <svg className="signup__content__form__errormsg__icon">
                    <use
                      xlinkHref={ico16_indicator_warning}
                      href={ico16_indicator_warning}
                    />
                  </svg>
                  <span className="signup__content__form__errormsg__label">
                    {state.error.msg}
                  </span>
                </div>
              )}
            <input
              name="recoveryKey"
              placeholder="Recovery Key"
              required
              type="text"
              onChange={changeRecoveryKey}
              value={state.recoveryKey}
            />
            {state.recoveryKey.length > 0 &&
              !isValidRecoveryKey(state.recoveryKey) && (
                <div className="signup__content__form__errormsg">
                  <svg className="signup__content__form__errormsg__icon">
                    <use
                      xlinkHref={ico16_indicator_warning}
                      href={ico16_indicator_warning}
                    />
                  </svg>
                  <span className="signup__content__form__errormsg__label">{`Not a valid Recovery Key`}</span>
                </div>
              )}
          </div>
          {state.error &&
            state.error.msg &&
            state.error.code === 8402 && (
              <div className="signup__content__form__errormsg">
                <svg className="signup__content__form__errormsg__icon">
                  <use
                    xlinkHref={ico16_indicator_warning}
                    href={ico16_indicator_warning}
                  />
                </svg>
                <span className="signup__content__form__errormsg__label">
                  {state.error.msg}
                </span>
              </div>
            )}
          <button
            className="won-button--filled secondary"
            disabled={!isValid()}
            onClick={resetPassword}
          >
            Reset Password
          </button>
        </div>
      </main>
    </WonGenericPage>
  );
}
