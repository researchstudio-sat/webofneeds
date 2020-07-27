import React from "react";
import urljoin from "url-join";
import PropTypes from "prop-types";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import * as messageUtils from "../../redux/utils/message-utils.js";
import * as connectionUtils from "../../redux/utils/connection-utils.js";
import * as processUtils from "../../redux/utils/process-utils.js";
import { get, getUri, generateLink } from "../../utils.js";
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
  originatorAtom,
  processState,
  allAtoms,
  ownedConnections,
  connection,
  onClick,
  groupChatMessage,
  shouldShowRdf,
}) {
  const dispatch = useDispatch();
  const history = useHistory();
  const messageUri = getUri(message);
  const connectionUri = getUri(connection);

  const isSent = messageUtils.isOutgoingMessage(message);
  const isReceived = messageUtils.isIncomingMessage(message);

  const isChangeNotificationMessage = messageUtils.isChangeNotificationMessage(
    message
  );
  const isCollapsed = messageUtils.isCollapsed(message);
  const showActions = messageUtils.showActions(message);
  const multiSelectType = connectionUtils.getMultiSelectType(connection);
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
    isClaimed ||
    isProposed ||
    isAgreed ||
    isRejected ||
    isRetracted ||
    isCancellationPending ||
    isCancelled;
  const isProposable =
    connectionUtils.isConnected(connection) &&
    messageUtils.isMessageProposable(connection, message);
  const isClaimable =
    connectionUtils.isConnected(connection) &&
    messageUtils.isMessageClaimable(connection, message);
  const isCancelable = messageUtils.isMessageCancelable(connection, message);
  const isUnread = messageUtils.isMessageUnread(message);

  const hasReferences = messageUtils.hasReferences(message);

  function generateParentCssClasses() {
    const isSelectable = () => {
      //TODO: Not allowed for certain high-level protocol states
      if (message && multiSelectType) {
        switch (multiSelectType) {
          case "rejects":
            return messageUtils.isMessageRejectable(connection, message);
          case "retracts":
            return messageUtils.isMessageRetractable(connection, message);
          case "proposesToCancel":
            return isCancelable;
          case "accepts":
            return messageUtils.isMessageAcceptable(connection, message);
          case "proposes":
            return isProposable;
          case "claims":
            return isClaimable;
        }
      }
      return false;
    };

    const cssClassNames = [];
    isReceived && cssClassNames.push("won-cm--left");
    isSent && cssClassNames.push("won-cm--right");
    !!multiSelectType && cssClassNames.push("won-is-multiSelect");
    !isSelectable() && cssClassNames.push("won-not-selectable");
    messageUtils.isMessageSelected(message) &&
      cssClassNames.push("won-is-selected");
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

  function showMessageAlways() {
    return (
      !message ||
      messageUtils.hasText(message) ||
      !hasReferences ||
      shouldShowRdf ||
      showActions ||
      messageUtils.hasProposesReferences(message) ||
      messageUtils.hasClaimsReferences(message) ||
      messageUtils.hasProposesToCancelReferences(message)
    );
  }

  if (showMessageAlways()) {
    const originatorUri = messageUtils.getOriginatorUri(message);
    const originatorHolderUri =
      groupChatMessage && atomUtils.getHeldByUri(originatorAtom);
    const originatorHolder = get(allAtoms, originatorHolderUri);

    const isOriginatorAtomFetchNecessary =
      isReceived &&
      groupChatMessage &&
      processUtils.isAtomFetchNecessary(
        processState,
        originatorUri,
        originatorAtom
      );

    const isOriginatorHolderFetchNecessary =
      isReceived &&
      groupChatMessage &&
      processUtils.isAtomFetchNecessary(
        processState,
        originatorHolderUri,
        originatorHolder
      );

    const ensureOriginatorIsFetched = () => {
      if (isOriginatorAtomFetchNecessary) {
        console.debug("fetch originatorUri, ", originatorUri);
        dispatch(actionCreators.atoms__fetchUnloadedAtom(originatorUri));
      }
    };
    const ensureOriginatorHolderIsFetched = () => {
      if (isOriginatorHolderFetchNecessary) {
        console.debug("fetch originatorHolderUri, ", originatorHolderUri);
        dispatch(actionCreators.atoms__fetchUnloadedAtom(originatorHolderUri));
      }
    };

    const wrapVisibilitySensor =
      isUnread ||
      isOriginatorAtomFetchNecessary ||
      isOriginatorHolderFetchNecessary;

    const onChange = isVisible => {
      if (isVisible) {
        if (isUnread) {
          setTimeout(() => {
            dispatch(
              actionCreators.messages__markAsRead({
                messageUri: messageUri,
                connectionUri: connectionUri,
                atomUri: getUri(senderAtom),
                read: true,
              })
            );
          }, MESSAGE_READ_TIMEOUT);
        }
        ensureOriginatorIsFetched();
        ensureOriginatorHolderIsFetched();
      }
    };

    return (
      <won-connection-message
        class={generateParentCssClasses()}
        onClick={onClick}
      >
        {isChangeNotificationMessage ? (
          wrapVisibilitySensor ? (
            <VisibilitySensor onChange={onChange} intervalDelay={200}>
              <WonChangeNotificationMessage />
            </VisibilitySensor>
          ) : (
            <WonChangeNotificationMessage />
          )
        ) : (
          <React.Fragment>
            {groupChatMessage && isReceived && originatorUri ? (
              <WonAtomIcon
                key={originatorUri}
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
            ) : (
              undefined
            )}
            {wrapVisibilitySensor ? (
              <VisibilitySensor onChange={onChange} intervalDelay={200}>
                <WonMessageContentWrapper
                  message={message}
                  groupChatMessage={groupChatMessage}
                  connection={connection}
                  senderAtom={senderAtom}
                  ownedConnections={ownedConnections}
                  originatorAtom={originatorAtom}
                  allAtoms={allAtoms}
                  shouldShowRdf={shouldShowRdf}
                />
              </VisibilitySensor>
            ) : (
              <WonMessageContentWrapper
                message={message}
                groupChatMessage={groupChatMessage}
                connection={connection}
                senderAtom={senderAtom}
                ownedConnections={ownedConnections}
                originatorAtom={originatorAtom}
                allAtoms={allAtoms}
                shouldShowRdf={shouldShowRdf}
              />
            )}
          </React.Fragment>
        )}
      </won-connection-message>
    );
  } else {
    return (
      <won-connection-message
        class={generateParentCssClasses()}
        onClick={onClick}
      />
    );
  }
}
WonConnectionMessage.propTypes = {
  message: PropTypes.object.isRequired,
  connection: PropTypes.object.isRequired,
  senderAtom: PropTypes.object.isRequired,
  processState: PropTypes.object.isRequired,
  allAtoms: PropTypes.object,
  ownedConnections: PropTypes.object,
  originatorAtom: PropTypes.object,
  onClick: PropTypes.func,
  groupChatMessage: PropTypes.bool,
  shouldShowRdf: PropTypes.bool,
};

function WonChangeNotificationMessage() {
  return (
    <WonLabelledHr
      className="won-cm__modified"
      label="Post has been modified"
    />
  );
}

function WonMessageExpandedContent({
  connection,
  ownedConnections,
  groupChatMessage,
  message,
  senderAtom,
  originatorAtom,
  allAtoms,
  onClick,
}) {
  const dispatch = useDispatch();
  const messageUri = getUri(message);
  const connectionUri = getUri(connection);
  const multiSelectType = connectionUtils.getMultiSelectType(connection);
  const showActions = messageUtils.showActions(message);
  //TODO: Not allowed for certain high-level protocol states

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

  const showActionButtons = () => {
    return (
      !groupChatMessage &&
      !isRetracted &&
      !isRejected &&
      !(messageUtils.hasProposesToCancelReferences(message) && isAccepted) &&
      (showActions ||
        messageUtils.isMessageCancelable(connection, message) ||
        messageUtils.hasProposesReferences(message) ||
        messageUtils.hasClaimsReferences(message) ||
        messageUtils.hasProposesToCancelReferences(message))
    );
  };

  const isCollapsible =
    isClaimed ||
    isProposed ||
    isAgreed ||
    isRejected ||
    isRetracted ||
    isCancellationPending ||
    isCancelled;

  return (
    <React.Fragment>
      {isCollapsible ? (
        <div
          className="won-cm__center__bubble__collapsed clickable"
          onClick={onClick}
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
      (!multiSelectType &&
        connectionUtils.isConnected(connection) &&
        (messageUtils.isMessageProposable(connection, message) ||
          messageUtils.isMessageClaimable(connection, message))) ? (
        <div
          className={
            "won-cm__center__bubble__carret clickable " +
            (showActions
              ? " won-cm__center__bubble__carret--expanded "
              : " won-cm__center__bubble__carret--collapsed ")
          }
          onClick={() =>
            dispatch(
              actionCreators.messages__viewState__markShowActions({
                messageUri: messageUri,
                connectionUri: connectionUri,
                atomUri: getUri(senderAtom),
                showActions: !showActions,
              })
            )
          }
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
WonMessageExpandedContent.propTypes = {
  connection: PropTypes.object,
  ownedConnections: PropTypes.object,
  message: PropTypes.object,
  groupChatMessage: PropTypes.bool,
  onClick: PropTypes.func.isRequired,
  allAtoms: PropTypes.object,
  senderAtom: PropTypes.object,
  originatorAtom: PropTypes.object,
};

function WonMessageContentWrapper({
  message,
  connection,
  senderAtom,
  originatorAtom,
  allAtoms,
  groupChatMessage,
  ownedConnections,
  shouldShowRdf,
}) {
  const dispatch = useDispatch();
  const messageUri = getUri(message);
  const connectionUri = getUri(connection);
  const isSent = messageUtils.isOutgoingMessage(message);

  const generateCenterBubbleCssClasses = () => {
    const isFailedToSend = messageUtils.hasFailedToSend(message);
    const isReceivedByOwn = messageUtils.isReceivedByOwn(message);
    const isReceivedByRemote = messageUtils.isReceivedByRemote(message);
    // determines if the sent message is not received by any of the servers yet but not failed either

    const isPending =
      isSent && !isFailedToSend && !isReceivedByOwn && !isReceivedByRemote;
    // determines if the sent message is received by any of the servers yet but not failed either
    const isPartiallyLoaded =
      isSent &&
      !isFailedToSend &&
      (!(isReceivedByOwn && isReceivedByRemote) &&
        (isReceivedByOwn || isReceivedByRemote));

    const cssClassNames = ["won-cm__center__bubble"];

    messageUtils.hasReferences(message) && cssClassNames.push("references");
    isPending && cssClassNames.push("pending");
    isPartiallyLoaded && cssClassNames.push("partiallyLoaded");
    isSent && isFailedToSend && cssClassNames.push("failure");

    return cssClassNames.join(" ");
  };

  const generateCenterCssClasses = () => {
    const cssClassNames = ["won-cm__center"];

    const injectInto = messageUtils.getInjectInto(message);
    const isInjectIntoMessage = injectInto && injectInto.size > 0;

    messageUtils.isConnectionMessage(message) &&
      !messageUtils.isParsable(message) &&
      cssClassNames.push("won-cm__center--nondisplayable");
    messageUtils.isSystemMessage(message) &&
      cssClassNames.push("won-cm__center--system");
    isInjectIntoMessage && cssClassNames.push("won-cm__center--inject-into");

    return cssClassNames.join(" ");
  };

  const expandMessage = expand => {
    //TODO: Not allowed for certain high-level protocol states
    if (message && !connectionUtils.getMultiSelectType(connection)) {
      dispatch(
        actionCreators.messages__viewState__markAsCollapsed({
          messageUri: messageUri,
          connectionUri: connectionUri,
          atomUri: getUri(senderAtom),
          isCollapsed: expand,
        })
      );
    }
  };

  return (
    <div className={generateCenterCssClasses()}>
      <div className={generateCenterBubbleCssClasses()}>
        {messageUtils.isCollapsed(message) ? (
          <WonMessageCollapsedContent
            message={message}
            onClick={() => expandMessage(false)}
          />
        ) : (
          <WonMessageExpandedContent
            message={message}
            connection={connection}
            allAtoms={allAtoms}
            senderAtom={senderAtom}
            originatorAtom={originatorAtom}
            ownedConnections={ownedConnections}
            groupChatMessage={groupChatMessage}
            onClick={() => expandMessage(true)}
          />
        )}
      </div>
      <WonConnectionMessageStatus message={message} />
      {shouldShowRdf && ownerBaseUrl && senderAtom && message ? (
        <a
          target="_blank"
          rel="noopener noreferrer"
          href={urljoin(
            ownerBaseUrl,
            "/rest/linked-data/",
            `?requester=${encodeURIComponent(getUri(senderAtom))}`,
            `&uri=${encodeURIComponent(getUri(message))}`,
            isSent ? "&deep=true" : ""
          )}
        >
          <svg className="rdflink__small clickable">
            <use xlinkHref={rdf_logo_2} href={rdf_logo_2} />
          </svg>
        </a>
      ) : (
        undefined
      )}
    </div>
  );
}

WonMessageContentWrapper.propTypes = {
  message: PropTypes.object,
  connection: PropTypes.object,
  senderAtom: PropTypes.object,
  originatorAtom: PropTypes.object,
  allAtoms: PropTypes.object,
  groupChatMessage: PropTypes.bool,
  ownedConnections: PropTypes.object,
  shouldShowRdf: PropTypes.bool,
};

function WonMessageCollapsedContent({ connection, message, onClick }) {
  let collapsedLabelText;

  let label;

  if (message) {
    if (messageUtils.isMessageClaimed(connection, message))
      label = "Message was claimed.";
    else if (messageUtils.isMessageProposed(connection, message))
      label = "Message was proposed.";
    else if (messageUtils.isMessageAccepted(connection, message))
      label = "Message was accepted.";
    else if (messageUtils.isMessageAgreedOn(connection, message))
      label = "Message is part of an agreement";
    else if (messageUtils.isMessageRejected(connection, message))
      label = "Message was rejected.";
    else if (messageUtils.isMessageRetracted(connection, message))
      label = "Message was retracted.";
    else if (messageUtils.isMessageCancellationPending(connection, message))
      label = "Cancellation pending.";
    else if (messageUtils.isMessageCancelled(connection, message))
      label = "Cancelled.";
    else label = "Message collapsed.";

    collapsedLabelText = label + " Click to expand.";
  }

  return (
    <div
      className="won-cm__center__bubble__collapsed clickable"
      onClick={onClick}
    >
      {collapsedLabelText}
    </div>
  );
}

WonMessageCollapsedContent.propTypes = {
  connection: PropTypes.object,
  message: PropTypes.object,
  onClick: PropTypes.func.isRequired,
};
