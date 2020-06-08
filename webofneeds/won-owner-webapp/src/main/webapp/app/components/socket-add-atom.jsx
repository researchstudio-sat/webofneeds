import React from "react";
import PropTypes from "prop-types";
import { useDispatch } from "react-redux";
import ico36_close from "~/images/won-icons/ico36_close.svg";
import ico36_plus from "~/images/won-icons/ico36_plus.svg";

import { sortBy, get } from "../utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import * as wonLabelUtils from "../won-label-utils.js";
import * as useCaseUtils from "../usecase-utils.js";

import WonAtomHeader from "./atom-header.jsx";
import "~/style/_socket-add-atom.scss";
import { actionCreators } from "../actions/actions.js";

import { Link } from "react-router-dom";
import { generateLink } from "../utils";

export default function WonSocketAddAtom({
  addToAtom,
  addToSocketType,
  storedAtoms,
  reactions,
  accountState,
  onClose,
}) {
  const dispatch = useDispatch();
  const addToAtomUri = get(addToAtom, "uri");
  const addToAtomSocketUri = atomUtils.getSocketUri(addToAtom, addToSocketType);
  const isAddToAtomOwned = accountUtils.isAtomOwned(accountState, addToAtomUri);

  //If the addToAtom is Owned, we preselect the holderUri (if present) to be used as the holder of the new atom to establish a connection with
  const addHolderUri = isAddToAtomOwned
    ? atomUtils.getHeldByUri(addToAtom)
    : undefined;

  const sortedPossibleAtoms = storedAtoms
    .filter(atom => atomUtils.isActive(atom))
    .filter(
      //If atom is owned then every atom that is currently stored can be added, if it is not owned we limit the options to owned atoms only
      (atom, atomUri) =>
        (isAddToAtomOwned || accountUtils.isAtomOwned(accountState, atomUri)) &&
        atomUri !== addToAtomUri
    )
    .filter(
      atom =>
        reactions &&
        !!reactions.find(
          (allowedUseCaseList, socketType) =>
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
        )
    );

  const createAtomReactionsArray = [];

  reactions &&
    reactions.map((useCaseIdentifierList, socketType) =>
      useCaseIdentifierList.map(ucIdentifier =>
        createAtomReactionsArray.push({
          ucIdentifier: ucIdentifier,
          socketType: socketType,
        })
      )
    );

  const sortedPossibleAtomsArray =
    sortedPossibleAtoms &&
    sortBy(sortedPossibleAtoms, elem =>
      (get(elem, "humanReadable") || "").toLowerCase()
    );

  function selectAtom(selectedAtom) {
    const selectedAtomUri = get(selectedAtom, "uri");
    const isSelectedAtomOwned = accountUtils.isAtomOwned(
      accountState,
      selectedAtomUri
    );

    const connectFunctions = [];

    reactions &&
      reactions.map((allowedUseCaseList, socketType) => {
        const includesSelectedUseCase =
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
              func: actionCreators.atoms__connectSocketsServerSide(
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
                  useCase: ucIdentifier,
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
    </won-socket-add-atom>
  );
}
WonSocketAddAtom.propTypes = {
  addToAtom: PropTypes.object.isRequired,
  addToSocketType: PropTypes.string.isRequired,
  reactions: PropTypes.object.isRequired,
  storedAtoms: PropTypes.object.isRequired,
  accountState: PropTypes.object.isRequired,
  onClose: PropTypes.func.isRequired,
};
