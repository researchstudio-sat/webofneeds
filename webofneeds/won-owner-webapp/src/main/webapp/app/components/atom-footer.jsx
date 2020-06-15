import React from "react";
import PropTypes from "prop-types";
import { useSelector, useDispatch } from "react-redux";
import { get } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import vocab from "../service/vocab";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as wonLabelUtils from "../won-label-utils.js";

import "~/style/_atom-footer.scss";
import WonGenericSocketActions from "./socket-actions/generic-actions";
import WonParticipantSocketActions from "./socket-actions/participant-actions";
import WonBuddySocketActions from "./socket-actions/buddy-actions";

const FooterType = {
  INACTIVE: 1,
  INACTIVE_OWNED: 2,
  CONNECTION: 3,
  UNKNOWN: 4,
};

export default function WonAtomFooter({ atom, ownedConnection, className }) {
  const dispatch = useDispatch();
  const atomUri = get(atom, "uri");

  const isOwned = useSelector(generalSelectors.isAtomOwned(atomUri));

  let footerType = FooterType.UNKNOWN;

  if (atomUtils.isInactive(atom)) {
    footerType = isOwned ? FooterType.INACTIVE_OWNED : FooterType.INACTIVE;
  } else {
    footerType = ownedConnection ? FooterType.CONNECTION : FooterType.UNKNOWN;
  }

  const targetAtom = useSelector(
    state =>
      ownedConnection &&
      generalSelectors.getAtom(get(ownedConnection, "targetAtomUri"))(state)
  );
  const senderAtom = useSelector(
    state =>
      ownedConnection &&
      generalSelectors.getAtom(get(ownedConnection, "uri").split("/c")[0])(
        state
      )
  );

  const targetSocketType = atomUtils.getSocketType(
    targetAtom,
    get(ownedConnection, "targetSocketUri")
  );
  const senderSocketType = atomUtils.getSocketType(
    senderAtom,
    get(ownedConnection, "socketUri")
  );

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
          <div className="atom-footer__buttons">
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
    case FooterType.CONNECTION:
      {
        let ActionElement;

        switch (senderSocketType) {
          case vocab.GROUP.GroupSocketCompacted:
            ActionElement = WonParticipantSocketActions;
            break;
          case vocab.BUDDY.BuddySocketCompacted:
            ActionElement = WonBuddySocketActions;
            break;
          default:
            ActionElement = WonGenericSocketActions;
            break;
        }

        footerElement = (
          <React.Fragment>
            <div className="atom-footer__infolabel">
              {wonLabelUtils.getSocketActionInfo(
                senderSocketType,
                targetSocketType,
                get(ownedConnection, "state")
              )}
            </div>
            <ActionElement connection={ownedConnection} goBackOnAction={true} />
          </React.Fragment>
        );
      }
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
  ownedConnection: PropTypes.object,
  className: PropTypes.string,
};
