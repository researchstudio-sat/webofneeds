import React from "react";
import { useDispatch, useSelector } from "react-redux";
import { useHistory } from "react-router-dom";
import { actionCreators } from "../../actions/actions";
import PropTypes from "prop-types";
import {
  extractAtomUriFromConnectionUri,
  generateLink,
  get,
  getUri,
} from "../../utils";
import vocab from "../../service/vocab";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import * as connectionUtils from "../../redux/utils/connection-utils";

import "~/style/_socket-actions.scss";
import ico32_message_nocircle from "~/images/won-icons/ico32_message_nocircle.svg";
import ico32_buddy_add from "~/images/won-icons/ico32_buddy_add.svg";
import ico32_buddy_deny from "~/images/won-icons/ico32_buddy_deny.svg";
import ico32_buddy_accept from "~/images/won-icons/ico32_buddy_accept.svg";
import ico32_buddy_waiting from "~/images/won-icons/ico32_buddy_waiting.svg";
import * as atomUtils from "../../redux/utils/atom-utils";

export default function WonBuddySocketActions({ connection, goBackOnAction }) {
  const dispatch = useDispatch();
  const history = useHistory();
  const connectionState = get(connection, "state");
  const connectionUri = getUri(connection);

  const senderAtom = useSelector(
    generalSelectors.getAtom(extractAtomUriFromConnectionUri(connectionUri))
  );
  const targetAtom = useSelector(
    generalSelectors.getAtom(connectionUtils.getTargetAtomUri(connection))
  );

  function openRequest() {
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
      actionCreators.atoms__connectSockets(senderSocketUri, targetSocketUri)
    );
    if (goBackOnAction) {
      history.goBack();
    }
  }

  function requestBuddy() {
    const payload = {
      caption: "Buddy",
      text: "Send Buddy Request?",
      buttons: [
        {
          caption: "Yes",
          callback: () => {
            const senderSocketUri = get(connection, "socketUri");
            const targetSocketUri = get(connection, "targetSocketUri");

            dispatch(
              actionCreators.atoms__connectSockets(
                senderSocketUri,
                targetSocketUri
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

  function closeConnection(dialogText = "Remove Buddy?") {
    const payload = {
      caption: "Buddy",
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

  function sendChatMessage() {
    const senderSocketUri = atomUtils.getSocketUri(
      senderAtom,
      vocab.CHAT.ChatSocketCompacted
    );
    const targetSocketUri = atomUtils.getSocketUri(
      targetAtom,
      vocab.CHAT.ChatSocketCompacted
    );

    const chatConnection = atomUtils.getConnectionBySocketUris(
      senderAtom,
      senderSocketUri,
      targetSocketUri
    );

    if (
      !chatConnection ||
      connectionUtils.isSuggested(chatConnection) ||
      connectionUtils.isClosed(chatConnection) ||
      connectionUtils.isRequestReceived(chatConnection)
    ) {
      dispatch(
        actionCreators.atoms__connectSockets(senderSocketUri, targetSocketUri)
      );
    }

    const link = chatConnection
      ? generateLink(
          history.location,
          {
            postUri: getUri(senderAtom),
            connectionUri: getUri(chatConnection),
          },
          "/connections"
        )
      : "/connections";

    if (goBackOnAction) {
      history.replace(link);
    } else {
      history.push(link);
    }
  }

  switch (connectionState) {
    case vocab.WON.RequestReceived:
      return (
        <won-socket-actions>
          {closeButtonElement("Reject", "Reject Buddy Request?")}
          <button className="won-button--filled primary" onClick={openRequest}>
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
          {closeButtonElement("Cancel", "Cancel Buddy Request?")}
          <button className="won-button--filled primary" disabled={true}>
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
          {closeButtonElement("Remove", "Remove Buddy?")}
          {atomUtils.hasChatSocket(targetAtom) ? (
            <button
              className="won-button--filled primary"
              onClick={sendChatMessage}
            >
              <svg className="won-button-icon">
                <use
                  xlinkHref={ico32_message_nocircle}
                  href={ico32_message_nocircle}
                />
              </svg>
              <span className="won-button-label">Message</span>
            </button>
          ) : (
            undefined
          )}
        </won-socket-actions>
      );

    case vocab.WON.Closed:
      return (
        <won-socket-actions>
          <button className="won-button--filled primary" onClick={requestBuddy}>
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
          {closeButtonElement("Remove", "Remove Suggestion?")}
          <button className="won-button--filled primary" onClick={requestBuddy}>
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
WonBuddySocketActions.propTypes = {
  connection: PropTypes.object.isRequired,
  goBackOnAction: PropTypes.bool,
};
