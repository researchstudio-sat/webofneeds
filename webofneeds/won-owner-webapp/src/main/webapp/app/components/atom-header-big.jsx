/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import { useSelector } from "react-redux";

import "~/style/_atom-header-big.scss";
import * as atomUtils from "../redux/utils/atom-utils";
import * as accountUtils from "../redux/utils/account-utils";
import { get, getIn } from "../utils.js";

import WonAtomContextDropdown from "../components/atom-context-dropdown.jsx";
import WonAtomIcon from "../components/atom-icon.jsx";
import WonShareDropdown from "../components/share-dropdown.jsx";
import WonAddBuddy from "../components/add-buddy.jsx";
import * as generalSelectors from "../redux/selectors/general-selectors";

export default function WonAtomHeaderBig({ atom }) {
  const atomUri = get(atom, "uri");
  const personaUri = atomUtils.getHeldByUri(atom);
  const persona = useSelector(
    state => personaUri && getIn(state, ["atoms", personaUri])
  );
  const personaName = get(persona, "humanReadable");
  const isDirectResponse = atomUtils.isDirectResponseAtom(atom);
  const responseToUri =
    isDirectResponse && getIn(atom, ["content", "responseToUri"]);
  const responseToAtom = useSelector(
    state => responseToUri && getIn(state, ["atoms", responseToUri])
  );
  const accountState = useSelector(generalSelectors.getAccountState);
  const ownedAtomsWithBuddySocket = useSelector(state =>
    generalSelectors.getOwnedAtomsWithBuddySocket(state)
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
  const atomTypeLabel = atom && atomUtils.generateTypeLabel(atom);

  function generateTitle() {
    if (isDirectResponse && responseToAtom) {
      return "Re: " + get(responseToAtom, "humanReadable");
    } else {
      return get(atom, "humanReadable");
    }
  }

  function hasTitle() {
    if (isDirectResponse && responseToAtom) {
      return !!get(responseToAtom, "humanReadable");
    } else {
      return !!get(atom, "humanReadable");
    }
  }

  let titleElement;

  if (hasTitle()) {
    titleElement = <h1 className="ahb__title">{generateTitle()}</h1>;
  } else if (isDirectResponse) {
    titleElement = (
      <h1 className="ahb__title ahb__title--notitle">RE: no title</h1>
    );
  } else {
    titleElement = <h1 className="ahb__title ahb__title--notitle">no title</h1>;
  }

  const personaNameElement = personaName && (
    <span className="ahb__titles__persona">{personaName}</span>
  );

  const groupChatElement = isGroupChatEnabled && (
    <span className="ahb__titles__groupchat">
      {"Group Chat" + (isChatEnabled ? " enabled" : "")}
    </span>
  );

  const buddyActionElement = showAddBuddyElement && (
    <WonAddBuddy atomUri={atomUri} />
  );

  return (
    <won-atom-header-big>
      <nav className="atom-header-big">
        <div className="ahb__inner">
          <WonAtomIcon atom={atom} />
          <hgroup>
            {titleElement}
            {personaNameElement}
            {groupChatElement}
            <div className="ahb__titles__type">{atomTypeLabel}</div>
          </hgroup>
        </div>
        {buddyActionElement}
        <WonShareDropdown atom={atom} />
        <WonAtomContextDropdown atom={atom} />
      </nav>
    </won-atom-header-big>
  );
}
WonAtomHeaderBig.propTypes = {
  atom: PropTypes.object,
};
