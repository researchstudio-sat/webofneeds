import React from "react";
import Immutable from "immutable";
import urljoin from "url-join";
import PropTypes from "prop-types";
import * as messageUtils from "../../redux/utils/message-utils.js";
import * as connectionUtils from "../../redux/utils/connection-utils.js";
import { get, getIn } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import { getOwnedAtomByConnectionUri } from "../../redux/selectors/general-selectors.js";
import { ownerBaseUrl } from "~/config/default.js";

import WonLabelledHr from "../labelled-hr.jsx";
import WonConnectionMessageStatus from "./connection-message-status.jsx";
import WonAtomIcon from "../atom-icon.jsx";
import WonCombinedMessageContent from "./combined-message-content.jsx";
import WonConnectionMessageActions from "./connection-message-actions.jsx";
import VisibilitySensor from "react-visibility-sensor";

import "~/style/_connection-message.scss";
import "~/style/_rdflink.scss";

const MESSAGE_READ_TIMEOUT = 1500;

export default class WonConnectionMessage extends React.Component {
  componentDidMount() {
    this.messageUri = this.props.messageUri;
    this.connectionUri = this.props.connectionUri;
    this.groupChatMessage = this.props.groupChatMessage;
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
    this.groupChatMessage = nextProps.groupChatMessage;
    this.setState(this.selectFromState(this.props.ngRedux.getState()));
  }

  selectFromState(state) {
    const ownedAtom =
      this.connectionUri &&
      getOwnedAtomByConnectionUri(state, this.connectionUri);
    const connection = getIn(ownedAtom, ["connections", this.connectionUri]);
    const theirAtom = getIn(state, ["atoms", get(connection, "targetAtomUri")]);
    const message =
      connection && this.messageUri
        ? getIn(connection, ["messages", this.messageUri])
        : Immutable.Map();

    const shouldShowRdf = getIn(state, ["view", "showRdf"]);

    let rdfLinkURL;
    if (shouldShowRdf && ownerBaseUrl && ownedAtom && message) {
      rdfLinkURL = urljoin(
        ownerBaseUrl,
        "/rest/linked-data/",
        `?requester=${this.encodeParam(get(ownedAtom, "uri"))}`,
        `&uri=${this.encodeParam(get(message, "uri"))}`,
        get(message, "outgoingMessage") ? "&deep=true" : ""
      );
    }
    const isSent = get(message, "outgoingMessage");
    const isReceived = !get(message, "outgoingMessage");
    const isFailedToSend = get(message, "failedToSend");
    const isReceivedByOwn = get(message, "isReceivedByOwn");
    const isReceivedByRemote = get(message, "isReceivedByRemote");

    // determines if the sent message is not received by any of the servers yet but not failed either
    const isPending =
      isSent && !isFailedToSend && !isReceivedByOwn && !isReceivedByRemote;

    // determines if the sent message is received by any of the servers yet but not failed either
    const isPartiallyLoaded =
      isSent &&
      !isFailedToSend &&
      (!(isReceivedByOwn && isReceivedByRemote) &&
        (isReceivedByOwn || isReceivedByRemote));

    const injectInto = get(message, "injectInto");

    return {
      ownedAtom,
      theirAtom,
      message,
      messageSenderUri: get(message, "senderUri"),
      isGroupChatMessage: this.groupChatMessage,
      originatorUri: get(message, "originatorUri"),
      isConnectionMessage: messageUtils.isConnectionMessage(message),
      isChangeNotificationMessage: messageUtils.isChangeNotificationMessage(
        message
      ),
      isSelected: getIn(message, ["viewState", "isSelected"]),
      isCollapsed: getIn(message, ["viewState", "isCollapsed"]),
      showActions: getIn(message, ["viewState", "showActions"]),
      multiSelectType: get(connection, "multiSelectType"),
      shouldShowRdf,
      rdfLinkURL,
      isParsable: messageUtils.isParsable(message),
      isClaimed: messageUtils.isMessageClaimed(message),
      isProposed: messageUtils.isMessageProposed(message),
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
      isInjectIntoMessage: injectInto && injectInto.size > 0,
      injectInto: injectInto,
      isReceived,
      isSent,
      isFailedToSend,
      isPending,
      isPartiallyLoaded,
      isFromSystem: get(message, "systemMessage"),
      hasReferences: get(message, "hasReferences"),
    };
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div />;
    }

    let messageContentElement;

