import React, { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { actionCreators } from "../actions/actions.js";

import "~/style/_menu.scss";
import ico16_indicator_warning from "~/images/won-icons/ico16_indicator_warning.svg";
import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";
import * as viewSelectors from "../redux/selectors/view-selectors.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import Immutable from "immutable";
import { NavLink, useHistory } from "react-router-dom";

export default function WonMenu() {
  const dispatch = useDispatch();
  const history = useHistory();
  const hasSlideIns = useSelector(state =>
    viewSelectors.hasSlideIns(state, history)
  );
  const isMenuVisible = useSelector(viewSelectors.isMenuVisible);
  const isSlideInsVisible = useSelector(viewSelectors.isSlideInsVisible);
  const isLocationAccessDenied = useSelector(
    generalSelectors.isLocationAccessDenied
  );
  const hasChatConnections = useSelector(generalSelectors.hasChatConnections);
  const hasUnreadSuggestedConnections = useSelector(
    generalSelectors.hasUnreadSuggestedConnections
  );
  const hasUnreadBuddyConnections = useSelector(state =>
    generalSelectors.hasUnreadBuddyConnections(state, true, false)
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

    hasSlideIns && classes.push("won-has-slideins");
    isMenuVisible && classes.push("won-menu--show-mobile");

    return classes.join(" ");
  }

  function toggleSlideIns() {
    hideMenuIfVisible();
    dispatch(actionCreators.view__toggleSlideIns());
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
          className={generateTabClasses(
            !hasChatConnections,
            hasUnreadChatConnections
          )}
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
        {hasSlideIns ? (
          <div
            className="menu__slideintoggle hide-in-responsive"
            onClick={toggleSlideIns}
          >
            <svg className="menu__slideintoggle__icon">
              <use
                xlinkHref={ico16_indicator_warning}
                href={ico16_indicator_warning}
              />
            </svg>
            <svg
              className={
                "menu__slideintoggle__carret " +
                (isSlideInsVisible
                  ? " menu__slideintoggle__carret--expanded "
                  : "")
              }
            >
              <use xlinkHref={ico16_arrow_down} href={ico16_arrow_down} />
            </svg>
            <span className="menu__slideintoggle__label">
              {isSlideInsVisible ? (
                <span>Hide Info Slide-Ins</span>
              ) : (
                <span>Show Info Slide-Ins</span>
              )}
            </span>
          </div>
        ) : (
          undefined
        )}
      </div>
    </won-menu>
  );
}
