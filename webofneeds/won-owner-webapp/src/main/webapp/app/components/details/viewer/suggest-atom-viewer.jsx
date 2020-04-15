import React from "react";

import PropTypes from "prop-types";
import WonAtomCard from "../../atom-card.jsx";
import { connect } from "react-redux";
import * as generalSelectors from "../../../redux/selectors/general-selectors.js";
import * as atomUtils from "../../../redux/utils/atom-utils.js";
import { get, getIn, getQueryParams } from "../../../utils.js";
import { actionCreators } from "../../../actions/actions.js";

import "~/style/_suggest-atom-viewer.scss";
import * as processSelectors from "../../../redux/selectors/process-selectors";
import { withRouter, Link } from "react-router-dom";
import { generateQueryString, getPathname } from "../../../utils";

const mapStateToProps = (state, ownProps) => {
  const { connectionUri } = getQueryParams(ownProps.location);
  const openedConnectionUri = connectionUri;
  const openedOwnPost =
    openedConnectionUri &&
    generalSelectors.getOwnedAtomByConnectionUri(state, openedConnectionUri);
  const connection = getIn(openedOwnPost, ["connections", openedConnectionUri]);

  const suggestedPost = getIn(state, ["atoms", ownProps.content]);
  const suggestedPostUri = get(suggestedPost, "uri");

  const connectionsOfOpenedOwnPost = get(openedOwnPost, "connections");
  const connectionsBetweenPosts =
    suggestedPostUri &&
    connectionsOfOpenedOwnPost &&
    connectionsOfOpenedOwnPost.filter(
      conn => conn.get("targetAtomUri") === suggestedPostUri
    );

  const hasConnectionBetweenPosts =
    connectionsBetweenPosts && connectionsBetweenPosts.size > 0;

  const isLoading = processSelectors.isAtomLoading(state, ownProps.content);
  const toLoad = processSelectors.isAtomToLoad(state, ownProps.content);

  const failedToLoad = processSelectors.hasAtomFailedToLoad(
    state,
    ownProps.content
  );

  const fetchedSuggestion = !isLoading && !toLoad && !failedToLoad;
  const isSuggestedOwned = generalSelectors.isAtomOwned(
    state,
    suggestedPostUri
  );

  const showConnectAction =
    suggestedPost &&
    fetchedSuggestion &&
    atomUtils.isActive(suggestedPost) &&
    !atomUtils.hasGroupSocket(suggestedPost) &&
    atomUtils.hasChatSocket(suggestedPost) &&
    !hasConnectionBetweenPosts &&
    !isSuggestedOwned &&
    openedOwnPost;
  const showJoinAction =
    suggestedPost &&
    fetchedSuggestion &&
    atomUtils.isActive(suggestedPost) &&
    atomUtils.hasGroupSocket(suggestedPost) &&
    !atomUtils.hasChatSocket(suggestedPost) &&
    !hasConnectionBetweenPosts &&
    !isSuggestedOwned &&
    openedOwnPost;

  return {
    content: ownProps.content,
    detail: ownProps.detail,
    suggestedPost,
    openedOwnPost,
    hasChatSocket: atomUtils.hasChatSocket(suggestedPost),
    hasGroupSocket: atomUtils.hasGroupSocket(suggestedPost),
    chatSocketUri: atomUtils.getChatSocket(suggestedPost),
    groupSocketUri: atomUtils.getGroupSocket(suggestedPost),
    ownChatSocketUri: atomUtils.getChatSocket(openedOwnPost),
    ownGroupSocketUri: atomUtils.getGroupSocket(openedOwnPost),
    isSuggestedOwned,
    showActions:
      failedToLoad ||
      showConnectAction ||
      showJoinAction ||
      hasConnectionBetweenPosts,
    showConnectAction,
    showJoinAction,
    isLoading,
    toLoad,
    failedToLoad,
    currentLocation: generalSelectors.getCurrentLocation(state),
    multiSelectType: connection && connection.get("multiSelectType"),
    hasConnectionBetweenPosts,
    establishedConnectionUri:
      hasConnectionBetweenPosts && get(connectionsBetweenPosts.first(), "uri"),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    fetchAtom: uri => {
      dispatch(actionCreators.atoms__fetchUnloadedAtom(uri));
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
  };
};

class WonSuggestAtomViewer extends React.Component {
  render() {
    const icon = this.props.detail.icon && (
      <svg className="suggestatomv__header__icon">
        <use xlinkHref={this.props.detail.icon} href={this.props.detail.icon} />
      </svg>
    );

    const label = this.props.detail.icon && (
      <span className="suggestatomv__header__label">
        {this.props.detail.label}
      </span>
    );

    return (
      <won-suggest-atom-viewer class={this.props.className}>
        <div className="suggestatomv__header">
          {icon}
          {label}
        </div>
        <div className="suggestatomv__content">
          <div className="suggestatomv__content__post">
            <WonAtomCard
              atomUri={this.props.content}
              currentLocation={this.props.currentLocation}
              showHolder={true}
              showSuggestions={false}
            />
            {this.props.showActions ? (
              <div className="suggestatomv__content__post__actions">
                {this.props.failedToLoad ? (
                  <button
                    className="suggestatomv__content__post__actions__button won-button--outlined thin red"
                    onClick={this.reloadSuggestion.bind(this)}
                  >
                    Reload
                  </button>
                ) : (
                  undefined
                )}
                {this.props.showConnectAction ? (
                  <button
                    className="suggestatomv__content__post__actions__button won-button--outlined thin red"
                    onClick={this.connectWithPost.bind(this)}
                  >
                    Connect
                  </button>
                ) : (
                  undefined
                )}
                {this.props.showJoinAction ? (
                  <button
                    className="suggestatomv__content__post__actions__button won-button--outlined thin red"
                    onClick={this.connectWithPost.bind(this)}
                  >
                    Join
                  </button>
                ) : (
                  undefined
                )}
                {this.props.hasConnectionBetweenPosts ? (
                  <Link
                    className="suggestatomv__content__post__actions__button won-button--outlined thin red"
                    to={location =>
                      generateQueryString(getPathname(location), {
                        connectionUri: this.props.establishedConnectionUri,
                      })
                    }
                  >
                    View Chat
                  </Link>
                ) : (
                  undefined
                )}
              </div>
            ) : (
              undefined
            )}
          </div>
          <div className="suggestatomv__content__info">
            {this.getInfoText()}
          </div>
        </div>
      </won-suggest-atom-viewer>
    );
  }

  getInfoText() {
    if (this.props.isLoading) {
      return "Loading Atom...";
    } else if (this.props.toLoad) {
      return "Atom marked toLoad";
    } else if (this.props.failedToLoad) {
      return "Failed to load Atom";
    } else if (atomUtils.isInactive(this.props.suggestedPost)) {
      return "This Atom is inactive";
    }

    if (atomUtils.isPersona(this.props.suggestedPost)) {
      return this.props.isSuggestedOwned
        ? "This is one of your Personas"
        : "This is someone elses Persona";
    } else if (this.props.hasConnectionBetweenPosts) {
      return "Already established a Connection with this Atom";
    } else if (this.props.isSuggestedOwned) {
      return "This is one of your own Atoms";
    } else if (this.props.hasChatSocket && !this.props.hasGroupSocket) {
      return "Click 'Connect' to connect with this Atom";
    } else if (!this.props.hasChatSocket && this.props.hasGroupSocket) {
      return "Click 'Join' to connect with this Group";
    }

    return "Click on the Icon to view Details";
  }

  reloadSuggestion() {
    if (this.props.content && this.props.failedToLoad) {
      this.props.fetchAtom(this.props.content);
    }
  }

  connectWithPost() {
    const openedOwnPostUri =
      this.props.openedOwnPost && this.props.openedOwnPost.get("uri");
    const suggestedPostUri =
      this.props.suggestedPost && this.props.suggestedPost.get("uri");

    if (openedOwnPostUri && suggestedPostUri) {
      let targetSocketUri;

      if (this.props.groupSocketUri) {
        targetSocketUri = this.props.groupSocketUri;
      } else if (this.props.chatSocketUri) {
        targetSocketUri = this.props.chatSocketUri;
      }

      let senderSocketUri;
      if (this.props.ownChatSocketUri) {
        senderSocketUri = this.props.ownChatSocketUri;
      }

      this.props.connectSockets(
        senderSocketUri,
        targetSocketUri,
        "Hey a Friend told me about you, let's chat!"
      );
    } else {
      console.warn(
        "No Connect, either openedOwnPost(Uri) or suggestedPost(Uri) not present"
      );
    }
  }
}
WonSuggestAtomViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.string,
  className: PropTypes.string,
  fetchAtom: PropTypes.func,
  connectSockets: PropTypes.func,
  suggestedPost: PropTypes.object,
  openedOwnPost: PropTypes.object,
  hasChatSocket: PropTypes.bool,
  hasGroupSocket: PropTypes.bool,
  groupSocketUri: PropTypes.string,
  chatSocketUri: PropTypes.string,
  ownGroupSocketUri: PropTypes.string,
  ownChatSocketUri: PropTypes.string,
  isSuggestedOwned: PropTypes.bool,
  showActions: PropTypes.bool,
  showConnectAction: PropTypes.bool,
  showJoinAction: PropTypes.bool,
  isLoading: PropTypes.bool,
  toLoad: PropTypes.bool,
  failedToLoad: PropTypes.bool,
  currentLocation: PropTypes.object,
  multiSelectType: PropTypes.string,
  hasConnectionBetweenPosts: PropTypes.bool,
  establishedConnectionUri: PropTypes.string,
};

export default withRouter(
  connect(
    mapStateToProps,
    mapDispatchToProps
  )(WonSuggestAtomViewer)
);
