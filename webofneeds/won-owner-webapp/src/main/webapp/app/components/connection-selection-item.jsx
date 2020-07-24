import React, { useState } from "react";
import PropTypes from "prop-types";
import { useHistory, Link } from "react-router-dom";
import { useSelector, useDispatch } from "react-redux";
import { getUri, generateLink, getQueryParams } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import WonAtomIcon from "./atom-icon.jsx";
import SwipeableViews from "react-swipeable-views";
import WonConnectionHeader from "./connection-header.jsx";
import WonChatSocketActions from "./socket-actions/chat-actions.jsx";

import ico16_contextmenu from "~/images/won-icons/ico16_contextmenu.svg";

import "~/style/_connection-selection-item-line.scss";
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

  const targetAtomUri = connectionUtils.getTargetAtomUri(connection);
  const processState = useSelector(generalSelectors.getProcessState);

  const [showActions, setShowActions] = useState(false);

  const connectionUri = getUri(connection);
  const targetAtomFailedToLoad = processUtils.hasAtomFailedToLoad(
    processState,
    targetAtomUri
  );
  const isUnread = connectionUtils.isUnread(connection);

  const closeButton = targetAtomFailedToLoad ? (
    <button
      className="csi__closebutton secondary won-button--outlined thin"
      onClick={() => closeConnection()}
    >
      Close
    </button>
  ) : (
    undefined
  );

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

  const connectionContent = (
    <div className="csi__main__connection">
      {senderAtom ? (
        <Link
          className="csi__senderAtom"
          to={generateLink(
            history.location,
            {
              postUri: getUri(senderAtom),
              connectionUri: undefined,
              tab: undefined,
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
              <WonChatSocketActions connection={connection} />
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
