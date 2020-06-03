import React from "react";
import PropTypes from "prop-types";
import { useDispatch, useSelector } from "react-redux";
import { actionCreators } from "../actions/actions.js";
import * as generalSelectors from "../redux/selectors/general-selectors";
import * as accountUtils from "../redux/utils/account-utils";
import { Link, useHistory } from "react-router-dom";

export default function WonLoggedInMenu({ className }) {
  const history = useHistory();
  const dispatch = useDispatch();
  const accountState = useSelector(generalSelectors.getAccountState);
  const email = accountUtils.getEmail(accountState);
  const isAnonymous = accountUtils.isAnonymous(accountState);

  function logout() {
    dispatch(actionCreators.account__logout(history));
  }

  function closeMenu() {
    dispatch(actionCreators.view__hideMainMenu());
  }

  return (
    <won-logged-in-menu class={className ? className : ""}>
      <span
        className="wlim__userlabel show-in-responsive"
        title={isAnonymous ? "Anonymous" : email}
      >
        {isAnonymous ? "Anonymous" : email}
      </span>
      <hr className="show-in-responsive" />
      {isAnonymous && (
        <Link
          className="won-button--outlined thin red wlim__button--signup"
          onClick={closeMenu}
          to="/signup"
        >
          <span>Sign up</span>
        </Link>
      )}
      <Link
        className="won-button--outlined thin red"
        onClick={closeMenu}
        to="/settings"
      >
        <span>Account Settings</span>
      </Link>
      <hr />
      <button
        className="won-button--filled lighterblue"
        style={{ width: "100%" }}
        onClick={logout}
      >
        <span>Sign out</span>
      </button>
    </won-logged-in-menu>
  );
}
WonLoggedInMenu.propTypes = {
  className: PropTypes.string,
};
