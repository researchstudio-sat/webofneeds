import React from "react";
import PropTypes from "prop-types";
import { actionCreators } from "../actions/actions.js";
import { useDispatch } from "react-redux";
import { Link, useHistory } from "react-router-dom";

import { sortBy, get, generateLink } from "../utils.js";
import vocab from "../service/vocab.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import * as wonLabelUtils from "../won-label-utils.js";
import * as useCaseUtils from "../usecase-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";

import WonLabelledHr from "./labelled-hr.jsx";
import ChatTextfield from "./chat-textfield.jsx";
import WonAtomHeader from "./atom-header.jsx";

import ico36_close from "~/images/won-icons/ico36_close.svg";
import ico36_plus from "~/images/won-icons/ico36_plus.svg";

import "~/style/_socket-add-atom.scss";

import Immutable from "immutable";

/**
 * Component that lets you pick certain Atoms (or create New "matching" atoms) that are connected with the `addToAtom`
 * @param addToAtom - atom to connect to
 * @param addToSocketType - socketType of the atom to connect to
 * @param storedAtoms - pool of atoms (usually all stored atoms within the redux state)
 * @param reactions - useCase reactions: json object see uc-persona.js as an example
 * @param accountState - accountState redux store -> to determine ownership of the storedAtoms, and login status
 * @param onClose - function that is executed when the close Button is clicked
 * @returns {*}
 */
