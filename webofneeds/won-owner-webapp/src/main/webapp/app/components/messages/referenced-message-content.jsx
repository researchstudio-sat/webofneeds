/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { actionCreators } from "../../actions/actions.js";
import { connect } from "react-redux";

import PropTypes from "prop-types";
import { getOwnedAtomByConnectionUri } from "../../redux/selectors/general-selectors.js";
import { get, getIn } from "../../utils.js";
import Immutable from "immutable";
import * as ownerApi from "../../api/owner-api";
import won from "../../won-es6";
import WonCombinedMessageContent from "./combined-message-content.jsx";

import "~/style/_referenced-message-content.scss";

const mapStateToProps = (state, ownProps) => {
  const ownedAtom =
    ownProps.connectionUri &&
    getOwnedAtomByConnectionUri(state, ownProps.connectionUri);
  const connection = getIn(ownedAtom, ["connections", ownProps.connectionUri]);
  const message =
    connection && ownProps.messageUri
      ? getIn(connection, ["messages", ownProps.messageUri])
      : Immutable.Map();

  const expandedReferences = getIn(message, [
    "viewState",
    "expandedReferences",
  ]);

  const chatMessages = get(connection, "messages");

  const references = get(message, "references");

  const rejectUris = get(references, "rejects");
  const retractUris = get(references, "retracts");
  const proposeUris = get(references, "proposes");
  const proposeToCancelUris = get(references, "proposesToCancel");
  const acceptUris = get(references, "accepts");
  const forwardUris = get(references, "forwards");
  const claimUris = get(references, "claims");

  const acceptUrisSize = acceptUris ? acceptUris.size : 0;
  const proposeUrisSize = proposeUris ? proposeUris.size : 0;
  const proposeToCancelUrisSize = proposeToCancelUris
    ? proposeToCancelUris.size
    : 0;
  const rejectUrisSize = rejectUris ? rejectUris.size : 0;
  const retractUrisSize = retractUris ? retractUris.size : 0;
  const forwardUrisSize = forwardUris ? forwardUris.size : 0;
  const claimUrisSize = claimUris ? claimUris.size : 0;

  return {
    connectionUri: ownProps.connectionUri,
    messageUri: ownProps.messageUri,
    ownedAtomUri: get(ownedAtom, "uri"),
    message,
    chatMessages: chatMessages,
    connection,
    acceptUrisSize,
    proposeUrisSize,
    proposeToCancelUrisSize,
    rejectUrisSize,
    retractUrisSize,
    forwardUrisSize,
    claimUrisSize,
    expandedReferences,
    hasProposeUris: proposeUrisSize > 0,
    hasAcceptUris: acceptUrisSize > 0,
    hasProposeToCancelUris: proposeToCancelUrisSize > 0,
    hasRetractUris: retractUrisSize > 0,
    hasRejectUris: rejectUrisSize > 0,
    hasForwardUris: forwardUrisSize > 0,
    hasClaimUris: claimUrisSize > 0,
    proposeUrisArray: proposeUris && Array.from(proposeUris.toSet()),
    retractUrisArray: retractUris && Array.from(retractUris.toSet()),
    rejectUrisArray: rejectUris && Array.from(rejectUris.toSet()),
    forwardUrisArray: forwardUris && Array.from(forwardUris.toSet()),
    proposeToCancelUrisArray:
      proposeToCancelUris && Array.from(proposeToCancelUris.toSet()),
    acceptUrisArray: acceptUris && Array.from(acceptUris.toSet()),
    claimUrisArray: claimUris && Array.from(claimUris.toSet()),
    hasContent: get(message, "hasContent"),
    hasNotBeenLoaded: !message,
    multiSelectType: get(connection, "multiSelectType"),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    processAgreementMessage: msg => {
      dispatch(actionCreators.messages__processAgreementMessage(msg));
    },
    messageMarkExpandReference: (
      msgUri,
      connUri,
      atomUri,
      expanded,
      reference
    ) => {
      this.props.ngRedux.dispatch(
        actionCreators.messages__viewState__markExpandReference({
          messageUri: msgUri,
          connectionUri: connUri,
          atomUri: atomUri,
          isExpanded: expanded,
          reference: reference,
        })
      );
    },
  };
};

