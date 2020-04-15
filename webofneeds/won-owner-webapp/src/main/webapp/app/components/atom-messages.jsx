import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";

import * as generalSelectors from "../redux/selectors/general-selectors";
import * as viewSelectors from "../redux/selectors/view-selectors";
import * as messageUtils from "../redux/utils/message-utils";
import { hasMessagesToLoad } from "../redux/selectors/connection-selectors";
import {
  getAgreementMessagesByConnectionUri,
  getCancellationPendingMessagesByConnectionUri,
  getProposalMessagesByConnectionUri,
  getUnreadMessagesByConnectionUri,
} from "../redux/selectors/message-selectors";
import {
  generateQueryString,
  get,
  getIn,
  getPathname,
  getQueryParams,
} from "../utils";
import * as processUtils from "../redux/utils/process-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import won from "../won-es6.js";
import vocab from "../service/vocab.js";

import "~/style/_atom-messages.scss";
import "~/style/_rdflink.scss";
import ico36_close from "~/images/won-icons/ico36_close.svg";
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
import { actionCreators } from "../actions/actions.js";
import * as ownerApi from "../api/owner-api.js";
import Immutable from "immutable";
import { Link, withRouter } from "react-router-dom";

const rdfTextfieldHelpText =
  "Expects valid turtle. " +
  `<${vocab.WONMSG.uriPlaceholder.event}> will ` +
  "be replaced by the uri generated for this message. " +
  "Use it, so your TTL can be found when parsing the messages. " +
  "See `won.defaultTurtlePrefixes` " +
  "for prefixes that will be added automatically. E.g." +
  `\`<${vocab.WONMSG.uriPlaceholder.event}> con:text "hello world!". \``;

const mapStateToProps = (state, ownProps) => {
  const { connectionUri } = getQueryParams(ownProps.location);
  const selectedConnectionUri = ownProps.connectionUri
    ? ownProps.connectionUri
    : connectionUri;
  const ownedAtom = generalSelectors.getOwnedAtomByConnectionUri(
    state,
    selectedConnectionUri
  );
  const connection = getIn(ownedAtom, ["connections", selectedConnectionUri]);
  const targetAtomUri = get(connection, "targetAtomUri");
  const targetAtom = targetAtomUri && getIn(state, ["atoms", targetAtomUri]);
  const chatMessages =
    get(connection, "messages") &&
    get(connection, "messages")
      .filter(msg => !get(msg, "forwardMessage"))
      .filter(msg => !messageUtils.isAtomHintMessage(msg))
      .filter(msg => !messageUtils.isSocketHintMessage(msg));
  const hasConnectionMessagesToLoad = hasMessagesToLoad(
    state,
    selectedConnectionUri
  );

  const agreementData = get(connection, "agreementData");
  const petriNetData = get(connection, "petriNetData");

  const agreementMessages = getAgreementMessagesByConnectionUri(
    state,
    selectedConnectionUri
  );
  const cancellationPendingMessages = getCancellationPendingMessagesByConnectionUri(
    state,
    selectedConnectionUri
  );
  const proposalMessages = getProposalMessagesByConnectionUri(
    state,
    selectedConnectionUri
  );

  let sortedMessages = chatMessages && chatMessages.toArray();
  sortedMessages &&
    sortedMessages.sort(function(a, b) {
      const aDate = get(a, "date");
      const bDate = get(b, "date");

      const aTime = aDate && aDate.getTime();
      const bTime = bDate && bDate.getTime();

      return aTime - bTime;
    });

  const unreadMessages = getUnreadMessagesByConnectionUri(
    state,
    selectedConnectionUri
  );

  const chatMessagesWithUnknownState =
    chatMessages &&
    chatMessages.filter(msg => !get(msg, "isMessageStatusUpToDate"));

  const showChatData =
    connection &&
    !(
      get(connection, "showAgreementData") ||
      get(connection, "showPetriNetData")
    );

  const multiSelectType = get(connection, "multiSelectType");

  const process = get(state, "process");

  return {
    className: ownProps.className,
    connectionUri: ownProps.connectionUri,
    ownedAtom,
    targetAtom,
    targetAtomUri,
    selectedConnectionUri,
    connection,
    sortedMessageUris: sortedMessages && [
      ...sortedMessages.flatMap(msg => get(msg, "uri")),
    ],
    chatMessages,
    chatMessagesWithUnknownState,
    unreadMessageCount: unreadMessages && unreadMessages.size,
    isProcessingLoadingMessages:
      connection &&
      processUtils.isConnectionLoadingMessages(process, selectedConnectionUri),
    isProcessingLoadingAgreementData:
      connection &&
      processUtils.isConnectionAgreementDataLoading(
        process,
        selectedConnectionUri
      ),
    isProcessingLoadingPetriNetData:
      connection &&
      processUtils.isConnectionPetriNetDataLoading(
        process,
        selectedConnectionUri
      ),
    showAgreementData: get(connection, "showAgreementData"),
    showPetriNetData: get(connection, "showPetriNetData"),
    showChatData,
    agreementData,
    petriNetData,
    petriNetDataArray: petriNetData && petriNetData.toArray(),
    agreementDataLoaded:
      agreementData &&
      processUtils.isConnectionAgreementDataLoaded(
        process,
        selectedConnectionUri
      ),
    petriNetDataLoaded:
      petriNetData &&
      processUtils.isConnectionPetriNetDataLoaded(
        process,
        selectedConnectionUri
      ),
    multiSelectType,
    lastUpdateTimestamp: get(connection, "lastUpdateDate"),
    isSentRequest: connectionUtils.isRequestSent(connection),
    isReceivedRequest: connectionUtils.isRequestReceived(connection),
    isConnected: connectionUtils.isConnected(connection),
    isSuggested: connectionUtils.isSuggested(connection),
    shouldShowRdf: viewSelectors.showRdf(state),
    hasConnectionMessagesToLoad,
    hasAgreementMessages: agreementMessages && agreementMessages.size > 0,
    hasPetriNetData: petriNetData && petriNetData.size > 0,
    agreementMessageArray: agreementMessages && agreementMessages.toArray(),
    hasProposalMessages: proposalMessages && proposalMessages.size > 0,
    proposalMessageArray: proposalMessages && proposalMessages.toArray(),
    hasCancellationPendingMessages:
      cancellationPendingMessages && cancellationPendingMessages.size > 0,
    cancellationPendingMessageArray:
      cancellationPendingMessages && cancellationPendingMessages.toArray(),
    connectionOrAtomsLoading:
      !connection ||
      !targetAtom ||
      !ownedAtom ||
      processUtils.isAtomLoading(process, get(ownedAtom, "uri")) ||
      processUtils.isAtomLoading(process, targetAtomUri) ||
      processUtils.isConnectionLoading(process, selectedConnectionUri),
    isConnectionLoading: processUtils.isConnectionLoading(
      process,
      selectedConnectionUri
    ),
    showPostContentMessage: !!(
      showChatData &&
      !multiSelectType &&
      targetAtomUri
    ),
    showOverlayConnection: !!ownProps.connectionUri,
  };
};

