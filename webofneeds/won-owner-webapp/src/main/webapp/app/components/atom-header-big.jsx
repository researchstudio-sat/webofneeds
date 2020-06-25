/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import { useSelector } from "react-redux";

import "~/style/_atom-header-big.scss";
import * as atomUtils from "../redux/utils/atom-utils";
import * as wonLabelUtils from "../won-label-utils.js";
import * as accountUtils from "../redux/utils/account-utils";
import { get } from "../utils.js";

import WonAtomContextDropdown from "../components/atom-context-dropdown.jsx";
import WonAtomIcon from "../components/atom-icon.jsx";
import WonShareDropdown from "../components/share-dropdown.jsx";
import WonAddBuddy from "../components/add-buddy.jsx";
import * as generalSelectors from "../redux/selectors/general-selectors";

import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";
import { extractAtomUriFromConnectionUri } from "../utils";

export default function WonAtomHeaderBig({
  atom,
  ownedConnection,
  showActions,
  toggleActions,
}) {
  const atomUri = get(atom, "uri");
  const personaUri = atomUtils.getHeldByUri(atom);
  const storedAtoms = useSelector(generalSelectors.getAtoms);
  const persona = get(storedAtoms, personaUri);
  const personaName =
    atomUtils.hasHoldableSocket(atom) && !atomUtils.hasGroupSocket(atom)
      ? get(persona, "humanReadable") || get(atom, "fakePersonaName")
      : undefined;
  const accountState = useSelector(generalSelectors.getAccountState);
  const ownedAtomsWithBuddySocket = useSelector(
    generalSelectors.getOwnedAtomsWithBuddySocket
  );
  const hasOwnedAtomsWithBuddySocket =
    ownedAtomsWithBuddySocket &&
    ownedAtomsWithBuddySocket
      .filter(atom => atomUtils.isActive(atom))
      .filter(atom => get(atom, "uri") !== atomUri).size > 0;

  const isGroupChatEnabled = atomUtils.hasGroupSocket(atom);
  const isChatEnabled = atomUtils.hasChatSocket(atom);
  const showAddBuddyElement =
    atomUtils.hasBuddySocket(atom) &&
    hasOwnedAtomsWithBuddySocket &&
    !accountUtils.isAtomOwned(accountState, atomUri);

  const title = get(atom, "humanReadable");

  const titleElement = title ? (
    <h1 className="ahb__title">{title}</h1>
  ) : (
    <h1 className="ahb__title ahb__title--notitle">No Title</h1>
  );

  const personaNameElement = personaName && (
    <span className="ahb__info__persona">{personaName}</span>
  );

  const groupChatElement = isGroupChatEnabled && (
    <span className="ahb__info__groupchat">
      {"Group Chat" + (isChatEnabled ? " enabled" : "")}
    </span>
  );

  const buddyActionElement = showAddBuddyElement && <WonAddBuddy atom={atom} />;

  const generateAtomActionButton = () => {
    const isInactive = atomUtils.isInactive(atom);
    if (ownedConnection || isInactive) {
      const connectionState = get(ownedConnection, "state");
      const targetAtom = get(
        storedAtoms,
        get(ownedConnection, "targetAtomUri")
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

      return (
        <won-toggle-actions>
          <button
            onClick={() => toggleActions(!showActions)}
            className={
              "won-toggle-actions__button " +
              (showActions
                ? " won-toggle-actions__button--expanded "
                : " won-toggle-actions__button--collapsed ")
            }
          >
            {isInactive ? (
              <span className="won-toggle-actions__button__label">
                Atom Inactive
              </span>
            ) : (
              <React.Fragment>
                <div className="won-toggle-actions__button__infoicon">
                  <WonAtomIcon atom={targetAtom} />
                </div>
                <span className="won-toggle-actions__button__label">
                  {wonLabelUtils.getSocketActionInfoLabel(
                    senderSocketType,
                    connectionState,
                    targetSocketType
                  )}
                </span>
                <div className="won-toggle-actions__button__infoicon">
                  <WonAtomIcon atom={senderAtom} />
                </div>
              </React.Fragment>
            )}
            <svg className="won-toggle-actions__button__carret">
              <use xlinkHref={ico16_arrow_down} href={ico16_arrow_down} />
            </svg>
          </button>
        </won-toggle-actions>
      );
    }
  };

  return (
    <won-atom-header-big
      class={showActions ? "won-atom-header-big--actions-expanded" : ""}
    >
      <WonAtomIcon atom={atom} />
      {titleElement}
      {(groupChatElement || personaNameElement) && (
        <div className="ahb__info">
          {groupChatElement}
          {personaNameElement}
        </div>
      )}
      {buddyActionElement}
      {generateAtomActionButton()}
      <WonShareDropdown atom={atom} />
      <WonAtomContextDropdown atom={atom} />
    </won-atom-header-big>
  );
}
WonAtomHeaderBig.propTypes = {
  atom: PropTypes.object,
  ownedConnection: PropTypes.object,
  showActions: PropTypes.bool.isRequired,
  toggleActions: PropTypes.func.isRequired,
};
