/**
 * Created by quasarchimaere on 05.08.2019.
 */
import React from "react";
import { actionCreators } from "../actions/actions.js";
import { connect } from "react-redux";
import WonAtomCard from "./atom-card.jsx";

import "~/style/_atom-content-suggestions.scss";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as connectionSelectors from "../redux/selectors/connection-selectors.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import VisibilitySensor from "react-visibility-sensor";
import { get } from "../utils.js";
import vocab from "../service/vocab.js";
import PropTypes from "prop-types";

const mapStateToProps = (state, ownProps) => {
  const suggestions = connectionSelectors.getSuggestedConnectionsByAtomUri(
    state,
    ownProps.atomUri
  );

  return {
    atomUri: ownProps.atomUri,
    hasSuggestions: suggestions && suggestions.size > 0,
    suggestionsArray: suggestions && suggestions.toArray(),
    currentLocation: generalSelectors.getCurrentLocation(state),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    routerGoCurrent: props => {
      dispatch(actionCreators.router__stateGoCurrent(props));
    },
    routerGo: (path, props) => {
      dispatch(actionCreators.router__stateGo(path, props));
    },
    connectionClose: connectionUri => {
      dispatch(actionCreators.connections__close(connectionUri));
    },
    connectionMarkAsRead: (connectionUri, atomUri) => {
      dispatch(
        actionCreators.connections__markAsRead({
          connectionUri: connectionUri,
          atomUri: atomUri,
        })
      );
    },
    connectSockets: (senderSocketUri, targetSocketUri, message) => {
      dispatch(
        actionCreators.atoms__connectSockets(
          senderSocketUri,
          targetSocketUri,
          message
        )
      );
    },
    rateConnection: (connectionUri, rating) => {
      dispatch(actionCreators.connections__rate(connectionUri, rating));
    },
  };
};

class WonAtomContentSuggestions extends React.Component {
  markAsRead(conn) {
    if (connectionUtils.isUnread(conn)) {
      setTimeout(() => {
        this.props.connectionMarkAsRead(get(conn, "uri"), this.props.atomUri);
      }, 1500);
    }
  }

  viewSuggestion(conn) {
    if (!conn) {
      return;
    }

    const connUri = get(conn, "uri");

    if (connectionUtils.isUnread(conn)) {
      this.props.connectionMarkAsRead(connUri, this.props.atomUri);
    }
    this.props.routerGoCurrent({ viewConnUri: connUri });
  }

  closeConnection(conn, rateBad = false) {
    if (!conn) {
      return;
    }

    const connUri = get(conn, "uri");

    if (rateBad) {
      this.props.rateConnection(connUri, vocab.WONCON.binaryRatingBad);
    }

    if (connectionUtils.isUnread(conn)) {
      this.props.connectionMarkAsRead(connUri, this.props.atomUri);
    }
    this.props.connectionClose(connUri);
  }

  sendRequest(conn, message = "") {
    if (!conn) {
      return;
    }

    const connUri = get(conn, "uri");

    if (connectionUtils.isUnread(conn)) {
      this.props.connectionMarkAsRead(connUri, this.props.atomUri);
    }

    this.props.rateConnection(connUri, vocab.WONCON.binaryRatingGood);

    const socketUri = get(conn, "socketUri");
    const targetSocketUri = get(conn, "targetSocketUri");

    this.props.connectSockets(socketUri, targetSocketUri, message);
    this.props.routerGo("connections", {
      connectionUri: connUri,
      viewConnUri: undefined,
    });
  }

  render() {
    if (this.props.hasSuggestions) {
      const atomCards = this.props.suggestionsArray.map(suggestion => {
        return (
          <VisibilitySensor
            key={get(suggestion, "uri")}
            onChange={isVisible => {
              isVisible &&
                connectionUtils.isUnread(suggestion) &&
                this.markAsRead(suggestion);
            }}
            intervalDelay={2000}
          >
            <div
              className={
                "acs__atom " +
                (connectionUtils.isUnread(suggestion) ? "won-unread" : "")
              }
            >
              <WonAtomCard
                atomUri={get(suggestion, "targetAtomUri")}
                currentLocation={this.props.currentLocation}
                showSuggestions={false}
                showHolder={true}
                onAtomClick={() => {
                  this.viewSuggestion(suggestion);
                }}
              />
              <div className="acs__atom__actions">
                <div
                  className="acs__atom__actions__button red won-button--filled"
                  onClick={() => {
                    this.sendRequest(suggestion);
                  }}
                >
                  Request
                </div>
                <div
                  className="acs__atom__actions__button red won-button--outlined thin"
                  onClick={() => {
                    this.closeConnection(suggestion);
                  }}
                >
                  Remove
                </div>
              </div>
            </div>
          </VisibilitySensor>
        );
      });

      return (
        <won-atom-content-suggestions>{atomCards}</won-atom-content-suggestions>
      );
    } else {
      return (
        <won-atom-content-suggestions>
          <div className="acs__empty">No Suggestions for this Atom.</div>
        </won-atom-content-suggestions>
      );
    }
  }
}
WonAtomContentSuggestions.propTypes = {
  atomUri: PropTypes.string.isRequired,
  hasSuggestions: PropTypes.bool,
  suggestionsArray: PropTypes.arrayOf(PropTypes.object),
  currentLocation: PropTypes.object,
  routerGoCurrent: PropTypes.func,
  routerGo: PropTypes.func,
  connectionClose: PropTypes.func,
  connectionMarkAsRead: PropTypes.func,
  connectSockets: PropTypes.func,
  rateConnection: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonAtomContentSuggestions);
