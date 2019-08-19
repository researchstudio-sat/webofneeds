import React from "react";
import PropTypes from "prop-types";
import { connect, ReactReduxContext } from "react-redux";

import * as generalSelectors from "../redux/selectors/general-selectors";
import * as messageUtils from "../redux/utils/message-utils";
import { hasMessagesToLoad } from "../redux/selectors/connection-selectors";
import {
  getAgreementMessagesByConnectionUri,
  getCancellationPendingMessagesByConnectionUri,
  getProposalMessagesByConnectionUri,
  getUnreadMessagesByConnectionUri,
} from "../redux/selectors/message-selectors";
import { get } from "../utils";
import * as processUtils from "../redux/utils/process-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import won from "../won-es6.js";

import "~/style/_atom-messages.scss";
import "~/style/_rdflink.scss";
import WonConnectionHeader from "./connection-header.jsx";
import WonShareDropdown from "./share-dropdown.jsx";
import WonConnectionContextDropdown from "./connection-context-dropdown.jsx";
import ChatTextfield from "./chat-textfield.jsx";
import WonLabelledHr from "./labelled-hr.jsx";
import WonPetrinetState from "./petrinet-state.jsx";
import WonAtomContentMessage from "./messages/atom-content-message.jsx";
import WonConnectionMessage from "./messages/connection-message";
import { actionCreators } from "../actions/actions";

const rdfTextfieldHelpText =
  "Expects valid turtle. " +
  `<${won.WONMSG.uriPlaceholder.event}> will ` +
  "be replaced by the uri generated for this message. " +
  "Use it, so your TTL can be found when parsing the messages. " +
  "See `won.defaultTurtlePrefixes` " +
  "for prefixes that will be added automatically. E.g." +
  `\`<${won.WONMSG.uriPlaceholder.event}> con:text "hello world!". \``;

const mapStateToProps = (state, ownProps) => {
  const selectedConnectionUri = ownProps.connectionUri
    ? ownProps.connectionUri
    : generalSelectors.getConnectionUriFromRoute(state);
  const ownedAtom = generalSelectors.getOwnedAtomByConnectionUri(
    state,
    selectedConnectionUri
  );
  const connection =
    ownedAtom && ownedAtom.getIn(["connections", selectedConnectionUri]);
  const targetAtomUri = connection && connection.get("targetAtomUri");
  const targetAtom = targetAtomUri && state.getIn(["atoms", targetAtomUri]);
  const chatMessages =
    connection &&
    connection.get("messages") &&
    connection
      .get("messages")
      .filter(msg => !msg.get("forwardMessage"))
      .filter(msg => !messageUtils.isAtomHintMessage(msg))
      .filter(msg => !messageUtils.isSocketHintMessage(msg));
  const hasConnectionMessagesToLoad = hasMessagesToLoad(
    state,
    selectedConnectionUri
  );

  const agreementData = connection && connection.get("agreementData");
  const petriNetData = connection && connection.get("petriNetData");

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
      const aDate = a.get("date");
      const bDate = b.get("date");

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
    chatMessages.filter(msg => !msg.get("isMessageStatusUpToDate"));

  const showChatData =
    connection &&
    !(
      connection.get("showAgreementData") || connection.get("showPetriNetData")
    );

  const multiSelectType = connection && connection.get("multiSelectType");

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
      ...sortedMessages.flatMap(msg => msg.get("uri")),
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
    showAgreementData: connection && connection.get("showAgreementData"),
    showPetriNetData: connection && connection.get("showPetriNetData"),
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
    lastUpdateTimestamp: connection && connection.get("lastUpdateDate"),
    isSentRequest: connectionUtils.isRequestSent(connection),
    isReceivedRequest: connectionUtils.isRequestReceived(connection),
    isConnected: connectionUtils.isConnected(connection),
    isSuggested: connectionUtils.isSuggested(connection),
    debugmode: won.debugmode,
    shouldShowRdf: state.getIn(["view", "showRdf"]),
    hasConnectionMessagesToLoad,
    hasAgreementMessages: agreementMessages && agreementMessages.size > 0,
    hasPetriNetData: petriNetData && petriNetData.size > 0,
    agreementMessageUris: agreementMessages && agreementMessages.toArray(),
    hasProposalMessages: proposalMessages && proposalMessages.size > 0,
    proposalMessageUris: proposalMessages && proposalMessages.toArray(),
    hasCancellationPendingMessages:
      cancellationPendingMessages && cancellationPendingMessages.size > 0,
    cancellationPendingMessageUris:
      cancellationPendingMessages && cancellationPendingMessages.toArray(),
    connectionOrAtomsLoading:
      !connection ||
      !targetAtom ||
      !ownedAtom ||
      processUtils.isAtomLoading(process, ownedAtom.get("uri")) ||
      processUtils.isAtomLoading(process, targetAtomUri) ||
      processUtils.isConnectionLoading(process, selectedConnectionUri),
    isConnectionLoading: processUtils.isConnectionLoading(
      process,
      selectedConnectionUri
    ),
    showPostContentMessage: showChatData && !multiSelectType && targetAtomUri,
    showOverlayConnection: !!ownProps.connectionUri,
  };
};

