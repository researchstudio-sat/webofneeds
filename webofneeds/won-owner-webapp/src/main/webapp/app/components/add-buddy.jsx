import React, { useState, useEffect } from "react";
import vocab from "../service/vocab.js";
import { useSelector, useDispatch } from "react-redux";
import { get } from "../utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import WonAtomHeader from "../components/atom-header.jsx";

import PropTypes from "prop-types";

import "~/style/_add-buddy.scss";
import ico32_buddy_add from "~/images/won-icons/ico32_buddy_add.svg";
import ico32_buddy_waiting from "~/images/won-icons/ico32_buddy_waiting.svg";
import ico32_buddy_accept from "~/images/won-icons/ico32_buddy_accept.svg";
import ico32_buddy_deny from "~/images/won-icons/ico32_buddy_deny.svg";

import { actionCreators } from "../actions/actions";

// TODO: Change Icon: suggestion maybe a PersonIcon with a Plus
export default function WonAddBuddy({ atom, className }) {
  const dispatch = useDispatch();
  const [contextMenuOpen, setContextMenuOpen] = useState(false);
  let thisNode;
  const ownedAtomsWithBuddySocket = useSelector(state =>
    generalSelectors.getOwnedAtomsWithBuddySocket(state)
  );

  const ownedBuddyOptions =
    ownedAtomsWithBuddySocket &&
    ownedAtomsWithBuddySocket
      .filter(atom => atomUtils.isActive(atom))
      .filter(atom => get(atom, "uri") !== get(atom, "uri"));

  const immediateConnectBuddy =
    ownedBuddyOptions.size == 1 ? ownedBuddyOptions.first() : undefined;
  const ownedAtomsWithBuddySocketArray =
    ownedBuddyOptions && ownedBuddyOptions.toArray();
  const targetBuddySocketUri = atomUtils.getSocketUri(
    atom,
    vocab.BUDDY.BuddySocketCompacted
  );

  useEffect(() => {
    function handleClick(e) {
      if (thisNode && !thisNode.contains(e.target) && contextMenuOpen) {
        setContextMenuOpen(false);

        return;
      }
    }
    document.addEventListener("mousedown", handleClick, false);

    return function cleanup() {
      document.removeEventListener("mousedown", handleClick, false);
    };
  });

  function removeBuddy(existingBuddyConnection, message = "") {
    let dialogText;
    if (connectionUtils.isConnected(existingBuddyConnection)) {
      dialogText = "Remove Buddy?";
    } else if (connectionUtils.isRequestSent(existingBuddyConnection)) {
      dialogText = "Cancel Buddy Request?";
    } else {
      return;
    }

    const existingBuddyConnectionUri = get(existingBuddyConnection, "uri");

    const payload = {
      caption: "Buddy",
      text: dialogText,
      buttons: [
        {
          caption: "Yes",
          callback: () => {
            dispatch(
              actionCreators.connections__close(
                existingBuddyConnectionUri,
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

  function connectBuddy(
    selectedAtomUri,
    existingBuddyConnection,
    message = ""
  ) {
    const dialogText = connectionUtils.isRequestReceived(
      existingBuddyConnection
    )
      ? "Accept Buddy Request?"
      : "Send Buddy Request?";

    const existingBuddyConnectionUri = get(existingBuddyConnection, "uri");

    const payload = {
      caption: "Buddy",
      text: dialogText,
      buttons: [
        {
          caption: "Yes",
          callback: () => {
            if (connectionUtils.isRequestReceived(existingBuddyConnection)) {
              const senderSocketUri = get(existingBuddyConnection, "socketUri");
              const targetSocketUri = get(
                existingBuddyConnection,
                "targetSocketUri"
              );
              dispatch(
                actionCreators.atoms__connectSockets(
                  senderSocketUri,
                  targetSocketUri,
                  message
                )
              );
            } else {
              dispatch(
                actionCreators.atoms__connect(
                  selectedAtomUri,
                  get(atom, "uri"),
                  vocab.BUDDY.BuddySocketCompacted,
                  vocab.BUDDY.BuddySocketCompacted,
                  message
                )
              );
            }
            dispatch(actionCreators.view__hideModalDialog());
          },
        },
        {
          caption: "No",
          callback: () => {
            if (connectionUtils.isRequestReceived(existingBuddyConnection)) {
              dispatch(
                actionCreators.connections__close(
                  existingBuddyConnectionUri,
                  message
                )
              );
            }

            dispatch(actionCreators.view__hideModalDialog());
          },
        },
      ],
    };
    dispatch(actionCreators.view__showModalDialog(payload));
  }

  let buddySelectionElement =
    ownedAtomsWithBuddySocketArray &&
    ownedAtomsWithBuddySocketArray.map(atom => {
      const existingBuddyConnection = get(atom, "connections").find(
        conn => get(conn, "targetSocketUri") === targetBuddySocketUri
      );

      let connectionStateClass;
      let onClickAction = undefined;
      let connectionStateIcon;

      if (connectionUtils.isConnected(existingBuddyConnection)) {
        connectionStateClass = "connected";
        connectionStateIcon = ico32_buddy_accept;
        onClickAction = () => {
          removeBuddy(existingBuddyConnection);
        };
      } else if (connectionUtils.isRequestSent(existingBuddyConnection)) {
        connectionStateClass = "sent";
        connectionStateIcon = ico32_buddy_waiting;
        onClickAction = () => {
          removeBuddy(existingBuddyConnection);
        };
      } else if (connectionUtils.isClosed(existingBuddyConnection)) {
        connectionStateClass = "closed";
        connectionStateIcon = ico32_buddy_deny;
      } else if (connectionUtils.isRequestReceived(existingBuddyConnection)) {
        connectionStateClass = "received";
        connectionStateIcon = ico32_buddy_accept;
        onClickAction = () => {
          connectBuddy(get(atom, "uri"), existingBuddyConnection);
        };
      } else {
        // also includes suggested (BuddySocket)Connections
        connectionStateClass = "requestable";
        connectionStateIcon = ico32_buddy_add;
        onClickAction = () => {
          connectBuddy(get(atom, "uri"), existingBuddyConnection);
        };
      }

      return (
        <div
          className={
            "add-buddy__addbuddymenu__content__selection__buddy " +
            connectionStateClass
          }
          key={get(atom, "uri")}
          onClick={onClickAction}
        >
          <WonAtomHeader atom={atom} hideTimestamp={true} />
          <svg className="add-buddy__addbuddymenu__content__selection__buddy__status">
            <use xlinkHref={connectionStateIcon} href={connectionStateIcon} />
          </svg>
        </div>
      );
    });

  const dropdownElement = contextMenuOpen && (
    <div className="add-buddy__addbuddymenu">
      <div className="add-buddy__addbuddymenu__content">
        <div className="topline">
          <div
            className="add-buddy__addbuddymenu__header clickable"
            onClick={() => setContextMenuOpen(false)}
          >
            <svg className="add-buddy__addbuddymenu__header__icon">
              <use xlinkHref={ico32_buddy_add} href={ico32_buddy_add} />
            </svg>
            <span className="add-buddy__addbuddymenu__header__text hide-in-responsive">
              Add as Buddy&#8230;
            </span>
          </div>
        </div>
        <div className="add-buddy__addbuddymenu__content__selection">
          {buddySelectionElement}
        </div>
      </div>
    </div>
  );

  let actionButton;

  if (immediateConnectBuddy) {
    const existingBuddyConnection = get(
      immediateConnectBuddy,
      "connections"
    ).find(conn => get(conn, "targetSocketUri") === targetBuddySocketUri);

    let connectionStateClass;
    let onClickAction = undefined;
    let connectionStateIcon;
    let connectionStateLabel;

    if (connectionUtils.isConnected(existingBuddyConnection)) {
      connectionStateClass = "connected";
      connectionStateIcon = ico32_buddy_accept;
      connectionStateLabel = "Already a Buddy";
      onClickAction = () => {
        removeBuddy(existingBuddyConnection);
      };
    } else if (connectionUtils.isRequestSent(existingBuddyConnection)) {
      connectionStateClass = "sent";
      connectionStateIcon = ico32_buddy_waiting;
      connectionStateLabel = "Buddy Request sent";
      onClickAction = () => {
        removeBuddy(existingBuddyConnection);
      };
    } else if (connectionUtils.isClosed(existingBuddyConnection)) {
      connectionStateClass = "closed";
      connectionStateIcon = ico32_buddy_deny;
      connectionStateLabel = "Buddy Request denied";
    } else if (connectionUtils.isRequestReceived(existingBuddyConnection)) {
      connectionStateClass = "received";
      connectionStateIcon = ico32_buddy_accept;
      connectionStateLabel = "Accept Buddy Request";
      onClickAction = () => {
        connectBuddy(
          get(immediateConnectBuddy, "uri"),
          existingBuddyConnection
        );
      };
    } else {
      // also includes suggested (BuddySocket)Connections
      connectionStateClass = "requestable";
      connectionStateIcon = ico32_buddy_add;
      connectionStateLabel = "Add as Buddy";
      onClickAction = () => {
        connectBuddy(
          get(immediateConnectBuddy, "uri"),
          existingBuddyConnection
        );
      };
    }

    actionButton = (
      <div
        className={"add-buddy__addbuddymenu__header " + connectionStateClass}
        onClick={onClickAction}
      >
        <svg className="add-buddy__addbuddymenu__header__icon">
          <use xlinkHref={connectionStateIcon} href={connectionStateIcon} />
        </svg>
        <span className="add-buddy__addbuddymenu__header__text hide-in-responsive">
          {connectionStateLabel}
        </span>
      </div>
    );
  } else {
    actionButton = (
      <div
        className="add-buddy__addbuddymenu__header clickable"
        onClick={() => setContextMenuOpen(true)}
      >
        <svg className="add-buddy__addbuddymenu__header__icon">
          <use xlinkHref={ico32_buddy_add} href={ico32_buddy_add} />
        </svg>
        <span className="add-buddy__addbuddymenu__header__text hide-in-responsive">
          Add as Buddy&#8230;
        </span>
      </div>
    );
  }

  return (
    <won-add-buddy
      class={className ? className : ""}
      ref={node => (thisNode = node)}
    >
      {actionButton}
      {dropdownElement}
    </won-add-buddy>
  );
}
WonAddBuddy.propTypes = {
  atom: PropTypes.object.isRequired,
  className: PropTypes.string,
};
