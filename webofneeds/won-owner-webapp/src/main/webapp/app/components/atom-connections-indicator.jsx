/**
 * Created by sigpie on 21.09.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import { get } from "../utils.js";

import * as atomUtils from "../redux/utils/atom-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";

import "~/style/_atom-connections-indicator.scss";
import ico36_message from "~/images/won-icons/ico36_message.svg";
import ico36_incoming from "~/images/won-icons/ico36_incoming.svg";
import { Link } from "react-router-dom";

export default function WonAtomConnectionsIndicator({ atom }) {
  const requests = atomUtils.getRequestReceivedConnections(atom);
  const unreadRequests = requests.filter(conn =>
    connectionUtils.isUnread(conn)
  );

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
    return "/connections?connectionUri=" + connUri;
  }

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
}
WonAtomConnectionsIndicator.propTypes = { atom: PropTypes.object.isRequired };
