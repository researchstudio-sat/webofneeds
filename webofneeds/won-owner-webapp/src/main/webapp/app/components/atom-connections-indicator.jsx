/**
 * Created by sigpie on 21.09.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { actionCreators } from "../actions/actions.js";
import { get, getIn } from "../utils.js";

import * as connectionSelectors from "../redux/selectors/connection-selectors.js";

import "~/style/_atom-connections-indicator.scss";
import ico36_message from "~/images/won-icons/ico36_message.svg";
import ico36_incoming from "~/images/won-icons/ico36_incoming.svg";
import { Link } from "react-router-dom";

const mapStateToProps = (state, ownProps) => {
  const atom = getIn(state, ["atoms", ownProps.atomUri]);

  const requests = connectionSelectors.getRequestedConnections(state, atom);
  const unreadRequests = connectionSelectors.getUnreadRequestedConnections(
    state,
    atom
  );

  const unreadChats = connectionSelectors.getUnreadChatMessageConnections(
    state,
    atom
  );
  const hasUnreadChats = !!unreadChats && unreadChats.size > 0;

  const requestsCount = requests ? requests.size : 0;
  const unreadRequestsCount = unreadRequests ? unreadRequests.size : 0;
  // TODO: unread msgs count?

  return {
    atomUri: ownProps.atomUri,
    requestsCount,
    unreadRequestsCount,
    unreadRequests,
    hasUnreadChats,
    unreadChats,
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
  }

  getRoute() {
    const connUri = this.props.hasUnreadChats
      ? get(this.props.unreadChats.first(), "uri")
      : get(this.props.unreadRequests.first(), "uri");
    return "/connections?connectionUri=" + connUri;
  }

  render() {
    const hasNoUnreadConnections =
      !this.props.requestsCount > 0 && !this.props.hasUnreadChats;
    return (
      <Link
        className={
          "won-atom-connections-indicator " +
          (hasNoUnreadConnections ? "won-no-connections" : "")
        }
        to={this.getRoute()}
      >
        <svg
          className={
            "asi__icon " +
            (this.props.hasUnreadChats || this.props.unreadRequestsCount > 0
              ? "asi__icon--unreads"
              : "asi__icon--reads")
          }
        >
          <use
            xlinkHref={
              this.props.hasUnreadChats ? ico36_message : ico36_incoming
            }
            href={this.props.hasUnreadChats ? ico36_message : ico36_incoming}
          />
        </svg>
        <div className="asi__right">
          <div className="asi__right__topline">
            <div className="asi__right__topline__title">
              {this.props.hasUnreadChats
                ? "Unread Messages"
                : "Connection Requests"}
            </div>
          </div>
          <div className="asi__right__subtitle">
            <div className="asi__right__subtitle__label">
              <span>
                {this.props.hasUnreadChats
                  ? "You have unread Chat Messages"
                  : this.props.requestsCount + " Requests"}
              </span>
              {!this.props.hasUnreadChats &&
              this.props.unreadRequestsCount > 0 ? (
                <span>{", " + this.props.unreadRequestsCount + " new"}</span>
              ) : null}
            </div>
          </div>
        </div>
      </Link>
    );
  }
}

WonAtomConnectionsIndicator.propTypes = {
  atomUri: PropTypes.string.isRequired,
  hasUnreadChats: PropTypes.bool,
  unreadChats: PropTypes.object,
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
