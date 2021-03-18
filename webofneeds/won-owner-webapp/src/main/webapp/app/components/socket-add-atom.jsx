import React from "react";
import PropTypes from "prop-types";
import { actionCreators } from "../actions/actions.js";
import { useDispatch, useSelector } from "react-redux";
import { Link, useHistory } from "react-router-dom";

import { generateLink, get, getUri } from "../utils.js";
import vocab from "../service/vocab.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import * as wonLabelUtils from "../won-label-utils.js";
import * as useCaseUtils from "../usecase-utils.js";

import WonLabelledHr from "./labelled-hr.jsx";
import ChatTextfield from "./chat-textfield.jsx";
import WonAtomHeader from "./atom-header.jsx";

import ico36_close from "~/images/won-icons/ico36_close.svg";
import ico36_plus from "~/images/won-icons/ico36_plus.svg";

import "~/style/_socket-add-atom.scss";

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
  const addToAtomUri = getUri(addToAtom);
  const addToAtomSocketUri = atomUtils.getSocketUri(addToAtom, addToSocketType);
  const addToUseCase = atomUtils.getMatchedUseCaseIdentifier(addToAtom);
  const isAddToAtomOwned = accountUtils.isAtomOwned(accountState, addToAtomUri);
  const externalDataState = useSelector(generalSelectors.getExternalDataState);

  const adHocUseCaseIdentifiers = get(
    reactions.find(
      (reaction, socketType) =>
        socketType === vocab.CHAT.ChatSocketCompacted &&
        !(isAddToAtomOwned && get(reaction, "refuseOwned"))
    ),
    "useCaseIdentifiers"
  );
  const allowAdHoc =
    !!adHocUseCaseIdentifiers &&
    adHocUseCaseIdentifiers.filter(
      ucIdentifier => ucIdentifier !== "persona" && ucIdentifier !== "*"
    ).size === 1;

  //If the addToAtom is Owned, we preselect the holderUri (if present) to be used as the holder of the new atom to establish a connection with
  const addHolderUri = isAddToAtomOwned
    ? atomUtils.getHeldByUri(addToAtom)
    : undefined;

  const sortedPossibleAtoms = storedAtoms
    .filter(atomUtils.isActive)
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
          //const labels = get(reaction, "labels");

          return (
            allowedUseCaseList &&
            (isOwned
              ? !refuseOwned || !isAddToAtomOwned
              : !refuseNonOwned && isAddToAtomOwned) &&
            atomUtils.hasSocket(atom, socketType) &&
            (!vocab.socketCapacity[socketType] ||
              atomUtils.getConnectedConnections(atom, socketType).size <
                vocab.socketCapacity[socketType]) &&
            !!allowedUseCaseList.find(
              ucIdentifier =>
                (ucIdentifier === "*" ||
                  atomUtils.getMatchedUseCaseIdentifier(atom) ===
                    ucIdentifier) &&
                (vocab.socketCapacity[addToSocketType] === 1 ||
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
                      )))
            )
          );
        })
      );
    })
    .toOrderedMap()
    .sortBy(atom => {
      const humanReadable = atomUtils.getTitle(atom, externalDataState);
      return humanReadable ? humanReadable.toLowerCase() : undefined;
    });

  const createAtomReactionsElements = [];

  reactions &&
    reactions
      .filter(reaction => !isAddToAtomOwned || !get(reaction, "refuseOwned"))
      .map((reaction, socketType) =>
        get(reaction, "useCaseIdentifiers").map(ucIdentifier =>
          createAtomReactionsElements.push(
            <Link
              key={ucIdentifier + "-" + socketType}
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
                    {wonLabelUtils.getAddNewSocketItemLabel(
                      isAddToAtomOwned,
                      addToUseCase,
                      addToSocketType,
                      ucIdentifier,
                      socketType,
                      addToAtom
                    )}
                  </div>
                </div>
                <div className="wsaa__content__create__right__subtitle">
                  <span className="wsaa__content__create__right__subtitle__type">
                    <span>
                      {ucIdentifier !== "*"
                        ? useCaseUtils.getUseCaseLabel(ucIdentifier)
                        : "Pick a UseCase"}
                    </span>
                  </span>
                </div>
              </div>
            </Link>
          )
        )
      );

  const sortedPossibleAtomsElements = [];

  sortedPossibleAtoms &&
    sortedPossibleAtoms.map((atom, atomUri) =>
      sortedPossibleAtomsElements.push(
        <WonAtomHeader
          key={atomUri}
          atom={atom}
          hideTimestamp={true}
          onClick={() => selectAtom(atom)}
        />
      )
    );

  function selectAtom(selectedAtom) {
    const selectedAtomUri = getUri(selectedAtom);
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
          allowedUseCaseList &&
          (isSelectedAtomOwned
            ? !refuseOwned || !isAddToAtomOwned
            : !refuseNonOwned && isAddToAtomOwned) &&
          (!vocab.socketCapacity[socketType] ||
            atomUtils.getConnectedConnections(selectedAtom, socketType).size <
              vocab.socketCapacity[socketType]) &&
          !!allowedUseCaseList.find(
            ucIdentifier =>
              (ucIdentifier === "*" ||
                atomUtils.getMatchedUseCaseIdentifier(selectedAtom) ===
                  ucIdentifier) &&
              (vocab.socketCapacity[addToSocketType] === 1 ||
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
                    )))
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
              label: `As ${wonLabelUtils.getSocketItemLabel(
                addToSocketType,
                socketType
              )}`,
              func: actionCreators.atoms__connectSockets(
                addToAtomSocketUri,
                selectedAtomSocketUri
              ),
            });
          } else if (!isAddToAtomOwned && isSelectedAtomOwned) {
            connectFunctions.push({
              label: `As ${wonLabelUtils.getSocketItemLabel(
                addToSocketType,
                socketType
              )}`,
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
          ? `Add '${atomUtils.getTitle(
              selectedAtom,
              externalDataState
            )}' to ${wonLabelUtils.getSocketTabLabel(addToSocketType)}`
          : `Request '${atomUtils.getTitle(
              selectedAtom,
              externalDataState
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

  function sendAdHocRequest(
    message,
    targetSocketUri,
    personaUri,
    adHocUseCaseIdentifier
  ) {
    if (addToAtomUri) {
      dispatch(
        actionCreators.connections__connectAdHoc(
          targetSocketUri,
          message,
          adHocUseCaseIdentifier,
          addToAtom,
          personaUri,
          () => {
            history.push("/connections");
          }
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
          {wonLabelUtils.getSocketPickerLabel(
            addToAtom,
            addToSocketType,
            isAddToAtomOwned
          )}
        </div>
      </div>
      <div className="wsaa__content">
        {sortedPossibleAtomsElements}
        {createAtomReactionsElements}
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
                selectedPersona && selectedPersona.personaId,
                adHocUseCaseIdentifiers.first()
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
