/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { actionCreators } from "../../actions/actions.js";

import PropTypes from "prop-types";
import { getOwnedAtomByConnectionUri } from "../../redux/selectors/general-selectors.js";
import { get, getIn } from "../../utils.js";
import Immutable from "immutable";
import * as ownerApi from "../../api/owner-api";
import won from "../../won-es6";
import WonCombinedMessageContent from "./combined-message-content.jsx";

import "~/style/_referenced-message-content.scss";

export default class WonReferencedMessageContent extends React.Component {
  componentDidMount() {
    this.messageUri = this.props.messageUri;
    this.connectionUri = this.props.connectionUri;
    this.disconnect = this.props.ngRedux.connect(
      this.selectFromState.bind(this),
      actionCreators
    )(state => {
      this.setState(state);
    });
  }

  componentWillUnmount() {
    this.disconnect();
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    this.messageUri = nextProps.messageUri;
    this.connectionUri = nextProps.connectionUri;
    this.setState(this.selectFromState(this.props.ngRedux.getState()));
  }

  selectFromState(state) {
    const ownedAtom =
      this.connectionUri &&
      getOwnedAtomByConnectionUri(state, this.connectionUri);
    const connection = getIn(ownedAtom, ["connections", this.connectionUri]);
    const message =
      connection && this.messageUri
        ? getIn(connection, ["messages", this.messageUri])
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
    };
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div />;
    }

    let claimElements;
    let acceptElements;
    let forwardElements;
    let proposeElements;
    let retractElements;
    let proposeToCancelElements;
    let rejectElements;

    if (this.state.hasClaimUris) {
      let fragmentBody;

      if (this.isReferenceExpanded("claims")) {
        const messageElements =
          this.state.claimUrisArray &&
          this.state.claimUrisArray.map(msgUri => (
            <WonCombinedMessageContent
              key={msgUri}
              messageUri={get(this.getReferencedMessage(msgUri), "uri")}
              connectionUri={get(this.state.connection, "uri")}
              ngRedux={this.props.ngRedux}
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
              {this.state.claimUrisSize +
                (this.state.claimUrisSize == 1 ? " Message" : " Messages")}
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
    if (this.state.hasAcceptUris) {
      let fragmentBody;

      if (this.isReferenceExpanded("accepts")) {
        const messageElements =
          this.state.acceptUrisArray &&
          this.state.acceptUrisArray.map(msgUri => (
            <WonCombinedMessageContent
              key={msgUri}
              messageUri={get(this.getReferencedMessage(msgUri), "uri")}
              connectionUri={get(this.state.connection, "uri")}
              ngRedux={this.props.ngRedux}
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
              {this.state.acceptUrisSize +
                (this.state.acceptUrisSize == 1 ? " Message" : " Messages")}
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
    if (this.state.hasRetractUris) {
      let fragmentBody;

      if (this.isReferenceExpanded("retracts")) {
        const messageElements =
          this.state.retractUrisArray &&
          this.state.retractUrisArray.map(msgUri => (
            <WonCombinedMessageContent
              key={msgUri}
              messageUri={get(this.getReferencedMessage(msgUri), "uri")}
              connectionUri={get(this.state.connection, "uri")}
              ngRedux={this.props.ngRedux}
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
              {this.state.retractUrisSize +
                (this.state.retractUrisSize == 1 ? " Message" : " Messages")}
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
    if (this.state.hasRejectUris) {
      let fragmentBody;

      if (this.isReferenceExpanded("rejects")) {
        const messageElements =
          this.state.rejectUrisArray &&
          this.state.rejectUrisArray.map(msgUri => (
            <WonCombinedMessageContent
              key={msgUri}
              messageUri={get(this.getReferencedMessage(msgUri), "uri")}
              connectionUri={get(this.state.connection, "uri")}
              ngRedux={this.props.ngRedux}
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
              {this.state.rejectUrisSize +
                (this.state.rejectUrisSize == 1 ? " Message" : " Messages")}
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
    if (this.state.hasProposeUris) {
      let fragmentBody;

      if (this.isReferenceExpanded("proposes")) {
        const messageElements =
          this.state.proposeUrisArray &&
          this.state.proposeUrisArray.map(msgUri => (
            <WonCombinedMessageContent
              key={msgUri}
              messageUri={get(this.getReferencedMessage(msgUri), "uri")}
              connectionUri={get(this.state.connection, "uri")}
              ngRedux={this.props.ngRedux}
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
              {this.state.proposeUrisSize +
                (this.state.proposeUrisSize == 1 ? " Message" : " Messages")}
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
    if (this.state.hasProposeToCancelUris) {
      let fragmentBody;

      if (this.isReferenceExpanded("proposesToCancel")) {
        const messageElements =
          this.state.proposeToCancelUrisArray &&
          this.state.proposeToCancelUrisArray.map(msgUri => (
            <WonCombinedMessageContent
              key={msgUri}
              messageUri={get(this.getReferencedMessage(msgUri), "uri")}
              connectionUri={get(this.state.connection, "uri")}
              ngRedux={this.props.ngRedux}
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
              {this.state.proposeToCancelUrisSize +
                (this.state.proposeToCancelUrisSize == 1
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
    if (this.state.hasForwardUris) {
      let fragmentBody;

      if (this.isReferenceExpanded("forwards")) {
        const messageElements =
          this.state.forwardUrisArray &&
          this.state.forwardUrisArray.map(msgUri => (
            <WonCombinedMessageContent
              key={msgUri}
              messageUri={get(this.getReferencedMessage(msgUri), "uri")}
              connectionUri={get(this.state.connection, "uri")}
              ngRedux={this.props.ngRedux}
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
              {this.state.forwardUrisSize +
                (this.state.forwardUrisSize == 1 ? " Message" : " Messages")}
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
          this.state.hasContent || this.state.hasNotBeenLoaded
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
    const ownedAtomUri = this.state.ownedAtomUri;
    return ownerApi.getMessage(ownedAtomUri, eventUri).then(response => {
      won.wonMessageFromJsonLd(response).then(msg => {
        if (msg.isFromOwner() && msg.getRecipientAtom() === ownedAtomUri) {
          /*if we find out that the recipientatom of the crawled event is actually our
           atom we will call the method again but this time with the correct eventUri
           */
          this.addMessageToState(msg.getRemoteMessageUri());
        } else {
          //If message isnt in the state we add it
          this.props.ngRedux.dispatch(
            actionCreators.messages__processAgreementMessage(msg)
          );
        }
      });
    });
  }
  getReferencedMessage(messageUri) {
    let referencedMessage = get(this.state.chatMessages, messageUri);
    if (referencedMessage) {
      return referencedMessage;
    } else {
      return (
        this.state.chatMessages &&
        this.state.chatMessages.find(
          msg => get(msg, "remoteUri") === messageUri
        )
      );
    }
  }

  toggleReferenceExpansion(reference) {
    const currentExpansionState = get(this.state.expandedReferences, reference);

    if (this.state.message && !this.state.multiSelectType) {
      this.props.ngRedux.dispatch(
        actionCreators.messages__viewState__markExpandReference({
          messageUri: this.messageUri,
          connectionUri: this.connectionUri,
          atomUri: this.state.ownedAtomUri,
          isExpanded: !currentExpansionState,
          reference: reference,
        })
      );
    }
  }

  isReferenceExpanded(reference) {
    return get(this.state.expandedReferences, reference);
  }
}

WonReferencedMessageContent.propTypes = {
  messageUri: PropTypes.string.isRequired,
  connectionUri: PropTypes.string.isRequired,
  ngRedux: PropTypes.object.isRequired,
};
