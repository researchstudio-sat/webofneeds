import React from "react";
import PropTypes from "prop-types";
import { useSelector, useDispatch } from "react-redux";
import { get, extractAtomUriFromConnectionUri } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import vocab from "../service/vocab";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as wonLabelUtils from "../won-label-utils.js";

import "~/style/_atom-actions.scss";
import WonGenericSocketActions from "./socket-actions/generic-actions";
import WonAtomHeader from "./atom-header.jsx";
import WonParticipantSocketActions from "./socket-actions/participant-actions";
import WonBuddySocketActions from "./socket-actions/buddy-actions";
import WonChatSocketActions from "./socket-actions/chat-actions";
import WonAtomIcon from "./atom-icon";

const ActionType = {
  INACTIVE: 1,
  INACTIVE_OWNED: 2,
  CONNECTION: 3,
  UNKNOWN: 4,
};

export default function WonAtomActions({
  atom,
  ownedConnection,
  storedAtoms,
  className,
}) {
  const dispatch = useDispatch();
  const atomUri = get(atom, "uri");

  const isOwned = useSelector(generalSelectors.isAtomOwned(atomUri));

  let actionType = ActionType.UNKNOWN;

  if (atomUtils.isInactive(atom)) {
    actionType = isOwned ? ActionType.INACTIVE_OWNED : ActionType.INACTIVE;
  } else {
    actionType = ownedConnection ? ActionType.CONNECTION : ActionType.UNKNOWN;
  }

  let actionElement;

  switch (actionType) {
    case ActionType.INACTIVE:
      actionElement = (
        <div className="atom-actions__infolabel">
          Atom is inactive, no requests allowed
        </div>
      );
      break;
    case ActionType.INACTIVE_OWNED:
      actionElement = (
        <React.Fragment>
          <div className="atom-actions__infolabel">
            This Atom is inactive. Others will not be able to interact with it.
          </div>
          <div className="atom-actions__buttons">
            <button
              className="won-publish-button secondary won-button--filled"
              onClick={() => dispatch(actionCreators.atoms__reopen(atomUri))}
            >
              Reopen
            </button>
          </div>
        </React.Fragment>
      );
      break;
    case ActionType.CONNECTION:
      {
        const targetAtom = get(
          storedAtoms,
          connectionUtils.getTargetAtomUri(ownedConnection)
        );
        const senderAtom = get(
          storedAtoms,
          extractAtomUriFromConnectionUri(get(ownedConnection, "uri"))
        );

        const senderSocketType = atomUtils.getSocketType(
          senderAtom,
          get(ownedConnection, "socketUri")
        );
        const targetSocketType = atomUtils.getSocketType(
          targetAtom,
          get(ownedConnection, "targetSocketUri")
        );

        const isViewOfTargetAtom = get(targetAtom, "uri") === atomUri;

        let ActionComponent;

        //TODO: Display correct SocketItem based on socketType instead of WonAtomHeader

        switch (senderSocketType) {
          case vocab.GROUP.GroupSocketCompacted:
            ActionComponent = WonParticipantSocketActions;
            break;
          case vocab.BUDDY.BuddySocketCompacted:
            ActionComponent = WonBuddySocketActions;
            break;
          case vocab.CHAT.ChatSocketCompacted:
            ActionComponent = WonChatSocketActions;
            break;
          default:
            ActionComponent = WonGenericSocketActions;
            break;
        }

        actionElement = (
          <React.Fragment>
            <div
              className={
                "atom-actions__info " +
                (isViewOfTargetAtom
                  ? " atom-actions__info--targetVisible "
                  : " atom-actions__info--senderVisible ")
              }
            >
              <div className="atom-actions__info__target">
                {isViewOfTargetAtom ? (
                  <WonAtomIcon atom={targetAtom} />
                ) : (
                  <WonAtomHeader atom={targetAtom} hideTimestamp={true} />
                )}
              </div>
              <div className="atom-actions__info__label">
                {wonLabelUtils.getSocketActionInfoLabel(
                  senderSocketType,
                  get(ownedConnection, "state"),
                  targetSocketType
                )}
              </div>
              <div className="atom-actions__info__sender">
                {isViewOfTargetAtom ? (
                  <WonAtomHeader atom={senderAtom} hideTimestamp={true} />
                ) : (
                  <WonAtomIcon atom={senderAtom} />
                )}
              </div>
            </div>
            <ActionComponent
              connection={ownedConnection}
              goBackOnAction={true}
            />
          </React.Fragment>
        );
      }
      break;
    case ActionType.UNKNOWN:
    default:
      actionElement = undefined;
      break;
  }

  return (
    <won-atom-actions class={className ? className : ""}>
      {actionElement}
    </won-atom-actions>
  );
}
WonAtomActions.propTypes = {
  atom: PropTypes.object,
  ownedConnection: PropTypes.object,
  storedAtoms: PropTypes.object.isRequired,
  className: PropTypes.string,
};
