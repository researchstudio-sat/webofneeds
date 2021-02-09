import React, { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { actionCreators } from "../actions/actions.js";
import { getIn } from "../utils.js";
import WonLoginForm from "./login-form.jsx";

import "~/style/_login.scss";
import "~/style/_login-menu.scss";
import ico36_person from "~/images/won-icons/ico36_person.svg";

export default function WonLoginMenu() {
  const dispatch = useDispatch();
  const mainMenuVisible = useSelector(state =>
    getIn(state, ["view", "showMainMenu"])
  );

  let thisNode;
  useEffect(() => {
    function handleClick(e) {
      if (thisNode && !thisNode.contains(e.target) && mainMenuVisible) {
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
      class={mainMenuVisible ? " wlm--open " : " wlm--closed "}
      ref={node => (thisNode = node)}
    >
      <div
        className="wlm__header clickable"
        onClick={
          mainMenuVisible
            ? () => dispatch(actionCreators.view__hideMainMenu())
            : () => dispatch(actionCreators.view__showMainMenu())
        }
      >
        <span className="wlm__header__caption hide-in-responsive">Sign In</span>
        <svg className="wlm__header__icon">
          <use xlinkHref={ico36_person} href={ico36_person} />
        </svg>
      </div>
      {mainMenuVisible && (
        <div className="wlm__content">
          <WonLoginForm />
        </div>
      )}
    </won-account-menu>
  );
}
