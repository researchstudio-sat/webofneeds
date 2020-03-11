import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";

import * as generalSelectors from "../redux/selectors/general-selectors";
import * as messageUtils from "../redux/utils/message-utils";
import { hasMessagesToLoad } from "../redux/selectors/connection-selectors";
import { getUnreadMessagesByConnectionUri } from "../redux/selectors/message-selectors";
import { get, getIn } from "../utils";
import * as processUtils from "../redux/utils/process-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import vocab from "../service/vocab.js";

import "~/style/_group-atom-messages.scss";
import "~/style/_rdflink.scss";
import WonConnectionHeader from "./connection-header.jsx";
import WonShareDropdown from "./share-dropdown.jsx";
import WonConnectionContextDropdown from "./connection-context-dropdown.jsx";
import ChatTextfield from "./chat-textfield.jsx";
import WonLabelledHr from "./labelled-hr.jsx";
import WonAtomContentMessage from "./messages/atom-content-message.jsx";
import WonConnectionMessage from "./messages/connection-message.jsx";
import { actionCreators } from "../actions/actions.js";
import * as viewSelectors from "../redux/selectors/view-selectors";

const rdfTextfieldHelpText =
  "Expects valid turtle. " +
  `<${vocab.WONMSG.uriPlaceholder.event}> will ` +
  "be replaced by the uri generated for this message. " +
  "Use it, so your TTL can be found when parsing the messages. " +
  "See `won.defaultTurtlePrefixes` " +
  "for prefixes that will be added automatically. E.g." +
  `\`<${vocab.WONMSG.uriPlaceholder.event}> con:text "hello world!". \``;

