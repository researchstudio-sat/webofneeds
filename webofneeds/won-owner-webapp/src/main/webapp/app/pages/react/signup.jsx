import React, { useState, useEffect } from "react";
import { useSelector, useDispatch } from "react-redux";
import * as accountUtils from "../../redux/utils/account-utils.js";
import * as viewSelectors from "../../redux/selectors/view-selectors.js";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import WonModalDialog from "../../components/modal-dialog";
import WonTopnav from "../../components/topnav";
import WonMenu from "../../components/menu";
import WonToasts from "../../components/toasts";
import WonSlideIn from "../../components/slide-in";
import WonFooter from "../../components/footer";
import WonLabelledHr from "../../components/labelled-hr.jsx";

import "~/style/_signup.scss";
import ico16_indicator_warning from "~/images/won-icons/ico16_indicator_warning.svg";
import { actionCreators } from "../../actions/actions";
import { Link, useHistory } from "react-router-dom";

const MINPW_LENGTH = 6;

export default function PageSignUp() {
  const history = useHistory();
  const dispatch = useDispatch();

  const accountState = useSelector(generalSelectors.getAccountState);
  const privateId = accountUtils.getPrivateId(accountState);
  const isLoggedIn = accountUtils.isLoggedIn(accountState);
  const registerError = accountUtils.getRegisterError(accountState);
  const isAnonymous = accountUtils.isAnonymous(accountState);
  const showModalDialog = useSelector(viewSelectors.showModalDialog);
  const showSlideIns = useSelector(viewSelectors.showSlideIns(history));

  const [state, setState] = useState({
    email: "",
    validEmail: false,
    privateId: privateId,
    password: "",
    passwordAgain: "",
    rememberMe: false,
    acceptToS: false,
  });

  useEffect(
    () => {
      setState({
        ...state,
        privateId: privateId,
      });
    },
    [privateId]
  );

  function transfer() {
    dispatch(
      actionCreators.account__transfer({
        email: state.email,
        password: state.password,
        privateId: state.privateId,
        rememberMe: state.rememberMe,
      })
    );
  }

  function register() {
    dispatch(
      actionCreators.account__register({
        email: state.email,
        password: state.password,
        rememberMe: state.rememberMe,
      })
    );
  }

  function isValid() {
    if (!state.validEmail) {
      return false;
    }
    if (state.password.length < MINPW_LENGTH) {
      return false;
    }
    if (state.passwordAgain !== state.password) {
      return false;
    }
    if (!state.acceptToS) {
      return false;
    }

    return true;
  }

  /*function formKeyUp(event) {
    if (registerError) {
      dispatch(actionCreators.view__clearRegisterError());
    }
    if (event.keyCode == 13 && isValid()) {
      if (isAnonymous) {
        transfer();
      } else {
        register();
      }
    }
  }*/

  function changePassword(event) {
    setState({
      ...state,
      password: event.target.value,
    });
  }

  function changePasswordAgain(event) {
    setState({
      ...state,
      passwordAgain: event.target.value,
    });
  }

  function changeRememberMe(event) {
    setState({
      ...state,
      rememberMe: event.target.checked,
    });
  }

  function changeAcceptToS(event) {
    setState({
      ...state,
      acceptToS: event.target.checked,
    });
  }

  function changeEmail(event) {
    setState({
      ...state,
      email: event.target.value,
      validEmail: event.target.validity.valid,
    });
  }

  return (
    <section className={!isLoggedIn ? "won-signed-out" : ""}>
      {showModalDialog && <WonModalDialog />}
      <WonTopnav pageTitle="Sign Up" />
      {isLoggedIn && <WonMenu />}
      <WonToasts />
      {showSlideIns && <WonSlideIn />}
      <main className="signup" id="signupSection">
        <div className="signup__content">
          <div className="signup__content__form" name="registerForm">
            <input
              id="registerEmail"
              name="email"
              placeholder="Email address"
              className={registerError ? "ng-invalid" : ""}
              required
              type="email"
              onChange={changeEmail}
              value={state.email}
            />

            {state.email.length > 0 &&
              !state.validEmail && (
                <div className="signup__content__form__errormsg">
                  <svg className="signup__content__form__errormsg__icon">
                    <use
                      xlinkHref={ico16_indicator_warning}
                      href={ico16_indicator_warning}
                    />
                  </svg>
                  Not a valid E-Mail address
                </div>
              )}
            {registerError && (
              <div className="signup__content__form__errormsg">
                <svg className="signup__content__form__errormsg__icon">
                  <use
                    xlinkHref={ico16_indicator_warning}
                    href={ico16_indicator_warning}
                  />
                </svg>
                {registerError}
              </div>
            )}

            <input
              name="password"
              placeholder="Password"
              required
              type="password"
              onChange={changePassword}
              value={state.password}
            />

            {state.password.length > 0 &&
              state.password.length < MINPW_LENGTH && (
                <div className="signup__content__form__errormsg">
                  <svg className="signup__content__form__errormsg__icon">
                    <use
                      xlinkHref={ico16_indicator_warning}
                      href={ico16_indicator_warning}
                    />
                  </svg>
                  {"Password too short, must be at least " +
                    MINPW_LENGTH +
                    " Characters"}
                </div>
              )}

            <input
              name="password_repeat"
              placeholder="Repeat Password"
              required
              type="password"
              onChange={changePasswordAgain}
              value={state.passwordAgain}
            />

            {state.passwordAgain.length > 0 &&
              state.password !== state.passwordAgain && (
                <div className="signup__content__form__errormsg">
                  <svg className="signup__content__form__errormsg__icon">
                    <use
                      xlinkHref={ico16_indicator_warning}
                      href={ico16_indicator_warning}
                    />
                  </svg>
                  Password is not equal
                </div>
              )}

            <div>
              <input
                id="rememberMe"
                type="checkbox"
                onChange={changeRememberMe}
                value={state.rememberMe}
              />
              <label htmlFor="rememberMe">remember me</label>
            </div>
            <div>
              <input
                id="acceptToS"
                type="checkbox"
                required
                value={state.acceptToS}
                onChange={changeAcceptToS}
              />
              <label htmlFor="acceptToS">
                I accept the{" "}
                <Link
                  className="clickable"
                  to="/about?aboutSection=aboutTermsOfService"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Terms Of Service
                </Link>
              </label>
            </div>
          </div>
          {isAnonymous && (
            <button
              className="won-button--filled red"
              disabled={!isValid()}
              onClick={transfer}
            >
              <span>Keep Postings</span>
            </button>
          )}
          {isAnonymous && <WonLabelledHr label="or" className="labelledHr" />}
          <button
            className="won-button--filled red"
            disabled={!isValid()}
            onClick={register}
          >
            <span>
              {isAnonymous
                ? "Start from Scratch"
                : "That’s all we need. Let’s go!"}
            </span>
          </button>
        </div>
      </main>
      <WonFooter />
    </section>
  );
}
