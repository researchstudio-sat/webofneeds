import React from "react";

import PropTypes from "prop-types";
import WonAtomCard from "../../atom-card.jsx";
import { connect } from "react-redux";
import * as generalSelectors from "../../../redux/selectors/general-selectors.js";
import * as atomUtils from "../../../redux/utils/atom-utils.js";
import { get, getIn } from "../../../utils.js";
import { actionCreators } from "../../../actions/actions.js";

import "~/style/_suggest-atom-viewer.scss";

const mapStateToProps = (state, ownProps) => {
  const openedConnectionUri = generalSelectors.getConnectionUriFromRoute(state);
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

  const isLoading = state.getIn([
    "process",
    "atoms",
    ownProps.content,
    "loading",
  ]);
  const toLoad = state.getIn(["process", "atoms", ownProps.content, "toLoad"]);
  const failedToLoad = state.getIn([
    "process",
    "atoms",
    ownProps.content,
    "failedToLoad",
  ]);

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
    connect: (ownedAtomUri, connectionUri, targetAtomUri, message) => {
      dispatch(
        actionCreators.atoms__connect(
          ownedAtomUri,
          connectionUri,
          targetAtomUri,
          message
        )
      );
    },
    routerGoCurrent: props => {
      dispatch(actionCreators.router__stateGoCurrent(props));
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
              showPersona={true}
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
                  <button
                    className="suggestatomv__content__post__actions__button won-button--outlined thin red"
                    onClick={() =>
                      this.props.routerGoCurrent({
                        connectionUri: this.props.establishedConnectionUri,
                      })
                    }
                  >
                    View Chat
                  </button>
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
      return "Loading Suggestion...";
    } else if (this.props.toLoad) {
      return "Suggestion marked toLoad";
    } else if (this.props.failedToLoad) {
      return "Failed to load Suggestion";
    } else if (atomUtils.isInactive(this.props.suggestedPost)) {
      return "This Suggestion is inactive";
    }

    if (atomUtils.isPersona(this.props.suggestedPost)) {
      return this.props.isSuggestedOwned
        ? "This is one of your Personas"
        : "This is someone elses Persona";
    } else if (this.props.hasConnectionBetweenPosts) {
      return "Already established a Connection with this Suggestion";
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
      this.props.connect(
        this.props.openedOwnPost.get("uri"),
        undefined,
        this.props.suggestedPost.get("uri"),
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
  connect: PropTypes.func,
  suggestedPost: PropTypes.object,
  openedOwnPost: PropTypes.object,
  hasChatSocket: PropTypes.bool,
  hasGroupSocket: PropTypes.bool,
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
  routerGoCurrent: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonSuggestAtomViewer);
