import React from "react";
import Immutable from "immutable";
import PropTypes from "prop-types";
import { get, getUri } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import { useDispatch } from "react-redux";

import "~/style/_connection-message-actions.scss";
import * as messageUtils from "../../redux/utils/message-utils.js";
import * as connectionUtils from "../../redux/utils/connection-utils.js";

export default function WonConnectionMessageActions({ message, connection }) {
  const dispatch = useDispatch();
  const multiSelectType = get(connection, "multiSelectType");
  const isProposed = messageUtils.isMessageProposed(connection, message);
  const isCancellationPending = messageUtils.isMessageCancellationPending(
    connection,
    message
  );
  const isProposable =
    connectionUtils.isConnected(connection) &&
    messageUtils.isMessageProposable(connection, message);
  const isClaimable =
    connectionUtils.isConnected(connection) &&
    messageUtils.isMessageClaimable(connection, message);
  const isCancelable = messageUtils.isMessageCancelable(connection, message);
  const isRetractable = messageUtils.isMessageRetractable(connection, message);
  const isRejectable = messageUtils.isMessageRejectable(connection, message);
  const isAcceptable = messageUtils.isMessageAcceptable(connection, message);

  function sendActionMessage(type) {
    const senderSocketUri = connectionUtils.getSocketUri(connection);
    const targetSocketUri = connectionUtils.getTargetSocketUri(connection);

    dispatch(
      actionCreators.connections__sendChatMessage(
        undefined,
        undefined,
        new Map().set(type, Immutable.Map().set(getUri(message), message)),
        senderSocketUri,
        targetSocketUri,
        getUri(connection),
        false
      )
    );
  }

  function generateButton(
    label,
    actionName,
    buttonColorClassName = "red",
    disabled = false
  ) {
    return (
      <button
        className={"won-button--filled thin " + buttonColorClassName}
        disabled={disabled}
        onClick={actionName ? () => sendActionMessage(actionName) : undefined}
      >
        {label}
      </button>
    );
  }

  return (
    <won-connection-message-actions>
      {isProposable
        ? generateButton(
            isProposed ? "Propose (again)" : "Propose",
            "proposes",
            "black",
            multiSelectType
          )
        : undefined}
      {isClaimable
        ? generateButton("Claim", "claims", "black", multiSelectType)
        : undefined}
      {isAcceptable
        ? generateButton("Accept", "accepts", "red", multiSelectType)
        : undefined}
      {isRejectable
        ? generateButton("Reject", "rejects", "black", multiSelectType)
        : undefined}
      {isRetractable
        ? generateButton("Retract", "retracts", "black", multiSelectType)
        : undefined}
      {isCancelable
        ? generateButton(
            "Propose To Cancel",
            "proposesToCancel",
            "red",
            multiSelectType
          )
        : undefined}
      {isCancellationPending
        ? generateButton("Cancellation Pending...", undefined, "red", true)
        : undefined}
    </won-connection-message-actions>
  );
}
WonConnectionMessageActions.propTypes = {
  message: PropTypes.object.isRequired,
  connection: PropTypes.object.isRequired,
};
