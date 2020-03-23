/**
 * Component for rendering the icon of a groupChat (renders participants icons)
 * Can be included with either:
 *    connection-uri: then the participants of the targetAtom are shown
 *    atom-uri: then the participants of the atom behind the atom uri are shown
 * Created by quasarchimaere on 15.01.2019.
 */
import React from "react";
import { connect } from "react-redux";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as connectionSelectors from "../redux/selectors/connection-selectors.js";
import * as processSelectors from "../redux/selectors/process-selectors.js";
import { get, sortByDate } from "../utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";

import "~/style/_connection-indicators.scss";
import PropTypes from "prop-types";
import vocab from "../service/vocab";

const mapStateToProps = (state, ownProps) => {
  const ownedAtoms = generalSelectors.getOwnedAtoms(state);
  const allAtoms = generalSelectors.getAtoms(state);
  const ownedAtom = get(ownedAtoms, ownProps.atomUri);
  const chatConnectionsByAtomUri =
    ownProps.atomUri &&
    connectionSelectors.getChatConnectionsByAtomUri(state, ownProps.atomUri);

  const connected =
    chatConnectionsByAtomUri &&
    chatConnectionsByAtomUri.filter(conn => {
      const senderSocketUri = get(conn, "socketUri");
      const senderSocketType = atomUtils.getSocketType(
        ownedAtom,
        senderSocketUri
      );

      if (
        !(
          senderSocketType === vocab.CHAT.ChatSocketCompacted ||
          senderSocketType === vocab.GROUP.GroupSocketCompacted
        )
      ) {
        return false;
      }

      if (connectionUtils.isSuggested(conn) || connectionUtils.isClosed(conn)) {
        return false;
      }

      const targetAtomUri = get(conn, "targetAtomUri");
      const targetAtom = get(allAtoms, targetAtomUri);

      return (
        targetAtom &&
        (atomUtils.isActive(targetAtom) ||
          processSelectors.isAtomLoading(state, targetAtomUri))
      );
    });

  const unreadConnected =
    connected && !!connected.find(conn => conn.get("unread"));

  /**
   * This method returns either the latest unread uri of the given connection elements, or the latest uri of a read connection, if nothing is found undefined is returned
   * @param elements connection elements to retrieve the latest uri from
   * @returns {*}
   */
  const retrieveLatestUri = elements => {
    const unreadElements =
      elements && elements.filter(conn => get(conn, "unread"));

    const sortedUnreadElements = sortByDate(unreadElements);
    const unreadUri =
      sortedUnreadElements &&
      sortedUnreadElements[0] &&
      sortedUnreadElements[0].get("uri");

    if (unreadUri) {
      return unreadUri;
    } else {
      const sortedElements = sortByDate(elements);
      return (
        sortedElements && sortedElements[0] && sortedElements[0].get("uri")
      );
    }
  };

  return {
    atomUri: ownProps.atomUri,
    onClick: ownProps.onClick,
    postLoading:
      !ownedAtom || processSelectors.isAtomLoading(state, ownProps.atomUri),
    unreadConnected,
    latestConnectedUri: retrieveLatestUri(connected),
  };
};

class WonConnectionIndicators extends React.Component {
  render() {
    if (this.props.postLoading) {
      return (
        <won-connection-indicators class="won-is-loading">
          <div className="indicators__item indicators__item--skeleton">
            <svg className="indicators__item__icon">
              <use xlinkHref="#ico36_message" href="#ico36_message" />
            </svg>
            <span className="indicators__item__caption" />
          </div>
        </won-connection-indicators>
      );
    } else {
      return (
        <won-connection-indicators>
          <a
            className={
              "indicators__item " +
              (!this.props.unreadConnected && this.props.latestConnectedUri
                ? " indicators__item--reads "
                : "") +
              (this.props.unreadConnected && this.props.latestConnectedUri
                ? " indicators__item--unreads "
                : "") +
              (!this.props.latestConnectedUri
                ? " indicators__item--disabled "
                : "")
            }
            onClick={() => this.setOpen()}
          >
            <svg
              className="indicators__item__icon"
              title="Show latest message/request"
            >
              <use xlinkHref="#ico36_message" href="#ico36_message" />
            </svg>
          </a>
        </won-connection-indicators>
      );
    }
  }

  setOpen() {
    if (this.props.latestConnectedUri) {
      this.props.onClick(this.props.latestConnectedUri);
    }
  }
}
WonConnectionIndicators.propTypes = {
  atomUri: PropTypes.string.isRequired,
  onClick: PropTypes.func.isRequired,
  postLoading: PropTypes.bool,
  unreadConnected: PropTypes.bool,
  latestConnectedUri: PropTypes.string,
};

export default connect(mapStateToProps)(WonConnectionIndicators);
