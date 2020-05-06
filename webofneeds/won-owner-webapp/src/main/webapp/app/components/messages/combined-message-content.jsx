/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { connect } from "react-redux";

import PropTypes from "prop-types";
import {
  getAtoms,
  getOwnedAtomByConnectionUri,
} from "../../redux/selectors/general-selectors.js";
import { get, getIn, generateLink } from "../../utils.js";
import { getOwnedConnections } from "../../redux/selectors/connection-selectors.js";
import { labels } from "../../won-label-utils.js";
import vocab from "../../service/vocab.js";
import WonMessageContent from "./message-content.jsx";
import WonAtomIcon from "../atom-icon.jsx";
import WonReferencedMessageContent from "./referenced-message-content.jsx";

import "~/style/_combined-message-content.scss";
import { withRouter } from "react-router-dom";

const mapStateToProps = (state, ownProps) => {
  const ownedAtom =
    ownProps.connectionUri &&
    getOwnedAtomByConnectionUri(state, ownProps.connectionUri);
  const connection =
    ownedAtom && ownedAtom.getIn(["connections", ownProps.connectionUri]);

  const message =
    connection &&
    ownProps.messageUri &&
    getIn(connection, ["messages", ownProps.messageUri]);

  const messageType = message && message.get("messageType");
  const injectInto = message && message.get("injectInto");

  const hasReferences = message && message.get("hasReferences");
  const references = message && message.get("references");
  const referencesProposes = references && references.get("proposes");
  const referencesProposesToCancel =
    references && references.get("proposesToCancel");
  const referencesClaims = references && references.get("claims");

  const allConnections = getOwnedConnections(state);
  const allAtoms = getAtoms(state);

  /*Extract persona name from message:

   either within the atom of the originatorUri-atom (in group-chat-messages)
   or
   within the targetAtomUri-atom of the connection (for 1:1 chats)
   */
  const relevantAtomUri =
    !get(message, "outgoingMessage") &&
    (ownProps.groupChatMessage
      ? get(message, "originatorUri")
      : get(connection, "targetAtomUri"));
  const relevantPersonaUri =
    relevantAtomUri && getIn(allAtoms, [relevantAtomUri, "heldBy"]);
  const personaName = relevantPersonaUri
    ? getIn(allAtoms, [relevantPersonaUri, "content", "personaName"])
    : undefined;

  return {
    messageUri: ownProps.messageUri,
    connectionUri: ownProps.connectionUri,
    groupChatMessage: ownProps.groupChatMessage,
    className: ownProps.className,
    onClick: ownProps.onClick,
    allAtoms,
    allConnections,
    personaName,
    multiSelectType: connection && connection.get("multiSelectType"),
    hasContent: message && message.get("hasContent"),
    hasNotBeenLoaded: !message,
    hasReferences,
    hasClaims: referencesClaims && referencesClaims.size > 0,
    hasProposes: referencesProposes && referencesProposes.size > 0,
    hasProposesToCancel:
      referencesProposesToCancel && referencesProposesToCancel.size > 0,
    agreementData: connection && connection.get("agreementData"),
    isInjectIntoMessage: injectInto && injectInto.size > 0, //contains the targetConnectionUris
    originatorUri: message && message.get("originatorUri"),
    injectIntoArray: injectInto && Array.from(injectInto.toSet()),
    messageType,
    isConnectionMessage: messageType === vocab.WONMSG.connectionMessage,
  };
};

