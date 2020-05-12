import React from "react";
import PropTypes from "prop-types";
import { generateLink, get } from "../../utils";
import vocab from "../../service/vocab";
import VisibilitySensor from "react-visibility-sensor";
import * as connectionUtils from "../../redux/utils/connection-utils";
import WonAtomContextSwipeableView from "../atom-context-swipeable-view";
import { actionCreators } from "../../actions/actions";
import { useDispatch } from "react-redux";

import ico16_checkmark from "~/images/won-icons/ico16_checkmark.svg";
import ico36_close from "~/images/won-icons/ico36_close.svg";
import ico36_outgoing from "~/images/won-icons/ico36_outgoing.svg";

import "~/style/_socket-item.scss";

export default function WonGenericItem({ connection, atom, isOwned }) {
  const dispatch = useDispatch();

  let actionButtons;
  let headerClassName;

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

  function closeConnection(
    conn,
    dialogText = "Do you want to remove the Connection?"
  ) {
    if (!conn) {
      return;
    }

    const payload = {
      caption: "Connect",
      text: dialogText,
      buttons: [
        {
          caption: "Yes",
          callback: () => {
            const connUri = get(conn, "uri");

            if (connectionUtils.isUnread(conn)) {
              dispatch(
                actionCreators.connections__markAsRead({
                  connectionUri: get(conn, "uri"),
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

    if (connectionUtils.isUnread(conn)) {
      dispatch(
        actionCreators.connections__markAsRead({
          connectionUri: get(conn, "uri"),
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
      caption: "Connect",
      text: "Do you want to send a Request?",
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
                  connectionUri: get(conn, "uri"),
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

  switch (get(connection, "state")) {
    case vocab.WON.RequestReceived:
      headerClassName = "status--received";
      actionButtons = isOwned ? (
        <div className="si__actions">
          <svg
            className="si__actions__icon request won-icon"
            onClick={() => openRequest(connection)}
          >
            <use xlinkHref={ico16_checkmark} href={ico16_checkmark} />
          </svg>
          <svg
            className="si__actions__icon primary won-icon"
            onClick={() => closeConnection(connection, "Reject Request?")}
          >
            <use xlinkHref={ico36_close} href={ico36_close} />
          </svg>
        </div>
      ) : (
        undefined
      );
      break;

    case vocab.WON.RequestSent:
      headerClassName = "status--sent";
      actionButtons = isOwned ? (
        <div className="si__actions">
          <svg className="si__actions__icon disabled won-icon" disabled={true}>
            <use xlinkHref={ico36_outgoing} href={ico36_outgoing} />
          </svg>
          <svg
            className="si__actions__icon secondary won-icon"
            onClick={() => closeConnection(connection, "Cancel Request?")}
          >
            <use xlinkHref={ico36_close} href={ico36_close} />
          </svg>
        </div>
      ) : (
        undefined
      );
      break;

    case vocab.WON.Connected:
      actionButtons = isOwned ? (
        <div className="si__actions">
          <svg
            className="si__actions__icon secondary won-icon"
            onClick={() => closeConnection(connection)}
          >
            <use xlinkHref={ico36_close} href={ico36_close} />
          </svg>
        </div>
      ) : (
        undefined
      );
      break;

    case vocab.WON.Closed:
      headerClassName = "status--closed";
      actionButtons = isOwned ? (
        <div className="si__actions">Connection has been closed</div>
      ) : (
        undefined
      );
      break;

    case vocab.WON.Suggested:
      headerClassName = "status--suggested";
      actionButtons = isOwned ? (
        <div className="si__actions">
          <svg
            className="si__actions__icon request won-icon"
            onClick={() => sendRequest(connection)}
          >
            <use xlinkHref={ico16_checkmark} href={ico16_checkmark} />
          </svg>
          <svg
            className="si__actions__icon primary won-icon"
            onClick={() => closeConnection(connection, "Remove Suggestion?")}
          >
            <use xlinkHref={ico36_close} href={ico36_close} />
          </svg>
        </div>
      ) : (
        undefined
      );
      break;

    default:
      actionButtons = isOwned ? (
        <div className="si__actions">Unknown State</div>
      ) : (
        undefined
      );
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
          atomUri={get(connection, "targetAtomUri")}
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
WonGenericItem.propTypes = {
  connection: PropTypes.object.isRequired,
  atom: PropTypes.object.isRequired,
  isOwned: PropTypes.bool.isRequired,
};
