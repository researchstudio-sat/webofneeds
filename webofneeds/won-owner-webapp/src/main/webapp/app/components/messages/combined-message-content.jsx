/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { actionCreators } from "../../actions/actions.js";
import { connect } from "react-redux";

import PropTypes from "prop-types";
import {
  getAtoms,
  getOwnedAtomByConnectionUri,
} from "../../redux/selectors/general-selectors.js";
import { get, getIn } from "../../utils.js";
import { getOwnedConnections } from "../../redux/selectors/connection-selectors.js";
import { labels } from "../../won-label-utils.js";
import vocab from "../../service/vocab.js";
import WonTrig from "../trig.jsx";
import WonMessageContent from "./message-content.jsx";
import WonAtomIcon from "../atom-icon.jsx";
import WonReferencedMessageContent from "./referenced-message-content.jsx";

import "~/style/_combined-message-content.scss";
import * as viewSelectors from "../../redux/selectors/view-selectors";

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
    contentGraphTrig: get(message, "contentGraphTrigRaw"),
    shouldShowRdf: viewSelectors.showRdf(state),
    hasContent: message && message.get("hasContent"),
    hasNotBeenLoaded: !message,
    hasReferences,
    hasClaims: referencesClaims && referencesClaims.size > 0,
    hasProposes: referencesProposes && referencesProposes.size > 0,
    messageStatus: message && message.get("messageStatus"),
    isInjectIntoMessage: injectInto && injectInto.size > 0, //contains the targetConnectionUris
    originatorUri: message && message.get("originatorUri"),
    injectIntoArray: injectInto && Array.from(injectInto.toSet()),
    messageType,
    isConnectionMessage: messageType === vocab.WONMSG.connectionMessage,
  };
};

const mapDispatchToProps = dispatch => {
  return {
    routerGoCurrent: props => {
      dispatch(actionCreators.router__stateGoCurrent(props));
    },
  };
};

class WonCombinedMessageContent extends React.Component {
  render() {
    const trigElement =
      this.props.shouldShowRdf && this.props.contentGraphTrig ? (
        <WonTrig trig={this.props.contentGraphTrig} />
      ) : (
        undefined
      );

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
                        this.props.routerGoCurrent({
                          connectionUri: connUri,
                        });
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
        {trigElement}
      </won-combined-message-content>
    );
  }

  getAgreementHeaderLabel() {
    if (this.props.hasClaims && this.props.hasProposes) {
      if (this.props.messageStatus) {
        if (this.props.messageStatus.get("isCancelled"))
          return "Agreement/Claim - Cancelled";
        if (this.props.messageStatus.get("isCancellationPending"))
          return "Agreement/Claim - Accepted(Pending Cancellation)";
        if (this.props.messageStatus.get("isAccepted"))
          return "Agreement/Claim - Accepted";
        if (this.props.messageStatus.get("isRetracted"))
          return "Proposal/Claim - Retracted";
        if (this.props.messageStatus.get("isRejected"))
          return "Proposal/Claim - Rejected";
      }
      return "Proposal/Claim";
    } else if (this.props.hasClaims) {
      if (this.props.messageStatus) {
        if (this.props.messageStatus.get("isCancelled"))
          return "Claim - Cancelled";
        if (this.props.messageStatus.get("isCancellationPending"))
          return "Claim - Accepted(Pending Cancellation)";
        if (this.props.messageStatus.get("isAccepted"))
          return "Claim - Accepted";
        if (this.props.messageStatus.get("isRetracted"))
          return "Claim - Retracted";
        if (this.props.messageStatus.get("isRejected"))
          return "Claim - Rejected";
      }
      return "Claim";
    } else if (this.props.hasProposes) {
      if (this.props.messageStatus) {
        if (this.props.messageStatus.get("isCancelled"))
          return "Agreement - Cancelled";
        if (this.props.messageStatus.get("isCancellationPending"))
          return "Agreement - Pending Cancellation";
        if (this.props.messageStatus.get("isAccepted")) return "Agreement";
        if (this.props.messageStatus.get("isRetracted"))
          return "Proposal - Retracted";
        if (this.props.messageStatus.get("isRejected"))
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
  contentGraphTrig: PropTypes.string,
  shouldShowRdf: PropTypes.bool,
  hasContent: PropTypes.bool,
  hasNotBeenLoaded: PropTypes.bool,
  hasReferences: PropTypes.bool,
  hasClaims: PropTypes.bool,
  hasProposes: PropTypes.bool,
  messageStatus: PropTypes.object,
  isInjectIntoMessage: PropTypes.bool,
  originatorUri: PropTypes.string,
  injectIntoArray: PropTypes.arrayOf(PropTypes.string),
  messageType: PropTypes.string,
  isConnectionMessage: PropTypes.bool,
  routerGoCurrent: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonCombinedMessageContent);
