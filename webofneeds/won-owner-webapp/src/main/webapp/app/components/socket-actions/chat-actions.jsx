import React from "react";
import { useDispatch } from "react-redux";
import { useHistory } from "react-router-dom";
import { actionCreators } from "../../actions/actions";
import PropTypes from "prop-types";
import { get } from "../../utils";
import vocab from "../../service/vocab";
import * as connectionUtils from "../../redux/utils/connection-utils";

import "~/style/_socket-actions.scss";
import ico16_checkmark from "~/images/won-icons/ico16_checkmark.svg";
import ico36_close from "~/images/won-icons/ico36_close.svg";
import ico36_outgoing from "~/images/won-icons/ico36_outgoing.svg";

export default function WonChatSocketActions({ connection, goBackOnAction }) {
  const dispatch = useDispatch();
  const history = useHistory();
  const connectionState = get(connection, "state");
  const connectionUri = get(connection, "uri");

  function closeConnection(
    dialogText = "Do you want to remove the Connection?"
  ) {
    if (!connection) {
      return;
    }

    const payload = {
      caption: "Chat",
      text: dialogText,
      buttons: [
        {
          caption: "Yes",
          callback: () => {
            if (connectionUtils.isUnread(connection)) {
              dispatch(
                actionCreators.connections__markAsRead({
                  connectionUri: connectionUri,
                })
              );
            }

            dispatch(actionCreators.connections__close(connectionUri));
            dispatch(actionCreators.view__hideModalDialog());
            if (goBackOnAction) {
              history.goBack();
            }
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

  function openRequest(message = "") {
    if (!connection) {
      return;
    }

    if (connectionUtils.isUnread(connection)) {
      dispatch(
        actionCreators.connections__markAsRead({
          connectionUri: connectionUri,
        })
      );
    }

    const senderSocketUri = get(connection, "socketUri");
    const targetSocketUri = get(connection, "targetSocketUri");
    dispatch(
      actionCreators.atoms__connectSockets(
        senderSocketUri,
        targetSocketUri,
        message
      )
    );
    if (goBackOnAction) {
      history.goBack();
    }
  }

  function sendRequest(message = "") {
    if (!connection) {
      return;
    }

    const payload = {
      caption: "Chat",
      text: "Do you want to send a Request?",
      buttons: [
        {
          caption: "Yes",
          callback: () => {
            const senderSocketUri = get(connection, "socketUri");
            const targetSocketUri = get(connection, "targetSocketUri");

            if (connectionUtils.isUnread(connection)) {
              dispatch(
                actionCreators.connections__markAsRead({
                  connectionUri: connectionUri,
                })
              );
            }

            dispatch(
              actionCreators.connections__rate(
                connectionUri,
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
            if (goBackOnAction) {
              history.goBack();
            }
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

  const closeButtonElement = (label, dialogText) => (
    <button
      className="won-button--outlined white"
      onClick={() => closeConnection(dialogText)}
    >
      <svg className="won-button-icon">
        <use xlinkHref={ico36_close} href={ico36_close} />
      </svg>
      <span className="won-button-label">{label}</span>
    </button>
  );

  switch (connectionState) {
    case vocab.WON.RequestReceived:
      return (
        <won-socket-actions>
          {closeButtonElement("Reject", "Reject Request?")}
          <button
            className="won-button--filled red"
            onClick={() => openRequest()}
          >
            <svg className="won-button-icon">
              <use xlinkHref={ico16_checkmark} href={ico16_checkmark} />
            </svg>
            <span className="won-button-label">Accept</span>
          </button>
        </won-socket-actions>
      );

    case vocab.WON.RequestSent:
      return (
        <won-socket-actions>
          {closeButtonElement("Cancel", "Cancel Request?")}
          <button className="won-button--filled red" disabled={true}>
            <svg className="won-button-icon">
              <use xlinkHref={ico36_outgoing} href={ico36_outgoing} />
            </svg>
            <span className="won-button-label">Pending...</span>
          </button>
        </won-socket-actions>
      );

    case vocab.WON.Connected:
      return (
        <won-socket-actions>
          {closeButtonElement("Close", "Do you want to close the Chat?")}
        </won-socket-actions>
      );

    case vocab.WON.Closed:
      return (
        <won-socket-actions>
          <button
            className="won-button--filled red"
            onClick={() => sendRequest()}
          >
            <svg className="won-button-icon">
              <use xlinkHref={ico16_checkmark} href={ico16_checkmark} />
            </svg>
            <span className="won-button-label">Reopen</span>
          </button>
        </won-socket-actions>
      );

    case vocab.WON.Suggested:
      return (
        <won-socket-actions>
          {closeButtonElement("Remove", "Remove Suggestion?")}
          <button
            className="won-button--filled red"
            onClick={() => sendRequest()}
          >
            <svg className="won-button-icon">
              <use xlinkHref={ico16_checkmark} href={ico16_checkmark} />
            </svg>
            <span className="won-button-label">Request</span>
          </button>
        </won-socket-actions>
      );

    default:
      return <won-socket-actions>Unknown State</won-socket-actions>;
  }
}
WonChatSocketActions.propTypes = {
  connection: PropTypes.object.isRequired,
  goBackOnAction: PropTypes.bool,
};
