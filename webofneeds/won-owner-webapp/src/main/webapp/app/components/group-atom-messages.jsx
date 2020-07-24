import React, { useState, useEffect } from "react";
import PropTypes from "prop-types";
import { useDispatch, useSelector } from "react-redux";

import * as generalSelectors from "../redux/selectors/general-selectors";
import * as messageUtils from "../redux/utils/message-utils";
import { rdfTextfieldHelpText } from "../won-label-utils.js";
import { get, generateLink, extractAtomUriFromConnectionUri } from "../utils";
import * as processUtils from "../redux/utils/process-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import vocab from "../service/vocab.js";

import "~/style/_atom-messages.scss";
import "~/style/_rdflink.scss";
import rdf_logo_1 from "~/images/won-icons/rdf_logo_1.svg";
import ico_loading_anim from "~/images/won-icons/ico_loading_anim.svg";
import ico36_backarrow from "~/images/won-icons/ico36_backarrow.svg";
import WonConnectionHeader from "./connection-header.jsx";
import WonShareDropdown from "./share-dropdown.jsx";
import WonConnectionContextDropdown from "./connection-context-dropdown.jsx";
import ChatTextfield from "./chat-textfield.jsx";
import WonLabelledHr from "./labelled-hr.jsx";
import WonConnectionMessage from "./messages/connection-message.jsx";
import { actionCreators } from "../actions/actions.js";
import * as viewSelectors from "../redux/selectors/view-selectors";
import { useHistory } from "react-router-dom";
import { getOwnedConnections } from "../redux/selectors/general-selectors";

