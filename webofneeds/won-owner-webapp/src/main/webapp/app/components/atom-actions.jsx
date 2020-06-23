import React from "react";
import PropTypes from "prop-types";
import { useSelector, useDispatch } from "react-redux";
import { get, extractAtomUriFromConnectionUri } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import vocab from "../service/vocab";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as wonLabelUtils from "../won-label-utils.js";

import "~/style/_atom-actions.scss";
import WonGenericSocketActions from "./socket-actions/generic-actions";
import WonAtomHeader from "./atom-header.jsx";
import WonParticipantSocketActions from "./socket-actions/participant-actions";
import WonBuddySocketActions from "./socket-actions/buddy-actions";
import WonChatSocketActions from "./socket-actions/chat-actions";

const ActionType = {
  INACTIVE: 1,
  INACTIVE_OWNED: 2,
  CONNECTION: 3,
  UNKNOWN: 4,
};

export default function WonAtomActions({ atom, ownedConnection, className }) {
  const dispatch = useDispatch();
  const atomUri = get(atom, "uri");
  const atomType = atomUtils.generateTypeLabel(atom);

  const isOwned = useSelector(generalSelectors.isAtomOwned(atomUri));

  let actionType = ActionType.UNKNOWN;

  if (atomUtils.isInactive(atom)) {
    actionType = isOwned ? ActionType.INACTIVE_OWNED : ActionType.INACTIVE;
  } else {
    actionType = ownedConnection ? ActionType.CONNECTION : ActionType.UNKNOWN;
  }

  const targetAtom = useSelector(
    state =>
      ownedConnection &&
      generalSelectors.getAtom(get(ownedConnection, "targetAtomUri"))(state)
  );
  const senderAtom = useSelector(
    state =>
      ownedConnection &&
      generalSelectors.getAtom(
        extractAtomUriFromConnectionUri(get(ownedConnection, "uri"))
      )(state)
  );

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
              className="won-publish-button red won-button--filled"
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
        const senderSocketType = atomUtils.getSocketType(
          senderAtom,
          get(ownedConnection, "socketUri")
        );
        const targetSocketType = atomUtils.getSocketType(
          targetAtom,
          get(ownedConnection, "targetSocketUri")
        );

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

        const isTargetAtomDisplayed = get(senderAtom, "uri") === atomUri;
        const toSocketType = isTargetAtomDisplayed
          ? targetSocketType
          : senderSocketType;
        actionElement = (
          <React.Fragment>
            <div className="atom-actions__infolabel">
              {wonLabelUtils.getSocketActionInfoLabel(
                atomType,
                toSocketType,
                get(ownedConnection, "state")
              )}
            </div>
            <WonAtomHeader
              atom={isTargetAtomDisplayed ? targetAtom : senderAtom}
            />
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
  className: PropTypes.string,
};
