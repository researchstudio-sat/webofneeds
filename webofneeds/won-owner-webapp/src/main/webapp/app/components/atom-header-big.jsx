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
import * as connectionUtils from "~/app/redux/utils/connection-utils";
import * as processUtils from "../redux/utils/process-utils";
import { get, getUri, extractAtomUriFromConnectionUri } from "../utils.js";

import WonAtomContextDropdown from "../components/atom-context-dropdown.jsx";
import WonAtomIcon from "../components/atom-icon.jsx";
import WonShareDropdown from "../components/share-dropdown.jsx";
import WonAddBuddy from "../components/add-buddy.jsx";

import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";
import "~/style/_atom-header-big.scss";

import VisibilitySensor from "react-visibility-sensor";

export default function WonAtomHeaderBig({
  atomUri,
  atom,
  ownedConnectionUri,
  ownedConnection,
  showActions,
  toggleActions,
}) {
  const dispatch = useDispatch();
  const storedAtoms = useSelector(generalSelectors.getAtoms);
  const accountState = useSelector(generalSelectors.getAccountState);
  const processState = useSelector(generalSelectors.getProcessState);
  const externalDataState = useSelector(generalSelectors.getExternalDataState);
  const ownedAtomsWithBuddySocket = useSelector(
    generalSelectors.getOwnedAtomsWithBuddySocket
  );
  const atomLoading =
    !atom || processUtils.isAtomLoading(processState, atomUri);
  const atomFailedToLoad =
    atom && processUtils.hasAtomFailedToLoad(processState, atomUri);

  const holderUri = atomUtils.getHeldByUri(atom);
  const holderAtom = get(storedAtoms, holderUri);

  let contentElement;

  const isAtomFetchNecessary = processUtils.isAtomFetchNecessary(
    processState,
    atomUri,
    atom
  );
  const isHolderFetchNecessary = processUtils.isAtomFetchNecessary(
    processState,
    holderUri,
    holderAtom
  );

  const senderAtomUri = extractAtomUriFromConnectionUri(ownedConnectionUri);
  const senderAtom = get(storedAtoms, senderAtomUri);
  const isSenderAtomFetchNecessary =
    senderAtomUri !== atomUri &&
    senderAtomUri !== holderUri &&
    processUtils.isAtomFetchNecessary(processState, senderAtomUri, senderAtom);

  const senderHolderUri = atomUtils.getHeldByUri(senderAtom);
  const connectionHolder = get(storedAtoms, senderHolderUri);
  const isSenderHolderFetchNecessary =
    senderHolderUri !== atomUri &&
    senderHolderUri !== holderUri &&
    processUtils.isAtomFetchNecessary(
      processState,
      senderHolderUri,
      connectionHolder
    );

  const targetAtomUri = connectionUtils.getTargetAtomUri(ownedConnection);
  const targetAtom = get(storedAtoms, targetAtomUri);
  const isTargetAtomFetchNecessary =
    targetAtomUri !== atomUri &&
    targetAtomUri !== holderUri &&
    processUtils.isAtomFetchNecessary(processState, targetAtomUri, targetAtom);

  const targetHolderUri = atomUtils.getHeldByUri(targetAtom);
  const targetHolder = get(storedAtoms, targetHolderUri);
  const isTargetHolderFetchNecessary =
    targetHolderUri !== atomUri &&
    targetHolderUri !== holderUri &&
    processUtils.isAtomFetchNecessary(
      processState,
      targetHolderUri,
      targetHolder
    );

  if (
    isAtomFetchNecessary ||
    isHolderFetchNecessary ||
    isSenderAtomFetchNecessary ||
    isSenderHolderFetchNecessary ||
    isTargetAtomFetchNecessary ||
    isTargetHolderFetchNecessary
  ) {
    const onChange = isVisible => {
      if (isVisible) {
        ensureAtomIsFetched();
        ensureHolderIsFetched();
        ensureSenderAtomIsFetched();
        ensureSenderHolderIsFetched();
        ensureTargetAtomIsFetched();
        ensureTargetHolderIsFetched();
      }
    };

    const ensureAtomIsFetched = () => {
      if (isAtomFetchNecessary) {
        console.debug("fetch atomUri, ", atomUri);
        dispatch(actionCreators.atoms__fetchUnloadedAtom(atomUri));
      }
    };

    const ensureHolderIsFetched = () => {
      if (isHolderFetchNecessary) {
        console.debug("fetch holderUri, ", holderUri);
        dispatch(actionCreators.atoms__fetchUnloadedAtom(holderUri));
      }
    };

    const ensureSenderAtomIsFetched = () => {
      if (isSenderAtomFetchNecessary) {
        console.debug("fetch senderAtomUri, ", senderAtomUri);
        dispatch(actionCreators.atoms__fetchUnloadedAtom(senderAtomUri));
      }
    };

    const ensureSenderHolderIsFetched = () => {
      if (isSenderHolderFetchNecessary) {
        console.debug("fetch senderHolderUri, ", senderHolderUri);
        dispatch(actionCreators.atoms__fetchUnloadedAtom(senderHolderUri));
      }
    };

    const ensureTargetAtomIsFetched = () => {
      if (isTargetAtomFetchNecessary) {
        console.debug("fetch targetAtomUri, ", targetAtomUri);
        dispatch(actionCreators.atoms__fetchUnloadedAtom(targetAtomUri));
      }
    };

    const ensureTargetHolderIsFetched = () => {
      if (isTargetHolderFetchNecessary) {
        console.debug("fetch targetHolderUri, ", targetHolderUri);
        dispatch(actionCreators.atoms__fetchUnloadedAtom(targetHolderUri));
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
        .filter(atom => getUri(atom) !== atomUri).size > 0;

    const showAddBuddyElement =
      atomUtils.hasBuddySocket(atom) &&
      hasOwnedAtomsWithBuddySocket &&
      !accountUtils.isAtomOwned(accountState, atomUri);

    const generateAtomActionButton = () => {
      const isInactive = atomUtils.isInactive(atom);
      if (ownedConnection || isInactive) {
        const connectionState = get(ownedConnection, "state");
        const senderSocketType = atomUtils.getSocketType(
          senderAtom,
          connectionUtils.getSocketUri(ownedConnection)
        );
        const targetSocketType = atomUtils.getSocketType(
          targetAtom,
          connectionUtils.getTargetSocketUri(ownedConnection)
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

    const holderName =
      atomUtils.hasHoldableSocket(atom) && !atomUtils.hasGroupSocket(atom)
        ? atomUtils.getTitle(holderAtom, externalDataState) ||
          get(atom, "fakePersonaName")
        : undefined;

    const holderNameElement = holderName && (
      <span className="ahb__info__holder">{holderName}</span>
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
        {(groupChatElement || holderNameElement) && (
          <div className="ahb__info">
            {groupChatElement}
            {holderNameElement}
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
            (isHolderFetchNecessary || isAtomFetchNecessary
              ? " won-to-load "
              : "")
      }
    >
      {contentElement}
    </won-atom-header-big>
  );
}
WonAtomHeaderBig.propTypes = {
  atomUri: PropTypes.string.isRequired,
  atom: PropTypes.object,
  ownedConnectionUri: PropTypes.string,
  ownedConnection: PropTypes.object,
  showActions: PropTypes.bool.isRequired,
  toggleActions: PropTypes.func.isRequired,
};
