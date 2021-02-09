import React, { useEffect } from "react";
import PropTypes from "prop-types";
import { useDispatch, useSelector } from "react-redux";
import { actionCreators } from "../actions/actions.js";
import { get, getUri, getIn, getPathname } from "../utils.js";
import * as viewSelectors from "../redux/selectors/view-selectors.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as processSelectors from "../redux/selectors/process-selectors.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import WonLoginMenu from "./login-menu.jsx";

import "~/style/_responsiveness-utils.scss";
import "~/style/_topnav.scss";
import ico16_burger from "~/images/won-icons/ico16_burger.svg";
import ico16_indicator_warning from "~/images/won-icons/ico16_indicator_warning.svg";
import ico_loading_anim from "~/images/won-icons/ico_loading_anim.svg";
import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";
import { Link, useHistory } from "react-router-dom";

export default function WonTopnav({ pageTitle }) {
  const history = useHistory();
  const dispatch = useDispatch();
  const currentPath = getPathname(history.location);
  const accountState = useSelector(generalSelectors.getAccountState);
  const theme = useSelector(generalSelectors.getTheme);
  const hasSlideIns = useSelector(viewSelectors.hasSlideIns(history));
  const isSlideInsVisible = useSelector(viewSelectors.isSlideInsVisible);
  const mainMenuVisible = useSelector(state =>
    getIn(state, ["view", "showMainMenu"])
  );
  const isMenuVisible = useSelector(viewSelectors.isMenuVisible);
  const themeName = get(theme, "name");
  const appTitle = get(theme, "title");
  const loggedIn = accountUtils.isLoggedIn(accountState);
  const isSignUpView = currentPath === "/signup";
  const showLoadingIndicator = useSelector(processSelectors.isLoading);
  const connectionsToCrawl = useSelector(
    generalSelectors.getConnectionsToCrawl
  );
  const hasUnreads = useSelector(
    state =>
      generalSelectors.hasUnreadSuggestedConnections(state) ||
      generalSelectors.hasUnreadBuddyConnections(true, false)(state) ||
      generalSelectors.hasUnreadChatConnections(state)
  );

  useEffect(() => {
    const MESSAGECOUNT = 3;

    if (connectionsToCrawl && connectionsToCrawl.size > 0) {
      console.debug(
        "connectionsToCrawl: ",
        connectionsToCrawl,
        " Size: ",
        connectionsToCrawl.size
      );
      connectionsToCrawl
        .filter(conn => connectionUtils.getMessagesSize(conn) === 0)
        .map(conn => {
          dispatch(
            actionCreators.connections__showLatestMessages(
              getUri(conn),
              MESSAGECOUNT
            )
          );
        });
    }
  });

  function toggleSlideIns() {
    hideMenu();
    dispatch(actionCreators.view__toggleSlideIns());
  }

  function hideMenu() {
    if (isMenuVisible) {
      dispatch(actionCreators.view__hideMenu());
    }
    return true;
  }

  function goDefault() {
    hideMenu();
    history.push("/");
  }

  function menuAction() {
    if (loggedIn) {
      dispatch(actionCreators.view__toggleMenu());
    } else {
      hideMenu();
      history.push("/");
    }
  }

  return (
    <won-topnav>
      <nav className="topnav">
        <svg
          className={
            "topnav__menuicon clickable " +
            (!loggedIn ? " topnav__menuicon--hide " : "") +
            (isMenuVisible ? " topnav__menuicon--show " : "")
          }
          onClick={menuAction}
        >
          <use xlinkHref={ico16_burger} href={ico16_burger} />
        </svg>
        <div className="topnav__logo clickable">
          <img
            src={"skin/" + themeName + "/images/logo.svg"}
            className="topnav__logo__image hide-in-responsive"
            onClick={goDefault}
            alt="logo"
          />
          <img
            src={"skin/" + themeName + "/images/logo.svg"}
            className="topnav__logo__image show-in-responsive"
            onClick={menuAction}
            alt="logo"
          />
          {hasUnreads &&
            !isMenuVisible && (
              <span className="topnav__logo__unreads show-in-responsive" />
            )}
        </div>
        <div className="topnav__title">
          {isMenuVisible ? (
            <span className="topnav__page-title" onClick={menuAction}>
              Menu
            </span>
          ) : (
            <React.Fragment>
              <span
                className="topnav__app-title hide-in-responsive"
                onClick={goDefault}
              >
                {appTitle}
              </span>
              {pageTitle && (
                <React.Fragment>
                  <span className="topnav__divider hide-in-responsive">
                    &mdash;
                  </span>
                  <span className="topnav__page-title">{pageTitle}</span>
                </React.Fragment>
              )}
            </React.Fragment>
          )}
        </div>
        {showLoadingIndicator && (
          <div className="topnav__loading">
            <svg className="topnav__loading__spinner hspinner">
              <use xlinkHref={ico_loading_anim} href={ico_loading_anim} />
            </svg>
          </div>
        )}
        {hasSlideIns && (
          <div className="topnav__slideintoggle" onClick={toggleSlideIns}>
            <svg className="topnav__slideintoggle__icon">
              <use
                xlinkHref={ico16_indicator_warning}
                href={ico16_indicator_warning}
              />
            </svg>
            <svg
              className={
                "topnav__slideintoggle__carret " +
                (isSlideInsVisible
                  ? " topnav__slideintoggle__carret--expanded "
                  : "")
              }
            >
              <use xlinkHref={ico16_arrow_down} href={ico16_arrow_down} />
            </svg>
          </div>
        )}
        {!isSignUpView &&
          !loggedIn &&
          !mainMenuVisible && (
            <Link
              to="/signup"
              className="topnav__signupbtn won-button--filled secondary hide-in-responsive"
            >
              Sign up
            </Link>
          )}
        {!loggedIn && <WonLoginMenu />}
      </nav>
    </won-topnav>
  );
}
WonTopnav.propTypes = {
  pageTitle: PropTypes.string,
};