const mapDispatchToProps = dispatch => {
  return {
    setShowPetriNetData: (connectionUri, showPetriNetData) => {
      dispatch(
        actionCreators.connections__showPetriNetData({
          connectionUri: connectionUri,
          showPetriNetData: showPetriNetData,
        })
      );
    },
    setShowAgreementData: (connectionUri, showAgreementData) => {
      dispatch(
        actionCreators.connections__showAgreementData({
          connectionUri: connectionUri,
          showAgreementData: showAgreementData,
        })
      );
    },
    setLoadingAgreementData: (connectionUri, loadingAgreementData) => {
      dispatch(
        actionCreators.connections__setLoadingAgreementData({
          connectionUri: connectionUri,
          loadingAgreementData: loadingAgreementData,
        })
      );
    },
    setLoadingPetriNetData: (connectionUri, loadingPetriNetData) => {
      dispatch(
        actionCreators.connections__setLoadingPetriNetData({
          connectionUri: connectionUri,
          loadingPetriNetData: loadingPetriNetData,
        })
      );
    },
    hideAddMessageContent: () => {
      dispatch(actionCreators.view__hideAddMessageContent());
    },
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
    connectSockets: (senderSocketUri, targetSocketUri, message) => {
      dispatch(
        actionCreators.atoms__connectSockets(
          senderSocketUri,
          targetSocketUri,
          message
        )
      );
    },
    rateConnection: (connectionUri, rating) => {
      dispatch(actionCreators.connections__rate(connectionUri, rating));
    },
    closeConnection: connectionUri => {
      dispatch(actionCreators.connections__close(connectionUri));
    },
    selectMessage: (messageUri, connectionUri, atomUri, isSelected) => {
      dispatch(
        actionCreators.messages__viewState__markAsSelected({
          messageUri: messageUri,
          connectionUri: connectionUri,
          atomUri: atomUri,
          isSelected: isSelected,
        })
      );
    },
    processAgreementMessage: message => {
      dispatch(actionCreators.messages__processAgreementMessage(message));
    },
    updateMessageStatus: (
      messageUri,
      connectionUri,
      atomUri,
      messageStatus
    ) => {
      dispatch(
        actionCreators.messages__updateMessageStatus({
          messageUri: messageUri,
          connectionUri: connectionUri,
          atomUri: atomUri,
          messageStatus: messageStatus,
        })
      );
    },
    updateAgreementData: (connectionUri, agreementDataImm) => {
      dispatch(
        actionCreators.connections__updateAgreementData({
          connectionUri: connectionUri,
          agreementData: agreementDataImm,
        })
      );
    },
    updatePetriNetData: (connectionUri, petriNetDataImm) => {
      dispatch(
        actionCreators.connections__updatePetriNetData({
          connectionUri: connectionUri,
          petriNetData: petriNetDataImm,
        })
      );
    },
    showMoreMessages: (connectionUri, msgCount) => {
      dispatch(
        actionCreators.connections__showMoreMessages(connectionUri, msgCount)
      );
    },
    showLatestMessages: (connectionUri, msgCount) => {
      dispatch(
        actionCreators.connections__showLatestMessages(connectionUri, msgCount)
      );
    },
  };
};

