import React from "react";
import Immutable from "immutable";
import urljoin from "url-join";
import PropTypes from "prop-types";
import * as messageUtils from "../../redux/utils/message-utils.js";
import * as connectionUtils from "../../redux/utils/connection-utils.js";
import { get, getIn } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import { connect } from "react-redux";
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
import * as viewSelectors from "../../redux/selectors/view-selectors";

const MESSAGE_READ_TIMEOUT = 1500;

const mapStateToProps = (state, ownProps) => {
  const ownedAtom =
    ownProps.connectionUri &&
    getOwnedAtomByConnectionUri(state, ownProps.connectionUri);
  const connection = getIn(ownedAtom, ["connections", ownProps.connectionUri]);
  const theirAtom = getIn(state, ["atoms", get(connection, "targetAtomUri")]);
  const message =
    connection && ownProps.messageUri
      ? getIn(connection, ["messages", ownProps.messageUri])
      : Immutable.Map();

  const shouldShowRdf = viewSelectors.showRdf(state);

  let rdfLinkURL;
  if (shouldShowRdf && ownerBaseUrl && ownedAtom && message) {
    rdfLinkURL = urljoin(
      ownerBaseUrl,
      "/rest/linked-data/",
      `?requester=${encodeURIComponent(get(ownedAtom, "uri"))}`,
      `&uri=${encodeURIComponent(get(message, "uri"))}`,
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
    connectionUri: ownProps.connectionUri,
    messageUri: ownProps.messageUri,
    onClick: ownProps.onClick,
    groupChatMessage: ownProps.groupChatMessage,
    ownedAtom,
    theirAtom,
    message,
    messageSenderUri: get(message, "senderUri"),
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
    isAgreed: messageUtils.isMessageAgreedOn(message),
    isRejected: messageUtils.isMessageRejected(message),
    isRetracted: messageUtils.isMessageRetracted(message),
    isCancellationPending: messageUtils.isMessageCancellationPending(message),
    isCancelled: messageUtils.isMessageCancelled(message),
    isCollapsible:
      messageUtils.isMessageClaimed(message) ||
      messageUtils.isMessageProposed(message) ||
      messageUtils.isMessageAccepted(message) ||
      messageUtils.isMessageAgreedOn(message) ||
      messageUtils.isMessageRejected(message) ||
      messageUtils.isMessageRetracted(message) ||
      messageUtils.isMessageCancellationPending(message) ||
      messageUtils.isMessageCancelled(message),
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
    isAgreeable: messageUtils.isMessageAgreeable(message),
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
};

const mapDispatchToProps = dispatch => {
  return {
    routerGo: (path, props) => {
      dispatch(actionCreators.router__stateGo(path, props));
    },
    messageMarkAsCollapsed: (messageUri, connectionUri, atomUri, collapsed) => {
      dispatch(
        actionCreators.messages__viewState__markAsCollapsed({
          messageUri: messageUri,
          connectionUri: connectionUri,
          atomUri: atomUri,
          isCollapsed: collapsed,
        })
      );
    },
    messageMarkAsRead: (messageUri, connectionUri, atomUri) => {
      dispatch(
        actionCreators.messages__markAsRead({
          messageUri: messageUri,
          connectionUri: connectionUri,
          atomUri: atomUri,
        })
      );
    },
    showMessageActions: (messageUri, connectionUri, atomUri, showActions) => {
      dispatch(
        actionCreators.messages__viewState__markShowActions({
          messageUri: messageUri,
          connectionUri: connectionUri,
          atomUri: atomUri,
          showActions: showActions,
        })
      );
    },
  };
};

class WonConnectionMessage extends React.Component {
  render() {
    let messageContentElement;

    if (this.props.isChangeNotificationMessage) {
      messageContentElement = (
        <VisibilitySensor
          onChange={isVisible => {
            isVisible && this.props.isUnread && this.markAsRead();
          }}
          intervalDelay={2000}
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
        !this.props.isSent &&
        !(this.props.groupChatMessage && this.props.originatorUri)
      ) {
        messageIcon.push(
          <WonAtomIcon
            key="theirAtomUri"
            atomUri={get(this.props.theirAtom, "uri")}
            onClick={
              !this.props.onClick
                ? () => {
                    this.props.routerGo("post", {
                      postUri: get(this.props.theirAtom, "uri"),
                    });
                  }
                : undefined
            }
          />
        );
      }

      if (
        this.props.isReceived &&
        this.props.groupChatMessage &&
        this.props.originatorUri
      ) {
        messageIcon.push(
          <WonAtomIcon
            key="originatorUri"
            atomUri={this.props.originatorUri}
            onClick={
              !this.props.onClick
                ? () => {
                    this.props.routerGo("post", {
                      postUri: this.props.originatorUri,
                    });
                  }
                : undefined
            }
          />
        );
      }

      if (this.props.isFromSystem) {
        messageIcon.push(
          <WonAtomIcon
            key="messageSenderUri"
            atomUri={this.props.messageSenderUri}
          />
        );
      }

      let messageCenterContentElement;
      if (this.props.isCollapsed) {
        messageCenterContentElement = (
          <div
            className="won-cm__center__bubble__collapsed clickable"
            onClick={() => this.expandMessage(false)}
          >
            {this.generateCollapsedLabel()}
          </div>
        );
      } else {
        messageCenterContentElement = (
          <React.Fragment>
            {this.props.isCollapsible ? (
              <div
                className="won-cm__center__bubble__collapsed clickable"
                onClick={() => this.expandMessage(true)}
              >
                Click to collapse again
              </div>
            ) : (
              undefined
            )}
            <WonCombinedMessageContent
              messageUri={this.props.messageUri}
              connectionUri={this.props.connectionUri}
              groupChatMessage={this.props.groupChatMessage}
            />
            {!this.props.groupChatMessage &&
            (this.props.isProposable || this.props.isClaimable) &&
            !this.props.multiSelectType ? (
              <div
                className="won-cm__center__bubble__carret clickable"
                onClick={() => this.toggleActions()}
              >
                <svg>
                  {this.props.showActions ? (
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
                messageUri={this.props.messageUri}
                connectionUri={this.props.connectionUri}
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
              isVisible && this.props.isUnread && this.markAsRead();
            }}
            intervalDelay={2000}
          >
            <div className={this.generateCenterCssClasses()}>
              <div className={this.generateCenterBubbleCssClasses()}>
                {messageCenterContentElement}
              </div>
              <WonConnectionMessageStatus
                messageUri={this.props.messageUri}
                connectionUri={this.props.connectionUri}
              />
              {this.props.rdfLinkURL ? (
                <a
                  target="_blank"
                  rel="noopener noreferrer"
                  href={this.props.rdfLinkURL}
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
    this.props.isReceived && cssClassNames.push("won-cm--left");
    this.props.isSent && cssClassNames.push("won-cm--right");
    !!this.props.multiSelectType && cssClassNames.push("won-is-multiSelect");
    !this.isSelectable() && cssClassNames.push("won-not-selectable");
    this.props.isSelected && cssClassNames.push("won-is-selected");
    this.props.isProposed && cssClassNames.push("won-is-proposed");
    this.props.isClaimed && cssClassNames.push("won-is-claimed");
    this.props.isRejected && cssClassNames.push("won-is-rejected");
    this.props.isRetracted && cssClassNames.push("won-is-retracted");
    this.props.isAccepted && cssClassNames.push("won-is-accepted");
    this.props.isAgreed && cssClassNames.push("won-is-agreed");
    this.props.isCancelled && cssClassNames.push("won-is-cancelled");
    this.props.isCollapsed && cssClassNames.push("won-is-collapsed");
    this.props.isCollapsible && cssClassNames.push("won-is-collapsible");
    this.props.isChangeNotificationMessage &&
      cssClassNames.push("won-is-changeNotification");
    this.props.isCancellationPending &&
      cssClassNames.push("won-is-cancellationPending");
    this.props.isUnread && cssClassNames.push("won-unread");

    return cssClassNames.join(" ");
  }

  generateCenterCssClasses() {
    const cssClassNames = ["won-cm__center"];

    this.props.isConnectionMessage &&
      !this.props.isParsable &&
      cssClassNames.push("won-cm__center--nondisplayable");
    this.props.isFromSystem && cssClassNames.push("won-cm__center--system");
    this.props.isInjectIntoMessage &&
      cssClassNames.push("won-cm__center--inject-into");

    return cssClassNames.join(" ");
  }

  generateCenterBubbleCssClasses() {
    const cssClassNames = ["won-cm__center__bubble"];

    this.props.hasReferences && cssClassNames.push("references");
    this.props.isPending && cssClassNames.push("pending");
    this.props.isPartiallyLoaded && cssClassNames.push("partiallyLoaded");
    this.props.isSent &&
      this.props.isFailedToSend &&
      cssClassNames.push("failure");

    return cssClassNames.join(" ");
  }

  generateCollapsedLabel() {
    if (this.props.message) {
      let label;

      if (this.props.isClaimed) label = "Message was claimed.";
      else if (this.props.isProposed) label = "Message was proposed.";
      else if (this.props.isAccepted) label = "Message was accepted.";
      else if (this.props.isAgreed) label = "Message is part of an agreement";
      else if (this.props.isRejected) label = "Message was rejected.";
      else if (this.props.isRetracted) label = "Message was retracted.";
      else if (this.props.isCancellationPending)
        label = "Cancellation pending.";
      else if (this.props.isCancelled) label = "Cancelled.";
      else label = "Message collapsed.";

      return label + " Click to expand.";
    }
    return undefined;
  }

  isSelectable() {
    //TODO: Not allowed for certain high-level protocol states
    if (this.props.message && this.props.multiSelectType) {
      switch (this.props.multiSelectType) {
        case "rejects":
          return this.props.isRejectable;
        case "retracts":
          return this.props.isRetractable;
        case "proposesToCancel":
          return this.props.isCancelable;
        case "accepts":
          return this.props.isAcceptable;
        case "proposes":
          return this.props.isProposable;
        case "claims":
          return this.props.isClaimable;
      }
    }
    return false;
  }

  expandMessage(expand) {
    //TODO: Not allowed for certain high-level protocol states
    if (this.props.message && !this.props.multiSelectType) {
      this.props.messageMarkAsCollapsed(
        get(this.props.message, "uri"),
        this.props.connectionUri,
        get(this.props.ownedAtom, "uri"),
        expand
      );
    }
  }

  //TODO: Not allowed for certain high-level protocol states
  showActionButtons() {
    return (
      !this.props.groupChatMessage &&
      (this.props.showActions ||
        messageUtils.hasProposesReferences(this.props.message) ||
        messageUtils.hasClaimsReferences(this.props.message) ||
        messageUtils.hasProposesToCancelReferences(this.props.message))
    );
  }

  toggleActions() {
    this.props.showMessageActions(
      get(this.props.message, "uri"),
      this.props.connectionUri,
      get(this.props.ownedAtom, "uri"),
      !this.props.showActions
    );
  }

  markAsRead() {
    if (this.props.isUnread) {
      setTimeout(() => {
        this.props.messageMarkAsRead(
          this.props.messageUri,
          this.props.connectionUri,
          get(this.props.ownedAtom, "uri")
        );
      }, MESSAGE_READ_TIMEOUT);
    }
  }
}

WonConnectionMessage.propTypes = {
  messageUri: PropTypes.string.isRequired,
  connectionUri: PropTypes.string.isRequired,
  groupChatMessage: PropTypes.bool,
  onClick: PropTypes.func,
  ownedAtom: PropTypes.object,
  theirAtom: PropTypes.object,
  message: PropTypes.object,
  messageSenderUri: PropTypes.string,
  originatorUri: PropTypes.string,
  isConnectionMessage: PropTypes.bool,
  isChangeNotificationMessage: PropTypes.bool,
  isSelected: PropTypes.bool,
  isCollapsed: PropTypes.bool,
  isCollapsible: PropTypes.bool,
  showActions: PropTypes.bool,
  multiSelectType: PropTypes.string,
  shouldShowRdf: PropTypes.bool,
  rdfLinkURL: PropTypes.string,
  isParsable: PropTypes.bool,
  isClaimed: PropTypes.bool,
  isProposed: PropTypes.bool,
  isAccepted: PropTypes.bool,
  isAgreed: PropTypes.bool,
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
  isAgreeable: PropTypes.bool,
  isUnread: PropTypes.bool,
  isInjectIntoMessage: PropTypes.bool,
  injectInto: PropTypes.object,
  isReceived: PropTypes.bool,
  isSent: PropTypes.bool,
  isFailedToSend: PropTypes.bool,
  isPending: PropTypes.bool,
  isPartiallyLoaded: PropTypes.bool,
  isFromSystem: PropTypes.bool,
  hasReferences: PropTypes.bool,
  routerGo: PropTypes.func,
  messageMarkAsCollapsed: PropTypes.func,
  messageMarkAsRead: PropTypes.func,
  showMessageActions: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonConnectionMessage);
