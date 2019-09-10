import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { actionCreators } from "../actions/actions.js";
import { get } from "../utils";
import * as accountUtils from "../redux/utils/account-utils";

const mapStateToProps = (state, ownProps) => {
  const accountState = get(state, "account");

  return {
    className: ownProps.className,
    email: accountUtils.getEmail(accountState),
    isAnonymous: accountUtils.isAnonymous(accountState),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    routerGo: (path, props) => {
      dispatch(actionCreators.router__stateGo(path, props));
    },
    hideMainMenu: () => {
      dispatch(actionCreators.view__hideMainMenu());
    },
    logout: () => {
      dispatch(actionCreators.account__logout());
    },
  };
};

class WonLoggedInMenu extends React.Component {
  constructor(props) {
    super(props);
    this.goToSettings = this.goToSettings.bind(this);
    this.goToSignUp = this.goToSignUp.bind(this);
  }
  render() {
    return (
      <won-logged-in-menu
        class={this.props.className ? this.props.className : ""}
      >
        <span
          className="wlim__userlabel show-in-responsive"
          title={this.props.isAnonymous ? "Anonymous" : this.props.email}
        >
          {this.props.isAnonymous ? "Anonymous" : this.props.email}
        </span>
        <hr className="show-in-responsive" />
        {this.props.isAnonymous && (
          <button
            className="won-button--outlined thin red wlim__button--signup"
            onClick={this.goToSignUp}
          >
            <span>Sign up</span>
          </button>
        )}
        <button
          className="won-button--outlined thin red"
          onClick={this.goToSettings}
        >
          <span>Account Settings</span>
        </button>
        <hr />
        <button
          className="won-button--filled lighterblue"
          style={{ width: "100%" }}
          onClick={this.props.logout}
        >
          <span>Sign out</span>
        </button>
      </won-logged-in-menu>
    );
  }

  goToSignUp() {
    this.props.hideMainMenu();
    this.props.routerGo("signup");
  }

  goToSettings() {
    this.props.hideMainMenu();
    this.props.routerGo("settings");
  }
}
WonLoggedInMenu.propTypes = {
  className: PropTypes.string,
  isAnonymous: PropTypes.bool,
  email: PropTypes.string,
  routerGo: PropTypes.func,
  hideMainMenu: PropTypes.func,
  logout: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonLoggedInMenu);
