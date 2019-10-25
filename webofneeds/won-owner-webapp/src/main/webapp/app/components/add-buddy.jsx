import React from "react";
import won from "../won-es6";
import { connect } from "react-redux";
import { get, getIn } from "../utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import WonAtomHeader from "../components/atom-header.jsx";

import PropTypes from "prop-types";

import "~/style/_add-buddy.scss";

const mapStateToProps = (state, ownProps) => {
  const ownedAtomsWithBuddySocket = generalSelectors.getOwnedAtomsWithBuddySocket(
    state
  );

  const atom = getIn(state, ["atoms", ownProps.atomUri]);

  return {
    ownedAtomsWithBuddySocketArray:
      ownedAtomsWithBuddySocket &&
      ownedAtomsWithBuddySocket
        .filter(atom => atomUtils.isActive(atom))
        .filter(atom => get(atom, "uri") !== ownProps.atomUri)
        .toArray(),
    targetBuddySocketUri: atomUtils.getSocketUri(
      atom,
      won.BUDDY.BuddySocketCompacted
    ),
    atomUri: ownProps.atomUri,
  };
};

// TODO: Change Icon maybe PersonIcon with a Plus
// TODO: Immediate Action if only one Persona is owned by the User -> Open Modal Dialog to ask
// TODO: Display Possible Personas and Personas that are already Buddies or have pending requests
class WonAddBuddy extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      contextMenuOpen: false,
    };
    this.handleClick = this.handleClick.bind(this);
  }

  render() {
    let buddySelectionElement =
      this.props.ownedAtomsWithBuddySocketArray &&
      this.props.ownedAtomsWithBuddySocketArray.map(atom => {
        const existingBuddyConnection = get(atom, "connections").find(
          conn =>
            get(conn, "targetSocketUri") === this.props.targetBuddySocketUri
        );

        if (connectionUtils.isConnected(existingBuddyConnection)) {
          //TODO: Already Buddies
          return (
            <div
              className="add-buddy__addbuddymenu__content__selection__buddy connected"
              key={get(atom, "uri")}
              onClick={() => {
                console.debug(
                  "You are already buddies ",
                  atomUtils.getSocketUri(atom, won.BUDDY.BuddySocketCompacted),
                  " to ",
                  this.props.targetBuddySocketUri
                );
              }}
            >
              <WonAtomHeader atomUri={get(atom, "uri")} hideTimestamp={true} />
              <svg className="add-buddy__addbuddymenu__content__selection__buddy__status">
                <use xlinkHref="#ico16_checkmark" href="#ico16_checkmark" />
              </svg>
            </div>
          );
        } else if (connectionUtils.isRequestReceived(existingBuddyConnection)) {
          //TODO: Request received pending from your end -> click to accept
          return (
            <div
              className="add-buddy__addbuddymenu__content__selection__buddy received"
              key={get(atom, "uri")}
              onClick={() => {
                console.debug(
                  "You want to accept the other persons request for ",
                  atomUtils.getSocketUri(atom, won.BUDDY.BuddySocketCompacted),
                  " to ",
                  this.props.targetBuddySocketUri
                );
              }}
            >
              <WonAtomHeader atomUri={get(atom, "uri")} hideTimestamp={true} />
              <svg className="add-buddy__addbuddymenu__content__selection__buddy__status">
                <use xlinkHref="#ico36_incoming" href="#ico36_incoming" />
              </svg>
            </div>
          );
        } else if (connectionUtils.isRequestSent(existingBuddyConnection)) {
          //TODO: Buddy request Pending on the other side -> wait and see
          return (
            <div
              className="add-buddy__addbuddymenu__content__selection__buddy sent"
              key={get(atom, "uri")}
              onClick={() => {
                console.debug(
                  "You have to wait until your request was accepted for ",
                  atomUtils.getSocketUri(atom, won.BUDDY.BuddySocketCompacted),
                  " to ",
                  this.props.targetBuddySocketUri
                );
              }}
            >
              <WonAtomHeader atomUri={get(atom, "uri")} hideTimestamp={true} />
              <svg className="add-buddy__addbuddymenu__content__selection__buddy__status">
                <use xlinkHref="#ico36_outgoing" href="#ico36_outgoing" />
              </svg>
            </div>
          );
        } else if (connectionUtils.isClosed(existingBuddyConnection)) {
          //TODO: Your request was denied or you used to be friends :-(
          return (
            <div
              className="add-buddy__addbuddymenu__content__selection__buddy closed"
              key={get(atom, "uri")}
              onClick={() => {
                console.debug(
                  "You used to be friends or your buddyRequest was denied for ",
                  atomUtils.getSocketUri(atom, won.BUDDY.BuddySocketCompacted),
                  " to ",
                  this.props.targetBuddySocketUri
                );
              }}
            >
              <WonAtomHeader atomUri={get(atom, "uri")} hideTimestamp={true} />
              <svg className="add-buddy__addbuddymenu__content__selection__buddy__status">
                <use
                  xlinkHref="#ico36_close_circle"
                  href="#ico36_close_circle"
                />
              </svg>
            </div>
          );
        } /*else if (connectionUtils.isSuggested(existingBuddyConnection)) {
          //TODO: Its suggested to be friends, why not try it
        } */ else {
          return (
            <div
              className="add-buddy__addbuddymenu__content__selection__buddy requestable"
              key={get(atom, "uri")}
              onClick={() => {
                console.debug(
                  "Send buddy request from",
                  atomUtils.getSocketUri(atom, won.BUDDY.BuddySocketCompacted),
                  " to ",
                  this.props.targetBuddySocketUri
                );
              }}
            >
              <WonAtomHeader atomUri={get(atom, "uri")} hideTimestamp={true} />
              <svg className="add-buddy__addbuddymenu__content__selection__buddy__status">
                <use xlinkHref="#ico36_plus_circle" href="#ico36_plus_circle" />
              </svg>
            </div>
          );
        }
      });

    const dropdownElement = this.state.contextMenuOpen && (
      <div className="add-buddy__addbuddymenu">
        <div className="add-buddy__addbuddymenu__content">
          <div className="topline">
            <div
              className="add-buddy__addbuddymenu__header clickable"
              onClick={() => this.setState({ contextMenuOpen: false })}
            >
              <svg className="add-buddy__addbuddymenu__header__icon">
                <use xlinkHref="#ico36_plus_circle" href="#ico36_plus_circle" />
              </svg>
              <span className="add-buddy__addbuddymenu__header__text hide-in-responsive">
                Add as Buddy
              </span>
            </div>
          </div>
          <div className="add-buddy__addbuddymenu__content__selection">
            {buddySelectionElement}
          </div>
        </div>
      </div>
    );

    return (
      <won-add-buddy
        class={this.props.className ? this.props.className : ""}
        ref={node => (this.node = node)}
      >
        <div
          className="add-buddy__addbuddymenu__header clickable"
          onClick={() => this.setState({ contextMenuOpen: true })}
        >
          <svg className="add-buddy__addbuddymenu__header__icon">
            <use xlinkHref="#ico36_plus_circle" href="#ico36_plus_circle" />
          </svg>
          <span className="add-buddy__addbuddymenu__header__text hide-in-responsive">
            Add as Buddy
          </span>
        </div>
        {dropdownElement}
      </won-add-buddy>
    );
  }

  componentWillMount() {
    document.addEventListener("mousedown", this.handleClick, false);
  }
  componentWillUnmount() {
    document.removeEventListener("mousedown", this.handleClick, false);
  }

  handleClick(e) {
    if (!this.node.contains(e.target) && this.state.contextMenuOpen) {
      this.setState({ contextMenuOpen: false });

      return;
    }
  }
}
WonAddBuddy.propTypes = {
  atomUri: PropTypes.string.isRequired,
  className: PropTypes.string,
  ownedAtomsWithBuddySocketArray: PropTypes.arrayOf(PropTypes.object),
  targetBuddySocketUri: PropTypes.string,
};

export default connect(mapStateToProps)(WonAddBuddy);
