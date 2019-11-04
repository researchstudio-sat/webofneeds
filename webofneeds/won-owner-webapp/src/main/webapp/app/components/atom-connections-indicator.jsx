/**
 * Created by sigpie on 21.09.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { actionCreators } from "../actions/actions.js";
import { get, getIn } from "../utils.js";

import * as atomUtils from "../redux/utils/atom-utils.js";

import "~/style/_atom-connections-indicator.scss";

const mapStateToProps = (state, ownProps) => {
  const atom = getIn(state, ["atoms", ownProps.atomUri]);

  const requestedConnections = atomUtils.getRequestedConnections(atom, state);
  const requestsCount = requestedConnections ? requestedConnections.size : 0;
  const unreadRequests =
    requestedConnections &&
    requestedConnections.filter(conn => conn.get("unread"));
  const unreadRequestsCount = unreadRequests ? unreadRequests.size : 0;

  const hasUnreadChatMessages = ownProps && ownProps.hasUnreadChatConnections;
  const unreadChatConnections = atomUtils.getUnreadChatMessageConnections(
    atom,
    state
  );

  return {
    atomUri: ownProps.atomUri,
    requestsCount,
    unreadRequestsCount,
    unreadRequests,
    hasUnreadChatMessages,
    unreadChatConnections,
  };
};

const mapDispatchToProps = dispatch => {
  return {
    routerGo: (path, props) => {
      dispatch(actionCreators.router__stateGo(path, props));
    },
  };
};

class WonAtomConnectionsIndicator extends React.Component {
  constructor(props) {
    super(props);
    window.con4dbg = this;
    this.showAtomConnections = this.showAtomConnections.bind(this);
  }

  showAtomConnections() {
    const connUri = this.props.hasUnreadChatMessages
      ? get(this.props.unreadChatConnections.first(), "uri")
      : get(this.props.unreadRequests.first(), "uri");
    this.props.routerGo("connections", { connectionUri: connUri });
  }

  render() {
    return (
      <won-atom-connections-indicator
        class={!this.props.requestsCount > 0 ? "won-no-connections" : ""}
        onClick={this.showAtomConnections}
      >
        <svg
          className={
            "asi__icon " +
            (this.props.unreadRequestsCount > 0
              ? "asi__icon--unreads"
              : "asi__icon--reads")
          }
        >
          <use xlinkHref="#ico36_incoming" href="#ico36_incoming" />
        </svg>
        <div className="asi__right">
          <div className="asi__right__topline">
            <div className="asi__right__topline__title">Connections</div>
          </div>
          <div className="asi__right__subtitle">
            <div className="asi__right__subtitle__label">
              <span>{this.props.requestsCount + " Connections"}</span>
              {this.props.unreadRequestsCount > 0 ? (
                <span>{", " + this.props.unreadRequestsCount + " new"}</span>
              ) : null}
            </div>
          </div>
        </div>
      </won-atom-connections-indicator>
    );
  }
}

WonAtomConnectionsIndicator.propTypes = {
  atomUri: PropTypes.string.isRequired,
  hasUnreadChatMessages: PropTypes.bool,
  unreadChatConnections: PropTypes.object,
  requestsCount: PropTypes.number,
  unreadRequestsCount: PropTypes.number,
  unreadRequests: PropTypes.object,
  routerGo: PropTypes.func,
  selectAtomTab: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonAtomConnectionsIndicator);
