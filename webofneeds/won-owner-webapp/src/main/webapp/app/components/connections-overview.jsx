import React from "react";
import Immutable from "immutable";
import { actionCreators } from "../actions/actions.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as connectionSelectors from "../redux/selectors/connection-selectors.js";
import { get, getIn, sortByDate } from "../utils.js";
import * as processUtils from "../redux/utils/process-utils";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import WonConnectionIndicators from "./connection-indicators.jsx";
import WonConnectionSelectionItem from "./connection-selection-item.jsx";
import WonAtomHeader from "./atom-header.jsx";

import "~/style/_connections-overview.scss";
import PropTypes from "prop-types";

export default class WonConnectionsOverview extends React.Component {
  static propTypes = {
    ngRedux: PropTypes.object.isRequired,
  };

  componentDidMount() {
    this.disconnect = this.props.ngRedux.connect(
      this.selectFromState.bind(this),
      actionCreators
    )(state => {
      this.setState(state);
    });
  }

  componentWillUnmount() {
    this.disconnect();
  }

  UNSAFE_componentWillReceiveProps(/*nextProps*/) {
    this.setState(this.selectFromState(this.props.ngRedux.getState()));
  }

  selectFromState(state) {
    const allAtoms = generalSelectors.getPosts(state);
    const openAtoms = generalSelectors.getChatAtoms(state);

    const connUriInRoute = generalSelectors.getConnectionUriFromRoute(state);

    const sortedOpenAtoms = sortByDate(openAtoms, "creationDate");
    const process = get(state, "process");

    return {
      allAtoms,
      process,
      connUriInRoute,
      sortedOpenAtomUris: sortedOpenAtoms && [
        ...sortedOpenAtoms.flatMap(atom => atom.get("uri")),
      ],
    };
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div />;
    }

    const atomElements =
      this.state.sortedOpenAtomUris &&
      this.state.sortedOpenAtomUris.map(atomUri => {
        const connectionUris = this.getOpenChatConnectionUrisArraySorted(
          atomUri,
          this.state.allAtoms,
          this.state.process
        );

        const connectionElements =
          connectionUris &&
          connectionUris.map(connUri => {
            return (
              <div
                className={
                  "co__item__connections__item " +
                  (this.isConnectionUnread(atomUri, connUri) && " won-unread ")
                }
                key={connUri}
              >
                <WonConnectionSelectionItem
                  connectionUri={connUri}
                  ngRedux={this.props.ngRedux}
                  onClick={() => this.selectConnection(connUri)}
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
                  ngRedux={this.props.ngRedux}
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
                  ngRedux={this.props.ngRedux}
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
    this.props.ngRedux.dispatch(
      actionCreators.atoms__selectTab(
        Immutable.fromJS({ atomUri: atomUri, selectTab: tab })
      )
    );
    this.props.ngRedux.dispatch(
      actionCreators.router__stateGo("post", { postUri: atomUri })
    );
  }

  isConnectionUnread(atomUri, connUri) {
    const conn = getIn(this.state.allAtoms, [atomUri, "connections", connUri]);
    return connectionUtils.isUnread(conn);
  }

  isAtomLoading(atomUri) {
    return processUtils.isAtomLoading(this.process, atomUri);
  }

  selectConnection(connectionUri) {
    this.markAsRead(connectionUri);
    this.props.ngRedux.dispatch(
      actionCreators.router__stateGoCurrent({ connectionUri: connectionUri })
    );
  }

  markAsRead(connectionUri) {
    const atom = generalSelectors.getOwnedAtomByConnectionUri(
      this.props.ngRedux.getState(),
      connectionUri
    );

    if (atom) {
      const connection = getIn(atom, ["connections", connectionUri]);
      const connUnread = connectionUtils.isUnread(connection);
      const connNotConnected = !connectionUtils.isConnected(connection);

      if (connUnread && connNotConnected) {
        this.props.ngRedux.dispatch(
          actionCreators.connections__markAsRead({
            connectionUri: connectionUri,
            atomUri: get(atom, "uri"),
          })
        );
      }
    }
  }

  getOpenChatConnectionUrisArraySorted(atomUri, allAtoms, process) {
    const atom = get(allAtoms, atomUri);

    if (!atom) {
      return undefined;
    }
    const sortedConnections = sortByDate(
      atom.get("connections").filter(conn => {
        if (
          !connectionSelectors.isChatToXConnection(allAtoms, conn) &&
          !connectionSelectors.isGroupToXConnection(allAtoms, conn)
        )
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

        return (
          targetAtomActiveOrLoading &&
          !connectionUtils.isClosed(conn) &&
          !connectionUtils.isSuggested(conn)
        );
      }),
      "creationDate"
    );
    return (
      sortedConnections && [
        ...sortedConnections.flatMap(conn => conn.get("uri")),
      ]
    );
  }
}
