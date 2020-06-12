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
import WonConnectionHeader from "../connection-header";

export default function WonParticipantItem({
  connection,
  atom,
  isOwned,
  targetAtom,
  flip,
}) {
  const history = useHistory();
  const dispatch = useDispatch();

  const addActionButtons = isOwned || flip;
  let actionButtons;
  let headerClassName;

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
                  atomUri: get(atom, "uri"),
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
          atomUri: get(atom, "uri"),
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
                  atomUri: get(atom, "uri"),
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
            atomUri: get(atom, "uri"),
          })
        );
      }, 1500);
    }
  }

  switch (get(connection, "state")) {
    case vocab.WON.RequestReceived: {
      headerClassName = "status--received";
      actionButtons = addActionButtons ? (
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
      ) : (
        undefined
      );
      break;
    }
    case vocab.WON.Suggested: {
      headerClassName = "status--suggested";
      actionButtons = addActionButtons ? (
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
      ) : (
        undefined
      );
      break;
    }
    case vocab.WON.RequestSent: {
      headerClassName = "status--sent";
      actionButtons = addActionButtons ? (
        <div className="si__actions">
          <svg className="si__actions__icon disabled won-icon" disabled={true}>
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
      ) : (
        undefined
      );
      break;
    }
    case vocab.WON.Connected: {
      actionButtons = addActionButtons ? (
        <div className="si__actions">
          <svg
            className="si__actions__icon secondary won-icon"
            onClick={() => closeConnection(connection)}
          >
            <use xlinkHref={ico32_buddy_deny} href={ico32_buddy_deny} />
          </svg>
        </div>
      ) : (
        undefined
      );
      break;
    }
    case vocab.WON.Closed: {
      headerClassName = "status--closed";
      actionButtons = addActionButtons ? (
        <div className="si__actions">Member has been removed</div>
      ) : (
        undefined
      );
      break;
    }
    default: {
      actionButtons = addActionButtons ? (
        <div className="si__actions">Unknown State</div>
      ) : (
        undefined
      );
      break;
    }
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
          "si " + (connectionUtils.isUnread(connection) ? " won-unread " : "")
        }
      >
        <WonAtomContextSwipeableView
          className={headerClassName}
          actionButtons={actionButtons}
        >
          <WonConnectionHeader
            connection={connection}
            toLink={generateLink(
              history.location,
              {
                postUri: flip ? get(atom, "uri") : get(targetAtom, "uri"),
              },
              "/post"
            )}
            flip={flip}
          />
        </WonAtomContextSwipeableView>
      </div>
    </VisibilitySensor>
  );
}
WonParticipantItem.propTypes = {
  connection: PropTypes.object.isRequired,
  atom: PropTypes.object.isRequired,
  isOwned: PropTypes.bool.isRequired,
  targetAtom: PropTypes.object.isRequired,
  flip: PropTypes.bool,
};