class AtomMessages extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      snapBottom: true,
    };
    this.chatContainerRef = React.createRef();
  }

  render() {
    let headerElement = undefined;
    let contentElement = undefined;
    let footerElement = undefined;

    const rdfLinkToConnection = this.props.shouldShowRdf && (
      <a
        className="rdflink clickable"
        target="_blank"
        rel="noopener noreferrer"
        href={this.props.selectedConnectionUri}
      >
        <svg className="rdflink__small">
          <use xlinkHref={rdf_logo_1} href={rdf_logo_1} />
        </svg>
        <span className="rdflink__label">Connection</span>
      </a>
    );

    const unreadIndicatorElement =
      this.props.unreadMessageCount && !this.state.snapBottom ? (
        <div className="pm__content__unreadindicator">
          <div
            className="pm__content__unreadindicator__content won-button--filled red"
            onClick={this.goToUnreadMessages.bind(this)}
          >
            {this.props.unreadMessageCount} unread Messages
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

    if (this.props.showChatData) {
      const backButtonElement = this.props.showOverlayConnection ? (
        <a
          className="pm__header__back__button clickable"
          onClick={this.props.history.goBack}
        >
          <svg className="pm__header__back__button__icon clickable hide-in-responsive">
            <use xlinkHref={ico36_close} href={ico36_close} />
          </svg>
          <svg className="pm__header__back__button__icon clickable show-in-responsive">
            <use xlinkHref={ico36_backarrow} href={ico36_backarrow} />
          </svg>
        </a>
      ) : (
        <React.Fragment>
          <a
            className="pm__header__back__button clickable show-in-responsive"
            onClick={this.props.history.goBack}
          >
            <svg className="pm__header__back__button__icon">
              <use xlinkHref={ico36_backarrow} href={ico36_backarrow} />
            </svg>
          </a>
          <Link
            className="pm__header__back__button clickable hide-in-responsive"
            to={location => location.pathname}
          >
            <svg className="pm__header__back__button__icon">
              <use xlinkHref={ico36_backarrow} href={ico36_backarrow} />
            </svg>
          </Link>
        </React.Fragment>
      );

      headerElement = (
        <div className="pm__header">
          <div className="pm__header__back">{backButtonElement}</div>
          <WonConnectionHeader
            connectionUri={this.props.selectedConnectionUri}
          />
          <WonShareDropdown atomUri={this.props.targetAtomUri} />
          <WonConnectionContextDropdown
            showPetriNetDataField={this.showPetriNetDataField.bind(this)}
            showAgreementDataField={this.showAgreementDataField.bind(this)}
          />
        </div>
      );

      const chatMessages =
        this.props.sortedMessageUris &&
        this.props.sortedMessageUris.map((msgUri, index) => {
          return (
            <WonConnectionMessage
              key={msgUri + "-" + index}
              messageUri={msgUri}
              connectionUri={this.props.selectedConnectionUri}
              onClick={
                this.props.multiSelectType
                  ? () => this.selectMessage(msgUri)
                  : undefined
              }
            />
          );
        });

      contentElement = (
        <div
          className="pm__content"
          ref={this.chatContainerRef}
          onScroll={this.onScroll.bind(this)}
        >
          {unreadIndicatorElement}
          {this.props.showPostContentMessage && (
            <WonAtomContentMessage atomUri={this.props.targetAtomUri} />
          )}
          {(this.props.isConnectionLoading ||
            this.props.isProcessingLoadingMessages) &&
            loadSpinnerElement}
          {!this.props.isSuggested &&
            !this.props.isConnectionLoading &&
            !connectionUtils.isUsingTemporaryUri(this.props.connection) &&
            !this.props.isProcessingLoadingMessages &&
            this.props.hasConnectionMessagesToLoad && (
              <button
                className="pm__content__loadbutton won-button--outlined thin red"
                onClick={this.loadPreviousMessages.bind(this)}
              >
                Load previous messages
              </button>
            )}

          {chatMessages}

          {rdfLinkToConnection}
        </div>
      );
    } else if (this.props.showAgreementData) {
      headerElement = (
        <div className="pm__header">
          <div className="pm__header__back">
            <a
              className="pm__header__back__button clickable"
              onClick={() =>
                this.props.setShowAgreementData(
                  this.props.selectedConnectionUri,
                  false
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
              this.props.setShowAgreementData(
                this.props.selectedConnectionUri,
                false
              )
            }
          >
            Showing Agreement Data
          </div>
          <WonConnectionContextDropdown
            showPetriNetDataField={this.showPetriNetDataField.bind(this)}
            showAgreementDataField={this.showAgreementDataField.bind(this)}
          />
        </div>
      );

      const agreementMessages =
        !this.props.isProcessingLoadingAgreementData &&
        this.props.agreementMessageArray &&
        this.props.agreementMessageArray.map((msg, index) => {
          return (
            <WonConnectionMessage
              key={get(msg, "uri") + "-" + index}
              messageUri={get(msg, "uri")}
              connectionUri={this.props.selectedConnectionUri}
              onClick={
                this.props.multiSelectType
                  ? () => this.selectMessage(get(msg, "uri"))
                  : undefined
              }
            />
          );
        });

      const cancellationPendingMessages =
        !this.props.isProcessingLoadingAgreementData &&
        this.props.cancellationPendingMessageArray &&
        this.props.cancellationPendingMessageArray.map((msg, index) => {
          return (
            <WonConnectionMessage
              key={get(msg, "uri") + "-" + index}
              messageUri={get(msg, "uri")}
              connectionUri={this.props.selectedConnectionUri}
              onClick={
                this.props.multiSelectType
                  ? () => this.selectMessage(get(msg, "uri"))
                  : undefined
              }
            />
          );
        });

      const proposalMessages =
        !this.props.isProcessingLoadingAgreementData &&
        this.props.proposalMessageArray &&
        this.props.proposalMessageArray.map((msg, index) => {
          return (
            <WonConnectionMessage
              key={get(msg, "uri") + "-" + index}
              messageUri={get(msg, "uri")}
              connectionUri={this.props.selectedConnectionUri}
              onClick={
                this.props.multiSelectType
                  ? () => this.selectMessage(get(msg, "uri"))
                  : undefined
              }
            />
          );
        });

      contentElement = (
        <div
          className="pm__content won-agreement-content"
          ref={this.chatContainerRef}
        >
          {unreadIndicatorElement}
          {(this.props.isConnectionLoading ||
            this.props.isProcessingLoadingMessages ||
            (this.props.showAgreementData &&
              this.props.isProcessingLoadingAgreementData)) &&
            loadSpinnerElement}
          {this.props.isProcessingLoadingAgreementData && (
            <div className="pm__content__agreement__loadingtext">
              Calculating Agreement Status
            </div>
          )}

          {!(
            this.props.hasAgreementMessages ||
            this.props.hasCancellationPendingMessages ||
            this.props.hasProposalMessages
          ) &&
            !this.props.isProcessingLoadingAgreementData && (
              <div className="pm__content__agreement__emptytext">
                No Agreements within this Conversation
              </div>
            )}

          {this.props.hasAgreementMessages &&
            !this.props.isProcessingLoadingAgreementData && (
              <div className="pm__content__agreement__title">Agreements</div>
            )}

          {agreementMessages}

          {this.props.hasCancellationPendingMessages &&
            !this.props.isProcessingLoadingAgreementData && (
              <div className="pm__content__agreement__title">
                Agreements with Pending Cancellation
              </div>
            )}

          {cancellationPendingMessages}

          {this.props.hasProposalMessages &&
            !this.props.isProcessingLoadingAgreementData && (
              <div className="pm__content__agreement__title">
                Open Proposals
              </div>
            )}

          {proposalMessages}

          {rdfLinkToConnection}
        </div>
      );
    } else if (this.props.showPetriNetData) {
      headerElement = (
        <div className="pm__header">
          <div className="pm__header__back">
            <a
              className="pm__header__back__button clickable"
              onClick={() =>
                this.props.setShowPetriNetData(
                  this.props.selectedConnectionUri,
                  false
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
              this.props.setShowPetriNetData(
                this.props.selectedConnectionUri,
                false
              )
            }
          >
            Showing PetriNet Data
          </div>
          <WonConnectionContextDropdown
            showPetriNetDataField={this.showPetriNetDataField.bind(this)}
            showAgreementDataField={this.showAgreementDataField.bind(this)}
          />
        </div>
      );

      const petrinetProcessArray =
        (!this.props.isProcessingLoadingPetriNetData ||
          this.props.hasPetriNetData) &&
        this.props.petriNetDataArray &&
        this.props.petriNetDataArray.map((process, index) => {
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
        <div
          className="pm__content won-petrinet-content"
          ref={this.chatContainerRef}
        >
          {unreadIndicatorElement}
          {(this.props.isConnectionLoading ||
            this.props.isProcessingLoadingMessages ||
            (this.props.isProcessingLoadingPetriNetData &&
              !this.props.hasPetriNetData)) &&
            loadSpinnerElement}
          {!this.props.hasPetriNetData &&
            (this.props.isProcessingLoadingPetriNetData ? (
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

    if (!this.props.showPetriNetData) {
      if (this.props.isConnected) {
        footerElement = (
          <div className="pm__footer">
            <ChatTextfield
              className="pm__footer__chattextfield"
              connectionUri={this.props.selectedConnectionUri}
              placeholder={
                this.props.shouldShowRdf ? "Enter TTL..." : "Your message..."
              }
              submitButtonLabel={this.props.shouldShowRdf ? "Send RDF" : "Send"}
              helpText={this.props.shouldShowRdf ? rdfTextfieldHelpText : ""}
              allowEmptySubmit={false}
              allowDetails={!this.props.shouldShowRdf}
              isCode={this.props.shouldShowRdf}
              onSubmit={({ value, additionalContent, referencedContent }) =>
                this.send(
                  value,
                  additionalContent,
                  referencedContent,
                  this.props.shouldShowRdf
                )
              }
            />
          </div>
        );
      } else if (!this.props.multiSelectType) {
        if (this.props.isSentRequest) {
          footerElement = (
            <div className="pm__footer">
              Waiting for them to accept your chat request.
            </div>
          );
        } else if (this.props.isReceivedRequest) {
          footerElement = (
            <div className="pm__footer">
              <ChatTextfield
                className="pm__footer__chattextfield"
                connectionUri={this.props.selectedConnectionUri}
                placeholder="Message (optional)"
                submitButtonLabel="Accept&#160;Chat"
                allowEmptySubmit={true}
                allowDetails={false}
                onSubmit={({ value }) => {
                  const senderSocketUri = get(
                    this.props.connection,
                    "socketUri"
                  );
                  const targetSocketUri = get(
                    this.props.connection,
                    "targetSocketUri"
                  );
                  this.props.connectSockets(
                    senderSocketUri,
                    targetSocketUri,
                    value
                  );
                }}
              />
              <WonLabelledHr className="pm__footer__labelledhr" label="Or" />
              <button
                className="pm__footer__button won-button--filled black"
                onClick={() => this.closeConnection()}
              >
                Decline
              </button>
            </div>
          );
        } else if (this.props.isSuggested) {
          footerElement = (
            <div className="pm__footer">
              <ChatTextfield
                className="pm__footer__chattextfield"
                connectionUri={this.props.selectedConnectionUri}
                placeholder="Message (optional)"
                submitButtonLabel="Ask&#160;to&#160;Chat"
                allowEmptySubmit={true}
                allowDetails={false}
                showPersonas={!this.props.connection}
                onSubmit={({ value }) => this.sendRequest(value)}
              />
              <WonLabelledHr className="pm__footer__labelledhr" label="Or" />
              <button
                className="pm__footer__button won-button--filled black"
                onClick={() => this.closeConnection(true)}
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
          (this.props.className || "") +
          (this.props.connectionOrAtomsLoading ? " won-is-loading " : "")
        }
      >
        {headerElement}
        {contentElement}
        {footerElement}
      </won-atom-messages>
    );
  }

  componentDidUpdate() {
    if (this.state.snapBottom && this.props.showChatData) {
      this.scrollToBottom();
    }

    this.ensureMessagesAreLoaded();
    this.ensureAgreementDataIsLoaded();
    this.ensurePetriNetDataIsLoaded();
    this.ensureMessageStateIsUpToDate();
  }

  showAgreementDataField() {
    this.props.setShowPetriNetData(this.props.selectedConnectionUri, false);
    this.props.setShowAgreementData(this.props.selectedConnectionUri, true);
  }

  showPetriNetDataField() {
    this.props.setShowAgreementData(this.props.selectedConnectionUri, false);
    this.props.setShowPetriNetData(this.props.selectedConnectionUri, true);
  }

  goToUnreadMessages() {
    if (this.props.showAgreementData) {
      this.props.setShowAgreementData(this.props.selectedConnectionUri, false);
    }
    if (this.props.showPetriNetData) {
      this.props.setShowPetriNetData(this.props.selectedConnectionUri, false);
    }
    this.snapToBottom();
  }

  snapToBottom() {
    if (!this.state.snapBottom) {
      this.setState({ snapBottom: true });
    }
  }
  unsnapFromBottom() {
    if (this.state.snapBottom) {
      this.setState({ snapBottom: false });
    }
  }

  scrollToBottom() {
    this.chatContainerRef.current.scrollTop = this.chatContainerRef.current.scrollHeight;
  }
  onScroll() {
    const sc = this.chatContainerRef.current;
    const isAlmostAtBottom =
      sc.scrollTop + sc.offsetHeight >= sc.scrollHeight - 75;
    if (isAlmostAtBottom) {
      this.snapToBottom();
    } else {
      this.unsnapFromBottom();
    }
  }

  send(chatMessage, additionalContent, referencedContent, isTTL = false) {
    this.props.setShowAgreementData(this.props.selectedConnectionUri, false);
    this.props.hideAddMessageContent();

    const trimmedMsg = chatMessage.trim();
    if (trimmedMsg || additionalContent || referencedContent) {
      const senderSocketUri = get(this.props.connection, "socketUri");
      const targetSocketUri = get(this.props.connection, "targetSocketUri");

      this.props.sendChatMessage(
        trimmedMsg,
        additionalContent,
        referencedContent,
        senderSocketUri,
        targetSocketUri,
        get(this.props.connection, "uri"),
        isTTL
      );
    }
  }

  addMessageToState(messageUri) {
    const ownedAtomUri = get(this.props.ownedAtom, "uri");
    return ownerApi.getMessage(ownedAtomUri, messageUri).then(response => {
      won.wonMessageFromJsonLd(response, messageUri).then(msg => {
        //If message isnt in the state we add it
        if (!get(this.props.chatMessages, messageUri)) {
          this.props.processAgreementMessage(msg);
        }
      });
    });
  }

  sendRequest(message) {
    this.props.rateConnection(
      this.props.selectedConnectionUri,
      vocab.WONCON.binaryRatingGood
    );

    this.props.connectSockets(
      get(this.props.connection, "socketUri"),
      get(this.props.connection, "targetSocketUri"),
      message
    );
    if (this.showOverlayConnection) {
      this.props.history.goBack();
    } else {
      this.props.history.push(
        generateQueryString(getPathname(this.props.history.location), {
          connectionUri: this.props.selectedConnectionUri,
        })
      );
    }
  }

  closeConnection(rateBad = false) {
    if (rateBad) {
      this.props.rateConnection(
        get(this.props.connection, "uri"),
        vocab.WONCON.binaryRatingBad
      );
    }
    this.props.closeConnection(get(this.props.connection, "uri"));

    if (this.showOverlayConnection) {
      this.props.history.goBack();
    } else {
      this.props.history.push(
        generateQueryString(getPathname(this.props.history.location), {
          connectionUri: undefined,
        })
      );
    }
  }

  selectMessage(msgUri) {
    const msg = getIn(this.props.connection, ["messages", msgUri]);

    if (msg) {
      this.props.selectMessage(
        msgUri,
        get(this.props.connection, "uri"),
        get(this.props.ownedAtom, "uri"),
        !getIn(msg, ["viewState", "isSelected"])
      );
    }
  }

  ensureMessagesAreLoaded() {
    // make sure latest messages are loaded
    const INITIAL_MESSAGECOUNT = 10;
    if (
      this.props.connection &&
      !connectionUtils.isUsingTemporaryUri(this.props.connection) &&
      !this.props.isConnectionLoading &&
      !this.props.isProcessingLoadingMessages &&
      get(this.props.connection, "messages").size < INITIAL_MESSAGECOUNT &&
      this.props.hasConnectionMessagesToLoad
    ) {
      this.props.showLatestMessages(
        get(this.props.connection, "uri"),
        INITIAL_MESSAGECOUNT
      );
    }
  }

  async ensurePetriNetDataIsLoaded(forceFetch = false) {
    if (
      forceFetch ||
      (this.props.isConnected &&
        !connectionUtils.isUsingTemporaryUri(this.props.connection) &&
        !this.props.isProcessingLoadingPetriNetData &&
        !this.props.petriNetDataLoaded)
    ) {
      const connectionUri = get(this.props.connection, "uri");

      this.props.setLoadingPetriNetData(connectionUri, true);

      try {
        const response = await ownerApi.getPetriNetUris(connectionUri);
        const petriNetData = {};

        response.forEach(entry => {
          if (entry.processURI) {
            petriNetData[entry.processURI] = entry;
          }
        });

        const petriNetDataImm = Immutable.fromJS(petriNetData);
        this.props.updatePetriNetData(connectionUri, petriNetDataImm);
      } catch (error) {
        console.error("Error:", error);
        this.props.setLoadingPetriNetData(connectionUri, false);
      }
    }
  }

  ensureAgreementDataIsLoaded(forceFetch = false) {
    if (
      forceFetch ||
      (this.props.isConnected &&
        !connectionUtils.isUsingTemporaryUri(this.props.connection) &&
        !this.props.isProcessingLoadingAgreementData &&
        !this.props.agreementDataLoaded)
    ) {
      this.props.setLoadingAgreementData(
        this.props.selectedConnectionUri,
        true
      );
      ownerApi
        .getAgreementProtocolUris(get(this.props.connection, "uri"))
        .then(response => {
          let proposedMessageUris = [];
          const pendingProposals = response.pendingProposals;

          if (pendingProposals) {
            pendingProposals.forEach(prop => {
              if (prop.proposes) {
                proposedMessageUris = proposedMessageUris.concat(prop.proposes);
              }
            });
          }

          const agreementDataImm = Immutable.fromJS({
            agreementUris: Immutable.Set(response.agreementUris),
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
            proposedMessageUris: Immutable.Set(proposedMessageUris),
            claimedMessageUris: Immutable.Set(response.claimedMessageUris),
          });

          this.props.updateAgreementData(
            this.props.selectedConnectionUri,
            agreementDataImm
          );

          //Retrieve all the relevant messages
          agreementDataImm.map(uriList =>
            uriList.map(uri => this.addMessageToState(uri))
          );
        })
        .catch(error => {
          console.error("Error:", error);
          this.props.setLoadingAgreementData(
            this.props.selectedConnectionUri,
            false
          );
        });
    }
  }

  ensureMessageStateIsUpToDate() {
    if (
      this.props.isConnected &&
      !connectionUtils.isUsingTemporaryUri(this.props.connection) &&
      !this.props.isConnectionLoading &&
      !this.props.isProcessingLoadingAgreementData &&
      !this.props.isProcessingLoadingMessages &&
      this.props.agreementDataLoaded &&
      this.props.chatMessagesWithUnknownState &&
      this.props.chatMessagesWithUnknownState.size > 0
    ) {
      console.debug(
        "Ensure Message Status is up-to-date for: ",
        this.props.chatMessagesWithUnknownState.size,
        " Messages"
      );
      this.props.chatMessagesWithUnknownState.forEach(msg => {
        let messageStatus = get(msg, "messageStatus");
        const msgUri = get(msg, "uri");

        const acceptedUris = get(this.props.agreementData, "agreementUris");
        const rejectedUris = get(
          this.props.agreementData,
          "rejectedMessageUris"
        );

        const retractedUris = get(
          this.props.agreementData,
          "retractedMessageUris"
        );
        const cancelledUris = get(
          this.props.agreementData,
          "cancelledAgreementUris"
        );
        const cancellationPendingUris = get(
          this.props.agreementData,
          "cancellationPendingAgreementUris"
        );
        const claimedUris = get(this.props.agreementData, "claimedMessageUris");
        const proposedUris = get(
          this.props.agreementData,
          "proposedMessageUris"
        );

        const isProposed = get(messageStatus, "isProposed");
        const isClaimed = get(messageStatus, "isClaimed");
        const isAccepted = get(messageStatus, "isAccepted");
        const isRejected = get(messageStatus, "isRejected");
        const isRetracted = get(messageStatus, "isRetracted");
        const isCancelled = get(messageStatus, "isCancelled");
        const isCancellationPending = get(
          messageStatus,
          "isCancellationPending"
        );

        const isOldProposed = !!get(proposedUris, msgUri);
        const isOldClaimed = !!get(claimedUris, msgUri);
        const isOldAccepted = !!get(acceptedUris, msgUri);
        const isOldRejected = !!get(rejectedUris, msgUri);
        const isOldRetracted = !!get(retractedUris, msgUri);
        const isOldCancelled = !!get(cancelledUris, msgUri);
        const isOldCancellationPending = !!get(cancellationPendingUris, msgUri);

        messageStatus = messageStatus
          .set("isProposed", isProposed || isOldProposed)
          .set("isClaimed", isClaimed || isOldClaimed)
          .set("isAccepted", isAccepted || isOldAccepted)
          .set("isRejected", isRejected || isOldRejected)
          .set("isRetracted", isRetracted || isOldRetracted)
          .set("isCancelled", isCancelled || isOldCancelled)
          .set(
            "isCancellationPending",
            isCancellationPending || isOldCancellationPending
          );

        this.props.updateMessageStatus(
          msgUri,
          this.props.selectedConnectionUri,
          get(this.props.ownedAtom, "uri"),
          messageStatus
        );
      });
    }
  }

  loadPreviousMessages() {
    const MORE_MESSAGECOUNT = 5;
    if (
      this.props.connection &&
      !this.props.isConnectionLoading &&
      !this.props.isProcessingLoadingMessages
    ) {
      this.props.showMoreMessages(
        get(this.props.connection, "uri"),
        MORE_MESSAGECOUNT
      );
    }
  }
}

AtomMessages.propTypes = {
  connectionUri: PropTypes.string,
  className: PropTypes.string,
  ownedAtom: PropTypes.object,
  targetAtom: PropTypes.object,
  targetAtomUri: PropTypes.string,
  selectedConnectionUri: PropTypes.string,
  connection: PropTypes.object,
  sortedMessageUris: PropTypes.arrayOf(PropTypes.string),
  chatMessages: PropTypes.object,
  chatMessagesWithUnknownState: PropTypes.object,
  unreadMessageCount: PropTypes.number,
  isProcessingLoadingMessages: PropTypes.bool,
  isProcessingLoadingAgreementData: PropTypes.bool,
  isProcessingLoadingPetriNetData: PropTypes.bool,
  showAgreementData: PropTypes.bool,
  showPetriNetData: PropTypes.bool,
  showChatData: PropTypes.bool,
  agreementData: PropTypes.object,
  petriNetData: PropTypes.object,
  petriNetDataArray: PropTypes.arrayOf(PropTypes.object),
  agreementDataLoaded: PropTypes.bool,
  petriNetDataLoaded: PropTypes.bool,
  multiSelectType: PropTypes.string,
  lastUpdateTimestamp: PropTypes.any,
  isSentRequest: PropTypes.bool,
  isReceivedRequest: PropTypes.bool,
  isConnected: PropTypes.bool,
  isSuggested: PropTypes.bool,
  shouldShowRdf: PropTypes.bool,
  hasConnectionMessagesToLoad: PropTypes.bool,
  hasAgreementMessages: PropTypes.bool,
  hasPetriNetData: PropTypes.bool,
  agreementMessageArray: PropTypes.arrayOf(PropTypes.object),
  hasProposalMessages: PropTypes.bool,
  proposalMessageArray: PropTypes.arrayOf(PropTypes.object),
  hasCancellationPendingMessages: PropTypes.bool,
  cancellationPendingMessageArray: PropTypes.arrayOf(PropTypes.object),
  connectionOrAtomsLoading: PropTypes.bool,
  isConnectionLoading: PropTypes.bool,
  showPostContentMessage: PropTypes.bool,
  showOverlayConnection: PropTypes.bool,
  setShowPetriNetData: PropTypes.func,
  setShowAgreementData: PropTypes.func,
  hideAddMessageContent: PropTypes.func,
  sendChatMessage: PropTypes.func,
  connectSockets: PropTypes.func,
  rateConnection: PropTypes.func,
  closeConnection: PropTypes.func,
  selectMessage: PropTypes.func,
  processAgreementMessage: PropTypes.func,
  setLoadingAgreementData: PropTypes.func,
  setLoadingPetriNetData: PropTypes.func,
  updateMessageStatus: PropTypes.func,
  updatePetriNetData: PropTypes.func,
  updateAgreementData: PropTypes.func,
  showMoreMessages: PropTypes.func,
  showLatestMessages: PropTypes.func,
  history: PropTypes.object,
};

export default withRouter(
  connect(
    mapStateToProps,
    mapDispatchToProps
  )(AtomMessages)
);
