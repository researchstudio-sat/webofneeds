import React from "react";
import PropTypes from "prop-types";
import Immutable from "immutable";
import { useSelector, useDispatch } from "react-redux";
import { get, generateLink } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import ChatTextfield from "./chat-textfield.jsx";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import vocab from "../service/vocab.js";

import "~/style/_atom-footer.scss";
import { useHistory } from "react-router-dom";

const FooterType = {
  INACTIVE: 1,
  INACTIVE_OWNED: 2,
  ADHOC: 3,
  UNKNOWN: 4,
};

export default function WonAtomFooter({ atom, className }) {
  const dispatch = useDispatch();
  const history = useHistory();
  const atomUri = get(atom, "uri");

  const ownedAtoms = useSelector(generalSelectors.getOwnedAtoms);

  const accountState = useSelector(generalSelectors.getAccountState);

  const isOwned = useSelector(generalSelectors.isAtomOwned(atomUri));

  let footerType = FooterType.UNKNOWN;

  if (atomUtils.isInactive(atom)) {
    footerType = isOwned ? FooterType.INACTIVE_OWNED : FooterType.INACTIVE;
  } else {
    if (atomUtils.hasChatSocket(atom) || atomUtils.hasGroupSocket(atom)) {
      footerType = FooterType.ADHOC;
    }
  }

  const ownedChatSocketAtoms =
    ownedAtoms && ownedAtoms.filter(atom => atomUtils.hasChatSocket(atom));
  const loggedIn = accountUtils.isLoggedIn(accountState);

  function sendAdHocRequest(message, targetSocketUri, personaUri) {
    const _atomUri = atomUri;

    if (loggedIn) {
      if (_atomUri) {
        const personaAtom = get(ownedChatSocketAtoms, personaUri);

        if (personaAtom) {
          const targetSocketType =
            targetSocketUri === atomUtils.getChatSocket(atom)
              ? vocab.CHAT.ChatSocketCompacted
              : vocab.GROUP.GroupSocketCompacted;

          // if the personaAtom already contains a chatSocket we will just use the persona as the Atom that connects
          const personaConnections = get(personaAtom, "connections")
            .filter(conn => get(conn, "targetAtomUri") === _atomUri)
            .filter(conn => get(conn, "targetSocketUri") === targetSocketUri)
            .filter(
              conn =>
                get(conn, "socketUri") === atomUtils.getChatSocket(personaAtom)
            );

          if (personaConnections.size == 0) {
            dispatch(
              actionCreators.atoms__connect(
                personaUri,
                _atomUri,
                vocab.CHAT.ChatSocketCompacted,
                targetSocketType,
                message
              )
            );
            history.push("/connections");
          } else if (personaConnections.size == 1) {
            const personaConnection = personaConnections.first();
            const personaConnectionUri = get(personaConnection, "uri");

            const senderSocketUri = get(personaConnection, "socketUri");
            const targetSocketUri = get(personaConnection, "targetSocketUri");

            if (
              connectionUtils.isSuggested(personaConnection) ||
              connectionUtils.isClosed(personaConnection)
            ) {
              dispatch(
                actionCreators.atoms__connectSockets(
                  senderSocketUri,
                  targetSocketUri,
                  message
                )
              );
            } else if (connectionUtils.isRequestSent(personaConnection)) {
              // Just go to the connection without sending another request
              /*
              //Send another Request with a new message if there is a message present
              dispatch(
                actionCreators.atoms__connect(
                  personaUri,
                  _atomUri,
                  vocab.CHAT.ChatSocketCompacted,
                  targetSocketType,
                  message
                )
              );
              */
            } else if (connectionUtils.isRequestReceived(personaConnection)) {
              dispatch(
                actionCreators.atoms__connectSockets(
                  senderSocketUri,
                  targetSocketUri,
                  message
                )
              );
            } else if (connectionUtils.isConnected(personaConnection)) {
              if (message) {
                dispatch(
                  actionCreators.connections__sendChatMessage(
                    message,
                    undefined,
                    undefined,
                    senderSocketUri,
                    targetSocketUri,
                    personaConnectionUri,
                    false
                  )
                );
              } /*else {
                // Just go to the conneciton without sending an empty message
              }
              */
            }

            history.push(
              generateLink(
                history.location,
                {
                  connectionUri: personaConnectionUri,
                },
                "/connections"
              )
            );
          } else {
            console.error(
              "more than one connection stored between two atoms that use the same exact sockets",
              personaAtom,
              targetSocketUri
            );
          }
        } else {
          history.push("/connections");

          dispatch(
            actionCreators.connections__connectAdHoc(targetSocketUri, message)
          );
        }
      }
    } else {
      dispatch(
        actionCreators.view__showTermsDialog(
          Immutable.fromJS({
            acceptCallback: () => {
              dispatch(actionCreators.view__hideModalDialog());
              history.push("/connections");

              dispatch(
                actionCreators.connections__connectAdHoc(
                  targetSocketUri,
                  message
                )
              );
            },
            cancelCallback: () => {
              dispatch(actionCreators.view__hideModalDialog());
            },
          })
        )
      );
    }
  }

  let footerElement;

  switch (footerType) {
    case FooterType.INACTIVE:
      footerElement = (
        <div className="atom-footer__infolabel">
          Atom is inactive, no requests allowed
        </div>
      );
      break;
    case FooterType.INACTIVE_OWNED:
      footerElement = (
        <React.Fragment>
          <div className="atom-footer__infolabel">
            This Atom is inactive. Others will not be able to interact with it.
          </div>
          <button
            className="won-publish-button red won-button--filled"
            onClick={() => dispatch(actionCreators.atoms__reopen(atomUri))}
          >
            Reopen
          </button>
        </React.Fragment>
      );
      break;
    case FooterType.ADHOC:
      footerElement = (
        <React.Fragment>
          {!!atomUtils.getChatSocket(atom) && (
            <ChatTextfield
              placeholder="Message (optional)"
              allowEmptySubmit={true}
              showPersonas={true}
              submitButtonLabel="Ask&#160;to&#160;Chat"
              onSubmit={({ value, selectedPersona }) =>
                sendAdHocRequest(
                  value,
                  atomUtils.getChatSocket(atom),
                  selectedPersona && selectedPersona.personaId
                )
              }
            />
          )}
          {!!atomUtils.getGroupSocket(atom) && (
            <ChatTextfield
              placeholder="Message (optional)"
              allowEmptySubmit={true}
              showPersonas={true}
              submitButtonLabel="Join&#160;Group"
              onSubmit={({ value, selectedPersona }) =>
                sendAdHocRequest(
                  value,
                  atomUtils.getGroupSocket(atom),
                  selectedPersona && selectedPersona.personaId
                )
              }
            />
          )}
        </React.Fragment>
      );
      break;
    case FooterType.UNKNOWN:
    default:
      footerElement = undefined;
      break;
  }

  return (
    <won-atom-footer class={className ? className : ""}>
      {footerElement}
    </won-atom-footer>
  );
}
WonAtomFooter.propTypes = {
  atom: PropTypes.object,
  className: PropTypes.string,
};
