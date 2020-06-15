import React from "react";
import { useDispatch } from "react-redux";
import { actionCreators } from "../../actions/actions";
import PropTypes from "prop-types";
import { get } from "../../utils";
import vocab from "../../service/vocab";
import * as connectionUtils from "../../redux/utils/connection-utils";

import "~/style/_socket-actions.scss";
import ico16_checkmark from "~/images/won-icons/ico16_checkmark.svg";
import ico36_close from "~/images/won-icons/ico36_close.svg";
import ico36_outgoing from "~/images/won-icons/ico36_outgoing.svg";

export default function WonGenericSocketActions({ connection }) {
  const dispatch = useDispatch();
  const connectionState = get(connection, "state");
  const connectionUri = get(connection, "uri");

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
                  connectionUri: connectionUri,
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
          connectionUri: connectionUri,
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
                  connectionUri: connectionUri,
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

  switch (connectionState) {
    case vocab.WON.RequestReceived:
      return (
        <won-socket-actions>
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
        </won-socket-actions>
      );

    case vocab.WON.RequestSent:
      return (
        <won-socket-actions>
          <svg className="si__actions__icon disabled won-icon" disabled={true}>
            <use xlinkHref={ico36_outgoing} href={ico36_outgoing} />
          </svg>
          <svg
            className="si__actions__icon secondary won-icon"
            onClick={() => closeConnection(connection, "Cancel Request?")}
          >
            <use xlinkHref={ico36_close} href={ico36_close} />
          </svg>
        </won-socket-actions>
      );

    case vocab.WON.Connected:
      return (
        <won-socket-actions>
          <svg
            className="si__actions__icon secondary won-icon"
            onClick={() => closeConnection(connection)}
          >
            <use xlinkHref={ico36_close} href={ico36_close} />
          </svg>
        </won-socket-actions>
      );

    case vocab.WON.Closed:
      return (
        <won-socket-actions>Connection has been closed</won-socket-actions>
      );

    case vocab.WON.Suggested:
      return (
        <won-socket-actions>
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
        </won-socket-actions>
      );

    default:
      return <won-socket-actions>Unknown State</won-socket-actions>;
  }
}
WonGenericSocketActions.propTypes = {
  connection: PropTypes.object.isRequired,
};
