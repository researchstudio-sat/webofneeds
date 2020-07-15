import React from "react";
import { useDispatch } from "react-redux";
import { useHistory } from "react-router-dom";
import { actionCreators } from "../../actions/actions";
import PropTypes from "prop-types";
import { get } from "../../utils";
import vocab from "../../service/vocab";
import * as connectionUtils from "../../redux/utils/connection-utils";

import "~/style/_socket-actions.scss";
import ico32_buddy_add from "~/images/won-icons/ico32_buddy_add.svg";
import ico32_buddy_deny from "~/images/won-icons/ico32_buddy_deny.svg";
import ico32_buddy_accept from "~/images/won-icons/ico32_buddy_accept.svg";
import ico32_buddy_waiting from "~/images/won-icons/ico32_buddy_waiting.svg";

export default function WonParticipantSocketActions({
  connection,
  goBackOnAction,
}) {
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
      caption: "Participant",
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
      caption: "Participant",
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
        <use xlinkHref={ico32_buddy_deny} href={ico32_buddy_deny} />
      </svg>
      <span className="won-button-label">{label}</span>
    </button>
  );

  switch (connectionState) {
    case vocab.WON.RequestReceived:
      return (
        <won-socket-actions>
          {closeButtonElement("Reject", "Reject Participant Request?")}
          <button
            className="won-button--filled secondary"
            onClick={() => openRequest()}
          >
            <svg className="won-button-icon">
              <use xlinkHref={ico32_buddy_accept} href={ico32_buddy_accept} />
            </svg>
            <span className="won-button-label">Accept</span>
          </button>
        </won-socket-actions>
      );

    case vocab.WON.RequestSent:
      return (
        <won-socket-actions>
          {closeButtonElement("Cancel", "Cancel Participant Request?")}
          <button className="won-button--filled secondary" disabled={true}>
            <svg className="won-button-icon">
              <use xlinkHref={ico32_buddy_waiting} href={ico32_buddy_waiting} />
            </svg>
            <span className="won-button-label">Pending...</span>
          </button>
        </won-socket-actions>
      );

    case vocab.WON.Connected:
      return (
        <won-socket-actions>
          {closeButtonElement("Remove", "Remove Participant?")}
        </won-socket-actions>
      );

    case vocab.WON.Closed:
      return (
        <won-socket-actions>
          <button
            className="won-button--filled secondary"
            onClick={() => sendRequest()}
          >
            <svg className="won-button-icon">
              <use xlinkHref={ico32_buddy_add} href={ico32_buddy_add} />
            </svg>
            <span className="won-button-label">Reopen</span>
          </button>
        </won-socket-actions>
      );

    case vocab.WON.Suggested:
      return (
        <won-socket-actions>
          {closeButtonElement("Remove", "Remove Participant Suggestion?")}
          <button
            className="won-button--filled secondary"
            onClick={() => sendRequest()}
          >
            <svg className="won-button-icon">
              <use xlinkHref={ico32_buddy_add} href={ico32_buddy_add} />
            </svg>
            <span className="won-button-label">Request</span>
          </button>
        </won-socket-actions>
      );

    default:
      return <won-socket-actions>Unknown State</won-socket-actions>;
  }
}
WonParticipantSocketActions.propTypes = {
  connection: PropTypes.object.isRequired,
  goBackOnAction: PropTypes.bool,
};