class WonCombinedMessageContent extends React.Component {
  render() {
    const messageContentElement =
      this.props.hasContent || this.props.hasNotBeenLoaded ? (
        <WonMessageContent
          messageUri={this.props.messageUri}
          connectionUri={this.props.connectionUri}
        />
      ) : (
        undefined
      );

    let messageHeaderElement;
    let messageHeaderOriginatorElement;
    let messageHeaderInjectInElement;
    if (!this.props.isConnectionMessage) {
      const headerLabel =
        labels.messageType[this.props.messageType] || this.props.messageType;

      messageHeaderElement = !this.props.hasNotBeenLoaded ? (
        <div className="msg__header">
          <div className="msg__header__type">{headerLabel}</div>
        </div>
      ) : (
        undefined
      );
    } else {
      if (!this.props.hasNotBeenLoaded) {
        if (this.props.hasClaims || this.props.hasProposes) {
          messageHeaderElement = (
            <div className="msg__header msg__header--agreement">
              <div className="msg__header__type">
                {this.getAgreementHeaderLabel()}
              </div>
            </div>
          );
        } else if (this.props.personaName) {
          messageHeaderElement = (
            <div className="msg__header">
              <div className="msg__header__type">{this.props.personaName}</div>
            </div>
          );
        }

        if (!this.props.groupChatMessage) {
          messageHeaderOriginatorElement = this.props.originatorUri ? (
            <div className="msg__header msg__header--forwarded-from">
              <div className="msg__header__type">Forwarded from:</div>
              <WonAtomIcon
                className="msg__header msg__header__originator"
                atomUri={this.props.originatorUri}
              />
            </div>
          ) : (
            undefined
          );

          if (this.props.isInjectIntoMessage) {
            const injectIntoIcons =
              this.props.injectIntoArray &&
              this.props.injectIntoArray.map(connUri => {
                return (
                  <WonAtomIcon
                    key={connUri}
                    className={
                      "msg__header__inject " +
                      (this.isInjectIntoConnectionPresent(connUri)
                        ? "clickable"
                        : "")
                    }
                    atomUri={this.getInjectIntoAtomUri(connUri)}
                    onClick={() => {
                      !this.props.multiSelectType &&
                        this.isInjectIntoConnectionPresent(connUri) &&
                        this.props.history.push(
                          generateLink(this.props.history.location, {
                            connectionUri: connUri,
                          })
                        );
                    }}
                  />
                );
              });

            messageHeaderInjectInElement = (
              <div className="msg__header msg__header--inject-into">
                <div className="msg__header__type">Forward to:</div>
                {injectIntoIcons}
              </div>
            );
          }
        }
      }
    }

    const referencedMessageElements = this.props.hasReferences ? (
      <WonReferencedMessageContent
        messageUri={this.props.messageUri}
        connectionUri={this.props.connectionUri}
      />
    ) : (
      undefined
    );

    return (
      <won-combined-message-content
        class={
          (this.props.className ? this.props.className : "") +
          " " +
          (this.props.onClick ? " clickable " : "") +
          (this.props.hasReferences ? " won-has-ref-content " : "") +
          (!this.props.isConnectionMessage ||
          this.props.hasContent ||
          this.props.hasNotBeenLoaded ||
          this.props.hasClaims ||
          this.props.hasProposes ||
          this.props.originatorUri
            ? " won-has-non-ref-content "
            : "")
        }
        onClick={this.props.onClick}
      >
        {messageHeaderElement}
        {messageHeaderOriginatorElement}
        {messageHeaderInjectInElement}
        {messageContentElement}
        {referencedMessageElements}
      </won-combined-message-content>
    );
  }

