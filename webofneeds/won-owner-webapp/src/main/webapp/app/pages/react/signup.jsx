import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { get } from "../../utils.js";
import * as accountUtils from "../../redux/utils/account-utils.js";
import * as viewSelectors from "../../redux/selectors/view-selectors.js";
import WonModalDialog from "../../components/modal-dialog";
import WonTopnav from "../../components/topnav";
import WonMenu from "../../components/menu";
import WonToasts from "../../components/toasts";
import WonSlideIn from "../../components/slide-in";
import WonFooter from "../../components/footer";
import WonLabelledHr from "../../components/labelled-hr.jsx";

import "~/style/_signup.scss";
import { actionCreators } from "../../actions/actions";

const mapStateToProps = state => {
  const accountState = get(state, "account");
  return {
    isLoggedIn: accountUtils.isLoggedIn(accountState),
    registerError: accountUtils.getRegisterError(accountState),
    isAnonymous: accountUtils.isAnonymous(accountState),
    privateId: accountUtils.getPrivateId(accountState),
    showModalDialog: state.getIn(["view", "showModalDialog"]),
    showSlideIns:
      viewSelectors.hasSlideIns(state) &&
      viewSelectors.isSlideInsVisible(state),
  };
};
const mapDispatchToProps = dispatch => {
  return {
    routerGo: (path, props) => {
      dispatch(actionCreators.router__stateGo(path, props));
    },
    clearRegisterError: () => {
      dispatch(actionCreators.view__clearRegisterError());
    },
    register: (email, password, rememberMe) => {
      dispatch(
        actionCreators.account__register({
          email: email,
          password: password,
          rememberMe: rememberMe,
        })
      );
    },
    transfer: (email, password, rememberMe, privateId) => {
      dispatch(
        actionCreators.account__transfer({
          email: email,
          password: password,
          privateId: privateId,
          rememberMe: rememberMe,
        })
      );
    },
  };
};

const MINPW_LENGTH = 6;

