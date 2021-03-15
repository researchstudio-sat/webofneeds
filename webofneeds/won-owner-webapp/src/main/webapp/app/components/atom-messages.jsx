import React, { useEffect, useState } from "react";
import PropTypes from "prop-types";
import { useDispatch, useSelector } from "react-redux";

import * as generalSelectors from "../redux/selectors/general-selectors";
import * as viewSelectors from "../redux/selectors/view-selectors";
import * as messageUtils from "../redux/utils/message-utils";
import { rdfTextfieldHelpText } from "../won-label-utils.js";
import {
  get,
  getUri,
  generateLink,
  extractAtomUriFromConnectionUri,
} from "../utils";
import * as processUtils from "../redux/utils/process-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import won from "../won-es6.js";

import "~/style/_atom-messages.scss";
import "~/style/_rdflink.scss";
import ico36_backarrow from "~/images/won-icons/ico36_backarrow.svg";
import rdf_logo_1 from "~/images/won-icons/rdf_logo_1.svg";
import ico_loading_anim from "~/images/won-icons/ico_loading_anim.svg";

import WonConnectionHeader from "./connection-header.jsx";
import WonShareDropdown from "./share-dropdown.jsx";
import WonConnectionContextDropdown from "./connection-context-dropdown.jsx";
import ChatTextfield from "./chat-textfield.jsx";
import WonPetrinetState from "./petrinet-state.jsx";
import WonConnectionMessage from "./messages/connection-message.jsx";
import WonConnectionAgreementDetails from "./connection-agreement-details.jsx";
import WonChatSocketActions from "~/app/components/socket-actions/chat-actions";
import { actionCreators } from "../actions/actions.js";
import * as ownerApi from "../api/owner-api.js";
import Immutable from "immutable";
import { useHistory } from "react-router-dom";
import { getOwnedConnections } from "../redux/selectors/general-selectors";

const MAXFAIL_COUNT = 3;

