import React, { useEffect } from "react";
import PropTypes from "prop-types";
import { useDispatch, useSelector } from "react-redux";
import { actionCreators } from "../actions/actions.js";

import "~/style/_menu.scss";
import * as viewSelectors from "../redux/selectors/view-selectors.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import Immutable from "immutable";
import { NavLink, useHistory } from "react-router-dom";
import ico36_person_anon from "~/images/won-icons/ico36_person_anon.svg";
import ico36_person from "~/images/won-icons/ico36_person.svg";

export default function WonMenu({ className }) {
  const dispatch = useDispatch();
  const history = useHistory();
  const hasSlideIns = useSelector(viewSelectors.hasSlideIns(history));
  const isMenuVisible = useSelector(viewSelectors.isMenuVisible);
  const accountState = useSelector(generalSelectors.getAccountState);

  const email = accountUtils.getEmail(accountState);
  const isAnonymous = accountUtils.isAnonymous(accountState);
  const isLocationAccessDenied = useSelector(
    generalSelectors.isLocationAccessDenied
  );
  const hasUnreadSuggestedConnections = useSelector(
    generalSelectors.hasUnreadSuggestedConnections
  );
  const hasUnreadBuddyConnections = useSelector(
    generalSelectors.hasUnreadBuddyConnections(true, false)
  );
  const hasUnreadChatConnections = useSelector(
    generalSelectors.hasUnreadChatConnections
  );

  function generateTabClasses(inactive = false, unread = false) {
    const classes = ["menu__tab"];

    inactive && classes.push("menu__tab--inactive");
    unread && classes.push("menu__tab--unread");

    return classes.join(" ");
  }

  function generateRootClasses() {
    const classes = [];
    className && classes.push(className);
    hasSlideIns && classes.push("won-has-slideins");
    isMenuVisible && classes.push("won-menu--show-mobile");

    return classes.join(" ");
  }

  function hideMenuIfVisible() {
    if (isMenuVisible) {
      dispatch(actionCreators.view__hideMenu());
    }
  }

  function viewWhatsAround() {
    viewWhatsX(() => {
      hideMenuIfVisible();
    });
  }

  function logout() {
    dispatch(actionCreators.account__logout(history));
  }

  function viewWhatsNew() {
    viewWhatsX(() => {
      hideMenuIfVisible();
    });
  }

  function viewWhatsX(callback) {
    if (isLocationAccessDenied) {
      callback();
    } else if ("geolocation" in navigator) {
      navigator.geolocation.getCurrentPosition(
        currentLocation => {
          const lat = currentLocation.coords.latitude;
          const lng = currentLocation.coords.longitude;

          dispatch(
            actionCreators.view__updateCurrentLocation(
              Immutable.fromJS({ location: { lat, lng } })
            )
          );
          callback();
        },
        error => {
          //error handler
          console.error(
            "Could not retrieve geolocation due to error: ",
            error.code,
            ", continuing map initialization without currentLocation. fullerror:",
            error
          );
          dispatch(actionCreators.view__locationAccessDenied());
          callback();
        },
        {
          //options
          enableHighAccuracy: true,
          maximumAge: 30 * 60 * 1000, //use if cache is not older than 30min
        }
      );
    } else {
      console.error("location could not be retrieved");
      dispatch(actionCreators.view__locationAccessDenied());
      callback();
    }
  }

  let thisNode;
  useEffect(() => {
    function handleClick(e) {
      if (thisNode && !thisNode.contains(e.target) && isMenuVisible) {
        // TODO: Fix me.
        // Handler is closing menu before actual MenuAction is toggled
        // dispatch(actionCreators.view__hideMenu());

        return;
      }
    }
    document.addEventListener("mousedown", handleClick, false);

    return function cleanup() {
      document.removeEventListener("mousedown", handleClick, false);
    };
  });

  return (
    <won-menu class={generateRootClasses()} ref={node => (thisNode = node)}>
      <div className="menu">
        <div className="menu__user">
          {isAnonymous ? (
            <React.Fragment>
              <svg className="menu__user__icon">
                <use xlinkHref={ico36_person_anon} href={ico36_person_anon} />
              </svg>
              <span className="menu__user__caption">Anonymous</span>
            </React.Fragment>
          ) : (
            <React.Fragment>
              <svg className="menu__user__icon">
                <use xlinkHref={ico36_person} href={ico36_person} />
              </svg>
              <span className="menu__user__caption">{email}</span>
            </React.Fragment>
          )}
        </div>
        <NavLink
          className={generateTabClasses(
            false,
            hasUnreadSuggestedConnections || hasUnreadBuddyConnections
          )}
          activeClassName="menu__tab--selected"
          onClick={hideMenuIfVisible}
          to="/inventory"
        >
          <span className="menu__tab__unread" />
          <span className="menu__tab__label">Inventory</span>
        </NavLink>
        <NavLink
          className={generateTabClasses(false, hasUnreadChatConnections)}
          activeClassName="menu__tab--selected"
          onClick={hideMenuIfVisible}
          to="/connections"
        >
          <span className="menu__tab__unread" />
          <span className="menu__tab__label">Chats</span>
        </NavLink>
        <NavLink
          className={generateTabClasses()}
          activeClassName="menu__tab--selected"
          onClick={hideMenuIfVisible}
          to="/create"
        >
          <span className="menu__tab__label">Create</span>
        </NavLink>
        <NavLink
          className={generateTabClasses()}
          activeClassName="menu__tab--selected"
          onClick={viewWhatsNew}
          to="/overview"
        >
          <span className="menu__tab__label">{"What's New"}</span>
        </NavLink>
        <NavLink
          className={generateTabClasses()}
          activeClassName="menu__tab--selected"
          onClick={viewWhatsAround}
          to="/map"
        >
          <span className="menu__tab__label">{"What's Around"}</span>
        </NavLink>
        <NavLink
          className={generateTabClasses()}
          activeClassName="menu__tab--selected"
          onClick={hideMenuIfVisible}
          to="/settings"
        >
          <span className="menu__tab__label">Account Settings</span>
        </NavLink>
        {isAnonymous && (
          <NavLink
            className={generateTabClasses()}
            activeClassName="menu__tab--selected"
            onClick={hideMenuIfVisible}
            to="/signup"
          >
            <span>Sign up</span>
          </NavLink>
        )}
        <button
          className="menu__signout won-button--filled secondary"
          onClick={logout}
        >
          <span>Sign out</span>
        </button>
      </div>
    </won-menu>
  );
}
WonMenu.propTypes = {
  className: PropTypes.string,
};
