import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { actionCreators } from "../actions/actions.js";
import { getIn } from "../utils.js";

import "~/style/_menu.scss";
import ico16_indicator_warning from "~/images/won-icons/ico16_indicator_warning.svg";
import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";
import * as viewSelectors from "../redux/selectors/view-selectors.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import Immutable from "immutable";

const mapStateToProps = state => {
  const currentRoute = getIn(state, ["router", "currentState", "name"]);

  return {
    hasSlideIns: viewSelectors.hasSlideIns(state),
    isMenuVisible: viewSelectors.isMenuVisible(state),
    isSlideInsVisible: viewSelectors.isSlideInsVisible(state),
    isLocationAccessDenied: generalSelectors.isLocationAccessDenied(state),
    showInventory: currentRoute === "inventory",
    showChats: currentRoute === "connections",
    showCreate: currentRoute === "create",
    showWhatsNew: currentRoute === "overview",
    showWhatsAround: currentRoute === "map",
    hasChatAtoms: generalSelectors.hasChatAtoms(state),
    hasUnreadSuggestedConnections: generalSelectors.hasUnreadSuggestedConnections(
      state
    ),
    hasUnreadBuddyConnections: generalSelectors.hasUnreadBuddyConnections(
      state,
      true,
      false
    ),
    hasUnreadChatConnections: generalSelectors.hasUnreadChatConnections(state),
  };
};
const mapDispatchToProps = dispatch => {
  return {
    hideMenu: () => {
      dispatch(actionCreators.view__hideMenu());
    },
    locationAccessDenied: () => {
      dispatch(actionCreators.view__locationAccessDenied());
    },
    routerGo: (path, props) => {
      dispatch(actionCreators.router__stateGo(path, props));
    },
    updateCurrentLocation: locImm => {
      dispatch(actionCreators.view__updateCurrentLocation(locImm));
    },
    toggleSlideIns: () => {
      dispatch(actionCreators.view__toggleSlideIns());
    },
  };
};

class WonMenu extends React.Component {
  constructor(props) {
    super(props);
    this.viewChats = this.viewChats.bind(this);
    this.viewCreate = this.viewCreate.bind(this);
    this.viewInventory = this.viewInventory.bind(this);
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
          <a
            className={this.generateTabClasses(
              this.props.showInventory,
              false,
              this.props.hasUnreadSuggestedConnections ||
                this.props.hasUnreadBuddyConnections
            )}
            onClick={this.viewInventory}
          >
            <span className="menu__tab__unread" />
            <span className="menu__tab__label">Inventory</span>
          </a>
          <a
            className={this.generateTabClasses(
              this.props.showChats,
              !this.props.hasChatAtoms,
              this.props.hasUnreadChatConnections
            )}
            onClick={this.viewChats}
          >
            <span className="menu__tab__unread" />
            <span className="menu__tab__label">Chats</span>
          </a>
          <a
            className={this.generateTabClasses(this.props.showCreate)}
            onClick={this.viewCreate}
          >
            <span className="menu__tab__label">Create</span>
          </a>
          <a
            className={this.generateTabClasses(this.props.showWhatsNew)}
            onClick={this.viewWhatsNew}
          >
            <span className="menu__tab__label">{"What's New"}</span>
          </a>
          <a
            className={this.generateTabClasses(this.props.showWhatsAround)}
            onClick={this.viewWhatsAround}
          >
            <span className="menu__tab__label">{"What's Around"}</span>
          </a>
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

  generateTabClasses(selected = false, inactive = false, unread = false) {
    const classes = ["menu__tab"];

    selected && classes.push("menu__tab--selected");
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
    if (this.props.isMenuVisible) {
      this.props.hideMenu();
    }
    this.props.toggleSlideIns();
  }

  viewCreate() {
    if (this.props.isMenuVisible) {
      this.props.hideMenu();
    }
    this.props.routerGo("create");
  }

  viewChats() {
    if (this.props.isMenuVisible) {
      this.props.hideMenu();
    }
    this.props.routerGo("connections");
  }

  viewInventory() {
    if (this.props.isMenuVisible) {
      this.props.hideMenu();
    }
    this.props.routerGo("inventory");
  }

  viewWhatsAround() {
    this.viewWhatsX(() => {
      if (this.props.isMenuVisible) {
        this.props.hideMenu();
      }
      this.props.routerGo("map");
    });
  }

  viewWhatsNew() {
    this.viewWhatsX(() => {
      if (this.props.isMenuVisible) {
        this.props.hideMenu();
      }
      this.props.routerGo("overview");
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
  showInventory: PropTypes.bool,
  showChats: PropTypes.bool,
  showCreate: PropTypes.bool,
  showWhatsNew: PropTypes.bool,
  showWhatsAround: PropTypes.bool,
  hasChatAtoms: PropTypes.bool,
  hasUnreadSuggestedConnections: PropTypes.bool,
  hasUnreadBuddyConnections: PropTypes.bool,
  hasUnreadChatConnections: PropTypes.bool,
  hideMenu: PropTypes.func,
  locationAccessDenied: PropTypes.func,
  routerGo: PropTypes.func,
  updateCurrentLocation: PropTypes.func,
  toggleSlideIns: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonMenu);
