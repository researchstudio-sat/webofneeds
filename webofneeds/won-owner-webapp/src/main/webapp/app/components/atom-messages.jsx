import React, { useEffect, useState } from "react";
import PropTypes from "prop-types";
import { useDispatch, useSelector } from "react-redux";

import * as generalSelectors from "../redux/selectors/general-selectors";
import * as viewSelectors from "../redux/selectors/view-selectors";
import * as messageUtils from "../redux/utils/message-utils";
import { rdfTextfieldHelpText } from "../won-label-utils.js";
import { get, getIn, generateLink } from "../utils";
import * as processUtils from "../redux/utils/process-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import won from "../won-es6.js";
import vocab from "../service/vocab.js";

import "~/style/_atom-messages.scss";
import "~/style/_rdflink.scss";
import ico36_backarrow from "~/images/won-icons/ico36_backarrow.svg";
import rdf_logo_1 from "~/images/won-icons/rdf_logo_1.svg";
import ico_loading_anim from "~/images/won-icons/ico_loading_anim.svg";

import WonConnectionHeader from "./connection-header.jsx";
import WonShareDropdown from "./share-dropdown.jsx";
import WonConnectionContextDropdown from "./connection-context-dropdown.jsx";
import ChatTextfield from "./chat-textfield.jsx";
import WonLabelledHr from "./labelled-hr.jsx";
import WonPetrinetState from "./petrinet-state.jsx";
import WonAtomContentMessage from "./messages/atom-content-message.jsx";
import WonConnectionMessage from "./messages/connection-message.jsx";
// import WonConnectionAgreementDetails from "./connection-agreement-details.jsx";
import { actionCreators } from "../actions/actions.js";
import * as ownerApi from "../api/owner-api.js";
import Immutable from "immutable";
import { useHistory } from "react-router-dom";
import { getOwnedConnections } from "../redux/selectors/general-selectors";

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
    // ensureAgreementDatasetIsLoaded();
    ensurePetriNetDataIsLoaded();
  });
  const [snapBottom, setSnapBottom] = useState(true);

  const connectionUri = get(connection, "uri");
  const senderAtom = useSelector(
    generalSelectors.getOwnedAtomByConnectionUri(connectionUri)
  );
  const senderAtomUri = get(senderAtom, "uri");
  const targetAtomUri = get(connection, "targetAtomUri");
  const targetAtom = useSelector(generalSelectors.getAtom(targetAtomUri));
  const allAtoms = useSelector(generalSelectors.getAtoms);
  const ownedConnections = useSelector(getOwnedConnections);

  const messages = get(connection, "messages");
  const chatMessages =
    messages &&
    messages
      .filter(msg => !get(msg, "forwardMessage"))
      .filter(msg => !messageUtils.isAtomHintMessage(msg))
      .filter(msg => !messageUtils.isSocketHintMessage(msg));

  const processState = useSelector(generalSelectors.getProcessState);
  const hasConnectionMessagesToLoad = processUtils.hasMessagesToLoad(
    processState,
    connectionUri
  );

  const agreementData = get(connection, "agreementData");
  const petriNetData = get(connection, "petriNetData");

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

  let sortedMessages = chatMessages
    ? chatMessages.toArray().sort(function(a, b) {
        const aDate = get(a, "date");
        const bDate = get(b, "date");

        const aTime = aDate && aDate.getTime();
        const bTime = bDate && bDate.getTime();

        return aTime - bTime;
      })
    : [];

  const unreadMessages =
    chatMessages &&
    chatMessages.filter(msg => messageUtils.isMessageUnread(msg));

  const showChatData =
    connection &&
    !(
      get(connection, "showAgreementData") ||
      get(connection, "showPetriNetData")
    );

  const multiSelectType = get(connection, "multiSelectType");

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
  // const isProcessingLoadingAgreementDataset =
  //   connection &&
  //   processUtils.isConnectionAgreementDatasetLoading(
  //     processState,
  //     connectionUri
  //   );
  const showAgreementData = get(connection, "showAgreementData");
  const showPetriNetData = get(connection, "showPetriNetData");
  const petriNetDataArray = petriNetData ? petriNetData.toArray() : [];
  const agreementDataLoaded =
    agreementData &&
    processUtils.isConnectionAgreementDataLoaded(processState, connectionUri);
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

  const showAtomContentMessage = !!(
    showChatData &&
    connectionUtils.isSuggested(connection) &&
    !multiSelectType &&
    targetAtomUri
  );

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
          isTTL
        )
      );
    }
  }

  function addMessageToState(messageUri) {
    return ownerApi.getMessage(senderAtomUri, messageUri).then(response => {
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
    history.push(
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
      history.push(
        generateLink(history.location, {
          connectionUri: undefined,
        })
      );
    } else {
      history.goBack();
    }
  }

  function selectMessage(msgUri) {
    const msg = getIn(connection, ["messages", msgUri]);

    if (msg) {
      dispatch(
        actionCreators.messages__viewState__markAsSelected({
          messageUri: msgUri,
          connectionUri: connectionUri,
          atomUri: senderAtomUri,
          isSelected: !getIn(msg, ["viewState", "isSelected"]),
        })
      );
    }
  }

  function ensureMessagesAreLoaded() {
    // make sure latest messages are loaded
    const INITIAL_MESSAGECOUNT = 10;
    if (
      hasConnectionMessagesToLoad &&
      !connectionUtils.isUsingTemporaryUri(connection) &&
      get(connection, "messages").size < INITIAL_MESSAGECOUNT
    ) {
      loadPreviousMessages(INITIAL_MESSAGECOUNT);
    }
  }

  async function ensurePetriNetDataIsLoaded(forceFetch = false) {
    if (
      forceFetch ||
      (connectionUtils.isConnected(connection) &&
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
        const response = await ownerApi.getPetriNetUris(connectionUri);
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
          actionCreators.connections__setLoadingPetriNetData({
            connectionUri: connectionUri,
            loadingPetriNetData: false,
          })
        );
      }
    }
  }

  // async function ensureAgreementDatasetIsLoaded(forceFetch = false) {
  //   if (
  //     forceFetch ||
  //     (connectionUtils.isConnected(connection) &&
  //       !connectionUtils.isUsingTemporaryUri(connection) &&
  //       !isProcessingLoadingAgreementDataset)
  //   ) {
  //     try {
  //       dispatch(
  //         actionCreators.connections__setLoadingAgreementDataset({
  //           connectionUri: connectionUri,
  //           loadingAgreementDataset: true,
  //         })
  //       );
  //       const response = await ownerApi.getAgreementProtocolDataset(
  //         connectionUri
  //       );
  //       dispatch(
  //         actionCreators.connections__updateAgreementDataset({
  //           connectionUri: connectionUri,
  //           agreementDataset: response,
  //         })
  //       );
  //     } catch (error) {
  //       console.error("Error:", error);
  //       dispatch(
  //         actionCreators.connections__setLoadingAgreementDataset({
  //           connectionUri: connectionUri,
  //           loadingAgreementDataset: false,
  //         })
  //       );
  //     }
  //   }
  // }

  function ensureAgreementDataIsLoaded(forceFetch = false) {
    if (
      forceFetch ||
      (connectionUtils.isConnected(connection) &&
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
        .getAgreementProtocolUris(connectionUri)
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
            actionCreators.connections__setLoadingAgreementData({
              connectionUri: connectionUri,
              loadingAgreementData: false,
            })
          );
        });
    }
  }
  function loadPreviousMessages(messagesToLoadCount = 5) {
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
      <div className="pm__content__unreadindicator">
        <div
          className="pm__content__unreadindicator__content won-button--filled red"
          onClick={goToUnreadMessages}
        >
          {unreadMessageCount} unread Messages
        </div>
      </div>
    ) : (
      undefined
    );

  const loadSpinnerElement = (
    <div className="pm__content__loadspinner">
      <svg className="hspinner">
        <use xlinkHref={ico_loading_anim} href={ico_loading_anim} />
      </svg>
    </div>
  );

  if (showChatData) {
    const backButtonElement = (
      <React.Fragment>
        <a
          className="pm__header__back__button clickable"
          onClick={
            backToChats ? () => history.push("/connections") : history.goBack
          }
        >
          <svg className="pm__header__back__button__icon">
            <use xlinkHref={ico36_backarrow} href={ico36_backarrow} />
          </svg>
        </a>
      </React.Fragment>
    );

    headerElement = (
      <div className="pm__header">
        <div className="pm__header__back">{backButtonElement}</div>
        <WonConnectionHeader connection={connection} />
        <WonShareDropdown atom={targetAtom} />
        <WonConnectionContextDropdown
          connection={connection}
          showPetriNetDataField={showPetriNetDataField}
          showAgreementDataField={showAgreementDataField}
        />
      </div>
    );

    contentElement = (
      <div className="pm__content" ref={chatContainerRef} onScroll={onScroll}>
        {unreadIndicatorElement}
        {showAtomContentMessage && <WonAtomContentMessage atom={targetAtom} />}
        {(isConnectionLoading || isProcessingLoadingMessages) &&
          loadSpinnerElement}
        {!connectionUtils.isSuggested(connection) &&
          !isConnectionLoading &&
          !connectionUtils.isUsingTemporaryUri(connection) &&
          !isProcessingLoadingMessages &&
          hasConnectionMessagesToLoad && (
            <button
              className="pm__content__loadbutton won-button--outlined thin red"
              onClick={() => loadPreviousMessages()}
            >
              Load previous messages
            </button>
          )}

        {sortedMessages.map((msg, index) => {
          return (
            <WonConnectionMessage
              key={get(msg, "uri") + "-" + index}
              message={msg}
              connection={connection}
              senderAtom={senderAtom}
              targetAtom={targetAtom}
              allAtoms={allAtoms}
              ownedConnections={ownedConnections}
              shouldShowRdf={shouldShowRdf}
              onClick={
                multiSelectType
                  ? () => selectMessage(get(msg, "uri"))
                  : undefined
              }
            />
          );
        })}

        {rdfLinkToConnection}

        {/*<WonConnectionAgreementDetails connection={connection} />*/}
      </div>
    );
  } else if (showAgreementData) {
    headerElement = (
      <div className="pm__header">
        <div className="pm__header__back">
          <a
            className="pm__header__back__button clickable"
            onClick={() =>
              dispatch(
                actionCreators.connections__showAgreementData({
                  connectionUri: connectionUri,
                  showAgreementData: false,
                })
              )
            }
          >
            <svg className="pm__header__back__button__icon clickable">
              <use xlinkHref={ico36_backarrow} href={ico36_backarrow} />
            </svg>
          </a>
        </div>
        <div
          className="pm__header__title clickable"
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
            key={get(msg, "uri") + "-" + index}
            message={msg}
            connection={connection}
            senderAtom={senderAtom}
            targetAtom={targetAtom}
            allAtoms={allAtoms}
            ownedConnections={ownedConnections}
            shouldShowRdf={shouldShowRdf}
            onClick={
              multiSelectType ? () => selectMessage(get(msg, "uri")) : undefined
            }
          />
        );
      });

    const cancellationPendingMessages =
      !isProcessingLoadingAgreementData &&
      cancellationPendingMessageArray.map((msg, index) => {
        return (
          <WonConnectionMessage
            key={get(msg, "uri") + "-" + index}
            message={msg}
            connection={connection}
            senderAtom={senderAtom}
            targetAtom={targetAtom}
            allAtoms={allAtoms}
            ownedConnections={ownedConnections}
            shouldShowRdf={shouldShowRdf}
            onClick={
              multiSelectType ? () => selectMessage(get(msg, "uri")) : undefined
            }
          />
        );
      });

    const proposalMessages =
      !isProcessingLoadingAgreementData &&
      proposalMessageArray.map((msg, index) => {
        return (
          <WonConnectionMessage
            key={get(msg, "uri") + "-" + index}
            message={msg}
            connection={connection}
            senderAtom={senderAtom}
            targetAtom={targetAtom}
            allAtoms={allAtoms}
            ownedConnections={ownedConnections}
            shouldShowRdf={shouldShowRdf}
            onClick={
              multiSelectType ? () => selectMessage(get(msg, "uri")) : undefined
            }
          />
        );
      });

    contentElement = (
      <div className="pm__content won-agreement-content" ref={chatContainerRef}>
        {unreadIndicatorElement}
        {(isConnectionLoading ||
          isProcessingLoadingMessages ||
          (showAgreementData && isProcessingLoadingAgreementData)) &&
          loadSpinnerElement}
        {isProcessingLoadingAgreementData && (
          <div className="pm__content__agreement__loadingtext">
            Calculating Agreement Status
          </div>
        )}

        {!(
          agreementMessageArray.length > 0 ||
          cancellationPendingMessageArray.length > 0 ||
          proposalMessageArray.length > 0
        ) &&
          !isProcessingLoadingAgreementData && (
            <div className="pm__content__agreement__emptytext">
              No Agreements within this Conversation
            </div>
          )}

        {agreementMessageArray.length > 0 &&
          !isProcessingLoadingAgreementData && (
            <div className="pm__content__agreement__title">Agreements</div>
          )}

        {agreementMessages}

        {cancellationPendingMessageArray.length > 0 &&
          !isProcessingLoadingAgreementData && (
            <div className="pm__content__agreement__title">
              Agreements with Pending Cancellation
            </div>
          )}

        {cancellationPendingMessages}

        {proposalMessageArray.length > 0 &&
          !isProcessingLoadingAgreementData && (
            <div className="pm__content__agreement__title">Open Proposals</div>
          )}

        {proposalMessages}

        {rdfLinkToConnection}
      </div>
    );
  } else if (showPetriNetData) {
    headerElement = (
      <div className="pm__header">
        <div className="pm__header__back">
          <a
            className="pm__header__back__button clickable"
            onClick={() =>
              dispatch(
                actionCreators.connections__showPetriNetData({
                  connectionUri: connectionUri,
                  showPetriNetData: false,
                })
              )
            }
          >
            <svg className="pm__header__back__button__icon clickable">
              <use xlinkHref={ico36_backarrow} href={ico36_backarrow} />
            </svg>
          </a>
        </div>
        <div
          className="pm__header__title clickable"
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
              className="pm__content__petrinet__process"
              key={get(process, "processURI") + "-" + index}
            >
              <div className="pm__content__petrinet__process__header">
                ProcessURI: {get(process, "processURI")}
              </div>
              <WonPetrinetState
                className="pm__content__petrinet__process__content"
                processUri={get(process, "processURI")}
              />
            </div>
          );
        }
      });

    contentElement = (
      <div className="pm__content won-petrinet-content" ref={chatContainerRef}>
        {unreadIndicatorElement}
        {(isConnectionLoading ||
          isProcessingLoadingMessages ||
          (isProcessingLoadingPetriNetData && !hasPetriNetData)) &&
          loadSpinnerElement}
        {!hasPetriNetData &&
          (isProcessingLoadingPetriNetData ? (
            <div className="pm__content__petrinet__loadingtext">
              Calculating PetriNet Status
            </div>
          ) : (
            <div className="pm__content__petrinet__emptytext">
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
        <div className="pm__footer">
          <ChatTextfield
            className="pm__footer__chattextfield"
            connection={connection}
            placeholder={shouldShowRdf ? "Enter TTL..." : "Your message..."}
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
          <div className="pm__footer">
            Waiting for them to accept your chat request.
          </div>
        );
      } else if (connectionUtils.isRequestReceived(connection)) {
        footerElement = (
          <div className="pm__footer">
            <ChatTextfield
              className="pm__footer__chattextfield"
              connection={connection}
              placeholder="Message (optional)"
              submitButtonLabel="Accept&#160;Chat"
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
            <WonLabelledHr className="pm__footer__labelledhr" label="Or" />
            <button
              className="pm__footer__button won-button--filled black"
              onClick={() => closeConnection()}
            >
              Decline
            </button>
          </div>
        );
      } else if (connectionUtils.isSuggested(connection)) {
        footerElement = (
          <div className="pm__footer">
            <ChatTextfield
              className="pm__footer__chattextfield"
              connection={connection}
              placeholder="Message (optional)"
              submitButtonLabel="Ask&#160;to&#160;Chat"
              allowEmptySubmit={true}
              allowDetails={false}
              showPersonas={!connection}
              onSubmit={({ value }) => sendRequest(value)}
            />
            <WonLabelledHr className="pm__footer__labelledhr" label="Or" />
            <button
              className="pm__footer__button won-button--filled black"
              onClick={() => closeConnection(true)}
            >
              Bad match - remove!
            </button>
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