export default function WonGroupAtomMessages({
  connection,
  backToChats,
  className,
}) {
  const history = useHistory();
  const dispatch = useDispatch();
  const chatContainerRef = React.createRef();
  useEffect(() => {
    if (snapBottom) {
      scrollToBottom();
    }

    ensureMessagesAreLoaded();
  });
  const [snapBottom, setSnapBottom] = useState(true);

  const connectionUri = get(connection, "uri");
  const allAtoms = useSelector(generalSelectors.getAtoms);
  const ownedConnections = useSelector(getOwnedConnections);
  const senderAtom = useSelector(
    generalSelectors.getOwnedAtomByConnectionUri(connectionUri)
  );
  const senderAtomUri = get(senderAtom, "uri");
  const targetAtomUri = get(connection, "targetAtomUri");
  const targetAtom = useSelector(generalSelectors.getAtom(targetAtomUri));
  const allChatMessages = connectionUtils.getMessages(connection);
  const chatMessages =
    allChatMessages &&
    allChatMessages
      .filter(msg => !messageUtils.hasForwardsReferences(msg)) //FILTER OUT ALL FORWARD MESSAGE ENVELOPES JUST IN CASE
      .filter(msg => !messageUtils.isAtomHintMessage(msg)) //FILTER OUT ALL HINT MESSAGES
      .filter(msg => !messageUtils.isSocketHintMessage(msg));

  const processState = useSelector(generalSelectors.getProcessState);
  const hasConnectionMessagesToLoad = processUtils.hasMessagesToLoad(
    processState,
    connectionUri
  );

  const unreadMessages =
    chatMessages &&
    chatMessages.filter(msg => messageUtils.isMessageUnread(msg));

  const isConnected = connectionUtils.isConnected(connection);
  const unreadMessageCount = unreadMessages && unreadMessages.size;
  const isProcessingLoadingMessages =
    connection &&
    processUtils.isConnectionLoadingMessages(processState, connectionUri);
  const isSentRequest = connection && connectionUtils.isRequestSent(connection);
  const isReceivedRequest =
    connection && connectionUtils.isRequestReceived(connection);

  const isSuggested = connection && connectionUtils.isSuggested(connection);
  const shouldShowRdf = useSelector(viewSelectors.showRdf);
  // if the connect-message is here, everything else should be as well

  const isConnectionLoading = processUtils.isConnectionLoading(
    processState,
    connectionUri
  );
  const connectionOrAtomsLoading =
    !connection ||
    !targetAtom ||
    !senderAtom ||
    processUtils.isAtomLoading(processState, senderAtomUri) ||
    processUtils.isAtomLoading(processState, targetAtomUri) ||
    isConnectionLoading;

  function goToUnreadMessages() {
    snapToBottom();
  }

  function snapToBottom() {
    if (!snapBottom) {
      setSnapBottom(true);
    }
  }

  function unsnapFromBottom() {
    if (snapBottom) {
      setSnapBottom(false);
    }
  }

  function scrollToBottom() {
    chatContainerRef.current.scrollTop = chatContainerRef.current.scrollHeight;
  }
  function onScroll() {
    const sc = chatContainerRef.current;
    const isAlmostAtBottom =
      sc.scrollTop + sc.offsetHeight >= sc.scrollHeight - 75;
    if (isAlmostAtBottom) {
      snapToBottom();
    } else {
      unsnapFromBottom();
    }
  }

  function send(
    chatMessage,
    additionalContent,
    referencedContent,
    isRDF = false
  ) {
    dispatch(actionCreators.view__hideAddMessageContent());

    const trimmedMsg = chatMessage.trim();
    if (trimmedMsg || additionalContent || referencedContent) {
      const senderSocketUri = get(connection, "socketUri");
      const targetSocketUri = get(connection, "targetSocketUri");

      dispatch(
        actionCreators.connections__sendChatMessage(
          trimmedMsg,
          additionalContent,
          referencedContent,
          senderSocketUri,
          targetSocketUri,
          connectionUri,
          isRDF
        )
      );
    }
  }

  function sendRequest(message) {
    dispatch(
      actionCreators.connections__rate(
        connectionUri,
        vocab.WONCON.binaryRatingGood
      )
    );
    dispatch(
      actionCreators.atoms__connectSockets(
        get(connection, "socketUri"),
        get(connection, "targetSocketUri"),
        message
      )
    );
    history.replace(
      generateLink(history.location, {
        connectionUri: connectionUri,
      })
    );
  }

  function closeConnection(rateBad = false) {
    if (rateBad) {
      dispatch(
        actionCreators.connections__rate(
          connectionUri,
          vocab.WONCON.binaryRatingBad
        )
      );
    }
    dispatch(actionCreators.connections__close(connectionUri));
    if (backToChats) {
      history.replace(
        generateLink(
          history.location,
          {
            connectionUri: undefined,
          },
          "/connections",
          false
        )
      );
    } else {
      history.replace(
        generateLink(
          history.location,
          { postUri: extractAtomUriFromConnectionUri(connectionUri) },
          "/post",
          false
        )
      );
    }
  }

  function ensureMessagesAreLoaded() {
    // make sure latest messages are loaded
    const INITIAL_MESSAGECOUNT = 15;
    if (
      hasConnectionMessagesToLoad &&
      !connectionUtils.isUsingTemporaryUri(connection) &&
      connectionUtils.getMessagesSize(connection) < INITIAL_MESSAGECOUNT
    ) {
      loadPreviousMessages(INITIAL_MESSAGECOUNT);
    }
  }

  function loadPreviousMessages(messagesToLoadCount = 10) {
    if (connection && !isConnectionLoading && !isProcessingLoadingMessages) {
      dispatch(
        actionCreators.connections__showMoreMessages(
          connectionUri,
          messagesToLoadCount
        )
      );
    }
  }

  let footerElement = undefined;

  const rdfLinkToConnection = shouldShowRdf && (
    <a
      className="rdflink clickable"
      target="_blank"
      rel="noopener noreferrer"
      href={connectionUri}
    >
      <svg className="rdflink__small">
        <use xlinkHref={rdf_logo_1} href={rdf_logo_1} />
      </svg>
      <span className="rdflink__label">Connection</span>
    </a>
  );

  const unreadIndicatorElement =
    unreadMessageCount && !snapBottom ? (
      <div className="am__content__unreadindicator">
        <div
          className="am__content__unreadindicator__content won-button--filled secondary"
          onClick={goToUnreadMessages}
        >
          {unreadMessageCount} unread Messages
        </div>
      </div>
    ) : (
      undefined
    );

  const loadSpinnerElement = (
    <div className="am__content__loadspinner">
      <svg className="hspinner">
        <use xlinkHref={ico_loading_anim} href={ico_loading_anim} />
      </svg>
    </div>
  );

  const headerElement = (
    <div className="am__header">
      <div className="am__header__back">
        <a
          className="am__header__back__button clickable"
          onClick={
            backToChats
              ? () =>
                  history.replace(
                    generateLink(
                      history.location,
                      {
                        connectionUri: undefined,
                      },
                      "/connections",
                      false
                    )
                  )
              : () =>
                  history.replace(
                    generateLink(
                      history.location,
                      {
                        postUri: extractAtomUriFromConnectionUri(connectionUri),
                      },
                      "/post",
                      false
                    )
                  )
          }
        >
          <svg className="am__header__back__button__icon">
            <use xlinkHref={ico36_backarrow} href={ico36_backarrow} />
          </svg>
        </a>
      </div>
      <WonConnectionHeader
        connection={connection}
        toLink={generateLink(
          history.location,
          {
            postUri: get(connection, "targetAtomUri"),
            connectionUri: get(connection, "uri"),
            tab: undefined,
          },
          "/post"
        )}
        hideTimestamp={true}
        hideMessageIndicator={true}
      />
      <WonShareDropdown atom={targetAtom} />
      <WonConnectionContextDropdown connection={connection} />
    </div>
  );

  const generateMessageElements = () => {
    const messageElements = [];

    if (chatMessages) {
      chatMessages.map((msg, messageUri) => {
        messageElements.push(
          <WonConnectionMessage
            key={messageUri}
            message={msg}
            connection={connection}
            senderAtom={senderAtom}
            targetAtom={targetAtom}
            processState={processState}
            allAtoms={allAtoms}
            ownedConnections={ownedConnections}
            originatorAtom={get(allAtoms, get(msg, "originatorUri"))}
            shouldShowRdf={shouldShowRdf}
            groupChatMessage={true}
          />
        );
      });
    }
    return messageElements;
  };

  if (isConnected) {
    footerElement = (
      <div className="am__footer">
        <ChatTextfield
          className="am__footer__chattextfield"
          connection={connection}
          placeholder={shouldShowRdf ? "Enter RDF..." : "Your message..."}
          submitButtonLabel={shouldShowRdf ? "Send RDF" : "Send"}
          helpText={shouldShowRdf ? rdfTextfieldHelpText : ""}
          allowEmptySubmit={false}
          allowDetails={!shouldShowRdf}
          isCode={shouldShowRdf}
          onSubmit={({ value, additionalContent, referencedContent }) =>
            send(value, additionalContent, referencedContent, shouldShowRdf)
          }
        />
      </div>
    );
  } else if (isSentRequest) {
    footerElement = (
      <div className="am__footer">
        Waiting for the Group Administrator to accept your request.
      </div>
    );
  } else if (isReceivedRequest) {
    footerElement = (
      <div className="am__footer">
        <ChatTextfield
          className="am__footer__chattextfield"
          connection={connection}
          placeholder="Message (optional)"
          submitButtonLabel="Accept&#160;Invite"
          allowEmptySubmit={true}
          allowDetails={false}
          onSubmit={({ value }) => {
            const senderSocketUri = get(connection, "socketUri");
            const targetSocketUri = get(connection, "targetSocketUri");
            dispatch(
              actionCreators.atoms__connectSockets(
                senderSocketUri,
                targetSocketUri,
                value
              )
            );
          }}
        />
        <WonLabelledHr className="am__footer__labelledhr" label="Or" />
        <button
          className="am__footer__button won-button--filled black"
          onClick={() => closeConnection()}
        >
          Decline
        </button>
      </div>
    );
  } else if (isSuggested) {
    footerElement = (
      <div className="am__footer">
        <ChatTextfield
          className="am__footer__chattextfield"
          connection={connection}
          placeholder="Message (optional)"
          submitButtonLabel="Ask&#160;to&#160;Join"
          allowEmptySubmit={true}
          allowDetails={false}
          showPersonas={!connection}
          onSubmit={({ value }) => sendRequest(value)}
        />
        <WonLabelledHr className="am__footer__labelledhr" label="Or" />
        <button
          className="am__footer__button won-button--filled black"
          onClick={() => closeConnection(true)}
        >
          Bad match - remove!
        </button>
      </div>
    );
  }

  return (
    <won-atom-messages
      class={
        (className || "") + (connectionOrAtomsLoading ? " won-is-loading " : "")
      }
    >
      {headerElement}
      <div className="am__content" ref={chatContainerRef} onScroll={onScroll}>
        {unreadIndicatorElement}
        {(isConnectionLoading || isProcessingLoadingMessages) &&
          loadSpinnerElement}
        {!isSuggested &&
          !isConnectionLoading &&
          !isProcessingLoadingMessages &&
          hasConnectionMessagesToLoad && (
            <button
              className="am__content__loadbutton won-button--outlined thin secondary"
              onClick={() => loadPreviousMessages()}
            >
              Load previous messages
            </button>
          )}
        {generateMessageElements()}
        {rdfLinkToConnection}
      </div>

      {footerElement}
    </won-atom-messages>
  );
}
WonGroupAtomMessages.propTypes = {
  connection: PropTypes.object,
  backToChats: PropTypes.bool,
  className: PropTypes.string,
};
