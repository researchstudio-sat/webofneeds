/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import { actionCreators } from "~/app/actions/actions";
import { useDispatch, useSelector } from "react-redux";

import * as generalSelectors from "../redux/selectors/general-selectors";
import * as atomUtils from "../redux/utils/atom-utils";
import * as wonLabelUtils from "../won-label-utils.js";
import * as accountUtils from "../redux/utils/account-utils";
import { get, extractAtomUriFromConnectionUri } from "../utils.js";

import WonAtomContextDropdown from "../components/atom-context-dropdown.jsx";
import WonAtomIcon from "../components/atom-icon.jsx";
import WonShareDropdown from "../components/share-dropdown.jsx";
import WonAddBuddy from "../components/add-buddy.jsx";

import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";
import "~/style/_atom-header-big.scss";
import * as processUtils from "~/app/redux/utils/process-utils";
import VisibilitySensor from "react-visibility-sensor";

export default function WonAtomHeaderBig({
  atomUri,
  atom,
  ownedConnection,
  showActions,
  toggleActions,
}) {
  const dispatch = useDispatch();
  const storedAtoms = useSelector(generalSelectors.getAtoms);
  const accountState = useSelector(generalSelectors.getAccountState);
  const processState = useSelector(generalSelectors.getProcessState);
  const ownedAtomsWithBuddySocket = useSelector(
    generalSelectors.getOwnedAtomsWithBuddySocket
  );
  const atomLoading =
    !atom || processUtils.isAtomLoading(processState, atomUri);
  const atomToLoad = !atom || processUtils.isAtomToLoad(processState, atomUri);
  const atomFailedToLoad =
    atom && processUtils.hasAtomFailedToLoad(processState, atomUri);
  const externalDataState = useSelector(generalSelectors.getExternalDataState);

  let contentElement;

  if (atomLoading || atomToLoad || !atom) {
    const onChange = isVisible => {
      console.debug("is visible...", isVisible);
      if (isVisible) {
        ensureAtomIsLoaded();
      }
    };

    const ensureAtomIsLoaded = () => {
      console.debug("ensureAtomIsLoaded...");
      if (atomUri && (!atom || (atomToLoad && !atomLoading))) {
        console.debug("fetch atomUri, ", atomUri);
        dispatch(actionCreators.atoms__fetchUnloadedAtom(atomUri));
      } else {
        console.debug(
          "fetch omitted atomUri, ",
          atomUri,
          atomToLoad,
          atomLoading,
          atom
        );
      }
    };

    //Loading View
    contentElement = (
      <React.Fragment>
        <VisibilitySensor
          onChange={onChange}
          intervalDelay={200}
          partialVisibility={true}
          offset={{ top: -300, bottom: -300 }}
        >
          <div className="ahb__icon__skeleton" />
        </VisibilitySensor>
        <div className="ahb__title" />
        <div className="ahb__info" />
      </React.Fragment>
    );
  } else if (atomFailedToLoad) {
    //FailedToLoad View
    contentElement = (
      <React.Fragment>
        <div className="ahb__icon__skeleton" />
        <div className="ahb__title" />
        <div className="ahb__info" />
      </React.Fragment>
    );
  } else {
    //Normal View
    const hasOwnedAtomsWithBuddySocket =
      ownedAtomsWithBuddySocket &&
      ownedAtomsWithBuddySocket
        .filter(atom => atomUtils.isActive(atom))
        .filter(atom => get(atom, "uri") !== atomUri).size > 0;

    const showAddBuddyElement =
      atomUtils.hasBuddySocket(atom) &&
      hasOwnedAtomsWithBuddySocket &&
      !accountUtils.isAtomOwned(accountState, atomUri);

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

    const buddyActionElement = showAddBuddyElement && (
      <WonAddBuddy atom={atom} />
    );

    const title = atomUtils.getTitle(atom, externalDataState);

    const titleElement = title ? (
      <h1 className="ahb__title">{title}</h1>
    ) : (
      <h1 className="ahb__title ahb__title--notitle">No Title</h1>
    );

    const personaUri = atomUtils.getHeldByUri(atom);
    const persona = get(storedAtoms, personaUri);
    const personaName =
      atomUtils.hasHoldableSocket(atom) && !atomUtils.hasGroupSocket(atom)
        ? atomUtils.getTitle(persona, externalDataState) ||
          get(atom, "fakePersonaName")
        : undefined;

    const personaNameElement = personaName && (
      <span className="ahb__info__persona">{personaName}</span>
    );

    const isGroupChatEnabled = atomUtils.hasGroupSocket(atom);
    const isChatEnabled = atomUtils.hasChatSocket(atom);

    const groupChatElement = isGroupChatEnabled && (
      <span className="ahb__info__groupchat">
        {"Group Chat" + (isChatEnabled ? " enabled" : "")}
      </span>
    );

    contentElement = (
      <React.Fragment>
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
      </React.Fragment>
    );
  }

  return (
    <won-atom-header-big
      class={
        showActions
          ? " won-atom-header-big--actions-expanded "
          : "" +
            (atomFailedToLoad ? " won-failed-to-load " : "") +
            (atomLoading ? " won-is-loading " : "") +
            (atomToLoad ? " won-to-load " : "")
      }
    >
      {contentElement}
    </won-atom-header-big>
  );
}
WonAtomHeaderBig.propTypes = {
  atomUri: PropTypes.string.isRequired,
  atom: PropTypes.object,
  ownedConnection: PropTypes.object,
  showActions: PropTypes.bool.isRequired,
  toggleActions: PropTypes.func.isRequired,
};
