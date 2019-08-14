/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { actionCreators } from "../../actions/actions.js";

import PropTypes from "prop-types";
import {
  getAtoms,
  getOwnedAtomByConnectionUri,
} from "../../redux/selectors/general-selectors.js";
import { get, getIn } from "../../utils.js";
import { getOwnedConnections } from "../../redux/selectors/connection-selectors.js";
import { labels } from "../../won-label-utils.js";
import won from "../../won-es6";
import WonTrig from "../trig.jsx";
import WonMessageContent from "./message-content.jsx";
import WonAtomIcon from "../atom-icon.jsx";
import WonReferencedMessageContent from "./referenced-message-content.jsx";

import "~/style/_combined-message-content.scss";

export default class WonCombinedMessageContent extends React.Component {
  componentDidMount() {
    this.messageUri = this.props.messageUri;
    this.connectionUri = this.props.connectionUri;
    this.groupChatMessage = this.props.groupChatMessage;
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
    this.groupChatMessage = nextProps.groupChatMessage;
    this.setState(this.selectFromState(this.props.ngRedux.getState()));
  }

  selectFromState(state) {
    const ownedAtom =
      this.connectionUri &&
      getOwnedAtomByConnectionUri(state, this.connectionUri);
    const connection =
      ownedAtom && ownedAtom.getIn(["connections", this.connectionUri]);

    const message =
      connection &&
      this.messageUri &&
      getIn(connection, ["messages", this.messageUri]);

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
      (this.groupChatMessage
        ? get(message, "originatorUri")
        : get(connection, "targetAtomUri"));
    const relevantPersonaUri =
      relevantAtomUri && getIn(allAtoms, [relevantAtomUri, "heldBy"]);
    const personaName =
      relevantPersonaUri &&
      getIn(allAtoms, [relevantPersonaUri, "content", "personaName"]);

    return {
      allAtoms,
      allConnections,
      personaName,
      multiSelectType: connection && connection.get("multiSelectType"),
      contentGraphTrig: get(message, "contentGraphTrigRaw"),
      shouldShowRdf: state.getIn(["view", "showRdf"]),
      hasContent: message && message.get("hasContent"),
      hasNotBeenLoaded: !message,
      hasReferences,
      hasClaims: referencesClaims && referencesClaims.size > 0,
      hasProposes: referencesProposes && referencesProposes.size > 0,
      messageStatus: message && message.get("messageStatus"),
      isGroupChatMessage: this.groupChatMessage,
      isInjectIntoMessage: injectInto && injectInto.size > 0, //contains the targetConnectionUris
      originatorUri: message && message.get("originatorUri"),
      injectIntoArray: injectInto && Array.from(injectInto.toSet()),
      messageType,
      isConnectionMessage: messageType === won.WONMSG.connectionMessage,
    };
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div />;
    }

    const trigElement =
      this.state.shouldShowRdf && this.state.contentGraphTrig ? (
        <WonTrig trig={this.state.contentGraphTrig} />
      ) : (
        undefined
      );

    const messageContentElement =
      this.state.hasContent || this.state.hasNotBeenLoaded ? (
        <WonMessageContent
          messageUri={this.messageUri}
          connectionUri={this.connectionUri}
          ngRedux={this.props.ngRedux}
        />
      ) : (
        undefined
      );

    let messageHeaderElement;
    let messageHeaderOriginatorElement;
    let messageHeaderInjectInElement;
    if (!this.state.isConnectionMessage) {
      const headerLabel =
        labels.messageType[this.state.messageType] || this.state.messageType;

      messageHeaderElement = !this.state.hasNotBeenLoaded ? (
        <div className="msg__header">
          <div className="msg__header__type">{headerLabel}</div>
        </div>
      ) : (
        undefined
      );
    } else {
      if (!this.state.hasNotBeenLoaded) {
        if (this.state.hasClaims || this.state.hasProposes) {
          messageHeaderElement = (
            <div className="msg__header msg__header--agreement">
              <div className="msg__header__type">
                {this.getAgreementHeaderLabel()}
              </div>
            </div>
          );
        } else if (this.state.personaName) {
          messageHeaderElement = (
            <div className="msg__header">
              <div className="msg__header__type">{this.state.personaName}</div>
            </div>
          );
        }

        if (!this.state.isGroupChatMessage) {
          messageHeaderOriginatorElement = this.state.originatorUri ? (
            <div className="msg__header msg__header--forwarded-from">
              <div className="msg__header__type">Forwarded from:</div>
              <WonAtomIcon
                className="msg__header msg__header__originator"
                atomUri={this.state.originatorUri}
                ngRedux={this.props.ngRedux}
              />
            </div>
          ) : (
            undefined
          );

          if (this.state.isInjectIntoMessage) {
            const injectIntoIcons =
              this.state.injectIntoArray &&
              this.state.injectIntoArray.map(connUri => {
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
                      !this.state.multiSelectType &&
                        this.isInjectIntoConnectionPresent(connUri) &&
                        this.props.ngRedux.dispatch(
                          actionCreators.router__stateGoCurrent({
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

    const referencedMessageElements = this.state.hasReferences ? (
      <WonReferencedMessageContent
        messageUri={this.props.messageUri}
        connectionUri={this.props.connectionUri}
        ngRedux={this.props.ngRedux}
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
          (this.state.hasReferences ? " won-has-ref-content " : "") +
          (!this.state.isConnectionMessage ||
          this.state.hasContent ||
          this.state.hasNotBeenLoaded ||
          this.state.hasClaims ||
          this.state.hasProposes ||
          this.state.originatorUri
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
    if (this.state.hasClaims && this.state.hasProposes) {
      if (this.state.messageStatus) {
        if (this.state.messageStatus.get("isCancelled"))
          return "Agreement/Claim - Cancelled";
        if (this.state.messageStatus.get("isCancellationPending"))
          return "Agreement/Claim - Accepted(Pending Cancellation)";
        if (this.state.messageStatus.get("isAccepted"))
          return "Agreement/Claim - Accepted";
        if (this.state.messageStatus.get("isRetracted"))
          return "Proposal/Claim - Retracted";
        if (this.state.messageStatus.get("isRejected"))
          return "Proposal/Claim - Rejected";
      }
      return "Proposal/Claim";
    } else if (this.state.hasClaims) {
      if (this.state.messageStatus) {
        if (this.state.messageStatus.get("isCancelled"))
          return "Claim - Cancelled";
        if (this.state.messageStatus.get("isCancellationPending"))
          return "Claim - Accepted(Pending Cancellation)";
        if (this.state.messageStatus.get("isAccepted"))
          return "Claim - Accepted";
        if (this.state.messageStatus.get("isRetracted"))
          return "Claim - Retracted";
        if (this.state.messageStatus.get("isRejected"))
          return "Claim - Rejected";
      }
      return "Claim";
    } else if (this.state.hasProposes) {
      if (this.state.messageStatus) {
        if (this.state.messageStatus.get("isCancelled"))
          return "Agreement - Cancelled";
        if (this.state.messageStatus.get("isCancellationPending"))
          return "Agreement - Pending Cancellation";
        if (this.state.messageStatus.get("isAccepted")) return "Agreement";
        if (this.state.messageStatus.get("isRetracted"))
          return "Proposal - Retracted";
        if (this.state.messageStatus.get("isRejected"))
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
  ngRedux: PropTypes.object.isRequired,
  className: PropTypes.string,
  onClick: PropTypes.func,
};
