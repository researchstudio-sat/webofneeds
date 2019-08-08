/**
 * Component for rendering the icon of a groupChat (renders participants icons)
 * Can be included with either:
 *    connection-uri: then the participants of the targetAtom are shown
 *    atom-uri: then the participants of the atom behind the atom uri are shown
 * Created by quasarchimaere on 15.01.2019.
 */
import React from "react";
import { actionCreators } from "../actions/actions.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as connectionSelectors from "../redux/selectors/connection-selectors.js";
import { get, getIn, sortByDate } from "../utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";

import "~/style/_connection-indicators.scss";
import PropTypes from "prop-types";

export default class WonConnectionIndicators extends React.Component {
  static propTypes = {
    atomUri: PropTypes.string.isRequired,
    ngRedux: PropTypes.object.isRequired,
    onClick: PropTypes.func.isRequired,
  };

  componentDidMount() {
    this.atomUri = this.props.atomUri;
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

  UNSAFE_componentWillReceiveProps(nextProps) {
    this.atomUri = nextProps.atomUri;
    this.setState(this.selectFromState(this.props.ngRedux.getState()));
  }

  selectFromState(state) {
    const ownedPosts = generalSelectors.getOwnedPosts(state);
    const allPosts = generalSelectors.getPosts(state);
    const ownedPost = ownedPosts && ownedPosts.get(this.atomUri);
    const chatConnectionsByAtomUri =
      this.atomUri &&
      connectionSelectors.getChatConnectionsByAtomUri(state, this.atomUri);

    const connected =
      chatConnectionsByAtomUri &&
      chatConnectionsByAtomUri.filter(conn => {
        const targetAtomUri = conn.get("targetAtomUri");
        const targetAtomActiveOrLoading =
          targetAtomUri &&
          allPosts &&
          allPosts.get(targetAtomUri) &&
          (getIn(state, ["process", "atoms", targetAtomUri, "loading"]) ||
            atomUtils.isActive(get(allPosts, targetAtomUri)));

        return (
          targetAtomActiveOrLoading &&
          (connectionSelectors.isChatToXConnection(allPosts, conn) ||
            connectionSelectors.isGroupToXConnection(allPosts, conn)) &&
          !(connectionUtils.isSuggested(conn) || connectionUtils.isClosed(conn))
        );
      });

    const unreadConnected =
      connected && !!connected.find(conn => conn.get("unread"));

    return {
      ownedPost,
      postLoading:
        !ownedPost ||
        getIn(state, ["process", "atoms", ownedPost.get("uri"), "loading"]),
      unreadConnected,
      latestConnectedUri: this.retrieveLatestUri(connected),
    };
  }

  /**
   * This method returns either the latest unread uri of the given connection elements, or the latest uri of a read connection, if nothing is found undefined is returned
   * @param elements connection elements to retrieve the latest uri from
   * @returns {*}
   */
  retrieveLatestUri(elements) {
    const unreadElements =
      elements && elements.filter(conn => conn.get("unread"));

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
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div />;
    }

    if (this.state.postLoading) {
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
              (!this.state.unreadConnected && this.state.latestConnectedUri
                ? " indicators__item--reads "
                : "") +
              (this.state.unreadConnected && this.state.latestConnectedUri
                ? " indicators__item--unreads "
                : "") +
              (!this.state.latestConnectedUri
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
    if (this.state.latestConnectedUri) {
      this.props.onClick(this.state.latestConnectedUri);
    }
  }
}
