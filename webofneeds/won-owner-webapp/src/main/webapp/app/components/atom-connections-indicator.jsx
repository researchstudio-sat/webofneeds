/**
 * Created by sigpie on 21.09.2019.
 */
import React from "react";
import { useDispatch } from "react-redux";
import PropTypes from "prop-types";
import { get, generateLink } from "../utils.js";

import * as atomUtils from "../redux/utils/atom-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";

import "~/style/_atom-connections-indicator.scss";
import ico36_message from "~/images/won-icons/ico36_message.svg";
import ico36_incoming from "~/images/won-icons/ico36_incoming.svg";
import ico36_match from "~/images/won-icons/ico36_match.svg";
import { Link, useHistory } from "react-router-dom";
import vocab from "../service/vocab";
import { actionCreators } from "../actions/actions";
import Immutable from "immutable";

export default function WonAtomConnectionsIndicator({ atom }) {
  //TODO: REWORK THE WHOLE THING BASED ON CONNECTIONS between generic-sockets
  const atomUri = get(atom, "uri");
  const dispatch = useDispatch();
  const history = useHistory();
  const requests = atomUtils.getRequestReceivedConnections(atom);
  const unreadRequests = requests.filter(conn =>
    connectionUtils.isUnread(conn)
  );
  const hasUnreadRequest = unreadRequests && unreadRequests.size > 0;

  const suggestedConnections = atomUtils.getSuggestedConnections(atom);

  const suggestionsCount = suggestedConnections ? suggestedConnections.size : 0;
  const unreadSuggestions =
    suggestedConnections &&
    suggestedConnections.filter(conn => conn.get("unread"));
  const unreadSuggestionsCount = unreadSuggestions ? unreadSuggestions.size : 0;

  const unreadChats = atomUtils
    .getConnectedConnections(atom)
    .filter(conn => connectionUtils.isUnread(conn));
  const hasUnreadChats = !!unreadChats && unreadChats.size > 0;

  const requestsCount = requests ? requests.size : 0;
  const unreadRequestsCount = unreadRequests ? unreadRequests.size : 0;
  // TODO: unread msgs count?

  const hasNoUnreadConnections = !requestsCount > 0 && !hasUnreadChats;

  function getRoute() {
    const connUri = hasUnreadChats
      ? get(unreadChats.first(), "uri")
      : get(unreadRequests.first(), "uri");

    return generateLink(
      history.location,
      { postUri: atomUri, connectionUri: connUri },
      "/connections",
      false
    );
  }

  function showAtomSuggestions() {
    dispatch(
      actionCreators.atoms__selectTab(
        Immutable.fromJS({
          atomUri: atomUri,
          selectTab: vocab.CHAT.ChatSocketCompacted,
        })
      ) //TODO: The suggestions indicator should link to the latest type of connection (since the atomTab SUGGESTIONS is not available any longer)
    );
    history.push(
      generateLink(
        history.location,
        { postUri: atomUri, tab: vocab.CHAT.ChatSocketCompacted },
        "/post"
      )
    );
  }

  if (hasUnreadRequest) {
    return (
      <Link
        className={
          "won-atom-connections-indicator " +
          (hasNoUnreadConnections ? "won-no-connections" : "")
        }
        to={getRoute()}
      >
        <svg
          className={
            "asi__icon " +
            (hasUnreadChats || unreadRequestsCount > 0
              ? "asi__icon--unreads"
              : "asi__icon--reads")
          }
        >
          <use
            xlinkHref={hasUnreadChats ? ico36_message : ico36_incoming}
            href={hasUnreadChats ? ico36_message : ico36_incoming}
          />
        </svg>
        <div className="asi__right">
          <div className="asi__right__topline">
            <div className="asi__right__topline__title">
              {hasUnreadChats ? "Unread Messages" : "Connection Requests"}
            </div>
          </div>
          <div className="asi__right__subtitle">
            <div className="asi__right__subtitle__label">
              <span>
                {hasUnreadChats
                  ? "You have unread Chat Messages"
                  : requestsCount + " Requests"}
              </span>
              {!hasUnreadChats && unreadRequestsCount > 0 ? (
                <span>{", " + unreadRequestsCount + " new"}</span>
              ) : null}
            </div>
          </div>
        </div>
      </Link>
    );
  } else {
    return (
      <won-atom-connections-indicator
        class={!suggestionsCount > 0 ? "won-no-connections" : ""}
        onClick={showAtomSuggestions}
      >
        <svg
          className={
            "asi__icon " +
            (unreadSuggestionsCount > 0
              ? "asi__icon--unreads"
              : "asi__icon--reads")
          }
        >
          <use xlinkHref={ico36_match} href={ico36_match} />
        </svg>
        <div className="asi__right">
          <div className="asi__right__topline">
            <div className="asi__right__topline__title">Suggestions</div>
          </div>
          <div className="asi__right__subtitle">
            <div className="asi__right__subtitle__label">
              <span>{suggestionsCount + " Suggestions"}</span>
              {unreadSuggestionsCount > 0 ? (
                <span>{", " + unreadSuggestionsCount + " new"}</span>
              ) : null}
            </div>
          </div>
        </div>
      </won-atom-connections-indicator>
    );
  }
}
WonAtomConnectionsIndicator.propTypes = { atom: PropTypes.object };
