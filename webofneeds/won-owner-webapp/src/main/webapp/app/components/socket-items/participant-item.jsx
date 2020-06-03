/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { get, generateLink } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import * as connectionUtils from "../../redux/utils/connection-utils";
import vocab from "../../service/vocab.js";
import WonAtomContextSwipeableView from "../atom-context-swipeable-view";

import "~/style/_socket-item.scss";
import ico32_buddy_deny from "~/images/won-icons/ico32_buddy_deny.svg";
import ico32_buddy_accept from "~/images/won-icons/ico32_buddy_accept.svg";
import ico32_buddy_waiting from "~/images/won-icons/ico32_buddy_waiting.svg";

import VisibilitySensor from "react-visibility-sensor";
import PropTypes from "prop-types";
import { useHistory } from "react-router-dom";
import { useDispatch } from "react-redux";

export default function WonParticipantItem({
  connection,
  atom,
  isOwned,
  targetAtom,
}) {
  const history = useHistory();
  const dispatch = useDispatch();
  const atomUri = get(atom, "uri");

  function closeConnection(conn, dialogText = "Remove Participant?") {
    if (!conn) {
      return;
    }

    const payload = {
      caption: "Group",
      text: dialogText,
      buttons: [
        {
          caption: "Yes",
          callback: () => {
            const connUri = get(conn, "uri");

            if (connectionUtils.isUnread(conn)) {
              dispatch(
                actionCreators.connections__markAsRead({
                  connectionUri: connUri,
                  atomUri: atomUri,
                })
              );
            }

            dispatch(actionCreators.connections__close(connUri));
            dispatch(actionCreators.view__hideModalDialog());
          },
        },
        {
          caption: "No",
          callback: () => {
            dispatch(actionCreators.view__hideModalDialog());
          },
        },
      ],
    };
    dispatch(actionCreators.view__showModalDialog(payload));
  }

  function openRequest(conn, message = "") {
    if (!conn) {
      return;
    }

    const connUri = get(conn, "uri");

    if (connectionUtils.isUnread(conn)) {
      dispatch(
        actionCreators.connections__markAsRead({
          connectionUri: connUri,
          atomUri: atomUri,
        })
      );
    }

    const senderSocketUri = get(conn, "socketUri");
    const targetSocketUri = get(conn, "targetSocketUri");
    dispatch(
      actionCreators.atoms__connectSockets(
        senderSocketUri,
        targetSocketUri,
        message
      )
    );
  }

  function sendRequest(conn, message = "") {
    if (!conn) {
      return;
    }

    const payload = {
      caption: "Group",
      text: "Add as Participant?",
      buttons: [
        {
          caption: "Yes",
          callback: () => {
            const connUri = get(conn, "uri");
            const senderSocketUri = get(conn, "socketUri");
            const targetSocketUri = get(conn, "targetSocketUri");

            if (connectionUtils.isUnread(conn)) {
              dispatch(
                actionCreators.connections__markAsRead({
                  connectionUri: connUri,
                  atomUri: atomUri,
                })
              );
            }

            dispatch(
              actionCreators.connections__rate(
                connUri,
                vocab.WONCON.binaryRatingGood
              )
            );

            dispatch(
              actionCreators.atoms__connectSockets(
                senderSocketUri,
                targetSocketUri,
                message
              )
            );
            dispatch(actionCreators.view__hideModalDialog());
          },
        },
        {
          caption: "No",
          callback: () => {
            dispatch(actionCreators.view__hideModalDialog());
          },
        },
      ],
    };
    dispatch(actionCreators.view__showModalDialog(payload));
  }

  function markAsRead(conn) {
    if (connectionUtils.isUnread(conn)) {
      setTimeout(() => {
        dispatch(
          actionCreators.connections__markAsRead({
            connectionUri: get(conn, "uri"),
            atomUri: atomUri,
          })
        );
      }, 1500);
    }
  }

  if (isOwned) {
    if (!connectionUtils.isClosed(connection)) {
      let actionButtons;
      let headerClassName;

      if (connectionUtils.isRequestReceived(connection)) {
        headerClassName = "status--received";
        actionButtons = (
          <div className="si__actions">
            <svg
              className="si__actions__icon request won-icon"
              onClick={() => openRequest(connection)}
            >
              <use xlinkHref={ico32_buddy_accept} href={ico32_buddy_accept} />
            </svg>
            <svg
              className="si__actions__icon primary won-icon"
              onClick={() =>
                closeConnection(connection, "Reject Participant Request?")
              }
            >
              <use xlinkHref={ico32_buddy_deny} href={ico32_buddy_deny} />
            </svg>
          </div>
        );
      } else if (connectionUtils.isSuggested(connection)) {
        headerClassName = "status--suggested";
        actionButtons = (
          <div className="si__actions">
            <svg
              className="si_actions__icon request won-icon"
              onClick={() => sendRequest(connection)}
            >
              <use xlinkHref={ico32_buddy_accept} href={ico32_buddy_accept} />
            </svg>
            <svg
              className="si__actions__icon primary won-icon"
              onClick={() =>
                closeConnection(connection, "Remove Participant Suggestion?")
              }
            >
              <use xlinkHref={ico32_buddy_deny} href={ico32_buddy_deny} />
            </svg>
          </div>
        );
      } else if (connectionUtils.isRequestSent(connection)) {
        headerClassName = "status--sent";
        actionButtons = (
          <div className="si__actions">
            <svg
              className="si__actions__icon disabled won-icon"
              disabled={true}
            >
              <use xlinkHref={ico32_buddy_waiting} href={ico32_buddy_waiting} />
            </svg>
            <svg
              className="si__actions__icon secondary won-icon"
              onClick={() =>
                closeConnection(connection, "Cancel Participant Request?")
              }
            >
              <use xlinkHref={ico32_buddy_deny} href={ico32_buddy_deny} />
            </svg>
          </div>
        );
      } else if (connectionUtils.isConnected(connection)) {
        actionButtons = (
          <div className="si__actions">
            <svg
              className="si__actions__icon secondary won-icon"
              onClick={() => closeConnection(connection)}
            >
              <use xlinkHref={ico32_buddy_deny} href={ico32_buddy_deny} />
            </svg>
          </div>
        );
      } else if (connectionUtils.isClosed(connection)) {
        headerClassName = "status--closed";
        actionButtons = (
          <div className="si__actions">Member has been removed</div>
        );
      } else {
        actionButtons = <div className="si__actions">Unknown State</div>;
      }

      return (
        <VisibilitySensor
          key={get(connection, "uri")}
          onChange={isVisible => {
            isVisible &&
              connectionUtils.isUnread(connection) &&
              markAsRead(connection);
          }}
          intervalDelay={2000}
        >
          <div
            className={
              "si " +
              (connectionUtils.isUnread(connection) ? " won-unread " : "")
            }
          >
            <WonAtomContextSwipeableView
              className={headerClassName}
              actionButtons={actionButtons}
              atom={targetAtom}
              toLink={generateLink(
                history.location,
                {
                  postUri: get(connection, "targetAtomUri"),
                },
                "/post"
              )}
            />
          </div>
        </VisibilitySensor>
      );
    }
  } else {
    return (
      <div className="si">
        <WonAtomContextSwipeableView
          atom={targetAtom}
          toLink={generateLink(
            history.location,
            {
              postUri: get(connection, "targetAtomUri"),
            },
            "/post"
          )}
        />
      </div>
    );
  }
}
WonParticipantItem.propTypes = {
  connection: PropTypes.object.isRequired,
  atom: PropTypes.object.isRequired,
  isOwned: PropTypes.bool.isRequired,
  targetAtom: PropTypes.object.isRequired,
};
