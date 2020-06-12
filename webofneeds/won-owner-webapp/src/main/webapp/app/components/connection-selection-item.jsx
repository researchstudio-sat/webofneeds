import React, { useState } from "react";
import PropTypes from "prop-types";
import { useHistory, Link } from "react-router-dom";
import { useSelector, useDispatch } from "react-redux";
import { get, generateLink, getQueryParams } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import WonAtomIcon from "./atom-icon.jsx";
import SwipeableViews from "react-swipeable-views";
import WonConnectionHeader from "./connection-header.jsx";

import ico16_contextmenu from "~/images/won-icons/ico16_contextmenu.svg";
import ico16_checkmark from "~/images/won-icons/ico16_checkmark.svg";
import ico36_close from "~/images/won-icons/ico36_close.svg";
import ico36_outgoing from "~/images/won-icons/ico36_outgoing.svg";

import "~/style/_connection-selection-item-line.scss";
import vocab from "../service/vocab";
import * as generalSelectors from "../redux/selectors/general-selectors";

export default function WonConnectionSelectionItem({
  senderAtom,
  connection,
  toLink,
  flip,
}) {
  const history = useHistory();
  const dispatch = useDispatch();
  const openConnectionUri = getQueryParams(history.location).connectionUri;

  const senderAtomUri = get(connection, "uri").split("/c")[0];
  const targetAtomUri = get(connection, "targetAtomUri");
  const processState = useSelector(generalSelectors.getProcessState);

  const [showActions, setShowActions] = useState(false);

  const connectionUri = get(connection, "uri");
  const targetAtomFailedToLoad = processUtils.hasAtomFailedToLoad(
    processState,
    targetAtomUri
  );
  const isUnread = connectionUtils.isUnread(connection);

  const closeButton = targetAtomFailedToLoad ? (
    <button
      className="csi__closebutton red won-button--outlined thin"
      onClick={() => closeConnection()}
    >
      Close
    </button>
  ) : (
    undefined
  );

  let actionButtons;

  function closeConnection(
    dialogText = "Do you want to remove the Connection?"
  ) {
    if (!connection) {
      return;
    }

    const payload = {
      caption: "Connect",
      text: dialogText,
      buttons: [
        {
          caption: "Yes",
          callback: () => {
            if (connectionUtils.isUnread(connection)) {
              dispatch(
                actionCreators.connections__markAsRead({
                  connectionUri: connectionUri,
                  atomUri: senderAtomUri,
                })
              );
            }

            dispatch(actionCreators.connections__close(connectionUri));
            setShowActions(!showActions);
            dispatch(actionCreators.view__hideModalDialog());
          },
        },
        {
          caption: "No",
          callback: () => {
            setShowActions(!showActions);
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
          atomUri: senderAtomUri,
        })
      );
    }

    const senderSocketUri = get(connection, "socketUri");
    const targetSocketUri = get(connection, "targetSocketUri");
    setShowActions(!showActions);
    dispatch(
      actionCreators.atoms__connectSockets(
        senderSocketUri,
        targetSocketUri,
        message
      )
    );
  }

  function sendRequest(message = "") {
    if (!connection) {
      return;
    }

    const payload = {
      caption: "Connect",
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
                  atomUri: senderAtomUri,
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
            setShowActions(!showActions);
            dispatch(actionCreators.view__hideModalDialog());
          },
        },
        {
          caption: "No",
          callback: () => {
            setShowActions(!showActions);
            dispatch(actionCreators.view__hideModalDialog());
          },
        },
      ],
    };
    dispatch(actionCreators.view__showModalDialog(payload));
  }

  switch (get(connection, "state")) {
    case vocab.WON.RequestReceived:
      actionButtons = (
        <React.Fragment>
          <svg
            className="csi__main__actions__icon request won-icon"
            onClick={() => openRequest()}
          >
            <use xlinkHref={ico16_checkmark} href={ico16_checkmark} />
          </svg>
          <svg
            className="csi__main__actions__icon primary won-icon"
            onClick={() => closeConnection("Reject Request?")}
          >
            <use xlinkHref={ico36_close} href={ico36_close} />
          </svg>
        </React.Fragment>
      );
      break;

    case vocab.WON.RequestSent:
      actionButtons = (
        <React.Fragment>
          <svg
            className="csi__main__actions__icon disabled won-icon"
            disabled={true}
          >
            <use xlinkHref={ico36_outgoing} href={ico36_outgoing} />
          </svg>
          <svg
            className="csi__main__actions__icon secondary won-icon"
            onClick={() => closeConnection("Cancel Request?")}
          >
            <use xlinkHref={ico36_close} href={ico36_close} />
          </svg>
        </React.Fragment>
      );
      break;

    case vocab.WON.Connected:
      actionButtons = (
        <React.Fragment>
          <svg
            className="csi__main__actions__icon secondary won-icon"
            onClick={() => closeConnection()}
          >
            <use xlinkHref={ico36_close} href={ico36_close} />
          </svg>
        </React.Fragment>
      );
      break;

    case vocab.WON.Closed:
      actionButtons = (
        <React.Fragment>Connection has been closed</React.Fragment>
      );
      break;

    case vocab.WON.Suggested:
      actionButtons = (
        <React.Fragment>
          <svg
            className="csi__main__actions__icon request won-icon"
            onClick={() => sendRequest()}
          >
            <use xlinkHref={ico16_checkmark} href={ico16_checkmark} />
          </svg>
          <svg
            className="csi__main__actions__icon primary won-icon"
            onClick={() => closeConnection("Remove Suggestion?")}
          >
            <use xlinkHref={ico36_close} href={ico36_close} />
          </svg>
        </React.Fragment>
      );
      break;

    default:
      actionButtons = <React.Fragment>Unknown State</React.Fragment>;
  }

  const connectionContent = (
    <div className="csi__main__connection">
      {senderAtom ? (
        <Link
          className="csi__senderAtom"
          to={generateLink(
            history.location,
            {
              postUri: get(senderAtom, "uri"),
              connectionUri: undefined,
              tab: "DETAIL",
            },
            "/post",
            false
          )}
        >
          <WonAtomIcon atom={senderAtom} flipIcons={true} />
        </Link>
      ) : (
        <div />
      )}
      <WonConnectionHeader
        connection={connection}
        toLink={toLink}
        flip={flip}
      />
      {closeButton}
    </div>
  );

  return (
    <won-connection-selection-item
      class={
        (openConnectionUri === connectionUri ? "selected " : "") +
        (isUnread ? "won-unread" : "")
      }
    >
      {openConnectionUri === connectionUri ? (
        <div className="csi__main">{connectionContent}</div>
      ) : (
        <React.Fragment>
          <div className="csi__main">
            <SwipeableViews
              index={showActions ? 1 : 0}
              enableMouseEvents={false}
              animateHeight={true}
            >
              {connectionContent}
              <div className="csi__main__actions">{actionButtons}</div>
            </SwipeableViews>
          </div>
          <svg
            className="csi__trigger clickable"
            onClick={() => setShowActions(!showActions)}
          >
            <use xlinkHref={ico16_contextmenu} href={ico16_contextmenu} />
          </svg>
        </React.Fragment>
      )}
    </won-connection-selection-item>
  );
}

WonConnectionSelectionItem.propTypes = {
  senderAtom: PropTypes.object,
  connection: PropTypes.object.isRequired,
  toLink: PropTypes.string,
  flip: PropTypes.bool,
};
