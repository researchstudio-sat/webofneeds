/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";

import PropTypes from "prop-types";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import * as messageUtils from "../../redux/utils/message-utils.js";
import * as wonLabelUtils from "../../won-label-utils.js";
import { get, getIn, generateLink } from "../../utils.js";
import vocab from "../../service/vocab.js";
import WonMessageContent from "./message-content.jsx";
import WonAtomIcon from "../atom-icon.jsx";
import WonReferencedMessageContent from "./referenced-message-content.jsx";

import "~/style/_combined-message-content.scss";
import { useHistory } from "react-router-dom";
import { useSelector } from "react-redux";

export default function WonCombinedMessageContent({
  message,
  connection,
  allAtoms,
  ownedConnections,
  senderAtom,
  originatorAtom,
  groupChatMessage,
  className,
  onClick,
}) {
  const history = useHistory();
  const messageType = get(message, "messageType");
  const injectInto = get(message, "injectInto");

  const hasReferences = get(message, "hasReferences");
  const referencesProposes = messageUtils.getProposesReferences(message);
  const referencesClaims = messageUtils.getClaimsReferences(message);
  const externalDataState = useSelector(generalSelectors.getExternalDataState);

  const originatorUri = get(message, "originatorUri");
  /*Extract persona name from message:

   either within the atom of the originatorUri-atom (in group-chat-messages)
   or
   within the targetAtomUri-atom of the connection (for 1:1 chats)
   */
  let personaName = undefined;

  if (!get(message, "outgoingMessage")) {
    const relevantAtomUri = groupChatMessage
      ? originatorUri
      : get(connection, "targetAtomUri");
    const relevantAtom = get(allAtoms, relevantAtomUri);
    const relevantPersona =
      atomUtils.isPersona(relevantAtom) || atomUtils.isServiceAtom(relevantAtom)
        ? relevantAtom
        : get(allAtoms, atomUtils.getHeldByUri(relevantAtom));

    personaName = relevantPersona
      ? atomUtils.getTitle(relevantPersona, externalDataState)
      : get(relevantAtom, "fakePersonaName");
  }

  const multiSelectType = get(connection, "multiSelectType");
  const hasContent = get(message, "hasContent");
  const hasNotBeenLoaded = !message;
  const hasClaims = referencesClaims && referencesClaims.size > 0;
  const hasProposes = referencesProposes && referencesProposes.size > 0;
  const agreementData = get(connection, "agreementData");
  const isInjectIntoMessage = injectInto && injectInto.size > 0; //contains the targetConnectionUris

  const injectIntoArray = injectInto && Array.from(injectInto.toSet());
  const isConnectionMessage = messageType === vocab.WONMSG.connectionMessage;

  function getAgreementHeaderLabel() {
    const messageUri = get(message, "uri");
    //TODO: integrate agreed message cases
    if (hasClaims && hasProposes) {
      if (agreementData) {
        if (getIn(agreementData, ["cancelledAgreementUris", messageUri]))
          return "Agreement/Claim - Cancelled";
        if (
          getIn(agreementData, ["pendingCancellationProposalUris", messageUri])
        )
          return "Agreement/Claim - Accepted(Pending Cancellation)";
        if (getIn(agreementData, ["agreementUris", messageUri]))
          return "Agreement/Claim - Accepted";
        if (getIn(agreementData, ["retractedMessageUris", messageUri]))
          return "Proposal/Claim - Retracted";
        if (getIn(agreementData, ["rejectedMessageUris", messageUri]))
          return "Proposal/Claim - Rejected";
      }
      return "Proposal/Claim";
    } else if (hasClaims) {
      if (agreementData) {
        if (getIn(agreementData, ["cancelledAgreementUris", messageUri]))
          return "Claim - Cancelled";
        if (
          getIn(agreementData, ["pendingCancellationProposalUris", messageUri])
        )
          return "Claim - Accepted(Pending Cancellation)";
        if (getIn(agreementData, ["agreementUris", messageUri]))
          return "Claim - Accepted";
        if (getIn(agreementData, ["retractedMessageUris", messageUri]))
          return "Claim - Retracted";
        if (getIn(agreementData, ["rejectedMessageUris", messageUri]))
          return "Claim - Retracted";
      }
      return "Claim";
    } else if (hasProposes) {
      if (agreementData) {
        if (getIn(agreementData, ["cancelledAgreementUris", messageUri]))
          return "Agreement - Cancelled";
        if (
          getIn(agreementData, ["pendingCancellationProposalUris", messageUri])
        )
          return "Agreement - Pending Cancellation";
        if (getIn(agreementData, ["agreementUris", messageUri]))
          return "Agreement";
        if (getIn(agreementData, ["retractedMessageUris", messageUri]))
          return "Proposal - Retracted";
        if (getIn(agreementData, ["rejectedMessageUris", messageUri]))
          return "Proposal - Rejected";
      }
      return "Proposal";
    }
    //should never happen
    return undefined;
  }

  function isInjectIntoConnectionPresent(connectionUri) {
    //TODO: THIS MIGHT BE A CONNECTION THAT WE DONT EVEN OWN, SO WE NEED TO BE MORE SMART ABOUT IT
    let connection = get(ownedConnections, connectionUri);

    if (connection) {
      return true;
    } else {
      return (
        ownedConnections &&
        !!ownedConnections.find(
          conn => get(conn, "targetConnectionUri") === connectionUri
        )
      );
    }
  }

  function getInjectIntoAtom(connectionUri) {
    //TODO: THIS MIGHT BE A CONNECTION THAT WE DONT EVEN OWN, SO WE NEED TO BE MORE SMART ABOUT IT
    let connection = get(ownedConnections, connectionUri);

    if (connection) {
      return get(allAtoms, get(connection, "targetAtomUri"));
    } else {
      connection =
        ownedConnections &&
        ownedConnections.find(
          conn => get(conn, "targetConnectionUri") === connectionUri
        );

      if (connection) {
        return get(allAtoms, get(connection, "targetAtomUri"));
      }
    }

    return undefined;
  }

  const messageContentElement =
    hasContent || hasNotBeenLoaded ? (
      <WonMessageContent message={message} />
    ) : (
      undefined
    );

  let messageHeaderElement;
  let messageHeaderOriginatorElement;
  let messageHeaderInjectInElement;
  if (!isConnectionMessage) {
    messageHeaderElement = !hasNotBeenLoaded ? (
      <div className="msg__header">
        <div className="msg__header__type">
          {wonLabelUtils.getMessageTypeLabel(messageType)}
        </div>
      </div>
    ) : (
      undefined
    );
  } else {
    if (!hasNotBeenLoaded) {
      if (hasClaims || hasProposes) {
        messageHeaderElement = (
          <div className="msg__header msg__header--agreement">
            <div className="msg__header__type">{getAgreementHeaderLabel()}</div>
          </div>
        );
      } else if (personaName && groupChatMessage) {
        messageHeaderElement = (
          <div className="msg__header">
            <div className="msg__header__type">{personaName}</div>
          </div>
        );
      }

      if (!groupChatMessage) {
        messageHeaderOriginatorElement = originatorAtom ? (
          <div className="msg__header msg__header--forwarded-from">
            <div className="msg__header__type">Forwarded from:</div>
            <WonAtomIcon
              className="msg__header msg__header__originator"
              atom={originatorAtom}
            />
          </div>
        ) : (
          undefined
        );

        if (isInjectIntoMessage) {
          const injectIntoIcons =
            injectIntoArray &&
            injectIntoArray.map(connUri => {
              return (
                <WonAtomIcon
                  key={connUri}
                  className={
                    "msg__header__inject " +
                    (isInjectIntoConnectionPresent(connUri) ? "clickable" : "")
                  }
                  atom={getInjectIntoAtom(connUri)}
                  onClick={() => {
                    !multiSelectType &&
                      isInjectIntoConnectionPresent(connUri) &&
                      history.push(
                        generateLink(history.location, {
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

  const referencedMessageElements = hasReferences ? (
    <WonReferencedMessageContent
      message={message}
      connection={connection}
      allAtoms={allAtoms}
      senderAtom={senderAtom}
      originatorAtom={originatorAtom}
      ownedConnections={ownedConnections}
    />
  ) : (
    undefined
  );

  return (
    <won-combined-message-content
      class={
        (className ? className : "") +
        " " +
        (onClick ? " clickable " : "") +
        (hasReferences ? " won-has-ref-content " : "") +
        (!isConnectionMessage ||
        hasContent ||
        hasNotBeenLoaded ||
        hasClaims ||
        hasProposes ||
        originatorUri
          ? " won-has-non-ref-content "
          : "")
      }
      onClick={onClick}
    >
      {messageHeaderElement}
      {messageHeaderOriginatorElement}
      {messageHeaderInjectInElement}
      {messageContentElement}
      {referencedMessageElements}
    </won-combined-message-content>
  );
}
WonCombinedMessageContent.propTypes = {
  message: PropTypes.object,
  connection: PropTypes.object.isRequired,
  originatorAtom: PropTypes.object,
  senderAtom: PropTypes.object,
  allAtoms: PropTypes.object.isRequired,
  ownedConnections: PropTypes.object.isRequired,
  groupChatMessage: PropTypes.bool,
  className: PropTypes.string,
  onClick: PropTypes.func,
};
