import React from "react";

import PropTypes from "prop-types";
import WonAtomCard from "../../atom-card.jsx";
import { useSelector, useDispatch } from "react-redux";
import * as generalSelectors from "../../../redux/selectors/general-selectors.js";
import * as atomUtils from "../../../redux/utils/atom-utils.js";
import { get, getIn, getQueryParams, generateLink } from "../../../utils.js";
import { actionCreators } from "../../../actions/actions.js";

import "~/style/_suggest-atom-viewer.scss";
import * as processUtils from "../../../redux/utils/process-utils.js";
import { useHistory, Link } from "react-router-dom";

export default function WonSuggestAtomViewer({ content, detail, className }) {
  const dispatch = useDispatch();
  const history = useHistory();

  const { connectionUri } = getQueryParams(history.location);
  const openedOwnPost = useSelector(state =>
    generalSelectors.getOwnedAtomByConnectionUri(state, connectionUri)
  );
  const suggestedAtom = useSelector(state => getIn(state, ["atoms", content]));
  const suggestedAtomUri = get(suggestedAtom, "uri");

  const connectionsOfOpenedOwnPost = get(openedOwnPost, "connections");
  const connectionsBetweenPosts =
    suggestedAtomUri &&
    connectionsOfOpenedOwnPost &&
    connectionsOfOpenedOwnPost.filter(
      conn => get(conn, "targetAtomUri") === suggestedAtomUri
    );

  const hasConnectionBetweenPosts =
    connectionsBetweenPosts && connectionsBetweenPosts.size > 0;

  const processState = useSelector(generalSelectors.getProcessState);

  const isLoading = processUtils.isAtomLoading(processState, content);
  const toLoad = processUtils.isAtomToLoad(processState, content);
  const failedToLoad = processUtils.hasAtomFailedToLoad(processState, content);

  const fetchedSuggestion = !isLoading && !toLoad && !failedToLoad;
  const isSuggestedOwned = useSelector(state =>
    generalSelectors.isAtomOwned(state, suggestedAtomUri)
  );

  const hasChatSocket = atomUtils.hasChatSocket(suggestedAtom);
  const hasGroupSocket = atomUtils.hasGroupSocket(suggestedAtom);
  const chatSocketUri = atomUtils.getChatSocket(suggestedAtom);
  const groupSocketUri = atomUtils.getGroupSocket(suggestedAtom);
  const ownChatSocketUri = atomUtils.getChatSocket(openedOwnPost);

  const showConnectAction =
    suggestedAtom &&
    fetchedSuggestion &&
    atomUtils.isActive(suggestedAtom) &&
    !hasGroupSocket &&
    hasChatSocket &&
    !hasConnectionBetweenPosts &&
    !isSuggestedOwned &&
    openedOwnPost;
  const showJoinAction =
    suggestedAtom &&
    fetchedSuggestion &&
    atomUtils.isActive(suggestedAtom) &&
    hasGroupSocket &&
    !hasChatSocket &&
    !hasConnectionBetweenPosts &&
    !isSuggestedOwned &&
    openedOwnPost;

  const showActions =
    failedToLoad ||
    showConnectAction ||
    showJoinAction ||
    hasConnectionBetweenPosts;
  const currentLocation = useSelector(generalSelectors.getCurrentLocation);
  const establishedConnectionUri =
    hasConnectionBetweenPosts && get(connectionsBetweenPosts.first(), "uri");

  function getInfoText() {
    if (isLoading) {
      return "Loading Atom...";
    } else if (toLoad) {
      return "Atom marked toLoad";
    } else if (failedToLoad) {
      return "Failed to load Atom";
    } else if (atomUtils.isInactive(suggestedAtom)) {
      return "This Atom is inactive";
    }

    if (atomUtils.isPersona(suggestedAtom)) {
      return isSuggestedOwned
        ? "This is one of your Personas"
        : "This is someone elses Persona";
    } else if (hasConnectionBetweenPosts) {
      return "Already established a Connection with this Atom";
    } else if (isSuggestedOwned) {
      return "This is one of your own Atoms";
    } else if (hasChatSocket && !hasGroupSocket) {
      return "Click 'Connect' to connect with this Atom";
    } else if (!hasChatSocket && hasGroupSocket) {
      return "Click 'Join' to connect with this Group";
    }

    return "Click on the Icon to view Details";
  }

  function reloadSuggestion() {
    if (content && failedToLoad) {
      dispatch(actionCreators.atoms__fetchUnloadedAtom(content));
    }
  }

  function connectWithPost() {
    const openedOwnPostUri = get(openedOwnPost, "uri");

    if (openedOwnPostUri && suggestedAtomUri) {
      let targetSocketUri;

      if (groupSocketUri) {
        targetSocketUri = groupSocketUri;
      } else if (chatSocketUri) {
        targetSocketUri = chatSocketUri;
      }

      let senderSocketUri;
      if (ownChatSocketUri) {
        senderSocketUri = ownChatSocketUri;
      }

      dispatch(
        actionCreators.atoms__connectSockets(
          senderSocketUri,
          targetSocketUri,
          "Hey a Friend told me about you, let's chat!"
        )
      );
    } else {
      console.warn(
        "No Connect, either openedOwnPost(Uri) or suggestedAtom(Uri) not present"
      );
    }
  }

  const icon = detail.icon && (
    <svg className="suggestatomv__header__icon">
      <use xlinkHref={detail.icon} href={detail.icon} />
    </svg>
  );

  const label = detail.icon && (
    <span className="suggestatomv__header__label">{detail.label}</span>
  );

  return (
    <won-suggest-atom-viewer class={className}>
      <div className="suggestatomv__header">
        {icon}
        {label}
      </div>
      <div className="suggestatomv__content">
        <div className="suggestatomv__content__post">
          <WonAtomCard
            atom={suggestedAtom}
            currentLocation={currentLocation}
            showHolder={true}
            showSuggestions={false}
          />
          {showActions ? (
            <div className="suggestatomv__content__post__actions">
              {failedToLoad ? (
                <button
                  className="suggestatomv__content__post__actions__button won-button--outlined thin red"
                  onClick={reloadSuggestion.bind(this)}
                >
                  Reload
                </button>
              ) : (
                undefined
              )}
              {showConnectAction ? (
                <button
                  className="suggestatomv__content__post__actions__button won-button--outlined thin red"
                  onClick={connectWithPost.bind(this)}
                >
                  Connect
                </button>
              ) : (
                undefined
              )}
              {showJoinAction ? (
                <button
                  className="suggestatomv__content__post__actions__button won-button--outlined thin red"
                  onClick={connectWithPost.bind(this)}
                >
                  Join
                </button>
              ) : (
                undefined
              )}
              {hasConnectionBetweenPosts ? (
                <Link
                  className="suggestatomv__content__post__actions__button won-button--outlined thin red"
                  to={location =>
                    generateLink(location, {
                      connectionUri: establishedConnectionUri,
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
        <div className="suggestatomv__content__info">{getInfoText()}</div>
      </div>
    </won-suggest-atom-viewer>
  );
}
WonSuggestAtomViewer.propTypes = {
  content: PropTypes.string, //atomUri in this case
  detail: PropTypes.object,
  className: PropTypes.string,
};
