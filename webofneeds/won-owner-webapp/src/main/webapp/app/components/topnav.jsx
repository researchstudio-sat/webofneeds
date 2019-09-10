import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { actionCreators } from "../actions/actions.js";
import { get, getIn } from "../utils.js";
import * as viewSelectors from "../redux/selectors/view-selectors.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import { isLoading } from "../redux/selectors/process-selectors.js";
import * as connectionSelectors from "../redux/selectors/connection-selectors.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import WonAccountMenu from "./account-menu.jsx";

import "~/style/_responsiveness-utils.scss";
import "~/style/_topnav.scss";

const mapStateToProps = (state, ownProps) => {
  const currentRoute = getIn(state, ["router", "currentState", "name"]);
  const accountState = get(state, "account");

  return {
    pageTitle: ownProps.pageTitle,
    hasSlideIns: viewSelectors.hasSlideIns(state),
    isSlideInsVisible: viewSelectors.isSlideInsVisible(state),
    mainMenuVisible: getIn(state, ["view", "showMainMenu"]),
    isMenuVisible: viewSelectors.isMenuVisible(state),
    themeName: getIn(state, ["config", "theme", "name"]),
    appTitle: getIn(state, ["config", "theme", "title"]),
    loggedIn: accountUtils.isLoggedIn(accountState),
    isSignUpView: currentRoute === "signup",
    showLoadingIndicator: isLoading(state),
    connectionsToCrawl: connectionSelectors.getChatConnectionsToCrawl(state),
    hasUnreads:
      generalSelectors.hasUnreadSuggestedConnections(state) ||
      generalSelectors.hasUnreadBuddyConnections(state, true, false) ||
      generalSelectors.hasUnreadChatConnections(state),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    hideMenu: () => {
      dispatch(actionCreators.view__hideMenu());
    },
    toggleMenu: () => {
      dispatch(actionCreators.view__toggleMenu());
    },
    routerGo: (path, props) => {
      dispatch(actionCreators.router__stateGo(path, props));
    },
    routerGoDefault: () => {
      dispatch(actionCreators.router__stateGoDefault());
    },
    toggleSlideIns: () => {
      dispatch(actionCreators.view__toggleSlideIns());
    },
    showMoreMessages: (connectionUri, msgCount) => {
      dispatch(
        actionCreators.connections__showMoreMessages(connectionUri, msgCount)
      );
    },
    showLatestMessages: (connectionUri, msgCount) => {
      dispatch(
        actionCreators.connections__showLatestMessages(connectionUri, msgCount)
      );
    },
  };
};

