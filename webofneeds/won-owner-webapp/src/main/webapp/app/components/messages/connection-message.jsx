import React from "react";
import urljoin from "url-join";
import PropTypes from "prop-types";
import * as messageUtils from "../../redux/utils/message-utils.js";
import * as connectionUtils from "../../redux/utils/connection-utils.js";
import { get, getIn, generateLink } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import { useDispatch } from "react-redux";
import { ownerBaseUrl } from "~/config/default.js";

import WonLabelledHr from "../labelled-hr.jsx";
import WonConnectionMessageStatus from "./connection-message-status.jsx";
import WonAtomIcon from "../atom-icon.jsx";
import WonCombinedMessageContent from "./combined-message-content.jsx";
import WonConnectionMessageActions from "./connection-message-actions.jsx";
import VisibilitySensor from "react-visibility-sensor";

import "~/style/_connection-message.scss";
import "~/style/_rdflink.scss";
import rdf_logo_2 from "~/images/won-icons/rdf_logo_2.svg";
import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";
import { useHistory } from "react-router-dom";

const MESSAGE_READ_TIMEOUT = 1500;

export default function WonConnectionMessage({
  message,
  senderAtom,
  targetAtom,
  originatorAtom,
  allAtoms,
  ownedConnections,
  connection,
  onClick,
  groupChatMessage,
  shouldShowRdf,
}) {
  const dispatch = useDispatch();
  const history = useHistory();
  const messageUri = get(message, "uri");
  const connectionUri = get(connection, "uri");

  let rdfLinkURL;
  if (shouldShowRdf && ownerBaseUrl && senderAtom && message) {
    rdfLinkURL = urljoin(
      ownerBaseUrl,
      "/rest/linked-data/",
      `?requester=${encodeURIComponent(get(senderAtom, "uri"))}`,
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

  const originatorUri = get(message, "originatorUri");

  const isConnectionMessage = messageUtils.isConnectionMessage(message);
  const isChangeNotificationMessage = messageUtils.isChangeNotificationMessage(
    message
  );
  const isSelected = getIn(message, ["viewState", "isSelected"]);
  const isCollapsed = getIn(message, ["viewState", "isCollapsed"]);
  const showActions = getIn(message, ["viewState", "showActions"]);
  const multiSelectType = get(connection, "multiSelectType");
  const isParsable = messageUtils.isParsable(message);
  const isClaimed = messageUtils.isMessageClaimed(connection, message);
  const isProposed = messageUtils.isMessageProposed(connection, message);
  const isAccepted = messageUtils.isMessageAccepted(connection, message);
  const isAgreed = messageUtils.isMessageAgreedOn(connection, message);
  const isRejected = messageUtils.isMessageRejected(connection, message);
  const isRetracted = messageUtils.isMessageRetracted(connection, message);
  const isCancellationPending = messageUtils.isMessageCancellationPending(
    connection,
    message
  );
  const isCancelled = messageUtils.isMessageCancelled(connection, message);
  const isCollapsible =
    messageUtils.isMessageClaimed(connection, message) ||
    messageUtils.isMessageProposed(connection, message) ||
    messageUtils.isMessageAgreedOn(connection, message) ||
    messageUtils.isMessageRejected(connection, message) ||
    messageUtils.isMessageRetracted(connection, message) ||
    messageUtils.isMessageCancellationPending(connection, message) ||
    messageUtils.isMessageCancelled(connection, message);
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
  const isUnread = messageUtils.isMessageUnread(message);
  const isInjectIntoMessage = injectInto && injectInto.size > 0;
  const isFromSystem = get(message, "systemMessage");
  const hasReferences = get(message, "hasReferences");

  function generateParentCssClasses() {
    const cssClassNames = [];
    isReceived && cssClassNames.push("won-cm--left");
    isSent && cssClassNames.push("won-cm--right");
    !!multiSelectType && cssClassNames.push("won-is-multiSelect");
    !isSelectable() && cssClassNames.push("won-not-selectable");
    isSelected && cssClassNames.push("won-is-selected");
    isProposed && cssClassNames.push("won-is-proposed");
    isClaimed && cssClassNames.push("won-is-claimed");
    isRejected && cssClassNames.push("won-is-rejected");
    isRetracted && cssClassNames.push("won-is-retracted");
    isAccepted && cssClassNames.push("won-is-accepted");
    isAgreed && cssClassNames.push("won-is-agreed");
    isCancelled && cssClassNames.push("won-is-cancelled");
    isCollapsed && cssClassNames.push("won-is-collapsed");
    isCollapsible && cssClassNames.push("won-is-collapsible");
    isChangeNotificationMessage &&
      cssClassNames.push("won-is-changeNotification");
    isCancellationPending && cssClassNames.push("won-is-cancellationPending");
    isUnread && cssClassNames.push("won-unread");

    return cssClassNames.join(" ");
  }

  function generateCenterCssClasses() {
    const cssClassNames = ["won-cm__center"];

    isConnectionMessage &&
      !isParsable &&
      cssClassNames.push("won-cm__center--nondisplayable");
    isFromSystem && cssClassNames.push("won-cm__center--system");
    isInjectIntoMessage && cssClassNames.push("won-cm__center--inject-into");

    return cssClassNames.join(" ");
  }

  function generateCenterBubbleCssClasses() {
    const cssClassNames = ["won-cm__center__bubble"];

    hasReferences && cssClassNames.push("references");
    isPending && cssClassNames.push("pending");
    isPartiallyLoaded && cssClassNames.push("partiallyLoaded");
    isSent && isFailedToSend && cssClassNames.push("failure");

    return cssClassNames.join(" ");
  }

  function generateCollapsedLabel() {
    if (message) {
      let label;

      if (isClaimed) label = "Message was claimed.";
      else if (isProposed) label = "Message was proposed.";
      else if (isAccepted) label = "Message was accepted.";
      else if (isAgreed) label = "Message is part of an agreement";
      else if (isRejected) label = "Message was rejected.";
      else if (isRetracted) label = "Message was retracted.";
      else if (isCancellationPending) label = "Cancellation pending.";
      else if (isCancelled) label = "Cancelled.";
      else label = "Message collapsed.";

      return label + " Click to expand.";
    }
    return undefined;
  }

  function isSelectable() {
    //TODO: Not allowed for certain high-level protocol states
    if (message && multiSelectType) {
      switch (multiSelectType) {
        case "rejects":
          return isRejectable;
        case "retracts":
          return isRetractable;
        case "proposesToCancel":
          return isCancelable;
        case "accepts":
          return isAcceptable;
        case "proposes":
          return isProposable;
        case "claims":
          return isClaimable;
      }
    }
    return false;
  }

  function expandMessage(expand) {
    //TODO: Not allowed for certain high-level protocol states
    if (message && !multiSelectType) {
      dispatch(
        actionCreators.messages__viewState__markAsCollapsed({
          messageUri: get(message, "uri"),
          connectionUri: connectionUri,
          atomUri: get(senderAtom, "uri"),
          isCollapsed: expand,
        })
      );
    }
  }

  function showMessageAlways() {
    if (message) {
      return getIn(message, ["content", "text"])
        ? true
        : !hasReferences ||
            shouldShowRdf ||
            showActions ||
            messageUtils.hasProposesReferences(message) ||
            messageUtils.hasClaimsReferences(message) ||
            messageUtils.hasProposesToCancelReferences(message);
    }
    return true;
  }

  //TODO: Not allowed for certain high-level protocol states
  function showActionButtons() {
    return (
      !groupChatMessage &&
      !isRetracted &&
      !isRejected &&
      !(messageUtils.hasProposesToCancelReferences(message) && isAccepted) &&
      (showActions ||
        isCancelable ||
        messageUtils.hasProposesReferences(message) ||
        messageUtils.hasClaimsReferences(message) ||
        messageUtils.hasProposesToCancelReferences(message))
    );
  }

  function toggleActions() {
    dispatch(
      actionCreators.messages__viewState__markShowActions({
        messageUri: get(message, "uri"),
        connectionUri: connectionUri,
        atomUri: get(senderAtom, "uri"),
        showActions: !showActions,
      })
    );
  }

  function markAsRead() {
    if (isUnread) {
      setTimeout(() => {
        dispatch(
          actionCreators.messages__markAsRead({
            messageUri: messageUri,
            connectionUri: connectionUri,
            atomUri: get(senderAtom, "uri"),
            read: true,
          })
        );
      }, MESSAGE_READ_TIMEOUT);
    }
  }

  let messageContentElement;

  if (isChangeNotificationMessage) {
    messageContentElement = (
      <VisibilitySensor
        onChange={isVisible => {
          isVisible && isUnread && markAsRead();
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
    if (isUnread) {
      <VisibilitySensor
        onChange={isVisible => {
          isVisible && isUnread && markAsRead();
        }}
        intervalDelay={2000}
      />;
    }
    const messageIcon = [];

    if (!isSent && !(groupChatMessage && originatorUri)) {
      messageIcon.push(
        <WonAtomIcon
          key="targetAtomUri"
          atom={targetAtom}
          flipIcons={true}
          onClick={
            !onClick
              ? () => {
                  history.push(
                    generateLink(
                      history.location,
                      {
                        postUri: get(targetAtom, "uri"),
                        tab: undefined,
                        connectionUri: undefined,
                      },
                      "/post"
                    )
                  );
                }
              : undefined
          }
        />
      );
    }

    if (isReceived && groupChatMessage && originatorUri) {
      messageIcon.push(
        <WonAtomIcon
          key="originatorUri"
          atom={originatorAtom}
          flipIcons={true}
          onClick={
            !onClick
              ? () => {
                  history.push(
                    generateLink(
                      history.location,
                      {
                        postUri: originatorUri,
                        connectionUri: undefined,
                        tab: undefined,
                      },
                      "/post"
                    )
                  );
                }
              : undefined
          }
        />
      );
    }

    let messageCenterContentElement;
    if (isCollapsed) {
      messageCenterContentElement = (
        <div
          className="won-cm__center__bubble__collapsed clickable"
          onClick={() => expandMessage(false)}
        >
          {generateCollapsedLabel()}
        </div>
      );
    } else {
      messageCenterContentElement = (
        <React.Fragment>
          {isCollapsible ? (
            <div
              className="won-cm__center__bubble__collapsed clickable"
              onClick={() => expandMessage(true)}
            >
              Click to collapse again
            </div>
          ) : (
            undefined
          )}
          <WonCombinedMessageContent
            message={message}
            connection={connection}
            senderAtom={senderAtom}
            originatorAtom={originatorAtom}
            allAtoms={allAtoms}
            ownedConnections={ownedConnections}
            groupChatMessage={groupChatMessage}
          />
          {!groupChatMessage &&
          (isProposable || isClaimable) &&
          !multiSelectType ? (
            <div
              className={
                "won-cm__center__bubble__carret clickable " +
                (showActions
                  ? " won-cm__center__bubble__carret--expanded "
                  : " won-cm__center__bubble__carret--collapsed ")
              }
              onClick={() => toggleActions()}
            >
              <svg>
                <use xlinkHref={ico16_arrow_down} href={ico16_arrow_down} />
              </svg>
            </div>
          ) : (
            undefined
          )}
          {showActionButtons() ? (
            <WonConnectionMessageActions
              message={message}
              connection={connection}
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
            isVisible && isUnread && markAsRead();
          }}
          intervalDelay={2000}
        >
          <div className={generateCenterCssClasses()}>
            <div className={generateCenterBubbleCssClasses()}>
              {messageCenterContentElement}
            </div>
            <WonConnectionMessageStatus message={message} />
            {rdfLinkURL ? (
              <a target="_blank" rel="noopener noreferrer" href={rdfLinkURL}>
                <svg className="rdflink__small clickable">
                  <use xlinkHref={rdf_logo_2} href={rdf_logo_2} />
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
      class={generateParentCssClasses()}
      onClick={onClick}
    >
      {showMessageAlways() ? messageContentElement : undefined}
    </won-connection-message>
  );
}
WonConnectionMessage.propTypes = {
  message: PropTypes.object.isRequired,
  connection: PropTypes.object.isRequired,
  senderAtom: PropTypes.object.isRequired,
  targetAtom: PropTypes.object.isRequired,
  allAtoms: PropTypes.object,
  ownedConnections: PropTypes.object,
  originatorAtom: PropTypes.object,
  onClick: PropTypes.func,
  groupChatMessage: PropTypes.bool,
  shouldShowRdf: PropTypes.bool,
};