export default function WonSocketAddAtom({
  addToAtom,
  addToSocketType,
  storedAtoms,
  reactions,
  accountState,
  onClose,
}) {
  const dispatch = useDispatch();
  const history = useHistory();
  const addToAtomUri = get(addToAtom, "uri");
  const addToAtomSocketUri = atomUtils.getSocketUri(addToAtom, addToSocketType);
  const isAddToAtomOwned = accountUtils.isAtomOwned(accountState, addToAtomUri);

  const allowAdHoc = !!reactions.find(
    (reaction, socketType) =>
      socketType === vocab.CHAT.ChatSocketCompacted &&
      (get(reaction, "useCaseIdentifiers", "*") ||
        (get(reaction, "useCaseIdentifiers", "persona") &&
          !(isAddToAtomOwned && get(reaction, "refuseOwned"))))
  );

  //If the addToAtom is Owned, we preselect the holderUri (if present) to be used as the holder of the new atom to establish a connection with
  const addHolderUri = isAddToAtomOwned
    ? atomUtils.getHeldByUri(addToAtom)
    : undefined;

  const sortedPossibleAtoms = storedAtoms
    .filter(atom => atomUtils.isActive(atom))
    .filter((atom, atomUri) => {
      const isOwned = accountUtils.isAtomOwned(accountState, atomUri);

      return (
        (isAddToAtomOwned || isOwned) && //Filters all non-owned Atoms if isAddToAtomOwned is false
        atomUri !== addToAtomUri //Filters the addToAtomUri from the list
      );
    })
    .filter((atom, atomUri) => {
      const isOwned = accountUtils.isAtomOwned(accountState, atomUri);

      return (
        reactions &&
        !!reactions.find((reaction, socketType) => {
          const allowedUseCaseList = get(reaction, "useCaseIdentifiers");

          const refuseOwned = get(reaction, "refuseOwned");
          const refuseNonOwned = get(reaction, "refuseNonOwned");

          return (
            (!isAddToAtomOwned || !(refuseOwned && isOwned)) &&
            (isAddToAtomOwned || !(refuseNonOwned && !isOwned)) &&
            atomUtils.hasSocket(atom, socketType) &&
            allowedUseCaseList &&
            !!allowedUseCaseList.find(
              ucIdentifier =>
                (ucIdentifier === "*" ||
                  atomUtils.getMatchedUseCaseIdentifier(atom) ===
                    ucIdentifier) &&
                (isAddToAtomOwned
                  ? !atomUtils.getConnectionBySocketUris(
                      addToAtom,
                      addToAtomSocketUri,
                      atomUtils.getSocketUri(atom, socketType)
                    )
                  : !atomUtils.getConnectionBySocketUris(
                      atom,
                      atomUtils.getSocketUri(atom, socketType),
                      addToAtomSocketUri
                    ))
            )
          );
        })
      );
    });

  const createAtomReactionsArray = [];

  reactions &&
    reactions
      .filter(reaction => !isAddToAtomOwned || !get(reaction, "refuseOwned"))
      .map((reaction, socketType) => {
        const allowedUseCaseList = get(reaction, "useCaseIdentifiers");

        return allowedUseCaseList.map(ucIdentifier =>
          createAtomReactionsArray.push({
            ucIdentifier: ucIdentifier,
            socketType: socketType,
          })
        );
      });

  const sortedPossibleAtomsArray =
    sortedPossibleAtoms &&
    sortBy(sortedPossibleAtoms, elem => {
      const humanReadable = get(elem, "humanReadable");
      return humanReadable ? humanReadable.toLowerCase() : undefined;
    });

  function selectAtom(selectedAtom) {
    const selectedAtomUri = get(selectedAtom, "uri");
    const isSelectedAtomOwned = accountUtils.isAtomOwned(
      accountState,
      selectedAtomUri
    );

    const connectFunctions = [];

    reactions &&
      reactions.map((reaction, socketType) => {
        const allowedUseCaseList = get(reaction, "useCaseIdentifiers");

        const refuseOwned = get(reaction, "refuseOwned");
        const refuseNonOwned = get(reaction, "refuseNonOwned");

        const includesSelectedUseCase =
          (!isAddToAtomOwned || !(refuseOwned && isSelectedAtomOwned)) &&
          (isAddToAtomOwned || !(refuseNonOwned && !isSelectedAtomOwned)) &&
          allowedUseCaseList &&
          !!allowedUseCaseList.find(
            ucIdentifier =>
              (ucIdentifier === "*" ||
                atomUtils.getMatchedUseCaseIdentifier(selectedAtom) ===
                  ucIdentifier) &&
              (isAddToAtomOwned
                ? !atomUtils.getConnectionBySocketUris(
                    addToAtom,
                    addToAtomSocketUri,
                    atomUtils.getSocketUri(selectedAtom, socketType)
                  )
                : !atomUtils.getConnectionBySocketUris(
                    selectedAtom,
                    atomUtils.getSocketUri(selectedAtom, socketType),
                    addToAtomSocketUri
                  ))
          );
        const selectedAtomSocketUri = atomUtils.getSocketUri(
          selectedAtom,
          socketType
        );

        if (includesSelectedUseCase && selectedAtomSocketUri) {
          if (isAddToAtomOwned && isSelectedAtomOwned) {
            connectFunctions.push({
              label: `As ${wonLabelUtils.getSocketTabLabel(socketType)}`,
              func: actionCreators.atoms__connectSockets(
                addToAtomSocketUri,
                selectedAtomSocketUri
              ),
            });
          } else if (isAddToAtomOwned && !isSelectedAtomOwned) {
            connectFunctions.push({
              label: `As ${wonLabelUtils.getSocketItemLabel(socketType)}`,
              func: actionCreators.atoms__connectSockets(
                addToAtomSocketUri,
                selectedAtomSocketUri
              ),
            });
          } else if (!isAddToAtomOwned && isSelectedAtomOwned) {
            connectFunctions.push({
              label: `As ${wonLabelUtils.getSocketItemLabel(socketType)}`,
              func: actionCreators.atoms__connectSockets(
                selectedAtomSocketUri,
                addToAtomSocketUri
              ),
            });
          } else {
            console.error(
              "both the addToAtom and the selectedAtom are not owned... this shouldn't have happened :-( "
            );
          }
        }
      });

    if (connectFunctions.length > 0) {
      const connectButtons =
        connectFunctions.length === 1
          ? [
              {
                caption: "Yes",
                callback: () => {
                  onClose();
                  dispatch(connectFunctions[0].func);
                  dispatch(actionCreators.view__hideModalDialog());
                },
              },
            ]
          : connectFunctions.map(connectFunction => ({
              caption: connectFunction.label,
              callback: () => {
                onClose();
                dispatch(connectFunction.func);
                dispatch(actionCreators.view__hideModalDialog());
              },
            }));

      const payload = {
        caption: wonLabelUtils.getSocketTabLabel(addToSocketType),
        text: isAddToAtomOwned
          ? `Add '${get(
              selectedAtom,
              "humanReadable"
            )}' to ${wonLabelUtils.getSocketTabLabel(addToSocketType)}`
          : `Request '${get(
              selectedAtom,
              "humanReadable"
            )}' to be added in ${wonLabelUtils.getSocketTabLabel(
              addToSocketType
            )}`,
        buttons: [
          ...connectButtons,
          {
            caption: "No",
            callback: () => {
              dispatch(actionCreators.view__hideModalDialog());
            },
          },
        ],
      };
      payload && dispatch(actionCreators.view__showModalDialog(payload));
    }
  }

  function sendAdHocRequest(message, targetSocketUri, personaUri) {
    if (accountUtils.isLoggedIn(accountState)) {
      if (addToAtomUri) {
        const personaAtom = get(storedAtoms, personaUri);

        if (personaAtom) {
          // if the personaAtom already contains a chatSocket we will just use the persona as the Atom that connects
          const senderSocketUri = atomUtils.getChatSocket(personaAtom);
          const personaConnection = atomUtils.getConnectionBySocketUris(
            personaAtom,
            senderSocketUri,
            targetSocketUri
          );

          let connectFunction = actionCreators.atoms__connectSockets(
            senderSocketUri,
            targetSocketUri,
            message
          );

          if (!personaConnection) {
            dispatch(connectFunction);
          } else if (personaConnection) {
            const personaConnectionUri = get(personaConnection, "uri");

            if (
              connectionUtils.isSuggested(personaConnection) ||
              connectionUtils.isClosed(personaConnection)
            ) {
              dispatch(connectFunction);
            } else if (connectionUtils.isRequestReceived(personaConnection)) {
              dispatch(
                actionCreators.atoms__connectSockets(
                  senderSocketUri,
                  targetSocketUri,
                  message
                )
              );
            } else if (
              connectionUtils.isConnected(personaConnection) &&
              message
            ) {
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
            // For status RequestSent and Connected(without a message) we simply view the go to the connection

            history.push(
              generateLink(
                history.location,
                {
                  connectionUri: personaConnectionUri,
                },
                "/connections"
              )
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

  return (
    <won-socket-add-atom>
      <div className="wsaa__header">
        <svg className="wsaa__header__icon" onClick={onClose}>
          <use xlinkHref={ico36_close} href={ico36_close} />
        </svg>
        <div className="wsaa__header__label">
          {`Pick an Atom below to ${
            isAddToAtomOwned ? "add" : "connect"
          } to the ${wonLabelUtils.getSocketTabLabel(addToSocketType)}`}
        </div>
      </div>
      <div className="wsaa__content">
        {sortedPossibleAtomsArray &&
          sortedPossibleAtomsArray.map((atom, index) => (
            <WonAtomHeader
              key={get(atom, "uri") + "-" + index}
              atom={atom}
              hideTimestamp={true}
              onClick={() => selectAtom(atom)}
            />
          ))}
        {createAtomReactionsArray.map(({ ucIdentifier, socketType }, index) => (
          <Link
            key={ucIdentifier + "-" + index}
            className="wsaa__content__create"
            to={location =>
              generateLink(
                location,
                {
                  useCase: ucIdentifier !== "*" ? ucIdentifier : undefined,
                  fromAtomUri: addToAtomUri,
                  senderSocketType: socketType,
                  targetSocketType: addToSocketType,
                  mode: "CONNECT",
                  holderUri: addHolderUri,
                },
                "/create"
              )
            }
          >
            <div className="wsaa__content__create__icon">
              {useCaseUtils.getUseCaseIcon(ucIdentifier) ? (
                <svg className="wsaa__content__create__icon__svg">
                  <use
                    xlinkHref={useCaseUtils.getUseCaseIcon(ucIdentifier)}
                    href={useCaseUtils.getUseCaseIcon(ucIdentifier)}
                  />
                </svg>
              ) : (
                <svg className="wsaa__content__create__icon__svg">
                  <use xlinkHref={ico36_plus} href={ico36_plus} />
                </svg>
              )}
            </div>
            <div className="wsaa__content__create__right">
              <div className="wsaa__content__create__right__topline">
                <div className="wsaa__content__create__right__topline__notitle">
                  {isAddToAtomOwned
                    ? `Add New ${wonLabelUtils.getSocketItemLabel(socketType)}`
                    : `Connect New ${wonLabelUtils.getSocketItemLabel(
                        socketType
                      )}`}
                </div>
              </div>
              <div className="wsaa__content__create__right__subtitle">
                <span className="wsaa__content__create__right__subtitle__type">
                  <span>{useCaseUtils.getUseCaseLabel(ucIdentifier)}</span>
                </span>
              </div>
            </div>
          </Link>
        ))}
      </div>
      {allowAdHoc ? (
        <React.Fragment>
          {reactions ? <WonLabelledHr label="Or" /> : undefined}
          <ChatTextfield
            placeholder="Message (optional)"
            allowEmptySubmit={true}
            showPersonas={true}
            submitButtonLabel={
              addToSocketType === vocab.CHAT.ChatSocketCompacted
                ? "Ask to Chat"
                : isAddToAtomOwned
                  ? "Join"
                  : "Ask to Join"
            }
            onSubmit={({ value, selectedPersona }) =>
              sendAdHocRequest(
                value,
                atomUtils.getSocketUri(addToAtom, addToSocketType),
                selectedPersona && selectedPersona.personaId
              )
            }
          />
        </React.Fragment>
      ) : (
        undefined
      )}
    </won-socket-add-atom>
  );
}
WonSocketAddAtom.propTypes = {
  addToAtom: PropTypes.object.isRequired,
  addToSocketType: PropTypes.string.isRequired,
  reactions: PropTypes.object,
  storedAtoms: PropTypes.object.isRequired,
  accountState: PropTypes.object.isRequired,
  onClose: PropTypes.func.isRequired,
};