  getAgreementHeaderLabel() {
    //TODO: integrate agreed message cases
    if (this.props.hasClaims && this.props.hasProposes) {
      if (this.props.agreementData) {
        if (
          this.props.agreementData.getIn([
            "cancelledAgreementUris",
            this.props.messageUri,
          ])
        )
          return "Agreement/Claim - Cancelled";
        if (
          this.props.agreementData.getIn([
            "pendingCancellationProposalUris",
            this.props.messageUri,
          ])
        )
          return "Agreement/Claim - Accepted(Pending Cancellation)";
        if (
          this.props.agreementData.getIn([
            "agreementUris",
            this.props.messageUri,
          ])
        )
          return "Agreement/Claim - Accepted";
        if (
          this.props.agreementData.getIn([
            "retractedMessageUris",
            this.props.messageUri,
          ])
        )
          return "Proposal/Claim - Retracted";
        if (
          this.props.agreementData.getIn([
            "rejectedMessageUris",
            this.props.messageUri,
          ])
        )
          return "Proposal/Claim - Rejected";
      }
      return "Proposal/Claim";
    } else if (this.props.hasClaims) {
      if (this.props.agreementData) {
        if (
          this.props.agreementData.getIn([
            "cancelledAgreementUris",
            this.props.messageUri,
          ])
        )
          return "Claim - Cancelled";
        if (
          this.props.agreementData.getIn([
            "pendingCancellationProposalUris",
            this.props.messageUri,
          ])
        )
          return "Claim - Accepted(Pending Cancellation)";
        if (
          this.props.agreementData.getIn([
            "agreementUris",
            this.props.messageUri,
          ])
        )
          return "Claim - Accepted";
        if (
          this.props.agreementData.getIn([
            "retractedMessageUris",
            this.props.messageUri,
          ])
        )
          return "Claim - Retracted";
        if (
          this.props.agreementData.getIn([
            "rejectedMessageUris",
            this.props.messageUri,
          ])
        )
          return "Claim - Retracted";
      }
      return "Claim";
    } else if (this.props.hasProposes) {
      if (this.props.agreementData) {
        if (
          this.props.agreementData.getIn([
            "cancelledAgreementUris",
            this.props.messageUri,
          ])
        )
          return "Agreement - Cancelled";
        if (
          this.props.agreementData.getIn([
            "pendingCancellationProposalUris",
            this.props.messageUri,
          ])
        )
          return "Agreement - Pending Cancellation";
        if (
          this.props.agreementData.getIn([
            "agreementUris",
            this.props.messageUri,
          ])
        )
          return "Agreement";
        if (
          this.props.agreementData.getIn([
            "retractedMessageUris",
            this.props.messageUri,
          ])
        )
          return "Proposal - Retracted";
        if (
          this.props.agreementData.getIn([
            "rejectedMessageUris",
            this.props.messageUri,
          ])
        )
          return "Proposal - Rejected";
      }
      return "Proposal";
    }
    //should never happen
    return undefined;
  }

  isInjectIntoConnectionPresent(connectionUri) {
    //TODO: THIS MIGHT BE A CONNECTION THAT WE DONT EVEN OWN, SO WE NEED TO BE MORE SMART ABOUT IT
    let connection =
      this.allConnections && this.allConnections.get(connectionUri);

    if (connection) {
      return true;
    } else {
      return (
        this.allConnections &&
        !!this.allConnections.find(
          conn => conn.get("targetConnectionUri") === connectionUri
        )
      );
    }
  }

  getInjectIntoAtomUri(connectionUri) {
    //TODO: THIS MIGHT BE A CONNECTION THAT WE DONT EVEN OWN, SO WE NEED TO BE MORE SMART ABOUT IT
    let connection =
      this.allConnections && this.allConnections.get(connectionUri);

    if (connection) {
      return connection.get("targetAtomUri");
    } else {
      connection =
        this.allConnections &&
        this.allConnections.find(
          conn => conn.get("targetConnectionUri") === connectionUri
        );

      if (connection) {
        return connection.get("targetAtomUri");
      }
    }
    return undefined;
  }
}

WonCombinedMessageContent.propTypes = {
  messageUri: PropTypes.string.isRequired,
  connectionUri: PropTypes.string.isRequired,
  groupChatMessage: PropTypes.bool,
  className: PropTypes.string,
  onClick: PropTypes.func,
  allAtoms: PropTypes.object,
  allConnections: PropTypes.object,
  personaName: PropTypes.string,
  multiSelectType: PropTypes.string,
  hasContent: PropTypes.bool,
  hasNotBeenLoaded: PropTypes.bool,
  hasReferences: PropTypes.bool,
  hasClaims: PropTypes.bool,
  hasProposes: PropTypes.bool,
  agreementData: PropTypes.object,
  isInjectIntoMessage: PropTypes.bool,
  originatorUri: PropTypes.string,
  injectIntoArray: PropTypes.arrayOf(PropTypes.string),
  messageType: PropTypes.string,
  isConnectionMessage: PropTypes.bool,
  history: PropTypes.object,
};

export default withRouter(connect(mapStateToProps)(WonCombinedMessageContent));
