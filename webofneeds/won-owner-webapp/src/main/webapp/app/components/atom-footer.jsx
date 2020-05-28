import React from "react";
import PropTypes from "prop-types";
import Immutable from "immutable";
import { useSelector, useDispatch } from "react-redux";
import { get, getIn, generateLink } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import WonAtomHeader from "./atom-header.jsx";
import ChatTextfield from "./chat-textfield.jsx";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import * as useCaseUtils from "../usecase-utils.js";
import * as wonLabelUtils from "../won-label-utils.js";
import vocab from "../service/vocab.js";

import "~/style/_atom-footer.scss";
import { Link, useHistory } from "react-router-dom";

const FooterType = {
  INACTIVE: 1,
  INACTIVE_OWNED: 2,
  ENABLED: 3,
  REACTION: 4,
  ADHOC: 5,
  UNKNOWN: 6,
};

export default function WonAtomFooter({ atom, className }) {
  const dispatch = useDispatch();
  const history = useHistory();
  const atomUri = get(atom, "uri");

  const ownedAtoms = useSelector(generalSelectors.getOwnedAtoms);

  const accountState = useSelector(generalSelectors.getAccountState);

  const isOwned = useSelector(generalSelectors.isAtomOwned(atomUri));

  const reactionUseCases = atomUtils.getReactionUseCases(atom);
  const enabledUseCases = atomUtils.getEnabledUseCases(atom);

  const ownedReactionAtoms =
    ownedAtoms &&
    ownedAtoms.filter(
      atom =>
        atomUtils.matchesDefinitions(atom, reactionUseCases) ||
        atomUtils.matchesDefinitions(atom, enabledUseCases)
    );

  let footerType = FooterType.UNKNOWN;
  let useCasesArray = [];

  if (atomUtils.isInactive(atom)) {
    footerType = isOwned ? FooterType.INACTIVE_OWNED : FooterType.INACTIVE;
  } else {
    if (isOwned && atomUtils.hasEnabledUseCases(atom)) {
      useCasesArray = atomUtils.getEnabledUseCases(atom).toArray();
      footerType = FooterType.ENABLED;
    } else if (atomUtils.hasReactionUseCases(atom)) {
      useCasesArray = atomUtils.getReactionUseCases(atom).toArray();
      footerType = FooterType.REACTION;
    } else if (
      atomUtils.hasChatSocket(atom) ||
      atomUtils.hasGroupSocket(atom)
    ) {
      footerType = FooterType.ADHOC;
    }
  }

  const ownedChatSocketAtoms =
    ownedAtoms && ownedAtoms.filter(atom => atomUtils.hasChatSocket(atom));
  const loggedIn = accountUtils.isLoggedIn(accountState);
  const ownedReactionAtomsArray = ownedReactionAtoms
    ? ownedReactionAtoms.toArray()
    : [];
  const addHolderUri =
    atomUtils.hasEnabledUseCases(atom) && footerType === FooterType.ENABLED
      ? atomUtils.getHeldByUri(atom)
      : undefined;

  function getUseCaseTypeButton(useCase, index) {
    const ucIdentifier = get(useCase, "identifier");
    const ucSenderSocketType = get(useCase, "senderSocketType");
    const ucTargetSocketType = get(useCase, "targetSocketType");

    const atomElements = ownedReactionAtomsArray
      .filter(reactionAtom =>
        atomUtils.matchesDefinition(reactionAtom, useCase)
      )
      .filter(reactionAtom => {
        // FILTER OUT ALL THE ATOMS THAT ALREADY HAVE AN EXISTING CONNECTION WITH THIS ATOM
        const senderSocketUri = atomUtils.getSocketUri(
          reactionAtom,
          ucSenderSocketType
        );
        const targetSocketUri = atomUtils.getSocketUri(
          atom,
          ucTargetSocketType
        );

        const existingConnections = get(reactionAtom, "connections")
          .filter(conn => get(conn, "targetAtomUri") === atomUri)
          .filter(conn =>
            connectionUtils.hasSocketUris(
              conn,
              senderSocketUri,
              targetSocketUri
            )
          );

        return !existingConnections || existingConnections.size == 0;
      })
      .map((reactionAtom, index) => {
        return (
          <WonAtomHeader
            key={get(reactionAtom, "uri") + "-" + index}
            atom={reactionAtom}
            hideTimestamp={true}
            onClick={() =>
              connectAtomSockets(
                reactionAtom,
                atomUtils.getSocketUri(reactionAtom, ucSenderSocketType),
                atomUtils.getSocketUri(atom, ucTargetSocketType)
              )
            }
          />
        );
      });

    const type =
      footerType === FooterType.ENABLED
        ? "enabled"
        : footerType === FooterType.REACTION
          ? "reaction"
          : undefined;
    let headerText =
      getIn(wonLabelUtils.reactionLabels, [
        type,
        ucSenderSocketType,
        ucTargetSocketType,
      ]) || `Connect ${ucSenderSocketType} with ${ucTargetSocketType}`;

    return (
      <React.Fragment>
        <div className="atom-footer__header">{headerText}</div>
        <div className="atom-footer__matches">
          {!!atomElements &&
            atomElements.length > 0 && (
              <div className="atom-footer__matches__header">
                These existing Atoms you own would match
              </div>
            )}
          <div className="atom-footer__matches__list">
            {atomElements}
            <Link
              key={ucIdentifier + "-" + index}
              className="atom-footer__adhocbutton"
              to={selectUseCaseRoute(
                ucIdentifier,
                ucSenderSocketType,
                ucTargetSocketType
              )}
            >
              <div className="atom-footer__adhocbutton__icon">
                {useCaseUtils.getUseCaseIcon(ucIdentifier) ? (
                  <svg className="atom-footer__adhocbutton__icon__svg">
                    <use
                      xlinkHref={useCaseUtils.getUseCaseIcon(ucIdentifier)}
                      href={useCaseUtils.getUseCaseIcon(ucIdentifier)}
                    />
                  </svg>
                ) : (
                  <svg className="atom-footer__adhocbutton__icon__svg">
                    <use xlinkHref="ico36_plus" href="ico36_plus" />
                  </svg>
                )}
              </div>
              <div className="atom-footer__adhocbutton__right">
                <div className="atom-footer__adhocbutton__right__topline">
                  <div className="atom-footer__adhocbutton__right__topline__notitle">
                    Create Atom
                  </div>
                </div>
                <div className="atom-footer__adhocbutton__right__subtitle">
                  <span className="atom-footer__adhocbutton__right__subtitle__type">
                    <span>{useCaseUtils.getUseCaseLabel(ucIdentifier)}</span>
                  </span>
                </div>
              </div>
            </Link>
          </div>
        </div>
      </React.Fragment>
    );
  }

  function selectUseCaseRoute(
    ucIdentifier,
    ucSenderSocketType,
    ucTargetSocketType
  ) {
    return generateLink(
      history.location,
      {
        useCase: ucIdentifier,
        fromAtomUri: atomUri,
        senderSocketType: ucSenderSocketType,
        targetSocketType: ucTargetSocketType,
        mode: "CONNECT",
        holderUri: addHolderUri,
      },
      "/create"
    );
  }

  function connectAtomSockets(
    senderAtom,
    senderSocketUri,
    targetSocketUri,
    message = ""
  ) {
    const targetAtom = atom;
    const footerType = footerType;
    const dialogText = "Connect with this Atom?";

    const payload = {
      caption: "Connect",
      text: dialogText,
      buttons: [
        {
          caption: "Yes",
          callback: () => {
            if (footerType === FooterType.ENABLED) {
              dispatch(
                actionCreators.atoms__connectSocketsServerSide(
                  senderSocketUri,
                  targetSocketUri
                )
              );
            } else {
              dispatch(
                actionCreators.atoms__connectSockets(
                  senderSocketUri,
                  targetSocketUri,
                  message
                )
              );
            }
            dispatch(actionCreators.view__hideModalDialog());

            const senderSocketType = atomUtils.getSocketType(
              senderAtom,
              senderSocketUri
            );
            const targetSocketType = atomUtils.getSocketType(
              targetAtom,
              targetSocketUri
            );
            if (
              senderSocketType === vocab.CHAT.ChatSocketCompacted ||
              targetSocketType === vocab.CHAT.ChatSocketCompacted
            ) {
              history.push("/connections");
            }
          },
        },
        {
          caption: "No",
          callback: () => {
            dispatch(actionCreators.view__hideModalDialog());
          },
        },
      ],
    };
    dispatch(actionCreators.view__showModalDialog(payload));
  }

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

            if (
              connectionUtils.isSuggested(personaConnection) ||
              connectionUtils.isClosed(personaConnection)
            ) {
              dispatch(
                actionCreators.atoms__connectSockets(
                  get(personaConnection, "socketUri"),
                  get(personaConnection, "targetSocketUri"),
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
              const senderSocketUri = get(personaConnection, "socketUri");
              const targetSocketUri = get(personaConnection, "targetSocketUri");
              dispatch(
                actionCreators.atoms__connectSockets(
                  senderSocketUri,
                  targetSocketUri,
                  message
                )
              );
            } else if (connectionUtils.isConnected(personaConnection)) {
              const senderSocketUri = get(personaConnection, "socketUri");
              const targetSocketUri = get(personaConnection, "targetSocketUri");
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
            actionCreators.connections__connectAdHoc(
              targetSocketUri,
              message,
              personaUri
            )
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
                  message,
                  personaUri
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
    case FooterType.REACTION:
      footerElement = useCasesArray.map((useCase, index) =>
        getUseCaseTypeButton(useCase, index)
      );
      break;
    case FooterType.ENABLED:
      footerElement = useCasesArray.map((useCase, index) =>
        getUseCaseTypeButton(useCase, index)
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