    if (this.state.isChangeNotificationMessage) {
      messageContentElement = (
        <VisibilitySensor
          onChange={isVisible => {
            isVisible && this.state.isUnread && this.markAsRead();
          }}
          intervalDelay={MESSAGE_READ_TIMEOUT}
        >
          <WonLabelledHr
            className="won-cm__modified"
            label="Post has been modified"
          />
        </VisibilitySensor>
      );
    } else {
      const messageIcon = [];

      if (
        !this.state.isSent &&
        !(this.state.isGroupChatMessage && this.state.originatorUri)
      ) {
        messageIcon.push(
          <WonAtomIcon
            atomUri={get(this.state.theirAtom, "uri")}
            ngRedux={this.props.ngRedux}
            onClick={
              !this.props.onClick
                ? () => {
                    this.props.ngRedux.dispatch(
                      actionCreators.router__stateGo({
                        postUri: get(this.state.theirAtom, "uri"),
                      })
                    );
                  }
                : undefined
            }
          />
        );
      }

      if (
        this.state.isReceived &&
        this.state.isGroupChatMessage &&
        this.state.originatorUri
      ) {
        messageIcon.push(
          <WonAtomIcon
            atomUri={this.state.originatorUri}
            ngRedux={this.props.ngRedux}
            onClick={
              !this.props.onClick
                ? () => {
                    this.props.ngRedux.dispatch(
                      actionCreators.router__stateGo({
                        postUri: this.state.originatorUri,
                      })
                    );
                  }
                : undefined
            }
          />
        );
      }

      if (this.state.isFromSystem) {
        messageIcon.push(
          <WonAtomIcon
            atomUri={this.state.messageSenderUri}
            ngRedux={this.props.ngRedux}
          />
        );
      }

      let messageCenterContentElement;
      if (this.state.isCollapsed) {
        messageCenterContentElement = (
          <div
            className="won-cm__center__bubble__collapsed clickable"
            onClick={() => this.expandMessage()}
          >
            {this.generateCollapsedLabel()}
          </div>
        );
      } else {
        messageCenterContentElement = (
          <React.Fragment>
            <WonCombinedMessageContent
              messageUri={this.messageUri}
              connectionUri={this.connectionUri}
              ngRedux={this.props.ngRedux}
              groupChatMessage={this.state.isGroupChatMessage}
            />
            {!this.state.isGroupChatMessage &&
            (this.state.isProposable || this.state.isClaimable) &&
            !this.state.multiSelectType ? (
              <div
                className="won-cm__center__bubble__carret clickable"
                onClick={() => this.toggleActions()}
              >
                <svg>
                  {this.state.showActions ? (
                    <use xlinkHref="#ico16_arrow_up" href="#ico16_arrow_up" />
                  ) : (
                    <use
                      xlinkHref="#ico16_arrow_down"
                      href="#ico16_arrow_down"
                    />
                  )}
                </svg>
              </div>
            ) : (
              undefined
            )}
            {this.showActionButtons() ? (
              <WonConnectionMessageActions
                messageUri={this.messageUri}
                connectionUri={this.connectionUri}
                ngRedux={this.props.ngRedux}
              />
            ) : (
              undefined
            )}
          </React.Fragment>
        );
      }

      messageContentElement = (
        <React.Fragment>
          {messageIcon}
          <VisibilitySensor
            onChange={isVisible => {
              isVisible && this.state.isUnread && this.markAsRead();
            }}
            intervalDelay={MESSAGE_READ_TIMEOUT}
          >
            <div className={this.generateCenterCssClasses()}>
              <div className={this.generateCenterBubbleCssClasses()}>
                {messageCenterContentElement}
              </div>
              <WonConnectionMessageStatus
                messageUri={this.messageUri}
                connectionUri={this.connectionUri}
                ngRedux={this.props.ngRedux}
              />
              {this.state.rdfLinkURL ? (
                <a
                  target="_blank"
                  rel="noopener noreferrer"
                  href={this.state.rdfLinkURL}
                >
                  <svg className="rdflink__small clickable">
                    <use xlinkHref="#rdf_logo_2" href="#rdf_logo_2" />
                  </svg>
                </a>
              ) : (
                undefined
              )}
            </div>
          </VisibilitySensor>
        </React.Fragment>
      );
    }