class WonReferencedMessageContent extends React.Component {
  render() {
    let claimElements;
    let acceptElements;
    let forwardElements;
    let proposeElements;
    let retractElements;
    let proposeToCancelElements;
    let rejectElements;

    if (this.props.hasClaimUris) {
      let fragmentBody;

      if (this.isReferenceExpanded("claims")) {
        const messageElements =
          this.props.claimUrisArray &&
          this.props.claimUrisArray.map(msgUri => (
            <WonCombinedMessageContent
              key={msgUri}
              messageUri={get(this.getReferencedMessage(msgUri), "uri")}
              connectionUri={get(this.props.connection, "uri")}
              className={
                (this.getReferencedMessage(msgUri) &&
                !get(this.getReferencedMessage(msgUri), "outgoingMessage")
                  ? " won-cm--left "
                  : "") +
                (this.getReferencedMessage(msgUri) &&
                get(this.getReferencedMessage(msgUri), "outgoingMessage")
                  ? " won-cm--right "
                  : "")
              }
              onClick={() => this.loadMessage(msgUri)}
            />
          ));

        fragmentBody = (
          <div className="refmsgcontent__fragment__body">{messageElements}</div>
        );
      }

      claimElements = (
        <div className="refmsgcontent__fragment">
          <div
            className={
              "refmsgcontent__fragment__header " +
              (!this.isReferenceExpanded("claims")
                ? "refmsgcontent__fragment__header--collapsed"
                : "")
            }
            onClick={() => this.toggleReferenceExpansion("claims")}
          >
            <div className="refmsgcontent__fragment__header__label">
              Claiming{" "}
              {this.props.claimUrisSize +
                (this.props.claimUrisSize == 1 ? " Message" : " Messages")}
            </div>
            <div className="refmsgcontent__fragment__header__carret">
              <svg>
                {this.isReferenceExpanded("claims") ? (
                  <use xlinkHref="#ico16_arrow_up" href="#ico16_arrow_up" />
                ) : (
                  <use xlinkHref="#ico16_arrow_down" href="#ico16_arrow_down" />
                )}
              </svg>
            </div>
          </div>
          {fragmentBody}
        </div>
      );
    }
    if (this.props.hasAcceptUris) {
      let fragmentBody;

      if (this.isReferenceExpanded("accepts")) {
        const messageElements =
          this.props.acceptUrisArray &&
          this.props.acceptUrisArray.map(msgUri => (
            <WonCombinedMessageContent
              key={msgUri}
              messageUri={get(this.getReferencedMessage(msgUri), "uri")}
              connectionUri={get(this.props.connection, "uri")}
              className={
                (this.getReferencedMessage(msgUri) &&
                !get(this.getReferencedMessage(msgUri), "outgoingMessage")
                  ? " won-cm--left "
                  : "") +
                (this.getReferencedMessage(msgUri) &&
                get(this.getReferencedMessage(msgUri), "outgoingMessage")
                  ? " won-cm--right "
                  : "")
              }
              onClick={() => this.loadMessage(msgUri)}
            />
          ));

        fragmentBody = (
          <div className="refmsgcontent__fragment__body">{messageElements}</div>
        );
      }

      acceptElements = (
        <div className="refmsgcontent__fragment">
          <div
            className={
              "refmsgcontent__fragment__header " +
              (!this.isReferenceExpanded("accepts")
                ? "refmsgcontent__fragment__header--collapsed"
                : "")
            }
            onClick={() => this.toggleReferenceExpansion("accepts")}
          >
            <div className="refmsgcontent__fragment__header__label">
              Accepting{" "}
              {this.props.acceptUrisSize +
                (this.props.acceptUrisSize == 1 ? " Message" : " Messages")}
            </div>
            <div className="refmsgcontent__fragment__header__carret">
              <svg>
                {this.isReferenceExpanded("accepts") ? (
                  <use xlinkHref="#ico16_arrow_up" href="#ico16_arrow_up" />
                ) : (
                  <use xlinkHref="#ico16_arrow_down" href="#ico16_arrow_down" />
                )}
              </svg>
            </div>
          </div>
          {fragmentBody}
        </div>
      );
    }
    if (this.props.hasRetractUris) {
      let fragmentBody;

      if (this.isReferenceExpanded("retracts")) {
        const messageElements =
          this.props.retractUrisArray &&
          this.props.retractUrisArray.map(msgUri => (
            <WonCombinedMessageContent
              key={msgUri}
              messageUri={get(this.getReferencedMessage(msgUri), "uri")}
              connectionUri={get(this.props.connection, "uri")}
              className={
                (this.getReferencedMessage(msgUri) &&
                !get(this.getReferencedMessage(msgUri), "outgoingMessage")
                  ? " won-cm--left "
                  : "") +
                (this.getReferencedMessage(msgUri) &&
                get(this.getReferencedMessage(msgUri), "outgoingMessage")
                  ? " won-cm--right "
                  : "")
              }
              onClick={() => this.loadMessage(msgUri)}
            />
          ));

        fragmentBody = (
          <div className="refmsgcontent__fragment__body">{messageElements}</div>
        );
      }

      retractElements = (
        <div className="refmsgcontent__fragment">
          <div
            className={
              "refmsgcontent__fragment__header " +
              (!this.isReferenceExpanded("retracts")
                ? "refmsgcontent__fragment__header--collapsed"
                : "")
            }
            onClick={() => this.toggleReferenceExpansion("retracts")}
          >
            <div className="refmsgcontent__fragment__header__label">
              Retracting{" "}
              {this.props.retractUrisSize +
                (this.props.retractUrisSize == 1 ? " Message" : " Messages")}
            </div>
            <div className="refmsgcontent__fragment__header__carret clickable">
              <svg>
                {this.isReferenceExpanded("retracts") ? (
                  <use xlinkHref="#ico16_arrow_up" href="#ico16_arrow_up" />
                ) : (
                  <use xlinkHref="#ico16_arrow_down" href="#ico16_arrow_down" />
                )}
              </svg>
            </div>
          </div>
          {fragmentBody}
        </div>
      );
    }
    if (this.props.hasRejectUris) {
      let fragmentBody;

      if (this.isReferenceExpanded("rejects")) {
        const messageElements =
          this.props.rejectUrisArray &&
          this.props.rejectUrisArray.map(msgUri => (
            <WonCombinedMessageContent
              key={msgUri}
              messageUri={get(this.getReferencedMessage(msgUri), "uri")}
              connectionUri={get(this.props.connection, "uri")}
              className={
                (this.getReferencedMessage(msgUri) &&
                !get(this.getReferencedMessage(msgUri), "outgoingMessage")
                  ? " won-cm--left "
                  : "") +
                (this.getReferencedMessage(msgUri) &&
                get(this.getReferencedMessage(msgUri), "outgoingMessage")
                  ? " won-cm--right "
                  : "")
              }
              onClick={() => this.loadMessage(msgUri)}
            />
          ));

        fragmentBody = (
          <div className="refmsgcontent__fragment__body">{messageElements}</div>
        );
      }

      rejectElements = (
        <div className="refmsgcontent__fragment">
          <div
            className={
              "refmsgcontent__fragment__header " +
              (!this.isReferenceExpanded("rejects")
                ? "refmsgcontent__fragment__header--collapsed"
                : "")
            }
            onClick={() => this.toggleReferenceExpansion("rejects")}
          >
            <div className="refmsgcontent__fragment__header__label">
              Rejecting{" "}
              {this.props.rejectUrisSize +
                (this.props.rejectUrisSize == 1 ? " Message" : " Messages")}
            </div>
            <div className="refmsgcontent__fragment__header__carret">
              <svg>
                {this.isReferenceExpanded("rejects") ? (
                  <use xlinkHref="#ico16_arrow_up" href="#ico16_arrow_up" />
                ) : (
                  <use xlinkHref="#ico16_arrow_down" href="#ico16_arrow_down" />
                )}
              </svg>
            </div>
          </div>
          {fragmentBody}
        </div>
      );
    }
    if (this.props.hasProposeUris) {
      let fragmentBody;

      if (this.isReferenceExpanded("proposes")) {
        const messageElements =
          this.props.proposeUrisArray &&
          this.props.proposeUrisArray.map(msgUri => (
            <WonCombinedMessageContent
              key={msgUri}
              messageUri={get(this.getReferencedMessage(msgUri), "uri")}
              connectionUri={get(this.props.connection, "uri")}
              className={
                (this.getReferencedMessage(msgUri) &&
                !get(this.getReferencedMessage(msgUri), "outgoingMessage")
                  ? " won-cm--left "
                  : "") +
                (this.getReferencedMessage(msgUri) &&
                get(this.getReferencedMessage(msgUri), "outgoingMessage")
                  ? " won-cm--right "
                  : "")
              }
              onClick={() => this.loadMessage(msgUri)}
            />
          ));

        fragmentBody = (
          <div className="refmsgcontent__fragment__body">{messageElements}</div>
        );
      }

      proposeElements = (
        <div className="refmsgcontent__fragment">
          <div
            className={
              "refmsgcontent__fragment__header " +
              (!this.isReferenceExpanded("proposes")
                ? "refmsgcontent__fragment__header--collapsed"
                : "")
            }
            onClick={() => this.toggleReferenceExpansion("proposes")}
          >
            <div className="refmsgcontent__fragment__header__label">
              Proposing{" "}
              {this.props.proposeUrisSize +
                (this.props.proposeUrisSize == 1 ? " Message" : " Messages")}
            </div>
            <div className="refmsgcontent__fragment__header__carret">
              <svg>
                {this.isReferenceExpanded("proposes") ? (
                  <use xlinkHref="#ico16_arrow_up" href="#ico16_arrow_up" />
                ) : (
                  <use xlinkHref="#ico16_arrow_down" href="#ico16_arrow_down" />
                )}
              </svg>
            </div>
          </div>
          {fragmentBody}
        </div>
      );
    }
    if (this.props.hasProposeToCancelUris) {
      let fragmentBody;

      if (this.isReferenceExpanded("proposesToCancel")) {
        const messageElements =
          this.props.proposeToCancelUrisArray &&
          this.props.proposeToCancelUrisArray.map(msgUri => (
            <WonCombinedMessageContent
              key={msgUri}
              messageUri={get(this.getReferencedMessage(msgUri), "uri")}
              connectionUri={get(this.props.connection, "uri")}
              className={
                (this.getReferencedMessage(msgUri) &&
                !get(this.getReferencedMessage(msgUri), "outgoingMessage")
                  ? " won-cm--left "
                  : "") +
                (this.getReferencedMessage(msgUri) &&
                get(this.getReferencedMessage(msgUri), "outgoingMessage")
                  ? " won-cm--right "
                  : "")
              }
              onClick={() => this.loadMessage(msgUri)}
            />
          ));

        fragmentBody = (
          <div className="refmsgcontent__fragment__body">{messageElements}</div>
        );
      }

      proposeToCancelElements = (
        <div className="refmsgcontent__fragment">
          <div
            className={
              "refmsgcontent__fragment__header " +
              (!this.isReferenceExpanded("proposesToCancel")
                ? "refmsgcontent__fragment__header--collapsed"
                : "")
            }
            onClick={() => this.toggleReferenceExpansion("proposesToCancel")}
          >
            <div className="refmsgcontent__fragment__header__label">
              Proposing to cancel{" "}
              {this.props.proposeToCancelUrisSize +
                (this.props.proposeToCancelUrisSize == 1
                  ? " Message"
                  : " Messages")}
            </div>
            <div className="refmsgcontent__fragment__header__carret">
              <svg>
                {this.isReferenceExpanded("proposesToCancel") ? (
                  <use xlinkHref="#ico16_arrow_up" href="#ico16_arrow_up" />
                ) : (
                  <use xlinkHref="#ico16_arrow_down" href="#ico16_arrow_down" />
                )}
              </svg>
            </div>
          </div>
          {fragmentBody}
        </div>
      );
    }
    if (this.props.hasForwardUris) {
      let fragmentBody;

      if (this.isReferenceExpanded("forwards")) {
        const messageElements =
          this.props.forwardUrisArray &&
          this.props.forwardUrisArray.map(msgUri => (
            <WonCombinedMessageContent
              key={msgUri}
              messageUri={get(this.getReferencedMessage(msgUri), "uri")}
              connectionUri={get(this.props.connection, "uri")}
              className="won-cm--forward"
              onClick={() => this.loadMessage(msgUri)}
            />
          ));

        fragmentBody = (
          <div className="refmsgcontent__fragment__body">{messageElements}</div>
        );
      }

      forwardElements = (
        <div className="refmsgcontent__fragment">
          <div
            className={
              "refmsgcontent__fragment__header " +
              (!this.isReferenceExpanded("forwards")
                ? "refmsgcontent__fragment__header--collapsed"
                : "")
            }
            onClick={() => this.toggleReferenceExpansion("forwards")}
          >
            <div className="refmsgcontent__fragment__header__label">
              Forwarding{" "}
              {this.props.forwardUrisSize +
                (this.props.forwardUrisSize == 1 ? " Message" : " Messages")}
            </div>
            <div className="refmsgcontent__fragment__header__carret">
              <svg>
                {this.isReferenceExpanded("forwards") ? (
                  <use xlinkHref="#ico16_arrow_up" href="#ico16_arrow_up" />
                ) : (
                  <use xlinkHref="#ico16_arrow_down" href="#ico16_arrow_down" />
                )}
              </svg>
            </div>
          </div>
          {fragmentBody}
        </div>
      );
    }

    return (
      <won-referenced-message-content
        class={
          this.props.hasContent || this.props.hasNotBeenLoaded
            ? "won-has-non-ref-content"
            : ""
        }
      >
        {claimElements}
        {proposeElements}
        {retractElements}
        {acceptElements}
        {proposeToCancelElements}
        {rejectElements}
        {forwardElements}
      </won-referenced-message-content>
    );
  }

  loadMessage(messageUri) {
    if (!this.getReferencedMessage(messageUri)) {
      this.addMessageToState(messageUri);
    }
  }

  addMessageToState(eventUri) {
    const ownedAtomUri = this.props.ownedAtomUri;
    return ownerApi.getMessage(ownedAtomUri, eventUri).then(response => {
      won.wonMessageFromJsonLd(response).then(msg => {
        if (msg.isFromOwner() && msg.getRecipientAtom() === ownedAtomUri) {
          /*if we find out that the recipientatom of the crawled event is actually our
           atom we will call the method again but this time with the correct eventUri
           */
          this.addMessageToState(msg.getRemoteMessageUri());
        } else {
          //If message isnt in the state we add it
          this.props.processAgreementMessage(msg);
        }
      });
    });
  }
  getReferencedMessage(messageUri) {
    let referencedMessage = get(this.props.chatMessages, messageUri);
    if (referencedMessage) {
      return referencedMessage;
    } else {
      return (
        this.props.chatMessages &&
        this.props.chatMessages.find(
          msg => get(msg, "remoteUri") === messageUri
        )
      );
    }
  }

  toggleReferenceExpansion(reference) {
    const currentExpansionState = get(this.props.expandedReferences, reference);

    if (this.props.message && !this.props.multiSelectType) {
      this.props.messageMarkExpandReference(
        this.props.messageUri,
        this.props.connectionUri,
        this.props.ownedAtomUri,
        !currentExpansionState,
        reference
      );
    }
  }

  isReferenceExpanded(reference) {
    return get(this.props.expandedReferences, reference);
  }
}

WonReferencedMessageContent.propTypes = {
  messageUri: PropTypes.string.isRequired,
  connectionUri: PropTypes.string.isRequired,
  ownedAtomUri: PropTypes.string,
  message: PropTypes.object,
  chatMessages: PropTypes.object,
  connection: PropTypes.object,
  acceptUrisSize: PropTypes.number,
  proposeUrisSize: PropTypes.number,
  proposeToCancelUrisSize: PropTypes.number,
  rejectUrisSize: PropTypes.number,
  retractUrisSize: PropTypes.number,
  forwardUrisSize: PropTypes.number,
  claimUrisSize: PropTypes.number,
  multiSelectType: PropTypes.string,
  expandedReferences: PropTypes.object,
  hasProposeUris: PropTypes.bool,
  hasAcceptUris: PropTypes.bool,
  hasProposeToCancelUris: PropTypes.bool,
  hasRetractUris: PropTypes.bool,
  hasRejectUris: PropTypes.bool,
  hasForwardUris: PropTypes.bool,
  hasClaimUris: PropTypes.bool,
  proposeUrisArray: PropTypes.arrayOf(PropTypes.string),
  retractUrisArray: PropTypes.arrayOf(PropTypes.string),
  rejectUrisArray: PropTypes.arrayOf(PropTypes.string),
  forwardUrisArray: PropTypes.arrayOf(PropTypes.string),
  proposeToCancelUrisArray: PropTypes.arrayOf(PropTypes.string),
  acceptUrisArray: PropTypes.arrayOf(PropTypes.string),
  claimUrisArray: PropTypes.arrayOf(PropTypes.string),
  hasContent: PropTypes.bool,
  hasNotBeenLoaded: PropTypes.bool,
  messageMarkExpandReference: PropTypes.func,
  processAgreementMessage: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonReferencedMessageContent);
