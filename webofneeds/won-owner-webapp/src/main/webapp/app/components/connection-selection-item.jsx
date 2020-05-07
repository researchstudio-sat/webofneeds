import React from "react";
import PropTypes from "prop-types";
import { get } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { useSelector, useDispatch } from "react-redux";
import * as processUtils from "../redux/utils/process-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import WonConnectionHeader from "./connection-header.jsx";

import "~/style/_connection-selection-item-line.scss";
import { generateLink, getQueryParams } from "../utils";
import { useHistory, Link } from "react-router-dom";
import WonAtomIcon from "./atom-icon";

export default function WonConnectionSelectionItem({
  senderAtom,
  connection,
  toLink,
}) {
  const history = useHistory();
  const dispatch = useDispatch();
  const openConnectionUri = getQueryParams(history.location).connectionUri;

  const targetAtomUri = get(connection, "targetAtomUri");
  const processState = useSelector(state => get(state, "process"));

  const connectionUri = get(connection, "uri");
  const targetAtomFailedToLoad = processUtils.hasAtomFailedToLoad(
    processState,
    targetAtomUri
  );
  const isUnread = connectionUtils.isUnread(connection);

  const closeButton = targetAtomFailedToLoad ? (
    <button
      className="csi__closebutton red won-button--outlined thin"
      onClick={closeConnection}
    >
      Close
    </button>
  ) : (
    undefined
  );

  function closeConnection() {
    dispatch(actionCreators.connections__close(connectionUri));
    history.push(
      generateLink(history.location, {
        useCase: undefined,
        connectionUri: undefined,
      })
    );
  }

  return (
    <won-connection-selection-item
      class={
        (openConnectionUri === connectionUri ? "selected " : "") +
        (isUnread ? "won-unread" : "")
      }
    >
      {senderAtom ? (
        <Link
          className="csi__senderAtom"
          to={generateLink(
            history.location,
            {
              postUri: get(senderAtom, "uri"),
              tab: "DETAIL",
            },
            "/post",
            false
          )}
        >
          <WonAtomIcon atomUri={get(senderAtom, "uri")} />
        </Link>
      ) : (
        <div />
      )}
      <WonConnectionHeader connection={connection} toLink={toLink} />
      {closeButton}
    </won-connection-selection-item>
  );
}

WonConnectionSelectionItem.propTypes = {
  senderAtom: PropTypes.object,
  connection: PropTypes.object.isRequired,
  toLink: PropTypes.string,
};
