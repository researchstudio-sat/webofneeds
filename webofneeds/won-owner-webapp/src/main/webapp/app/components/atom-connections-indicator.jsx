/**
 * Created by sigpie on 21.09.2019.
 */
import React from "react";
import Immutable from "immutable";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { actionCreators } from "../actions/actions.js";
import { getIn } from "../utils.js";

import * as atomUtils from "../redux/utils/atom-utils.js";
//import * as generalSelectors from "../redux/selectors/general-selectors.js";
import "~/style/_atom-connections-indicator.scss";

const mapStateToProps = (state, ownProps) => {
  const atom = getIn(state, ["atoms", ownProps.atomUri]);

  const requestedConnections = atomUtils.getRequestedConnections(atom);
  const requestsCount = requestedConnections ? requestedConnections.size : 0;
  const unreadRequests =
    requestedConnections &&
    requestedConnections.filter(conn => conn.get("unread"));
  const unreadRequestsCount = unreadRequests ? unreadRequests.size : 0;

  const unreadChatMessages = ownProps && ownProps.hasUnreadChatConnections;

  console.log(
    "has unread msgs: " +
      !!unreadChatMessages +
      " unread reqs: " +
      unreadRequestsCount
  );

  return {
    atomUri: ownProps.atomUri,
    requestsCount,
    unreadRequestsCount,
    unreadChatMessages,
  };
};

const mapDispatchToProps = dispatch => {
  return {
    routerGo: (path, props) => {
      dispatch(actionCreators.router__stateGo(path, props));
    },
    selectAtomTab: (atomUri, selectTab) => {
      dispatch(
        actionCreators.atoms__selectTab(
          Immutable.fromJS({
            atomUri: atomUri,
            selectTab: selectTab,
          })
        )
      );
    },
  };
};

class WonAtomConnectionsIndicator extends React.Component {
  constructor(props) {
    super(props);
    this.showAtomConnections = this.showAtomConnections.bind(this);
  }
  //   showAtomSuggestions() {
  //     this.props.selectAtomTab(this.props.atomUri, "SUGGESTIONS");
  //     this.props.routerGo("post", { postUri: this.props.atomUri });
  //   }

  showAtomConnections() {
    // TODO: add code to show appropriate connection!
    this.props.routerGo("connections");
  }

  render() {
    // TODO: add stuff for msgs
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
  unreadChatMessages: PropTypes.bool,
  requestsCount: PropTypes.number,
  unreadRequestsCount: PropTypes.number,
  routerGo: PropTypes.func,
  selectAtomTab: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonAtomConnectionsIndicator);
