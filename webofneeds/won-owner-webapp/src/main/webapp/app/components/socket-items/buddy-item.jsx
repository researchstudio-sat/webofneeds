/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { get, generateLink } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import { useDispatch } from "react-redux";
import { useHistory } from "react-router-dom";
import * as atomUtils from "../../redux/utils/atom-utils";
import * as connectionUtils from "../../redux/utils/connection-utils";
import vocab from "../../service/vocab.js";
import WonAtomContextSwipeableView from "../atom-context-swipeable-view";
import "~/style/_socket-item.scss";
import VisibilitySensor from "react-visibility-sensor";
import PropTypes from "prop-types";

import ico32_buddy_accept from "~/images/won-icons/ico32_buddy_accept.svg";
import ico32_buddy_deny from "~/images/won-icons/ico32_buddy_deny.svg";
import ico32_buddy_waiting from "~/images/won-icons/ico32_buddy_waiting.svg";
import ico36_message from "~/images/won-icons/ico36_message.svg";

export default function WonBuddyItem({ connection, atom, isOwned }) {
  const atomUri = get(atom, "uri");

  const hasBuddySocket = atomUtils.hasBuddySocket(atom);
  const chatConnections =
    isOwned && atomUtils.getConnections(atom, vocab.CHAT.ChatSocketCompacted);

  const hasChatConnections = chatConnections && chatConnections.size > 0;
  const chatConnectionsArray = chatConnections && chatConnections.toArray();

  const dispatch = useDispatch();
  const history = useHistory();

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

  function openRequest(conn, message = "") {
    if (!conn) {
      return;
    }

    if (connectionUtils.isUnread(conn)) {
      dispatch(
        actionCreators.connections__markAsRead({
          connectionUri: get(conn, "uri"),
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

  function requestBuddy(targetAtomUri, message = "") {
    if (!isOwned || !hasBuddySocket) {
      console.warn("Trying to request a non-owned or non buddySocket atom");
      return;
    }

    const payload = {
      caption: "Persona",
      text: "Send Buddy Request?",
      buttons: [
        {
          caption: "Yes",
          callback: () => {
            dispatch(
              actionCreators.atoms__connect(
                atomUri,
                targetAtomUri,
                vocab.CHAT.ChatSocketCompacted,
                vocab.CHAT.ChatSocketCompacted,
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

  function closeConnection(conn, dialogText = "Remove Buddy?") {
    if (!conn) {
      return;
    }

    const payload = {
      caption: "Persona",
      text: dialogText,
      buttons: [
        {
          caption: "Yes",
          callback: () => {
            if (connectionUtils.isUnread(conn)) {
              dispatch(
                actionCreators.connections__markAsRead({
                  connectionUri: get(conn, "uri"),
                  atomUri: atomUri,
                })
              );
            }

            dispatch(actionCreators.connections__close(get(conn, "uri")));
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

  function sendChatMessage(connection) {
    if (chatConnectionsArray && hasChatConnections) {
      //Check if connection is already an existing chatConnection
      const targetAtomUri = get(connection, "targetAtomUri");
      //const targetSocketUri = get(connection, "socketUri");
      const chatConnections = chatConnectionsArray.filter(
        conn => get(conn, "targetAtomUri") === targetAtomUri
      );
      //.filter(conn => get(conn, "socketUri") === targetSocketUri);

      if (chatConnections.length == 0) {
        //No chatConnection between buddies exists => connect
        dispatch(
          actionCreators.atoms__connect(
            atomUri,
            get(connection, "targetAtomUri"),
            vocab.CHAT.ChatSocketCompacted,
            vocab.CHAT.ChatSocketCompacted
          )
        );
        history.push("/connections");
      } else if (chatConnections.length == 1) {
        const chatConnection = chatConnections[0];
        const chatConnectionUri = get(chatConnection, "uri");

        if (
          connectionUtils.isSuggested(chatConnection) ||
          connectionUtils.isClosed(chatConnection)
        ) {
          dispatch(
            actionCreators.atoms__connect(
              atomUri,
              get(connection, "targetAtomUri"),
              vocab.CHAT.ChatSocketCompacted,
              vocab.CHAT.ChatSocketCompacted
            )
          );
        } else if (
          connectionUtils.isConnected(chatConnection) ||
          connectionUtils.isRequestSent(chatConnection) ||
          connectionUtils.isRequestReceived(chatConnection)
        ) {
          history.push(
            generateLink(
              history.location,
              { postUri: atomUri, connectionUri: chatConnectionUri },
              "/connections"
            )
          );
        }
      } else {
        console.error(
          "more than one connection stored between two atoms that use the same exact sockets",
          atom,
          atomUtils.getChatSocket(atom)
        );
      }
    } else {
      //No chatConnection between buddies exists => connect
      dispatch(
        actionCreators.atoms__connect(
          atomUri,
          get(connection, "targetAtomUri"),
          vocab.CHAT.ChatSocketCompacted,
          vocab.CHAT.ChatSocketCompacted
        )
      );
      history.push("/connections");
    }
  }

  if (!isOwned) {
    return (
      <div className="si ">
        <WonAtomContextSwipeableView
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
    );
  } else {
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
            onClick={() => closeConnection(connection, "Reject Buddy Request?")}
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
            className="si__actions__icon request won-icon"
            onClick={() => requestBuddy(connection)}
          >
            <use xlinkHref={ico32_buddy_accept} href={ico32_buddy_accept} />
          </svg>
          <svg
            className="si__actions__icon primary won-icon"
            onClick={() =>
              closeConnection(connection, "Reject Buddy Suggestion?")
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
          <svg className="si__actions__icon disabled won-icon" disabled={true}>
            <use xlinkHref={ico32_buddy_waiting} href={ico32_buddy_waiting} />
          </svg>
          <svg
            className="si__actions__icon secondary won-icon"
            onClick={() => closeConnection(connection, "Cancel Buddy Request?")}
          >
            <use xlinkHref={ico32_buddy_deny} href={ico32_buddy_deny} />
          </svg>
        </div>
      );
    } else if (connectionUtils.isConnected(connection)) {
      //TODO: Check chat socket connection
      actionButtons = (
        <div className="si__actions">
          <svg
            className="si__actions__icon primary won-icon"
            onClick={() => sendChatMessage(connection)}
          >
            <use xlinkHref={ico36_message} href={ico36_message} />
          </svg>
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
      actionButtons = <div className="si__actions">Buddy has been removed</div>;
    } else {
      actionButtons = <div className="si__actions">Unknown State</div>;
    }

    return (
      <VisibilitySensor
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
}
WonBuddyItem.propTypes = {
  connection: PropTypes.object.isRequired,
  atom: PropTypes.object.isRequired,
  isOwned: PropTypes.bool.isRequired,
};
