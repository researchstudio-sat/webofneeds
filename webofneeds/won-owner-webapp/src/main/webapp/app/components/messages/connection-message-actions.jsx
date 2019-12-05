import React from "react";
import Immutable from "immutable";
import PropTypes from "prop-types";
import { get, getIn } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import { connect } from "react-redux";
import { getOwnedAtomByConnectionUri } from "../../redux/selectors/general-selectors.js";

import "~/style/_connection-message-actions.scss";
import * as messageUtils from "../../redux/utils/message-utils.js";
import * as connectionUtils from "../../redux/utils/connection-utils.js";

const mapStateToProps = (state, ownProps) => {
  const ownedAtom =
    ownProps.connectionUri &&
    getOwnedAtomByConnectionUri(state, ownProps.connectionUri);
  const connection = getIn(ownedAtom, ["connections", ownProps.connectionUri]);
  const message =
    connection && ownProps.messageUri
      ? getIn(connection, ["messages", ownProps.messageUri])
      : Immutable.Map();

  return {
    messageUri: ownProps.messageUri,
    connectionUri: ownProps.connectionUri,
    connection,
    ownedAtom,
    message,
    multiSelectType: get(connection, "multiSelectType"),
    isProposed: messageUtils.isMessageProposed(message),
    isClaimed: messageUtils.isMessageClaimed(message),
    isAccepted: messageUtils.isMessageAccepted(message),
    isRejected: messageUtils.isMessageRejected(message),
    isRetracted: messageUtils.isMessageRetracted(message),
    isCancellationPending: messageUtils.isMessageCancellationPending(message),
    isCancelled: messageUtils.isMessageCancelled(message),
    isProposable:
      connectionUtils.isConnected(connection) &&
      messageUtils.isMessageProposable(message),
    isClaimable:
      connectionUtils.isConnected(connection) &&
      messageUtils.isMessageClaimable(message),
    isCancelable: messageUtils.isMessageCancelable(message),
    isRetractable: messageUtils.isMessageRetractable(message),
    isRejectable: messageUtils.isMessageRejectable(message),
    isAcceptable: messageUtils.isMessageAcceptable(message),
    isUnread: messageUtils.isMessageUnread(message),
    isFromSystem: get(message, "systemMessage"),
    hasReferences: get(message, "hasReferences"),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    sendChatMessage: (
      trimmedMsg,
      additionalContent,
      referencedContent,
      senderSocketUri,
      targetSocketUri,
      connectionUri,
      isTTL
    ) => {
      dispatch(
        actionCreators.connections__sendChatMessage(
          trimmedMsg,
          additionalContent,
          referencedContent,
          senderSocketUri,
          targetSocketUri,
          connectionUri,
          isTTL
        )
      );
    },
  };
};

class WonConnectionMessageActions extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      clicked: false,
    };
  }

  render() {
    const proposeButton = this.props.isProposable ? (
      <button
        className="won-button--filled thin black"
        disabled={this.props.multiSelectType || this.state.clicked}
        onClick={() => this.sendActionMessage("proposes")}
      >
        {this.props.isProposed ? "Propose (again)" : "Propose"}
      </button>
    ) : (
      undefined
    );

    const claimButton = this.props.isClaimable ? (
      <button
        className="won-button--filled thin black"
        disabled={this.props.multiSelectType || this.state.clicked}
        onClick={() => this.sendActionMessage("claims")}
      >
        Claim
      </button>
    ) : (
      undefined
    );

    const acceptButton = this.props.isAcceptable ? (
      <button
        className="won-button--filled thin red"
        disabled={this.props.multiSelectType || this.state.clicked}
        onClick={() => this.sendActionMessage("accepts")}
      >
        Accept
      </button>
    ) : (
      undefined
    );

    const rejectButton = this.props.isRejectable ? (
      <button
        className="won-button--filled thin black"
        disabled={this.props.multiSelectType || this.state.clicked}
        onClick={() => this.sendActionMessage("rejects")}
      >
        Reject
      </button>
    ) : (
      undefined
    );

    const retractButton = this.props.isRetractable ? (
      <button
        className="won-button--filled thin black"
        disabled={this.props.multiSelectType || this.state.clicked}
        onClick={() => this.sendActionMessage("retracts")}
      >
        Retract
      </button>
    ) : (
      undefined
    );

    const cancelButton = this.props.isCancelable ? (
      <button
        className="won-button--filled thin red"
        disabled={this.props.multiSelectType || this.state.clicked}
        onClick={() => this.sendActionMessage("proposesToCancel")}
      >
        Propose To Cancel
      </button>
    ) : (
      undefined
    );

    const cancelationPendingButton = this.props.isCancellationPending ? (
      <button className="won-button--filled thin red" disabled={true}>
        Cancellation Pending...
      </button>
    ) : (
      undefined
    );

    const cancelledButton = this.props.isCancelled ? (
      <button className="won-button--filled thin red" disabled={true}>
        Cancelled
      </button>
    ) : (
      undefined
    );

    const rejectedButton = this.props.isRejected ? (
      <button className="won-button--filled thin red" disabled={true}>
        Rejected
      </button>
    ) : (
      undefined
    );

    const retractedButton = this.props.isRetracted ? (
      <button className="won-button--filled thin red" disabled={true}>
        Retracted
      </button>
    ) : (
      undefined
    );

    return (
      <won-connection-message-actions>
        {proposeButton}
        {claimButton}
        {acceptButton}
        {rejectButton}
        {retractButton}
        {cancelButton}
        {cancelationPendingButton}
        {cancelledButton}
        {rejectedButton}
        {retractedButton}
      </won-connection-message-actions>
    );
  }

  sendActionMessage(type) {
    this.setState({ clicked: true }, () => {
      const senderSocketUri = get(this.props.connection, "socketUri");
      const targetSocketUri = get(this.props.connection, "targetSocketUri");

      this.props.sendChatMessage(
        undefined,
        undefined,
        new Map().set(
          type,
          Immutable.Map().set(this.props.messageUri, this.props.message)
        ),
        senderSocketUri,
        targetSocketUri,
        this.props.connectionUri,
        false
      );
      this.setState({ clicked: false });
    });
  }
}

WonConnectionMessageActions.propTypes = {
  messageUri: PropTypes.string.isRequired,
  connection: PropTypes.object,
  connectionUri: PropTypes.string.isRequired,
  ownedAtom: PropTypes.object,
  message: PropTypes.object,
  multiSelectType: PropTypes.string,
  isProposed: PropTypes.bool,
  isClaimed: PropTypes.bool,
  isAccepted: PropTypes.bool,
  isRejected: PropTypes.bool,
  isRetracted: PropTypes.bool,
  isCancellationPending: PropTypes.bool,
  isCancelled: PropTypes.bool,
  isProposable: PropTypes.bool,
  isClaimable: PropTypes.bool,
  isCancelable: PropTypes.bool,
  isRetractable: PropTypes.bool,
  isRejectable: PropTypes.bool,
  isAcceptable: PropTypes.bool,
  isUnread: PropTypes.bool,
  isFromSystem: PropTypes.bool,
  hasReferences: PropTypes.bool,
  sendChatMessage: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonConnectionMessageActions);
