import React, { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { actionCreators } from "../actions/actions.js";
import { getIn } from "../utils.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import WonLoggedInMenu from "./logged-in-menu.jsx";
import WonLoginForm from "./login-form.jsx";

import "~/style/_login.scss";
import "~/style/_account-menu.scss";
import ico36_person from "~/images/won-icons/ico36_person.svg";
import ico36_person_anon from "~/images/won-icons/ico36_person_anon.svg";

export default function WonAccountMenu() {
  const dispatch = useDispatch();
  const accountState = useSelector(generalSelectors.getAccountState);
  const loggedIn = accountUtils.isLoggedIn(accountState);
  const email = accountUtils.getEmail(accountState);
  const isAnonymous = accountUtils.isAnonymous(accountState);
  const mainMenuVisible = useSelector(state =>
    getIn(state, ["view", "showMainMenu"])
  );

  let thisNode;
  useEffect(() => {
    function handleClick(e) {
      if (!thisNode.contains(e.target) && mainMenuVisible) {
        dispatch(actionCreators.view__hideMainMenu());

        return;
      }
    }
    document.addEventListener("mousedown", handleClick, false);

    return function cleanup() {
      document.removeEventListener("mousedown", handleClick, false);
    };
  });

  return (
    <won-account-menu
      class={mainMenuVisible ? " wam--open " : " wam--closed "}
      ref={node => (thisNode = node)}
    >
      <div
        className="wam__header clickable"
        onClick={
          mainMenuVisible
            ? () => dispatch(actionCreators.view__hideMainMenu())
            : () => dispatch(actionCreators.view__showMainMenu())
        }
      >
        <span className="wam__header__caption hide-in-responsive">
          {loggedIn ? (isAnonymous ? "Anonymous" : email) : "Sign In"}
        </span>
        <svg className="wam__header__icon">
          {isAnonymous ? (
            <use xlinkHref={ico36_person_anon} href={ico36_person_anon} />
          ) : (
            <use xlinkHref={ico36_person} href={ico36_person} />
          )}
        </svg>
      </div>
      {mainMenuVisible && (
        <div className="wam__content">
          {loggedIn ? <WonLoggedInMenu /> : <WonLoginForm />}
        </div>
      )}
    </won-account-menu>
  );
}