const mapDispatchToProps = dispatch => {
  return {
    routerBack: () => {
      dispatch(actionCreators.router__back());
    },
    routerGoCurrent: props => {
      dispatch(actionCreators.router__stateGoCurrent(props));
    },
  };
};

class AtomMessages extends React.Component {
  render() {
    return (
      <ReactReduxContext.Consumer>
        {({ store }) => {
          //TODO
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
                <use xlinkHref="#rdf_logo_1" href="#rdf_logo_1" />
              </svg>
              <span className="rdflink__label">Connection</span>
            </a>
          );

          const unreadIndicatorElement =
            this.props.unreadMessageCount && !self._snapBottom ? (
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
                <use xlinkHref="#ico_loading_anim" href="#ico_loading_anim" />
              </svg>
            </div>
          );

          if (this.props.showChatData) {
            const backButtonElement = this.props.showOverlayConnection ? (
              <a
                className="pm__header__back__button clickable"
                onClick={this.props.routerBack.bind(this)}
              >
                <svg className="pm__header__back__button__icon clickable hide-in-responsive">
                  <use xlinkHref="#ico36_close" href="#ico36_close" />
                </svg>
                <svg className="pm__header__back__button__icon clickable show-in-responsive">
                  <use xlinkHref="#ico36_backarrow" href="#ico36_backarrow" />
                </svg>
              </a>
            ) : (
              <React.Fragment>
                <a
                  className="pm__header__back__button clickable show-in-responsive"
                  onClick={this.props.routerBack.bind(this)}
                >
                  <svg className="pm__header__back__button__icon">
                    <use xlinkHref="#ico36_backarrow" href="#ico36_backarrow" />
                  </svg>
                </a>
                <a
                  className="pm__header__back__button clickable hide-in-responsive"
                  onClick={() =>
                    this.props.routerGoCurrent({ connectionUri: undefined })
                  }
                >
                  <svg className="pm__header__back__button__icon">
                    <use xlinkHref="#ico36_backarrow" href="#ico36_backarrow" />
                  </svg>
                </a>
              </React.Fragment>
            );

            headerElement = (
              <div className="pm__header">
                <div className="pm__header__back">{backButtonElement}</div>
                <WonConnectionHeader
                  connectionUri={this.props.selectedConnectionUri}
                  ngRedux={store}
                />
                <WonShareDropdown
                  atomUri={this.props.targetAtomUri}
                  ngRedux={store}
                />
                <WonConnectionContextDropdown
                  ngRedux={store}
                  showPetriNetDataField={this.showPetriNetDataField}
                  showAgreementDataField={this.showAgreementDataField}
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
                    ngRedux={store}
                    ng-click="this.props.multiSelectType && this.selectMessage(msgUri)"
                  />
                );
              });

            contentElement = (
              <div className="pm__content">
                {unreadIndicatorElement}
                {this.props.showPostContentMessage && (
                  <WonAtomContentMessage atomUri={this.props.targetAtomUri} />
                )}
                {(this.props.isConnectionLoading ||
                  this.props.isProcessingLoadingMessages) &&
                  loadSpinnerElement}
                {!this.props.isSuggested &&
                  !this.props.isConnectionLoading &&
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
                    onClick={() => this.setShowAgreementData(false)}
                  >
                    <svg className="pm__header__back__button__icon clickable">
                      <use
                        xlinkHref="#ico36_backarrow"
                        href="#ico36_backarrow"
                      />
                    </svg>
                  </a>
                </div>
                <div
                  className="pm__header__title clickable"
                  onClick={() => this.setShowAgreementData(false)}
                >
                  Showing Agreement Data
                </div>
                <WonConnectionContextDropdown
                  ngRedux={store}
                  showPetriNetDataField={this.showPetriNetDataField}
                  showAgreementDataField={this.showAgreementDataField}
                />
              </div>
            );

            const agreementMessages =
              !this.props.isProcessingLoadingAgreementData &&
              this.props.agreementMessageUris &&
              this.props.agreementMessageUris.map((agreementUri, index) => {
                return (
                  <WonConnectionMessage
                    key={agreementUri + "-" + index}
                    messageUri={agreementUri}
                    connectionUri={this.props.selectedConnectionUri}
                    ngRedux={store}
                    ng-click="this.props.multiSelectType && this.selectMessage(agreementUri)"
                  />
                );
              });

            const cancellationPendingMessages =
              !this.props.isProcessingLoadingAgreementData &&
              this.props.cancellationPendingMessageUris &&
              this.props.cancellationPendingMessageUris.map(
                (proposesToCancelUri, index) => {
                  return (
                    <WonConnectionMessage
                      key={proposesToCancelUri + "-" + index}
                      messageUri={proposesToCancelUri}
                      connectionUri={this.props.selectedConnectionUri}
                      ngRedux={store}
                      ng-click="this.props.multiSelectType && this.selectMessage(proposesToCancelUri)"
                    />
                  );
                }
              );

            const proposalMessages =
              !this.props.isProcessingLoadingAgreementData &&
              this.props.proposalMessageUris &&
              this.props.proposalMessageUris.map((proposalUri, index) => {
                return (
                  <WonConnectionMessage
                    key={proposalUri + "-" + index}
                    messageUri={proposalUri}
                    connectionUri={this.props.selectedConnectionUri}
                    ngRedux={store}
                    ng-click="this.props.multiSelectType && this.selectMessage(proposalUri)"
                  />
                );
              });

            //TODO IMPL
            contentElement = (
              <div className="pm__content won-agreement-content">
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
                    <div className="pm__content__agreement__title">
                      Agreements
                    </div>
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
                    onClick={() => this.setShowPetriNetData(false)}
                  >
                    <svg className="pm__header__back__button__icon clickable">
                      <use
                        xlinkHref="#ico36_backarrow"
                        href="#ico36_backarrow"
                      />
                    </svg>
                  </a>
                </div>
                <div
                  className="pm__header__title clickable"
                  onClick={() => this.setShowPetriNetData(false)}
                >
                  Showing PetriNet Data
                </div>
                <WonConnectionContextDropdown
                  ngRedux={store}
                  showPetriNetDataField={this.showPetriNetDataField}
                  showAgreementDataField={this.showAgreementDataField}
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
                        ProcessURI: {process.get("processURI")}
                      </div>
                      <WonPetrinetState
                        className="pm__content__petrinet__process__content"
                        processUri={get(process, "processURI")}
                        ngRedux={store}
                      />
                    </div>
                  );
                }
              });

