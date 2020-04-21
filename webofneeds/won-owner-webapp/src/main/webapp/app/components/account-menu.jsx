import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { actionCreators } from "../actions/actions.js";
import { get, getIn } from "../utils.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import WonLoggedInMenu from "./logged-in-menu.jsx";
import WonLoginForm from "./login-form.jsx";

import "~/style/_login.scss";
import "~/style/_account-menu.scss";
import ico36_person from "~/images/won-icons/ico36_person.svg";
import ico36_person_anon from "~/images/won-icons/ico36_person_anon.svg";

const mapStateToProps = state => {
  const accountState = get(state, "account");

  return {
    loggedIn: accountUtils.isLoggedIn(accountState),
    email: accountUtils.getEmail(accountState),
    isAnonymous: accountUtils.isAnonymous(accountState),
    mainMenuVisible: getIn(state, ["view", "showMainMenu"]),
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
  constructor(props) {
    super(props);
    this.handleClick = this.handleClick.bind(this);
  }
  render() {
    return (
      <won-account-menu
        class={this.props.mainMenuVisible ? " wam--open " : " wam--closed "}
        ref={node => (this.node = node)}
      >
        <div
          className="wam__header clickable"
          onClick={
            this.props.mainMenuVisible
              ? this.props.hideMainMenu
              : this.props.showMainMenu
          }
        >
          <span className="wam__header__caption hide-in-responsive">
            {this.props.loggedIn
              ? this.props.isAnonymous
                ? "Anonymous"
                : this.props.email
              : "Sign In"}
          </span>
          <svg className="wam__header__icon">
            {this.props.isAnonymous ? (
              <use xlinkHref={ico36_person_anon} href={ico36_person_anon} />
            ) : (
              <use xlinkHref={ico36_person} href={ico36_person} />
            )}
          </svg>
        </div>
        {this.props.mainMenuVisible && (
          <div className="wam__content">
            {this.props.loggedIn ? <WonLoggedInMenu /> : <WonLoginForm />}
          </div>
        )}
      </won-account-menu>
    );
  }

  componentWillMount() {
    document.addEventListener("mousedown", this.handleClick, false);
  }
  componentWillUnmount() {
    document.removeEventListener("mousedown", this.handleClick, false);
  }

  handleClick(e) {
    if (!this.node.contains(e.target) && this.props.mainMenuVisible) {
      this.props.hideMainMenu();

      return;
    }
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
