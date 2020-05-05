import React from "react";
import Immutable from "immutable";
import { actionCreators } from "../actions/actions.js";
import { connect } from "react-redux";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import {
  get,
  getIn,
  sortByDate,
  getQueryParams,
  generateLink,
} from "../utils.js";
import * as processUtils from "../redux/utils/process-utils";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import WonConnectionIndicators from "./connection-indicators.jsx";
import WonConnectionSelectionItem from "./connection-selection-item.jsx";
import WonAtomHeader from "./atom-header.jsx";

import "~/style/_connections-overview.scss";
import PropTypes from "prop-types";
import { withRouter } from "react-router-dom";

const mapStateToProps = (state, ownProps) => {
  const allAtoms = generalSelectors.getAtoms(state);
  const ownedAtoms = generalSelectors.getOwnedAtoms(state);
  const openAtoms = generalSelectors.getChatAtoms(state);

  const { connectionUri } = getQueryParams(ownProps.location);

  const sortedOpenAtoms = sortByDate(openAtoms, "creationDate");
  const process = get(state, "process");

  return {
    ownedAtoms,
    allAtoms,
    process,
    connUriInRoute: connectionUri,
    sortedOpenAtomUris: sortedOpenAtoms
      ? [...sortedOpenAtoms.flatMap(atom => atom.get("uri"))]
      : [],
  };
};

const mapDispatchToProps = dispatch => {
  return {
    selectTab: (atomUri, tab) => {
      dispatch(
        actionCreators.atoms__selectTab(
          Immutable.fromJS({ atomUri: atomUri, selectTab: tab })
        )
      );
    },
    connectionMarkAsRead: (connectionUri, atomUri) => {
      dispatch(
        actionCreators.connections__markAsRead({
          connectionUri: connectionUri,
          atomUri: atomUri,
        })
      );
    },
  };
};

class WonConnectionsOverview extends React.Component {
  render() {
    const atomElements = this.props.sortedOpenAtomUris.map(atomUri => {
      const connections = this.getOpenChatConnectionArraySorted(
        atomUri,
        this.props.allAtoms,
        this.props.process
      );

      const connectionElements =
        connections &&
        connections.map(conn => {
          const connUri = get(conn, "uri");
          return (
            <div
              className={
                "co__item__connections__item " +
                (this.isConnectionUnread(atomUri, connUri)
                  ? " won-unread "
                  : "")
              }
              key={connUri}
            >
              <WonConnectionSelectionItem
                connection={conn}
                toLink={generateLink(this.props.history.location, {
                  connectionUri: connUri,
                })}
              />
            </div>
          );
        });

      return (
        <div className="co__item" key={atomUri}>
          <div className="co__item__atom">
            <div className="co__item__atom__header">
              <WonAtomHeader
                atomUri={atomUri}
                onClick={
                  !this.isAtomLoading(atomUri)
                    ? () => {
                        this.showAtomDetails(atomUri);
                      }
                    : undefined
                }
              />
              <WonConnectionIndicators
                atomUri={atomUri}
                onClick={
                  !this.isAtomLoading(atomUri)
                    ? connUri => {
                        this.selectConnection(connUri);
                      }
                    : undefined
                }
              />
            </div>
          </div>
          <div className="co__item__connections">{connectionElements}</div>
        </div>
      );
    });

    return <won-connections-overview>{atomElements}</won-connections-overview>;
  }

  showAtomDetails(atomUri) {
    this.showAtomTab(atomUri, "DETAIL");
  }

  showAtomTab(atomUri, tab = "DETAIL") {
    this.props.selectTab(atomUri, tab);
    this.props.history.push(
      generateLink(this.props.history.location, { postUri: atomUri }, "/post")
    );
  }

  isConnectionUnread(atomUri, connUri) {
    const conn = getIn(this.props.allAtoms, [atomUri, "connections", connUri]);
    return connectionUtils.isUnread(conn);
  }

  isAtomLoading(atomUri) {
    return processUtils.isAtomLoading(this.process, atomUri);
  }

  selectConnection(connectionUri) {
    this.markAsRead(connectionUri);
    this.props.history.push(
      generateLink(this.props.history.location, {
        connectionUri: connectionUri,
      })
    );
  }

  markAsRead(connectionUri) {
    const atom =
      this.props.ownedAtoms &&
      this.props.ownedAtoms.find(atom =>
        atom.getIn(["connections", connectionUri])
      );

    if (atom) {
      const connection = getIn(atom, ["connections", connectionUri]);
      const connUnread = connectionUtils.isUnread(connection);
      const connNotConnected = !connectionUtils.isConnected(connection);

      if (connUnread && connNotConnected) {
        setTimeout(() => {
          this.props.connectionMarkAsRead(connectionUri, get(atom, "uri"));
        }, 1500);
      }
    }
  }

  getOpenChatConnectionArraySorted(atomUri, allAtoms, process) {
    const atom = get(allAtoms, atomUri);

    if (!atom) {
      return undefined;
    }

    const chatSocketUri = atomUtils.getChatSocket(atom);
    const groupSocketUri = atomUtils.getGroupSocket(atom);

    const sortedConnections = sortByDate(
      atom.get("connections").filter(conn => {
        if (
          !connectionUtils.hasSocketUri(conn, chatSocketUri) &&
          !connectionUtils.hasSocketUri(conn, groupSocketUri)
        )
          return false;

        if (connectionUtils.isClosed(conn) || connectionUtils.isSuggested(conn))
          return false;

        if (processUtils.isConnectionLoading(process, conn.get("uri")))
          return true; //if connection is currently loading we assume its a connection we want to show

        const targetAtomUri = conn.get("targetAtomUri");
        const targetAtomPresent =
          targetAtomUri && allAtoms && !!allAtoms.get(targetAtomUri);

        if (!targetAtomPresent) return false;

        const targetAtomActiveOrLoading =
          process.getIn(["atoms", targetAtomUri, "loading"]) ||
          process.getIn(["atoms", targetAtomUri, "failedToLoad"]) ||
          atomUtils.isActive(get(allAtoms, targetAtomUri));

        return targetAtomActiveOrLoading;
      }),
      "creationDate"
    );
    return sortedConnections || [];
  }
}
WonConnectionsOverview.propTypes = {
  ownedAtoms: PropTypes.object,
  allAtoms: PropTypes.object,
  process: PropTypes.object,
  connUriInRoute: PropTypes.string,
  sortedOpenAtomUris: PropTypes.arrayOf(PropTypes.string),
  selectTab: PropTypes.func,
  connectionMarkAsRead: PropTypes.func,
  history: PropTypes.object,
};

export default withRouter(
  connect(
    mapStateToProps,
    mapDispatchToProps
  )(WonConnectionsOverview)
);