const mapStateToProps = (state, ownProps) => {
  const connectionUri = generalSelectors.getConnectionUriFromRoute(state);
  const ownedAtom = generalSelectors.getOwnedAtomByConnectionUri(
    state,
    connectionUri
  );
  const connection = getIn(ownedAtom, ["connections", connectionUri]);
  const targetAtomUri = get(connection, "targetAtomUri");
  const targetAtom = getIn(state, ["atoms", targetAtomUri]);
  const allChatMessages = get(connection, "messages");
  const chatMessages =
    allChatMessages &&
    allChatMessages
      .filter(msg => !msg.getIn(["references", "forwards"])) //FILTER OUT ALL FORWARD MESSAGE ENVELOPES JUST IN CASE
      .filter(msg => !messageUtils.isAtomHintMessage(msg)) //FILTER OUT ALL HINT MESSAGES
      .filter(msg => !messageUtils.isSocketHintMessage(msg));
  const hasConnectionMessagesToLoad = hasMessagesToLoad(state, connectionUri);

  let sortedMessages = chatMessages && chatMessages.toArray();
  sortedMessages &&
    sortedMessages.sort(function(a, b) {
      const aDate = a.get("date");
      const bDate = b.get("date");

      const aTime = aDate && aDate.getTime();
      const bTime = bDate && bDate.getTime();

      return aTime - bTime;
    });

  const unreadMessages = getUnreadMessagesByConnectionUri(state, connectionUri);

  const process = get(state, "process");

  return {
    className: ownProps.className,
    ownedAtom,
    targetAtom,
    targetAtomUri,
    connectionUri,
    connection,
    sortedMessageUris: sortedMessages && [
      ...sortedMessages.flatMap(msg => msg.get("uri")),
    ],
    chatMessages,
    unreadMessageCount: unreadMessages && unreadMessages.size,
    isProcessingLoadingMessages:
      connection &&
      processUtils.isConnectionLoadingMessages(process, connectionUri),
    lastUpdateTimestamp: connection && connection.get("lastUpdateDate"),
    isSentRequest: connection && connectionUtils.isRequestSent(connection),
    isReceivedRequest:
      connection && connectionUtils.isRequestReceived(connection),
    isConnected: connection && connectionUtils.isConnected(connection),
    isSuggested: connection && connectionUtils.isSuggested(connection),
    shouldShowRdf: viewSelectors.showRdf(state),
    // if the connect-message is here, everything else should be as well
    hasConnectionMessagesToLoad,
    connectionOrAtomsLoading:
      !connection ||
      !targetAtom ||
      !ownedAtom ||
      processUtils.isAtomLoading(process, ownedAtom.get("uri")) ||
      processUtils.isAtomLoading(process, targetAtomUri) ||
      processUtils.isConnectionLoading(process, connectionUri),
    isConnectionLoading: processUtils.isConnectionLoading(
      process,
      connectionUri
    ),
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
    routerGoResetParams: path => {
      dispatch(actionCreators.router__stateGoResetParams(path));
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

class GroupAtomMessages extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      snapBottom: true,
    };
    this.chatContainerRef = React.createRef();
  }

  render() {
    let footerElement = undefined;

    const rdfLinkToConnection = this.props.shouldShowRdf && (
      <a
        className="rdflink clickable"
        target="_blank"
        rel="noopener noreferrer"
        href={this.props.connectionUri}
      >
        <svg className="rdflink__small">
          <use xlinkHref="#rdf_logo_1" href="#rdf_logo_1" />
        </svg>
        <span className="rdflink__label">Connection</span>
      </a>
    );

    const unreadIndicatorElement =
      this.props.unreadMessageCount && !this.state.snapBottom ? (
        <div className="gpm__content__unreadindicator">
          <div
            className="gpm__content__unreadindicator__content won-button--filled red"
            onClick={this.goToUnreadMessages.bind(this)}
          >
            {this.props.unreadMessageCount} unread Messages
          </div>
        </div>
      ) : (
        undefined
      );

    const loadSpinnerElement = (
      <div className="gpm__content__loadspinner">
        <svg className="hspinner">
          <use xlinkHref="#ico_loading_anim" href="#ico_loading_anim" />
        </svg>
      </div>
    );

    const headerElement = (
      <div className="gpm__header">
        <div className="gpm__header__back">
          <a
            className="gpm__header__back__button clickable show-in-responsive"
            onClick={this.props.routerBack.bind(this)}
          >
            <svg className="gpm__header__back__button__icon">
              <use xlinkHref="#ico36_backarrow" href="#ico36_backarrow" />
            </svg>
          </a>
          <a
            className="gpm__header__back__button clickable hide-in-responsive"
            onClick={() =>
              this.props.routerGoCurrent({ connectionUri: undefined })
            }
          >
            <svg className="gpm__header__back__button__icon">
              <use xlinkHref="#ico36_backarrow" href="#ico36_backarrow" />
            </svg>
          </a>
        </div>
        <WonConnectionHeader connectionUri={this.props.connectionUri} />
        <WonShareDropdown atomUri={this.props.targetAtomUri} />
        <WonConnectionContextDropdown />
      </div>
    );

    const chatMessages =
      this.props.sortedMessageUris &&
      this.props.sortedMessageUris.map((msgUri, index) => {
        return (
          <WonConnectionMessage
            key={msgUri + "-" + index}
            messageUri={msgUri}
            connectionUri={this.props.connectionUri}
            groupChatMessage={true}
          />
        );
      });

    const contentElement = (
      <div
        className="gpm__content"
        ref={this.chatContainerRef}
        onScroll={this.onScroll.bind(this)}
      >
        {unreadIndicatorElement}
        {this.props.targetAtomUri && (
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
              className="gpm__content__loadbutton won-button--outlined thin red"
              onClick={this.loadPreviousMessages.bind(this)}
            >
              Load previous messages
            </button>
          )}

        {chatMessages}
        {rdfLinkToConnection}
      </div>
    );

    if (this.props.isConnected) {
      footerElement = (
        <div className="gpm__footer">
          <ChatTextfield
            className="gpm__footer__chattextfield"
            connectionUri={this.props.connectionUri}
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
    } else if (this.props.isSentRequest) {
      footerElement = (
        <div className="gpm__footer">
          Waiting for the Group Administrator to accept your request.
        </div>
      );
    } else if (this.props.isReceivedRequest) {
      footerElement = (
        <div className="gpm__footer">
          <ChatTextfield
            className="gpm__footer__chattextfield"
            connectionUri={this.props.connectionUri}
            placeholder="Message (optional)"
            submitButtonLabel="Accept&#160;Invite"
            allowEmptySubmit={true}
            allowDetails={false}
            onSubmit={({ value }) => {
              const senderSocketUri = get(this.props.connection, "socketUri");
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
          <WonLabelledHr className="gpm__footer__labelledhr" label="Or" />
          <button
            className="gpm__footer__button won-button--filled black"
            onClick={() => this.closeConnection()}
          >
            Decline
          </button>
        </div>
      );
    } else if (this.props.isSuggested) {
      footerElement = (
        <div className="gpm__footer">
          <ChatTextfield
            className="gpm__footer__chattextfield"
            connectionUri={this.props.connectionUri}
            placeholder="Message (optional)"
            submitButtonLabel="Ask&#160;to&#160;Join"
            allowEmptySubmit={true}
            allowDetails={false}
            showPersonas={!this.props.connection}
            onSubmit={({ value }) => this.sendRequest(value)}
          />
          <WonLabelledHr className="gpm__footer__labelledhr" label="Or" />
          <button
            className="gpm__footer__button won-button--filled black"
            onClick={() => this.closeConnection(true)}
          >
            Bad match - remove!
          </button>
        </div>
      );
    }

    return (
      <won-group-atom-messages
        class={
          (this.props.className || "") +
          (this.props.connectionOrAtomsLoading ? " won-is-loading " : "")
        }
      >
        {headerElement}
        {contentElement}
        {footerElement}
      </won-group-atom-messages>
    );
  }

  componentDidUpdate() {
    if (this.state.snapBottom) {
      this.scrollToBottom();
    }

    this.ensureMessagesAreLoaded();
  }

  goToUnreadMessages() {
    this.snapToBottom();
  }

  snapToBottom() {
    if (!this.state.snapBottom) {
      this.setState({ snapBottom: true });
    }
    this.scrollToBottom();
  }
  unsnapFromBottom() {
    this.setState({ snapBottom: false });
  }

  scrollToBottom() {
    this.chatContainerRef.current.scrollTop = this.chatContainerRef.current.scrollHeight;
  }
  onScroll() {
    //TODO IMPL PROGRAMMATIC SCROLLING
    /*console.debug("called onScroll, needs to be implemented still");
    if (!this._programmaticallyScrolling) {
      //only unsnap if the user scrolled themselves
      this.unsnapFromBottom();
    }

    const sc = this.chatContainerRef.current;
    const isAtBottom = sc.scrollTop + sc.offsetHeight >= sc.scrollHeight;
    if (isAtBottom) {
      this.snapToBottom();
    }

    this._programmaticallyScrolling = false;*/
  }

  send(chatMessage, additionalContent, referencedContent, isTTL = false) {
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

  sendRequest(message) {
    this.props.rateConnection(
      this.props.connectionUri,
      vocab.WONCON.binaryRatingGood
    );
    this.props.connectSockets(
      get(this.props.connection, "socketUri"),
      get(this.props.connection, "targetSocketUri"),
      message
    );
    this.props.routerGoCurrent({
      connectionUri: this.props.connectionUri,
    });
  }

  closeConnection(rateBad = false) {
    if (rateBad) {
      this.props.rateConnection(
        get(this.props.connection, "uri"),
        vocab.WONCON.binaryRatingBad
      );
    }
    this.props.closeConnection(get(this.props.connection, "uri"));
    this.props.routerGoCurrent({ connectionUri: null });
  }

  ensureMessagesAreLoaded() {
    const INITIAL_MESSAGECOUNT = 10;
    if (
      this.props.connection &&
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

GroupAtomMessages.propTypes = {
  connectionUri: PropTypes.string,
  className: PropTypes.string,
  ownedAtom: PropTypes.object,
  targetAtom: PropTypes.object,
  targetAtomUri: PropTypes.string,
  connection: PropTypes.object,
  sortedMessageUris: PropTypes.arrayOf(PropTypes.string),
  chatMessages: PropTypes.object,
  unreadMessageCount: PropTypes.number,
  isProcessingLoadingMessages: PropTypes.bool,
  lastUpdateTimestamp: PropTypes.any,
  isSentRequest: PropTypes.bool,
  isReceivedRequest: PropTypes.bool,
  isConnected: PropTypes.bool,
  isSuggested: PropTypes.bool,
  shouldShowRdf: PropTypes.bool,
  hasConnectionMessagesToLoad: PropTypes.bool,
  connectionOrAtomsLoading: PropTypes.bool,
  isConnectionLoading: PropTypes.bool,
  routerBack: PropTypes.func,
  routerGoCurrent: PropTypes.func,
  routerGoResetParams: PropTypes.func,
  hideAddMessageContent: PropTypes.func,
  sendChatMessage: PropTypes.func,
  connectSockets: PropTypes.func,
  rateConnection: PropTypes.func,
  closeConnection: PropTypes.func,
  showMoreMessages: PropTypes.func,
  showLatestMessages: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(GroupAtomMessages);
