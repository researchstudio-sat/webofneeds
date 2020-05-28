/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { actionCreators } from "../../actions/actions.js";
import { useDispatch } from "react-redux";

import PropTypes from "prop-types";
import { get, getIn } from "../../utils.js";
import * as ownerApi from "../../api/owner-api";
import won from "../../won-es6";
import WonCombinedMessageContent from "./combined-message-content.jsx";

import "~/style/_referenced-message-content.scss";
import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";

export default function WonReferencedMessageContent({
  message,
  connection,
  senderAtom,
  allAtoms,
  ownedConnections,
  originatorAtom,
}) {
  const dispatch = useDispatch();
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
  const senderAtomUri = get(senderAtom, "uri");
  const multiSelectType = get(connection, "multiSelectType");

  function toggleReferenceExpansion(reference) {
    if (message && !multiSelectType) {
      const currentExpansionState = get(expandedReferences, reference);

      dispatch(
        actionCreators.messages__viewState__markExpandReference({
          messageUri: get(message, "uri"),
          connectionUri: get(connection, "uri"),
          atomUri: senderAtomUri,
          isExpanded: !currentExpansionState,
          reference: reference,
        })
      );
    }
  }

  function generateCombinedMessageElement(msgUri, className) {
    const referencedMessage = get(chatMessages, msgUri);

    let messageClass = className;

    if (!className && referencedMessage) {
      if (get(referencedMessage, "outgoingMessage")) {
        messageClass = "won-cm--right";
      } else {
        messageClass = "won-cm--left";
      }
    }

    let onClick;
    if (!referencedMessage) {
      onClick = () =>
        ownerApi.getMessage(senderAtomUri, msgUri).then(response => {
          won.wonMessageFromJsonLd(response, msgUri).then(msg => {
            //If message isnt in the state we add it
            dispatch(actionCreators.messages__processAgreementMessage(msg));
          });
        });
    }

    return (
      <WonCombinedMessageContent
        key={msgUri}
        message={referencedMessage}
        connection={connection}
        senderAtom={senderAtom}
        originatorAtom={originatorAtom}
        allAtoms={allAtoms}
        ownedConnections={ownedConnections}
        className={messageClass}
        onClick={onClick}
      />
    );
  }

  function generateMessageElementFragment(
    label,
    reference,
    messageReferenceUris,
    combinedMessageElementClassName
  ) {
    if (messageReferenceUris && messageReferenceUris.size > 0) {
      const messageReferenceArray = Array.from(messageReferenceUris.toSet());
      const isExpanded = get(expandedReferences, reference);
      const messagesArraySize = messageReferenceArray.length;

      return (
        <div className="refmsgcontent__fragment">
          <div
            className={
              "refmsgcontent__fragment__header " +
              (!isExpanded ? "refmsgcontent__fragment__header--collapsed" : "")
            }
            onClick={() => toggleReferenceExpansion(reference)}
          >
            <div className="refmsgcontent__fragment__header__label">
              {label +
                " " +
                messagesArraySize +
                (messagesArraySize == 1 ? " Message" : " Messages")}
            </div>
            <div
              className={
                "refmsgcontent__fragment__header__carret " +
                (isExpanded
                  ? " refmsgcontent__fragment__header__carret--expanded "
                  : " refmsgcontent__fragment__header__carret--collapsed ")
              }
            >
              <svg>
                <use xlinkHref={ico16_arrow_down} href={ico16_arrow_down} />
              </svg>
            </div>
          </div>
          {isExpanded ? (
            <div className="refmsgcontent__fragment__body">
              {messageReferenceArray.map(msgUri =>
                generateCombinedMessageElement(
                  msgUri,
                  combinedMessageElementClassName
                    ? "won-cm--forward"
                    : undefined
                )
              )}
            </div>
          ) : (
            undefined
          )}
        </div>
      );
    }
    return undefined;
  }

  return (
    <won-referenced-message-content
      class={
        !message || get(message, "hasContent") ? "won-has-non-ref-content" : ""
      }
    >
      {generateMessageElementFragment("Claiming", "claims", claimUris)}
      {generateMessageElementFragment("Proposes", "proposes", proposeUris)}
      {generateMessageElementFragment("Retracting", "retracts", retractUris)}
      {generateMessageElementFragment("Accepts", "accepts", acceptUris)}
      {generateMessageElementFragment(
        "Proposing to cancel",
        "proposesToCancel",
        proposeToCancelUris
      )}
      {generateMessageElementFragment("Rejecting", "rejects", rejectUris)}
      {generateMessageElementFragment(
        "Forwarding",
        "forwards",
        forwardUris,
        "won-cm--forward"
      )}
    </won-referenced-message-content>
  );
}

WonReferencedMessageContent.propTypes = {
  message: PropTypes.object.isRequired,
  connection: PropTypes.object.isRequired,
  senderAtom: PropTypes.object.isRequired,
  allAtoms: PropTypes.object.isRequired,
  ownedConnections: PropTypes.object.isRequired,
  originatorAtom: PropTypes.object,
};
