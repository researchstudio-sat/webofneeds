import React from "react";
import Immutable from "immutable";
import PropTypes from "prop-types";
import { get, getIn } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import { getOwnedAtomByConnectionUri } from "../../redux/selectors/general-selectors.js";

import "~/style/_connection-message-actions.scss";
import * as messageUtils from "../../redux/utils/message-utils";
import * as connectionUtils from "../../redux/utils/connection-utils";

export default class WonConnectionMessageActions extends React.Component {
  componentDidMount() {
    this.messageUri = this.props.messageUri;
    this.connectionUri = this.props.connectionUri;
    this.disconnect = this.props.ngRedux.connect(
      this.selectFromState.bind(this),
      actionCreators
    )(state => {
      this.setState(state);
    });
  }

  componentWillUnmount() {
    this.disconnect();
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    this.messageUri = nextProps.messageUri;
    this.connectionUri = nextProps.connectionUri;
    this.setState(this.selectFromState(this.props.ngRedux.getState()));
  }

  selectFromState(state) {
    const ownedAtom =
      this.connectionUri &&
      getOwnedAtomByConnectionUri(state, this.connectionUri);
    const connection = getIn(ownedAtom, ["connections", this.connectionUri]);
    const message =
      connection && this.messageUri
        ? getIn(connection, ["messages", this.messageUri])
        : Immutable.Map();

    return {
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
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div />;
    }

    const proposeButton = this.state.isProposable ? (
      <button
        className="won-button--filled thin black"
        disabled={this.state.multiSelectType || this.state.clicked}
        onClick={() => this.sendActionMessage("proposes")}
      >
        {this.state.isProposed ? "Propose (again)" : "Propose"}
      </button>
    ) : (
      undefined
    );

    const claimButton = this.state.isClaimable ? (
      <button
        className="won-button--filled thin black"
        disabled={this.state.multiSelectType || this.state.clicked}
        onClick={() => this.sendActionMessage("claims")}
      >
        Claim
      </button>
    ) : (
      undefined
    );

    const acceptButton = this.state.isAcceptable ? (
      <button
        className="won-button--filled thin red"
        disabled={this.state.multiSelectType || this.state.clicked}
        onClick={() => this.sendActionMessage("accepts")}
      >
        Accept
      </button>
    ) : (
      undefined
    );

    const rejectButton = this.state.isRejectable ? (
      <button
        className="won-button--filled thin black"
        disabled={this.state.multiSelectType || this.state.clicked}
        onClick={() => this.sendActionMessage("rejects")}
      >
        Reject
      </button>
    ) : (
      undefined
    );

    const retractButton = this.state.isRetractable ? (
      <button
        className="won-button--filled thin black"
        disabled={this.state.multiSelectType || this.state.clicked}
        onClick={() => this.sendActionMessage("retracts")}
      >
        Retract
      </button>
    ) : (
      undefined
    );

    const cancelButton = this.state.isCancelable ? (
      <button
        className="won-button--filled thin red"
        disabled={this.state.multiSelectType || this.state.clicked}
        onClick={() => this.sendActionMessage("proposesToCancel")}
      >
        Propose To Cancel
      </button>
    ) : (
      undefined
    );

    const cancelationPendingButton = this.state.isCancellationPending ? (
      <button className="won-button--filled thin red" disabled={true}>
        Cancellation Pending...
      </button>
    ) : (
      undefined
    );

    const cancelledButton = this.state.isCancelled ? (
      <button className="won-button--filled thin red" disabled={true}>
        Cancelled
      </button>
    ) : (
      undefined
    );

    const rejectedButton = this.state.isRejected ? (
      <button className="won-button--filled thin red" disabled={true}>
        Rejected
      </button>
    ) : (
      undefined
    );

    const retractedButton = this.state.isRetracted ? (
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
    this.setState({ clicked: true });
    this.props.ngRedux.dispatch(
      actionCreators.connections__sendChatMessage(
        undefined,
        undefined,
        new Map().set(
          type,
          Immutable.Map().set(this.state.message.get("uri"), this.state.message)
        ),
        this.connectionUri,
        false
      )
    );
  }
}

WonConnectionMessageActions.propTypes = {
  messageUri: PropTypes.string.isRequired,
  connectionUri: PropTypes.string.isRequired,
  ngRedux: PropTypes.object.isRequired,
};
