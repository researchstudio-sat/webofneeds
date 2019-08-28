import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { actionCreators } from "../actions/actions.js";
import { get } from "../utils.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import WonLoggedInMenu from "./logged-in-menu.jsx";
import WonLoginForm from "./login-form.jsx";

import "~/style/_login.scss";
import "~/style/_covering-dropdown.scss";

const mapStateToProps = state => {
  const accountState = get(state, "account");

  return {
    loggedIn: accountUtils.isLoggedIn(accountState),
    email: accountUtils.getEmail(accountState),
    isAnonymous: accountUtils.isAnonymous(accountState),
    mainMenuVisible: state.getIn(["view", "showMainMenu"]),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    showMainMenu: () => {
      dispatch(actionCreators.view__showMainMenu());
    },
    hideMainMenu: () => {
      dispatch(actionCreators.view__hideMainMenu());
    },
  };
};

class WonAccountMenu extends React.Component {
  render() {
    return (
      <won-account-menu>
        <won-dropdown class="dd-right-aligned-header">
          <div
            className={
              "dd__open-button clickable topnav__button " +
              (!this.props.mainMenuVisible ? "dd--closed" : "")
            }
            onClick={this.props.showMainMenu}
          >
            <span className="topnav__button__caption hide-in-responsive">
              {this.props.loggedIn
                ? this.props.isAnonymous
                  ? "Anonymous"
                  : this.props.email
                : "Sign In"}
            </span>
            <svg className="topnav__button__icon">
              {this.props.isAnonymous ? (
                <use xlinkHref="#ico36_person_anon" href="#ico36_person_anon" />
              ) : (
                <use xlinkHref="#ico36_person" href="#ico36_person" />
              )}
            </svg>
          </div>
          {this.props.mainMenuVisible && (
            <div className="dd__dropdown">
              <div
                className="dd__open-button clickable topnav__button dd--open"
                onClick={this.props.hideMainMenu}
              >
                <span className="topnav__button__caption hide-in-responsive">
                  {this.props.loggedIn
                    ? this.props.isAnonymous
                      ? "Anonymous"
                      : this.props.email
                    : "Sign In"}
                </span>
                <svg className="topnav__button__icon">
                  {this.props.isAnonymous ? (
                    <use
                      xlinkHref="#ico36_person_anon"
                      href="#ico36_person_anon"
                    />
                  ) : (
                    <use xlinkHref="#ico36_person" href="#ico36_person" />
                  )}
                </svg>
              </div>
              <div className="dd__menu">
                {this.props.loggedIn ? (
                  <WonLoggedInMenu className="am__menu--loggedin" />
                ) : (
                  <WonLoginForm className="am__menu--loggedout" />
                )}
              </div>
            </div>
          )}
        </won-dropdown>
      </won-account-menu>
    );
  }
}
WonAccountMenu.propTypes = {
  loggedIn: PropTypes.bool,
  email: PropTypes.string,
  isAnonymous: PropTypes.bool,
  mainMenuVisible: PropTypes.bool,
  showMainMenu: PropTypes.func,
  hideMainMenu: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonAccountMenu);