export default function WonAtomMessages({
  connection,
  backToChats,
  className,
}) {
  const history = useHistory();
  const dispatch = useDispatch();
  const chatContainerRef = React.createRef();
  useEffect(() => {
    if (snapBottom && showChatData) {
      scrollToBottom();
    }

    ensureMessagesAreLoaded();
    ensureAgreementDataIsLoaded();
    ensureAgreementDatasetIsLoaded();
    ensurePetriNetDataIsLoaded();
  });
  const [snapBottom, setSnapBottom] = useState(true);

  const connectionUri = getUri(connection);
  const senderAtom = useSelector(
    generalSelectors.getOwnedAtomByConnectionUri(connectionUri)
  );
  const senderAtomUri = getUri(senderAtom);
  const targetAtomUri = connectionUtils.getTargetAtomUri(connection);
  const targetAtom = useSelector(generalSelectors.getAtom(targetAtomUri));
  const allAtoms = useSelector(generalSelectors.getAtoms);
  const ownedConnections = useSelector(getOwnedConnections);

  const messages = connectionUtils.getMessages(connection);
  const chatMessages =
    messages &&
    messages
      .filterNot(messageUtils.getForwardMessage)
      .filterNot(messageUtils.isAtomHintMessage)
      .filterNot(messageUtils.isSocketHintMessage);

  const processState = useSelector(generalSelectors.getProcessState);
  const hasConnectionMessagesToLoad = processUtils.hasMessagesToLoad(
    processState,
    connectionUri
  );

  const agreementData = connectionUtils.getAgreementData(connection);
  const agreementDataset = connectionUtils.getAgreementDataset(connection);
  const petriNetData = connectionUtils.getPetriNetData(connection);

  //TODO: calculate this based on the uris in the agreementData and not based on every possible message
  const agreementMessages =
    messages &&
    messages.filter(msg => messageUtils.isMessageAgreement(connection, msg));

  const cancellationPendingMessages =
    messages &&
    messages.filter(msg =>
      messageUtils.isMessageCancellationPending(connection, msg)
    );
  const proposalMessages =
    messages &&
    messages.filter(msg => messageUtils.isMessageProposal(connection, msg));
  //TODO Refactor End

  const unreadMessages =
    chatMessages && chatMessages.filter(messageUtils.isMessageUnread);

  const showChatData =
    connection &&
    !(
      connectionUtils.showAgreementData(connection) ||
      connectionUtils.showPetriNetData(connection)
    );

  const multiSelectType = connectionUtils.getMultiSelectType(connection);

  const unreadMessageCount = unreadMessages && unreadMessages.size;
  const isProcessingLoadingMessages =
    connection &&
    processUtils.isConnectionLoadingMessages(processState, connectionUri);
  const isProcessingLoadingAgreementData =
    connection &&
    processUtils.isConnectionAgreementDataLoading(processState, connectionUri);
  const isProcessingLoadingPetriNetData =
    connection &&
    processUtils.isConnectionPetriNetDataLoading(processState, connectionUri);
  const isProcessingLoadingAgreementDataset =
    connection &&
    processUtils.isConnectionAgreementDatasetLoading(
      processState,
      connectionUri
    );

  const agreementDatasetFailCount =
    connection &&
    processUtils.getConnectionAgreementDatasetFailCount(
      processState,
      connectionUri
    );
  const agreementDataFailCount =
    connection &&
    processUtils.getConnectionAgreementDataFailCount(
      processState,
      connectionUri
    );
  const petriNetDataFailCount =
    connection &&
    processUtils.getConnectionPetriNetDataFailCount(
      processState,
      connectionUri
    );

  const showAgreementData = connectionUtils.showAgreementData(connection);
  const showPetriNetData = connectionUtils.showPetriNetData(connection);
  const petriNetDataArray = petriNetData ? petriNetData.toArray() : [];
  const agreementDataLoaded =
    agreementData &&
    processUtils.isConnectionAgreementDataLoaded(processState, connectionUri);
  const agreementDatasetLoaded =
    agreementDataset &&
    processUtils.isConnectionAgreementDatasetLoaded(
      processState,
      connectionUri
    );
  const petriNetDataLoaded =
    petriNetData &&
    processUtils.isConnectionPetriNetDataLoaded(processState, connectionUri);
  const shouldShowRdf = useSelector(viewSelectors.showRdf);
  const hasPetriNetData = petriNetDataArray.length > 0;
  const agreementMessageArray = agreementMessages
    ? agreementMessages.toArray()
    : [];
  const proposalMessageArray = proposalMessages
    ? proposalMessages.toArray()
    : [];
  const cancellationPendingMessageArray = cancellationPendingMessages
    ? cancellationPendingMessages.toArray()
    : [];

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

  function showAgreementDataField() {
    dispatch(
      actionCreators.connections__showPetriNetData({
        connectionUri: connectionUri,
        showPetriNetData: false,
      })
    );
    dispatch(
      actionCreators.connections__showAgreementData({
        connectionUri: connectionUri,
        showAgreementData: true,
      })
    );
  }

  function showPetriNetDataField() {
    dispatch(
      actionCreators.connections__showAgreementData({
        connectionUri: connectionUri,
        showAgreementData: false,
      })
    );
    dispatch(
      actionCreators.connections__showPetriNetData({
        connectionUri: connectionUri,
        showPetriNetData: true,
      })
    );
  }

  function goToUnreadMessages() {
    if (showAgreementData) {
      dispatch(
        actionCreators.connections__showAgreementData({
          connectionUri: connectionUri,
          showAgreementData: false,
        })
      );
    }
    if (showPetriNetData) {
      dispatch(
        actionCreators.connections__showPetriNetData({
          connectionUri: connectionUri,
          showPetriNetData: false,
        })
      );
    }
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
    isTTL = false
  ) {
    dispatch(
      actionCreators.connections__showAgreementData({
        connectionUri: connectionUri,
        showAgreementData: false,
      })
    );
    dispatch(actionCreators.view__hideAddMessageContent());

    const trimmedMsg = chatMessage.trim();
    if (trimmedMsg || additionalContent || referencedContent) {
      const senderSocketUri = connectionUtils.getSocketUri(connection);
      const targetSocketUri = connectionUtils.getTargetSocketUri(connection);

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
    }
  }

  function addMessageToState(messageUri) {
    return ownerApi.fetchMessage(senderAtomUri, messageUri).then(response => {
      won.wonMessageFromJsonLd(response, messageUri).then(msg => {
        //If message isnt in the state we add it
        if (!get(chatMessages, messageUri)) {
          dispatch(actionCreators.messages__processAgreementMessage(msg));
        }
      });
    });
  }

  function sendRequest(message) {
    dispatch(
      actionCreators.atoms__connectSockets(
        connectionUtils.getSocketUri(connection),
        connectionUtils.getTargetSocketUri(connection),
        message
      )
    );
    history.replace(
      generateLink(history.location, {
        connectionUri: connectionUri,
      })
    );
  }

  function selectMessage(msgUri) {
    const msg = connectionUtils.getMessage(connection, msgUri);

    if (msg) {
      dispatch(
        actionCreators.messages__viewState__markAsSelected({
          messageUri: msgUri,
          connectionUri: connectionUri,
          atomUri: senderAtomUri,
          isSelected: !messageUtils.isMessageSelected(msg),
        })
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

  async function ensurePetriNetDataIsLoaded(forceFetch = false) {
    if (
      forceFetch ||
      (petriNetDataFailCount < MAXFAIL_COUNT &&
        connectionUtils.isConnected(connection) &&
        !connectionUtils.isUsingTemporaryUri(connection) &&
        !isProcessingLoadingPetriNetData &&
        !petriNetDataLoaded)
    ) {
      dispatch(
        actionCreators.connections__setLoadingPetriNetData({
          connectionUri: connectionUri,
          loadingPetriNetData: true,
        })
      );

      try {
        const response = await ownerApi.fetchPetriNetUris(connectionUri);
        const petriNetData = {};

        response.forEach(entry => {
          if (entry.processURI) {
            petriNetData[entry.processURI] = entry;
          }
        });

        const petriNetDataImm = Immutable.fromJS(petriNetData);
        dispatch(
          actionCreators.connections__updatePetriNetData({
            connectionUri: connectionUri,
            petriNetData: petriNetDataImm,
          })
        );
      } catch (error) {
        console.error("Error:", error);
        dispatch(
          actionCreators.connections__failedLoadingPetriNetData({
            connectionUri: connectionUri,
          })
        );
      }
    }
  }

  async function ensureAgreementDatasetIsLoaded(forceFetch = false) {
    if (
      forceFetch ||
      (agreementDatasetFailCount < MAXFAIL_COUNT &&
        connectionUtils.isConnected(connection) &&
        !connectionUtils.isUsingTemporaryUri(connection) &&
        !isProcessingLoadingAgreementDataset &&
        !agreementDatasetLoaded)
    ) {
      try {
        dispatch(
          actionCreators.connections__setLoadingAgreementDataset({
            connectionUri: connectionUri,
            loadingAgreementDataset: true,
          })
        );
        const response = await ownerApi.fetchAgreementProtocolDataset(
          connectionUri
        );
        dispatch(
          actionCreators.connections__updateAgreementDataset({
            connectionUri: connectionUri,
            agreementDataset: response,
          })
        );
      } catch (error) {
        console.error("Error:", error);
        dispatch(
          actionCreators.connections__failedLoadingAgreementDataset({
            connectionUri: connectionUri,
          })
        );
        dispatch(
          actionCreators.connections__setLoadedAgreementDataset({
            connectionUri: connectionUri,
            loadedAgreementDataset: false,
          })
        );
      }
    }
  }

  function ensureAgreementDataIsLoaded(forceFetch = false) {
    if (
      forceFetch ||
      (agreementDataFailCount < MAXFAIL_COUNT &&
        connectionUtils.isConnected(connection) &&
        !connectionUtils.isUsingTemporaryUri(connection) &&
        !isProcessingLoadingAgreementData &&
        !agreementDataLoaded)
    ) {
      dispatch(
        actionCreators.connections__setLoadingAgreementData({
          connectionUri: connectionUri,
          loadingAgreementData: true,
        })
      );
      ownerApi
        .fetchAgreementProtocolUris(connectionUri)
        .then(response => {
          const agreementDataImm = Immutable.fromJS({
            agreementUris: Immutable.Set(response.agreementUris),
            agreedMessageUris: Immutable.Set(response.agreedMessageUris),
            pendingProposalUris: Immutable.Set(response.pendingProposalUris),

            acceptedCancellationProposalUris: Immutable.Set(
              response.acceptedCancellationProposalUris
            ),
            cancellationPendingAgreementUris: Immutable.Set(
              response.cancellationPendingAgreementUris
            ),
            pendingCancellationProposalUris: Immutable.Set(
              response.pendingCancellationProposalUris
            ),
            cancelledAgreementUris: Immutable.Set(
              response.cancelledAgreementUris
            ),

            rejectedMessageUris: Immutable.Set(response.rejectedMessageUris),
            retractedMessageUris: Immutable.Set(response.retractedMessageUris),
            proposedMessageUris: Immutable.Set(response.proposedMessageUris),
            proposedToCancelUris: Immutable.Set(response.proposedToCancelUris),
            claimedMessageUris: Immutable.Set(response.claimedMessageUris),
          });

          dispatch(
            actionCreators.connections__updateAgreementData({
              connectionUri: connectionUri,
              agreementData: agreementDataImm,
            })
          );

          //Retrieve all the relevant messages
          agreementDataImm.map(uriList =>
            uriList.map(uri => addMessageToState(uri))
          );
        })
        .catch(error => {
          console.error("Error:", error);
          dispatch(
            actionCreators.connections__failedLoadingAgreementData({
              connectionUri: connectionUri,
            })
          );
        });
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

  let headerElement = undefined;
  let contentElement = undefined;
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
          className="am__content__unreadindicator__content won-button--filled primary"
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

  const actionElement = (
    <div className="am__content__actions">
      <WonChatSocketActions connection={connection} />
    </div>
  );

  if (showChatData) {
    const backButtonElement = (
      <React.Fragment>
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
      </React.Fragment>
    );

    headerElement = (
      <div className="am__header">
        <div className="am__header__back">{backButtonElement}</div>
        <WonConnectionHeader
          connection={connection}
          toLink={generateLink(
            history.location,
            {
              postUri: connectionUtils.getTargetAtomUri(connection),
              connectionUri: getUri(connection),
              tab: undefined,
            },
            "/post"
          )}
          hideTimestamp={true}
          hideMessageIndicator={true}
        />
        <WonShareDropdown atom={targetAtom} />
        <WonConnectionContextDropdown
          connection={connection}
          showPetriNetDataField={showPetriNetDataField}
          showAgreementDataField={showAgreementDataField}
        />
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
              processState={processState}
              allAtoms={allAtoms}
              ownedConnections={ownedConnections}
              shouldShowRdf={shouldShowRdf}
              onClick={
                multiSelectType ? () => selectMessage(getUri(msg)) : undefined
              }
            />
          );
        });
      }
      return messageElements;
    };

    contentElement = (
      <div className="am__content" ref={chatContainerRef} onScroll={onScroll}>
        {unreadIndicatorElement}
        {(isConnectionLoading || isProcessingLoadingMessages) &&
          loadSpinnerElement}
        {!connectionUtils.isConnected(connection) && actionElement}
        {!connectionUtils.isSuggested(connection) &&
          !isConnectionLoading &&
          !connectionUtils.isUsingTemporaryUri(connection) &&
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
    );
  } else if (showAgreementData) {
    headerElement = (
      <div className="am__header">
        <div className="am__header__back">
          <a
            className="am__header__back__button clickable"
            onClick={() =>
              dispatch(
                actionCreators.connections__showAgreementData({
                  connectionUri: connectionUri,
                  showAgreementData: false,
                })
              )
            }
          >
            <svg className="am__header__back__button__icon clickable">
              <use xlinkHref={ico36_backarrow} href={ico36_backarrow} />
            </svg>
          </a>
        </div>
        <div
          className="am__header__title clickable"
          onClick={() =>
            dispatch(
              actionCreators.connections__showAgreementData({
                connectionUri: connectionUri,
                showAgreementData: false,
              })
            )
          }
        >
          Showing Agreement Data
        </div>
        <WonConnectionContextDropdown
          connection={connection}
          showPetriNetDataField={showPetriNetDataField}
          showAgreementDataField={showAgreementDataField}
        />
      </div>
    );

    const agreementMessages =
      !isProcessingLoadingAgreementData &&
      agreementMessageArray.map((msg, index) => {
        return (
          <WonConnectionMessage
            key={getUri(msg) + "-" + index}
            message={msg}
            connection={connection}
            senderAtom={senderAtom}
            processState={processState}
            allAtoms={allAtoms}
            ownedConnections={ownedConnections}
            shouldShowRdf={shouldShowRdf}
            onClick={
              multiSelectType ? () => selectMessage(getUri(msg)) : undefined
            }
          />
        );
      });

    const cancellationPendingMessages =
      !isProcessingLoadingAgreementData &&
      cancellationPendingMessageArray.map((msg, index) => {
        return (
          <WonConnectionMessage
            key={getUri(msg) + "-" + index}
            message={msg}
            connection={connection}
            senderAtom={senderAtom}
            processState={processState}
            allAtoms={allAtoms}
            ownedConnections={ownedConnections}
            shouldShowRdf={shouldShowRdf}
            onClick={
              multiSelectType ? () => selectMessage(getUri(msg)) : undefined
            }
          />
        );
      });

    const proposalMessages =
      !isProcessingLoadingAgreementData &&
      proposalMessageArray.map((msg, index) => {
        return (
          <WonConnectionMessage
            key={getUri(msg) + "-" + index}
            message={msg}
            connection={connection}
            senderAtom={senderAtom}
            processState={processState}
            allAtoms={allAtoms}
            ownedConnections={ownedConnections}
            shouldShowRdf={shouldShowRdf}
            onClick={
              multiSelectType ? () => selectMessage(getUri(msg)) : undefined
            }
          />
        );
      });

    contentElement = (
      <div className="am__content won-agreement-content" ref={chatContainerRef}>
        {unreadIndicatorElement}
        {(isConnectionLoading ||
          isProcessingLoadingMessages ||
          (showAgreementData && isProcessingLoadingAgreementData)) &&
          loadSpinnerElement}
        {isProcessingLoadingAgreementData && (
          <div className="am__content__agreement__loadingtext">
            Calculating Agreement Status
          </div>
        )}

        {!(
          agreementMessageArray.length > 0 ||
          cancellationPendingMessageArray.length > 0 ||
          proposalMessageArray.length > 0
        ) &&
          !isProcessingLoadingAgreementData && (
            <div className="am__content__agreement__emptytext">
              No Agreements within this Conversation
            </div>
          )}

        {agreementMessageArray.length > 0 &&
          !isProcessingLoadingAgreementData && (
            <div className="am__content__agreement__title">Agreements</div>
          )}

        {agreementMessages}

        {cancellationPendingMessageArray.length > 0 &&
          !isProcessingLoadingAgreementData && (
            <div className="am__content__agreement__title">
              Agreements with Pending Cancellation
            </div>
          )}

        {cancellationPendingMessages}

        {proposalMessageArray.length > 0 &&
          !isProcessingLoadingAgreementData && (
            <div className="am__content__agreement__title">Open Proposals</div>
          )}

        {proposalMessages}
        {shouldShowRdf && (
          <WonConnectionAgreementDetails connection={connection} />
        )}
        {rdfLinkToConnection}
      </div>
    );
  } else if (showPetriNetData) {
    headerElement = (
      <div className="am__header">
        <div className="am__header__back">
          <a
            className="am__header__back__button clickable"
            onClick={() =>
              dispatch(
                actionCreators.connections__showPetriNetData({
                  connectionUri: connectionUri,
                  showPetriNetData: false,
                })
              )
            }
          >
            <svg className="am__header__back__button__icon clickable">
              <use xlinkHref={ico36_backarrow} href={ico36_backarrow} />
            </svg>
          </a>
        </div>
        <div
          className="am__header__title clickable"
          onClick={() =>
            dispatch(
              actionCreators.connections__showPetriNetData({
                connectionUri: connectionUri,
                showPetriNetData: false,
              })
            )
          }
        >
          Showing PetriNet Data
        </div>
        <WonConnectionContextDropdown
          connection={connection}
          showPetriNetDataField={showPetriNetDataField}
          showAgreementDataField={showAgreementDataField}
        />
      </div>
    );

    const petrinetProcessArray =
      (!isProcessingLoadingPetriNetData || petriNetDataArray.length > 0) &&
      petriNetDataArray.map((process, index) => {
        if (get(process, "processURI")) {
          return (
            <div
              className="am__content__petrinet__process"
              key={get(process, "processURI") + "-" + index}
            >
              <div className="am__content__petrinet__process__header">
                ProcessURI: {get(process, "processURI")}
              </div>
              <WonPetrinetState
                className="am__content__petrinet__process__content"
                processUri={get(process, "processURI")}
              />
            </div>
          );
        }
      });

    contentElement = (
      <div className="am__content won-petrinet-content" ref={chatContainerRef}>
        {unreadIndicatorElement}
        {(isConnectionLoading ||
          isProcessingLoadingMessages ||
          (isProcessingLoadingPetriNetData && !hasPetriNetData)) &&
          loadSpinnerElement}
        {!hasPetriNetData &&
          (isProcessingLoadingPetriNetData ? (
            <div className="am__content__petrinet__loadingtext">
              Calculating PetriNet Status
            </div>
          ) : (
            <div className="am__content__petrinet__emptytext">
              No PetriNet Data within this Conversation
            </div>
          ))}
        {petrinetProcessArray}
        {rdfLinkToConnection}
      </div>
    );
  }

  if (!showPetriNetData) {
    if (connectionUtils.isConnected(connection)) {
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
    } else if (!multiSelectType) {
      if (connectionUtils.isRequestSent(connection)) {
        footerElement = (
          <div className="am__footer">
            Waiting for them to accept your chat request.
          </div>
        );
      } else if (connectionUtils.isClosed(connection)) {
        footerElement = (
          <div className="am__footer">Connection has been closed.</div>
        );
      } else if (connectionUtils.isRequestReceived(connection)) {
        footerElement = (
          <div className="am__footer">
            <ChatTextfield
              className="am__footer__chattextfield"
              connection={connection}
              placeholder="Message (optional)"
              submitButtonLabel="Accept&#160;Chat"
              allowEmptySubmit={true}
              allowDetails={false}
              onSubmit={({ value }) => {
                const senderSocketUri = connectionUtils.getSocketUri(
                  connection
                );
                const targetSocketUri = connectionUtils.getTargetSocketUri(
                  connection
                );
                dispatch(
                  actionCreators.atoms__connectSockets(
                    senderSocketUri,
                    targetSocketUri,
                    value
                  )
                );
              }}
            />
          </div>
        );
      } else if (connectionUtils.isSuggested(connection)) {
        footerElement = (
          <div className="am__footer">
            <ChatTextfield
              className="am__footer__chattextfield"
              connection={connection}
              placeholder="Message (optional)"
              submitButtonLabel="Ask&#160;to&#160;Chat"
              allowEmptySubmit={true}
              allowDetails={false}
              showPersonas={!connection}
              onSubmit={({ value }) => sendRequest(value)}
            />
          </div>
        );
      }
    }
  }

  return (
    <won-atom-messages
      class={
        (className || "") + (connectionOrAtomsLoading ? " won-is-loading " : "")
      }
    >
      {headerElement}
      {contentElement}
      {footerElement}
    </won-atom-messages>
  );
}
WonAtomMessages.propTypes = {
  connection: PropTypes.object,
  backToChats: PropTypes.bool,
  className: PropTypes.string,
};
