import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { actionCreators } from "../actions/actions.js";
import ReactMarkdown from "react-markdown";
import { parseRestErrorMessage } from "../won-utils.js";
import { get, getIn } from "../utils.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import won from "../won-es6";

import "~/style/_won-markdown.scss";
import WonLabelledHr from "./labelled-hr";
import { Link } from "react-router-dom";

const mapStateToProps = (state, ownProps) => {
  const accountState = get(state, "account");
  const loginError = accountUtils.getLoginError(accountState);
  const isNotVerified =
    get(loginError, "code") === won.RESPONSECODE.USER_NOT_VERIFIED;

  return {
    className: ownProps.className,
    loginError,
    processingResendVerificationEmail: getIn(state, [
      "process",
      "processingResendVerificationEmail",
    ]),
    isNotVerified,
  };
};

const mapDispatchToProps = dispatch => {
  return {
    accountResendVerificationEmail: email => {
      dispatch(actionCreators.account__resendVerificationEmail(email));
    },
    hideMainMenu: () => {
      dispatch(actionCreators.view__hideMainMenu());
    },
    login: (email, password, rememberMe) => {
      dispatch(
        actionCreators.account__login({
          email: email,
          password: password,
          rememberMe: rememberMe,
        })
      );
    },
    clearLoginError: () => {
      dispatch(actionCreators.view__clearLoginError());
    },
  };
};

class WonLoginForm extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      email: "",
      password: "",
      rememberMe: false,
    };

    this.closeMenu = this.closeMenu.bind(this);
    this.formKeyUp = this.formKeyUp.bind(this);
    this.changeEmail = this.changeEmail.bind(this);
    this.changePassword = this.changePassword.bind(this);
    this.changeRememberMe = this.changeRememberMe.bind(this);
  }

  render() {
    return (
      <won-login-form class={this.props.className ? this.props.className : ""}>
        <form
          onSubmit={e => {
            e.preventDefault();
            this.props.login(
              this.state.email,
              this.state.password,
              this.state.rememberMe
            );
          }}
          id="loginForm"
          className="loginForm"
        >
          <input
            id="loginEmail"
            placeholder="Email address"
            value={this.state.email}
            type="email"
            required
            autoFocus
            onKeyUp={this.formKeyUp}
            onChange={this.changeEmail}
          />
          {!!this.props.loginError && (
            <ReactMarkdown
              className="wl__errormsg markdown"
              source={parseRestErrorMessage(this.props.loginError)}
            />
          )}
          {this.props.isNotVerified &&
            (!this.props.processingResendVerificationEmail ? (
              <a
                className="wl__errormsg__resend"
                onClick={() =>
                  this.props.accountResendVerificationEmail(this.state.email)
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
            value={this.state.password}
            type="password"
            required
            onKeyUp={this.formKeyUp}
            onChange={this.changePassword}
          />
          <button
            className="won-button--filled lighterblue"
            disabled={this.state.password === "" || this.state.email === ""}
          >
            Sign In
          </button>
          <input
            id="remember-me"
            value={this.state.rememberMe}
            onChange={this.changeRememberMe}
            type="checkbox"
          />
          {" Remember me"}
        </form>
        <WonLabelledHr label="Or" />
        <div className="wl__register">
          <Link
            className="won-button--filled red"
            onClick={this.closeMenu}
            to="/signup"
          >
            Sign up
          </Link>
        </div>
      </won-login-form>
    );
  }

  closeMenu() {
    this.props.hideMainMenu();
  }

  formKeyUp(event) {
    if (this.props.loginError) {
      this.props.clearLoginError();
    }
    if (event.keyCode == 13) {
      this.props.login(
        this.state.email,
        this.state.password,
        this.state.rememberMe
      );
    }
  }

  changePassword(event) {
    this.setState({
      password: event.target.value,
    });
  }
  changeRememberMe(event) {
    this.setState({
      rememberMe: event.target.checked,
    });
  }
  changeEmail(event) {
    this.setState({
      email: event.target.value,
    });
  }
}
WonLoginForm.propTypes = {
  className: PropTypes.string,
  loginError: PropTypes.string,
  processingResendVerificationEmail: PropTypes.bool,
  isNotVerified: PropTypes.bool,
  accountResendVerificationEmail: PropTypes.func,
  hideMainMenu: PropTypes.func,
  login: PropTypes.func,
  clearLoginError: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonLoginForm);
