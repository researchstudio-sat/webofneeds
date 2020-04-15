import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { actionCreators } from "../actions/actions.js";
import { get } from "../utils";
import * as accountUtils from "../redux/utils/account-utils";
import { Link, withRouter } from "react-router-dom";

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
    hideMainMenu: () => {
      dispatch(actionCreators.view__hideMainMenu());
    },
    logout: history => {
      dispatch(actionCreators.account__logout(history));
    },
  };
};

class WonLoggedInMenu extends React.Component {
  constructor(props) {
    super(props);
    this.closeMenu = this.closeMenu.bind(this);
    this.logout = this.logout.bind(this);
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
          <Link
            className="won-button--outlined thin red wlim__button--signup"
            onClick={this.closeMenu}
            to="/signup"
          >
            <span>Sign up</span>
          </Link>
        )}
        <Link
          className="won-button--outlined thin red"
          onClick={this.closeMenu}
          to="/settings"
        >
          <span>Account Settings</span>
        </Link>
        <hr />
        <button
          className="won-button--filled lighterblue"
          style={{ width: "100%" }}
          onClick={this.logout}
        >
          <span>Sign out</span>
        </button>
      </won-logged-in-menu>
    );
  }

  logout() {
    this.props.logout(this.props.history);
  }

  closeMenu() {
    this.props.hideMainMenu();
  }
}
WonLoggedInMenu.propTypes = {
  className: PropTypes.string,
  isAnonymous: PropTypes.bool,
  email: PropTypes.string,
  hideMainMenu: PropTypes.func,
  logout: PropTypes.func,
  history: PropTypes.object,
};

export default withRouter(
  connect(
    mapStateToProps,
    mapDispatchToProps
  )(WonLoggedInMenu)
);
