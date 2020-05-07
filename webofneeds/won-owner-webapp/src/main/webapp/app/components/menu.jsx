import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { actionCreators } from "../actions/actions.js";

import "~/style/_menu.scss";
import ico16_indicator_warning from "~/images/won-icons/ico16_indicator_warning.svg";
import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";
import * as viewSelectors from "../redux/selectors/view-selectors.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import Immutable from "immutable";
import { NavLink, withRouter } from "react-router-dom";

const mapStateToProps = (state, ownProps) => ({
  hasSlideIns: viewSelectors.hasSlideIns(state, ownProps.history),
  isMenuVisible: viewSelectors.isMenuVisible(state),
  isSlideInsVisible: viewSelectors.isSlideInsVisible(state),
  isLocationAccessDenied: generalSelectors.isLocationAccessDenied(state),
  hasChatConnections: generalSelectors.hasChatConnections(state),
  hasUnreadSuggestedConnections: generalSelectors.hasUnreadSuggestedConnections(
    state
  ),
  hasUnreadBuddyConnections: generalSelectors.hasUnreadBuddyConnections(
    state,
    true,
    false
  ),
  hasUnreadChatConnections: generalSelectors.hasUnreadChatConnections(state),
});

const mapDispatchToProps = dispatch => ({
  hideMenu: () => {
    dispatch(actionCreators.view__hideMenu());
  },
  locationAccessDenied: () => {
    dispatch(actionCreators.view__locationAccessDenied());
  },
  updateCurrentLocation: locImm => {
    dispatch(actionCreators.view__updateCurrentLocation(locImm));
  },
  toggleSlideIns: () => {
    dispatch(actionCreators.view__toggleSlideIns());
  },
});

class WonMenu extends React.Component {
  constructor(props) {
    super(props);
    this.hideMenuIfVisible = this.hideMenuIfVisible.bind(this);
    this.viewWhatsAround = this.viewWhatsAround.bind(this);
    this.viewWhatsNew = this.viewWhatsNew.bind(this);
    this.toggleSlideIns = this.toggleSlideIns.bind(this);
    this.handleClick = this.handleClick.bind(this);
  }

  render() {
    return (
      <won-menu
        class={this.generateRootClasses()}
        ref={node => (this.node = node)}
      >
        <div className="menu">
          <NavLink
            className={this.generateTabClasses(
              false,
              this.props.hasUnreadSuggestedConnections ||
                this.props.hasUnreadBuddyConnections
            )}
            activeClassName="menu__tab--selected"
            onClick={this.hideMenuIfVisible}
            to="/inventory"
          >
            <span className="menu__tab__unread" />
            <span className="menu__tab__label">Inventory</span>
          </NavLink>
          <NavLink
            className={this.generateTabClasses(
              !this.props.hasChatConnections,
              this.props.hasUnreadChatConnections
            )}
            activeClassName="menu__tab--selected"
            onClick={this.hideMenuIfVisible}
            to="/connections"
          >
            <span className="menu__tab__unread" />
            <span className="menu__tab__label">Chats</span>
          </NavLink>
          <NavLink
            className={this.generateTabClasses()}
            activeClassName="menu__tab--selected"
            onClick={this.hideMenuIfVisible}
            to="/create"
          >
            <span className="menu__tab__label">Create</span>
          </NavLink>
          <NavLink
            className={this.generateTabClasses()}
            activeClassName="menu__tab--selected"
            onClick={this.viewWhatsNew}
            to="/overview"
          >
            <span className="menu__tab__label">{"What's New"}</span>
          </NavLink>
          <NavLink
            className={this.generateTabClasses()}
            activeClassName="menu__tab--selected"
            onClick={this.viewWhatsAround}
            to="/map"
          >
            <span className="menu__tab__label">{"What's Around"}</span>
          </NavLink>
          {this.props.hasSlideIns ? (
            <div
              className="menu__slideintoggle hide-in-responsive"
              onClick={this.toggleSlideIns}
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
                  (this.props.isSlideInsVisible
                    ? " menu__slideintoggle__carret--expanded "
                    : "")
                }
              >
                <use xlinkHref={ico16_arrow_down} href={ico16_arrow_down} />
              </svg>
              <span className="menu__slideintoggle__label">
                {this.props.isSlideInsVisible ? (
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

  generateTabClasses(inactive = false, unread = false) {
    const classes = ["menu__tab"];

    inactive && classes.push("menu__tab--inactive");
    unread && classes.push("menu__tab--unread");

    return classes.join(" ");
  }

  generateRootClasses() {
    const classes = [];

    this.props.hasSlideIns && classes.push("won-has-slideins");
    this.props.isMenuVisible && classes.push("won-menu--show-mobile");

    return classes.join(" ");
  }

  toggleSlideIns() {
    this.hideMenuIfVisible();
    this.props.toggleSlideIns();
  }

  hideMenuIfVisible() {
    if (this.props.isMenuVisible) {
      this.props.hideMenu();
    }
  }

  viewWhatsAround() {
    this.viewWhatsX(() => {
      this.hideMenuIfVisible();
    });
  }

  viewWhatsNew() {
    this.viewWhatsX(() => {
      this.hideMenuIfVisible();
    });
  }

  viewWhatsX(callback) {
    if (this.props.isLocationAccessDenied) {
      callback();
    } else if ("geolocation" in navigator) {
      navigator.geolocation.getCurrentPosition(
        currentLocation => {
          const lat = currentLocation.coords.latitude;
          const lng = currentLocation.coords.longitude;

          this.props.updateCurrentLocation(
            Immutable.fromJS({ location: { lat, lng } })
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
          this.props.locationAccessDenied();
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
      this.props.locationAccessDenied();
      callback();
    }
  }

  componentWillMount() {
    document.addEventListener("mousedown", this.handleClick, false);
  }
  componentWillUnmount() {
    document.removeEventListener("mousedown", this.handleClick, false);
  }

  handleClick(e) {
    if (!this.node.contains(e.target) && this.props.isMenuVisible) {
      // TODO: Fix me.
      // Handler is closing menu before actual MenuAction is toggled
      //this.props.hideMenu();

      return;
    }
  }
}
WonMenu.propTypes = {
  hasSlideIns: PropTypes.bool,
  isMenuVisible: PropTypes.bool,
  isSlideInsVisible: PropTypes.bool,
  isLocationAccessDenied: PropTypes.bool,
  hasChatConnections: PropTypes.bool,
  hasUnreadSuggestedConnections: PropTypes.bool,
  hasUnreadBuddyConnections: PropTypes.bool,
  hasUnreadChatConnections: PropTypes.bool,
  hideMenu: PropTypes.func,
  locationAccessDenied: PropTypes.func,
  updateCurrentLocation: PropTypes.func,
  toggleSlideIns: PropTypes.func,
  history: PropTypes.object,
};

export default withRouter(
  connect(
    mapStateToProps,
    mapDispatchToProps
  )(WonMenu)
);