    return (
      <won-connection-message
        class={this.generateParentCssClasses()}
        onClick={this.props.onClick}
      >
        {messageContentElement}
      </won-connection-message>
    );
  }

  generateParentCssClasses() {
    const cssClassNames = [];
    this.state.isReceived && cssClassNames.push("won-cm--left");
    this.state.isSent && cssClassNames.push("won-cm--right");
    !!this.state.multiSelectType && cssClassNames.push("won-is-multiSelect");
    !this.isSelectable() && cssClassNames.push("won-not-selectable");
    this.state.isSelected && cssClassNames.push("won-is-selected");
    this.state.isProposed && cssClassNames.push("won-is-proposed");
    this.state.isClaimed && cssClassNames.push("won-is-claimed");
    this.state.isRejected && cssClassNames.push("won-is-rejected");
    this.state.isRetracted && cssClassNames.push("won-is-retracted");
    this.state.isAccepted && cssClassNames.push("won-is-accepted");
    this.state.isCancelled && cssClassNames.push("won-is-cancelled");
    this.state.isCollapsed && cssClassNames.push("won-is-collapsed");
    this.state.isChangeNotificationMessage &&
      cssClassNames.push("won-is-changeNotification");
    this.state.isCancellationPending &&
      cssClassNames.push("won-is-cancellationPending");
    this.state.isUnread && cssClassNames.push("won-unread");

    return cssClassNames.join(" ");
  }

  generateCenterCssClasses() {
    const cssClassNames = ["won-cm__center"];

    this.state.isConnectionMessage &&
      !this.state.isParsable &&
      cssClassNames.push("won-cm__center--nondisplayable");
    this.state.isFromSystem && cssClassNames.push("won-cm__center--system");
    this.state.isInjectIntoMessage &&
      cssClassNames.push("won-cm__center--inject-into");

    return cssClassNames.join(" ");
  }

  generateCenterBubbleCssClasses() {
    const cssClassNames = ["won-cm__center__bubble"];

    this.state.hasReferences && cssClassNames.push("references");
    this.state.isPending && cssClassNames.push("pending");
    this.state.isPartiallyLoaded && cssClassNames.push("partiallyLoaded");
    this.state.isSent &&
      this.state.isFailedToSend &&
      cssClassNames.push("failure");

    return cssClassNames.join(" ");
  }

  generateCollapsedLabel() {
    if (this.message) {
      let label;

      if (this.state.isClaimed) label = "Message was claimed.";
      else if (this.state.isProposed) label = "Message was proposed.";
      else if (this.state.isAccepted) label = "Message was accepted.";
      else if (this.state.isRejected) label = "Message was rejected.";
      else if (this.state.isRetracted) label = "Message was retracted.";
      else if (this.state.isCancellationPending)
        label = "Cancellation pending.";
      else if (this.state.isCancelled) label = "Cancelled.";
      else label = "Message collapsed.";

      return label + " Click to expand.";
    }
    return undefined;
  }

  isSelectable() {
    if (this.state.message && this.state.multiSelectType) {
      switch (this.state.multiSelectType) {
        case "rejects":
          return this.state.isRejectable;
        case "retracts":
          return this.state.isRetractable;
        case "proposesToCancel":
          return this.state.isCancelable;
        case "accepts":
          return this.state.isAcceptable;
        case "proposes":
          return this.state.isProposable;
        case "claims":
          return this.state.isClaimable;
      }
    }
    return false;
  }

  expandMessage() {
    if (this.state.message && !this.state.multiSelectType) {
      this.props.ngRedux.dispatch(
        actionCreators.messages__viewState__markAsCollapsed({
          messageUri: get(this.state.message, "uri"),
          connectionUri: this.connectionUri,
          atomUri: get(this.state.ownedAtom, "uri"),
          isCollapsed: false,
        })
      );
    }
  }

  showActionButtons() {
    return (
      !this.state.isGroupChatMessage &&
      (this.state.showActions ||
        messageUtils.hasProposesReferences(this.state.message) ||
        messageUtils.hasClaimsReferences(this.state.message) ||
        messageUtils.hasProposesToCancelReferences(this.state.message))
    );
  }

  toggleActions() {
    this.props.ngRedux.dispatch(
      actionCreators.messages__viewState__markShowActions({
        messageUri: get(this.state.message, "uri"),
        connectionUri: this.state.connectionUri,
        atomUri: get(this.state.ownedAtom, "uri"),
        showActions: !this.state.showActions,
      })
    );
  }

  encodeParam(param) {
    return encodeURIComponent(param);
  }

  markAsRead() {
    if (this.state.isUnread) {
      this.props.ngRedux.dispatch(
        actionCreators.messages__markAsRead({
          messageUri: this.messageUri,
          connectionUri: this.connectionUri,
          atomUri: get(this.state.ownedAtom, "uri"),
        })
      );
    }
  }
}

WonConnectionMessage.propTypes = {
  messageUri: PropTypes.string.isRequired,
  connectionUri: PropTypes.string.isRequired,
  groupChatMessage: PropTypes.bool,
  ngRedux: PropTypes.object.isRequired,
  onClick: PropTypes.func,
};