class WonTopnav extends React.Component {
  constructor(props) {
    super(props);
    this.toggleSlideIns = this.toggleSlideIns.bind(this);
    this.menuAction = this.menuAction.bind(this);
    this.goDefault = this.goDefault.bind(this);
  }
  render() {
    return (
      <won-topnav>
        <nav className="topnav">
          <svg
            className={
              "topnav__menuicon clickable " +
              (!this.props.loggedIn ? " topnav__menuicon--hide " : "") +
              (this.props.isMenuVisible ? " topnav__menuicon--show " : "")
            }
            onClick={this.menuAction}
          >
            <use xlinkHref="#ico16_burger" href="#ico16_burger" />
          </svg>
          <div className="topnav__logo clickable">
            <img
              src={"skin/" + this.props.themeName + "/images/logo.svg"}
              className="topnav__logo__image hide-in-responsive"
              onClick={this.goDefault}
            />
            <img
              src={"skin/" + this.props.themeName + "/images/logo.svg"}
              className="topnav__logo__image show-in-responsive"
              onClick={this.menuAction}
            />
            {this.props.hasUnreads &&
              !this.props.isMenuVisible && (
                <span className="topnav__logo__unreads show-in-responsive" />
              )}
          </div>
          <div className="topnav__title">
            {this.props.isMenuVisible ? (
              <span className="topnav__page-title" onClick={this.menuAction}>
                Menu
              </span>
            ) : (
              <React.Fragment>
                <span
                  className="topnav__app-title hide-in-responsive"
                  onClick={this.goDefault}
                >
                  {this.props.appTitle}
                </span>
                {this.props.pageTitle && (
                  <React.Fragment>
                    <span className="topnav__divider hide-in-responsive">
                      &mdash;
                    </span>
                    <span className="topnav__page-title">
                      {this.props.pageTitle}
                    </span>
                  </React.Fragment>
                )}
              </React.Fragment>
            )}
          </div>
          {this.props.showLoadingIndicator && (
            <div className="topnav__loading">
              <svg className="topnav__loading__spinner hspinner">
                <use xlinkHref="#ico_loading_anim" href="#ico_loading_anim" />
              </svg>
            </div>
          )}
          {this.props.hasSlideIns && (
            <div
              className="topnav__slideintoggle show-in-responsive"
              onClick={this.toggleSlideIns}
            >
              <svg className="topnav__slideintoggle__icon">
                <use
                  xlinkHref="#ico16_indicator_warning"
                  href="#ico16_indicator_warning"
                />
              </svg>
              <svg
                className={
                  "topnav__slideintoggle__carret " +
                  (this.props.isSlideInsVisible
                    ? " topnav__slideintoggle__carret--expanded "
                    : "")
                }
              >
                <use xlinkHref="#ico16_arrow_down" href="#ico16_arrow_down" />
              </svg>
            </div>
          )}
          {!this.props.isSignUpView &&
            !this.props.loggedIn &&
            !this.props.mainMenuVisible && (
              <button
                onClick={() => this.props.routerGo("signup")}
                className="topnav__signupbtn won-button--filled red hide-in-responsive"
              >
                Sign up
              </button>
            )}
          <WonAccountMenu />
        </nav>
      </won-topnav>
    );
  }

  toggleSlideIns() {
    this.hideMenu();
    this.props.toggleSlideIns();
  }

  hideMenu() {
    if (this.props.isMenuVisible) {
      this.props.hideMenu();
    }
    return true;
  }

  goDefault() {
    this.hideMenu();
    this.props.routerGoDefault();
  }

  menuAction() {
    if (this.props.loggedIn) {
      this.props.toggleMenu();
    } else {
      this.hideMenu();
      this.props.routerGoDefault();
    }
  }

  componentDidUpdate() {
    const MESSAGECOUNT = 10;

    if (
      this.props.connectionsToCrawl &&
      this.props.connectionsToCrawl.size > 0
    ) {
      console.debug(
        "connectionsToCrawl: ",
        this.props.connectionsToCrawl,
        " Size: ",
        this.props.connectionsToCrawl.size
      );
      this.props.connectionsToCrawl.map(conn => {
        const messages = get(conn, "messages");
        const messageCount = messages ? messages.size : 0;

        if (messageCount == 0) {
          this.props.showLatestMessages(get(conn, "uri"), MESSAGECOUNT);
        } else {
          const receivedMessages = messages.filter(
            msg => !get(msg, "outgoingMessage")
          );
          const receivedMessagesReadPresent = receivedMessages.find(
            msg => !get(msg, "unread")
          );

          if (!receivedMessagesReadPresent) {
            this.props.showMoreMessages(get(conn, "uri"), MESSAGECOUNT);
          }
        }
      });
    }
  }
}
WonTopnav.propTypes = {
  pageTitle: PropTypes.string,
  hasSlideIns: PropTypes.bool,
  isSlideInsVisible: PropTypes.bool,
  isMenuVisible: PropTypes.bool,
  mainMenuVisible: PropTypes.bool,
  themeName: PropTypes.string,
  appTitle: PropTypes.string,
  loggedIn: PropTypes.bool,
  isSignUpView: PropTypes.bool,
  showLoadingIndicator: PropTypes.bool,
  connectionsToCrawl: PropTypes.object,
  hasUnreads: PropTypes.bool,
  toggleSlideIns: PropTypes.func,
  hideMenu: PropTypes.func,
  toggleMenu: PropTypes.func,
  routerGo: PropTypes.func,
  routerGoDefault: PropTypes.func,
  showMoreMessages: PropTypes.func,
  showLatestMessages: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonTopnav);