            contentElement = (
              <div className="pm__content won-petrinet-content">
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
                      this.props.shouldShowRdf
                        ? "Enter TTL..."
                        : "Your message..."
                    }
                    submitButtonLabel={
                      this.props.shouldShowRdf ? "Send&#160;RDF" : "Send"
                    }
                    helpText={
                      this.props.shouldShowRdf ? rdfTextfieldHelpText : ""
                    }
                    allowEmptySubmit={false}
                    allowDetails={!this.props.shouldShowRdf}
                    isCode={this.props.shouldShowRdf}
                    onSubmit={({
                      value,
                      additionalContent,
                      referencedContent,
                    }) =>
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
                      onSubmit={({ value }) => this.openRequest(value)}
                    />
                    <WonLabelledHr
                      className="pm__footer__labelledhr"
                      label="Or"
                    />
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
                      onSubmit={({ value, selectedPersona }) =>
                        this.sendRequest(value, selectedPersona)
                      }
                    />
                    <WonLabelledHr
                      className="pm__footer__labelledhr"
                      label="Or"
                    />
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
                (this.props.connectionOrAtomsLoading && " won-is-loading ")
              }
            >
              {headerElement}
              {contentElement}
              {footerElement}
            </won-atom-messages>
          );
        }}
      </ReactReduxContext.Consumer>
    );
  }

  showAgreementDataField() {
    //TODO
    this.setShowPetriNetData(false);
    this.setShowAgreementData(true);
  }

  showPetriNetDataField() {
    //TODO
    this.setShowAgreementData(false);
    this.setShowPetriNetData(true);
  }
  setShowAgreementData(value) {
    //TODO
    this.connections__showAgreementData({
      connectionUri: this.selectedConnectionUri,
      showAgreementData: value,
    });
  }

  setShowPetriNetData(value) {
    //TODO
    this.connections__showPetriNetData({
      connectionUri: this.selectedConnectionUri,
      showPetriNetData: value,
    });
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
  chatMessages: PropTypes.arrayOf(PropTypes.object),
  chatMessagesWithUnknownState: PropTypes.arrayOf(PropTypes.object),
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
  lastUpdateTimestamp: PropTypes.object,
  isSentRequest: PropTypes.bool,
  isReceivedRequest: PropTypes.bool,
  isConnected: PropTypes.bool,
  isSuggested: PropTypes.bool,
  debugmode: PropTypes.bool,
  shouldShowRdf: PropTypes.bool,
  hasConnectionMessagesToLoad: PropTypes.bool,
  hasAgreementMessages: PropTypes.bool,
  hasPetriNetData: PropTypes.bool,
  agreementMessageUris: PropTypes.arrayOf(PropTypes.string),
  hasProposalMessages: PropTypes.bool,
  proposalMessageUris: PropTypes.arrayOf(PropTypes.string),
  hasCancellationPendingMessages: PropTypes.bool,
  cancellationPendingMessageUris: PropTypes.arrayOf(PropTypes.string),
  connectionOrAtomsLoading: PropTypes.bool,
  isConnectionLoading: PropTypes.bool,
  showPostContentMessage: PropTypes.bool,
  showOverlayConnection: PropTypes.bool,
  routerBack: PropTypes.func,
  routerGoCurrent: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(AtomMessages);