class PageSignUp extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      email: "",
      validEmail: false,
      password: "",
      passwordAgain: "",
      rememberMe: false,
      acceptToS: false,
    };

    this.register = this.register.bind(this);
    this.transfer = this.transfer.bind(this);
    this.formKeyUp = this.formKeyUp.bind(this);
    this.changeEmail = this.changeEmail.bind(this);
    this.changePassword = this.changePassword.bind(this);
    this.changePasswordAgain = this.changePasswordAgain.bind(this);
    this.changeRememberMe = this.changeRememberMe.bind(this);
    this.changeAcceptToS = this.changeAcceptToS.bind(this);
    this.goToToS = this.goToToS.bind(this);
  }

  render() {
    return (
      <section className={!this.props.isLoggedIn ? "won-signed-out" : ""}>
        {this.props.showModalDialog && <WonModalDialog />}
        <WonTopnav pageTitle="Sign Up" />
        {this.props.isLoggedIn && <WonMenu />}
        <WonToasts />
        {this.props.showSlideIns && <WonSlideIn />}
        <main className="signup" id="signupSection">
          <div className="signup__content">
            <div className="signup__content__form" name="registerForm">
              <input
                id="registerEmail"
                name="email"
                placeholder="Email address"
                className={this.props.registerError ? "ng-invalid" : ""}
                required
                type="email"
                onChange={this.changeEmail}
                value={this.state.email}
              />

              {this.state.email.length > 0 &&
                !this.state.validEmail && (
                  <div className="signup__content__form__errormsg">
                    <svg className="signup__content__form__errormsg__icon">
                      <use
                        xlinkHref="#ico16_indicator_warning"
                        href="#ico16_indicator_warning"
                      />
                    </svg>
                    Not a valid E-Mail address
                  </div>
                )}
              {this.props.registerError && (
                <div className="signup__content__form__errormsg">
                  <svg className="signup__content__form__errormsg__icon">
                    <use
                      xlinkHref="#ico16_indicator_warning"
                      href="#ico16_indicator_warning"
                    />
                  </svg>
                  {this.props.registerError}
                </div>
              )}

              <input
                name="password"
                placeholder="Password"
                required
                type="password"
                onChange={this.changePassword}
                value={this.state.password}
              />

              {this.state.password.length > 0 &&
                this.state.password.length < MINPW_LENGTH && (
                  <div className="signup__content__form__errormsg">
                    <svg className="signup__content__form__errormsg__icon">
                      <use
                        xlinkHref="#ico16_indicator_warning"
                        href="#ico16_indicator_warning"
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
                onChange={this.changePasswordAgain}
                value={this.state.passwordAgain}
              />

              {this.state.passwordAgain.length > 0 &&
                this.state.password !== this.state.passwordAgain && (
                  <div className="signup__content__form__errormsg">
                    <svg className="signup__content__form__errormsg__icon">
                      <use
                        xlinkHref="#ico16_indicator_warning"
                        href="#ico16_indicator_warning"
                      />
                    </svg>
                    Password is not equal
                  </div>
                )}

              <div>
                <input
                  id="rememberMe"
                  type="checkbox"
                  onChange={this.changeRememberMe}
                  value={this.state.rememberMe}
                />
                <label htmlFor="rememberMe">remember me</label>
              </div>
              <div>
                <input
                  id="acceptToS"
                  type="checkbox"
                  required
                  value={this.state.acceptToS}
                  onChange={this.changeAcceptToS}
                />
                <label htmlFor="acceptToS">
                  I accept the{" "}
                  <a className="clickable" onChange={this.goToToS}>
                    Terms Of Service
                  </a>
                </label>
              </div>
            </div>
            {this.props.isAnonymous && (
              <button
                className="won-button--filled red"
                disabled={!this.isValid()}
                onClick={this.transfer}
              >
                <span>Keep Postings</span>
              </button>
            )}
            {this.props.isAnonymous && (
              <WonLabelledHr label="or" className="labelledHr" />
            )}
            <button
              className="won-button--filled red"
              disabled={!this.isValid()}
              onClick={this.register}
            >
              <span>
                {this.props.isAnonymous
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

  goToToS() {
    this.props.routerGo("about", { aboutSection: "aboutTermsOfService" });
  }

  transfer() {
    this.props.transfer(
      this.state.email,
      this.state.password,
      this.state.rememberMe,
      this.props.privateId
    );
  }

  register() {
    this.props.register(
      this.state.email,
      this.state.password,
      this.state.rememberMe
    );
  }

  isValid() {
    if (!this.state.validEmail) {
      return false;
    }
    if (this.state.password.length < MINPW_LENGTH) {
      return false;
    }
    if (this.state.passwordAgain !== this.state.password) {
      return false;
    }
    if (!this.state.acceptToS) {
      return false;
    }

    return true;
  }

  formKeyUp(event) {
    if (this.props.registerError) {
      this.props.clearRegisterError();
    }
    if (event.keyCode == 13 && this.isValid()) {
      if (this.props.isAnonymous) {
        this.props.transfer();
      } else {
        this.props.register();
      }
    }
  }

  changePassword(event) {
    this.setState({
      password: event.target.value,
    });
  }

  changePasswordAgain(event) {
    this.setState({
      passwordAgain: event.target.value,
    });
  }

  changeRememberMe(event) {
    this.setState({
      rememberMe: event.target.checked,
    });
  }

  changeAcceptToS(event) {
    this.setState({
      acceptToS: event.target.checked,
    });
  }

  changeEmail(event) {
    this.setState({
      email: event.target.value,
      validEmail: event.target.validity.valid,
    });
  }
}
PageSignUp.propTypes = {
  isLoggedIn: PropTypes.bool,
  registerError: PropTypes.string,
  isAnonymous: PropTypes.bool,
  privateId: PropTypes.string,
  showModalDialog: PropTypes.bool,
  showSlideIns: PropTypes.bool,
  register: PropTypes.func,
  transfer: PropTypes.func,
  routerGo: PropTypes.func,
  clearRegisterError: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(PageSignUp);
