/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { Link } from "react-router-dom";
import { getUri } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { relativeTime } from "../won-label-utils.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";

import "~/style/_atom-header.scss";
import WonAtomIcon from "./atom-icon.jsx";
import VisibilitySensor from "react-visibility-sensor";
import PropTypes from "prop-types";
import { useDispatch, useSelector } from "react-redux";
import vocab from "~/app/service/vocab";

export default function WonAtomHeader({
  atom,
  hideTimestamp,
  toLink,
  onClick,
  className,
}) {
  const dispatch = useDispatch();
  const atomUri = getUri(atom);

  const holderUri = atomUtils.getHeldByUri(atom);
  const externalDataState = useSelector(generalSelectors.getExternalDataState);
  const holderAtom = useSelector(generalSelectors.getAtom(holderUri));
  const holderName = atomUtils.getTitle(holderAtom, externalDataState);

  //Used for Role Atoms
  const orgUri = atomUtils.getOrganizationUriForRole(atom);
  const orgAtom = useSelector(generalSelectors.getAtom(orgUri));
  //------

  const processState = useSelector(generalSelectors.getProcessState);

  const atomTypeLabel = atom && atomUtils.generateTypeLabel(atom);
  const atomLoading =
    !atom || processUtils.isAtomLoading(processState, atomUri);
  const atomToLoad = !atom || processUtils.isAtomToLoad(processState, atomUri);
  const atomFailedToLoad =
    atom && processUtils.hasAtomFailedToLoad(processState, atomUri);
  const isGroupChatEnabled = atomUtils.hasGroupSocket(atom);
  const isChatEnabled = atomUtils.hasChatSocket(atom);
  const globalUpdateTimestamp = useSelector(
    generalSelectors.selectLastUpdateTime
  );
  const friendlyTimestamp =
    !hideTimestamp &&
    atom &&
    relativeTime(globalUpdateTimestamp, atomUtils.getLastUpdateDate(atom));

  const title = atomUtils.getTitle(atom, externalDataState);

  function ensureAtomIsFetched() {
    if (isAtomFetchNecessary) {
      console.debug("fetch atomUri, ", atomUri);
      dispatch(actionCreators.atoms__fetchUnloadedAtom(atomUri));
    }
  }

  function ensureHolderIsFetched() {
    if (isHolderFetchNecessary) {
      console.debug("fetch holderUri, ", holderUri);
      dispatch(actionCreators.atoms__fetchUnloadedAtom(holderUri));
    }
  }

  function ensureOrgIsFetched() {
    if (isOrgFetchNecessary) {
      console.debug("fetch orgUri, ", orgUri);
      dispatch(actionCreators.atoms__fetchUnloadedAtom(orgUri));
    }
  }

  function onChange(isVisible) {
    if (isVisible) {
      ensureAtomIsFetched();
      ensureHolderIsFetched();
      ensureOrgIsFetched();
    }
  }

  let atomHeaderContent;
  let atomHeaderIcon;

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
  const isOrgFetchNecessary = processUtils.isAtomFetchNecessary(
    processState,
    orgUri,
    orgAtom
  );

  if (
    isAtomFetchNecessary ||
    isHolderFetchNecessary ||
    isOrgFetchNecessary ||
    atomLoading
  ) {
    //Loading View

    atomHeaderIcon = <div className="ah__icon__skeleton" />;
    atomHeaderContent = (
      <VisibilitySensor
        onChange={onChange}
        intervalDelay={200}
        partialVisibility={true}
        offset={{ top: -300, bottom: -300 }}
      >
        <div className="ah__right">
          <div className="ah__right__topline">
            <div className="ah__right__topline__title" />
          </div>
          <div className="ah__right__subtitle">
            <span className="ah__right__subtitle__type" />
          </div>
        </div>
      </VisibilitySensor>
    );
  } else if (atomUtils.isBeingCreated(atom)) {
    //In Creation View
    atomHeaderIcon = <WonAtomIcon atom={atom} />;
    atomHeaderContent = (
      <div className="ah__right">
        <div className="ah__right__topline">
          <div className="ah__right__topline__notitle">Creating...</div>
        </div>
        <div className="ah__right__subtitle">
          <span className="ah__right__subtitle__type">
            {holderName ? (
              <span className="ah__right__subtitle__type__holder">
                {holderName}
              </span>
            ) : (
              undefined
            )}
            {isGroupChatEnabled ? (
              <span className="ah__right__subtitle__type__groupchat">
                {isChatEnabled ? "Group Chat enabled" : "Group Chat"}
              </span>
            ) : (
              undefined
            )}
            <span className="ah__right__subtitle__type">{atomTypeLabel}</span>
          </span>
        </div>
      </div>
    );
  } else if (atomFailedToLoad) {
    const isAtomDeleted = processUtils.isAtomDeleted(processState, atomUri);

    //FailedToLoad View
    atomHeaderIcon = <WonAtomIcon atom={atom} />;
    atomHeaderContent = (
      <div className="ah__right">
        <div className="ah__right__topline">
          <div className="ah__right__topline__notitle">
            {isAtomDeleted
              ? "Atom deleted"
              : processUtils.areAtomRequestsAccessDeniedOnly(
                  processState,
                  atomUri
                )
                ? "Atom Access Denied"
                : "Atom Loading failed"}
          </div>
        </div>
        <div className="ah__right__subtitle">
          <span className="ah__right__subtitle__type" />
        </div>
      </div>
    );
  } else if (atomUtils.isRole(atom)) {
    //Role View
    const connectedMembersSize = atomUtils.getConnectedConnections(
      atom,
      vocab.WXSCHEMA.MemberSocketCompacted
    ).size;
    const orgName = atomUtils.getTitle(orgAtom, externalDataState);

    atomHeaderIcon = <WonAtomIcon atom={atom} />;
    atomHeaderContent = (
      <div className="ah__right">
        <div className="ah__right__topline">
          <div className="ah__right__topline__title">
            {title ? title : "No Title"}
          </div>
        </div>
        <div className="ah__right__subtitle">
          <span className="ah__right__subtitle__type">
            {connectedMembersSize > 0 ? (
              <span className="ah__right__subtitle__type__holder">{`${connectedMembersSize} Member${
                connectedMembersSize !== 1 ? "s" : ""
              }`}</span>
            ) : (
              <span>0 Members</span>
            )}
          </span>
          {orgName && (
            <div className="ah__right__subtitle__date">{`Role of: ${orgName}`}</div>
          )}
        </div>
      </div>
    );
  } else {
    //Normal View
    atomHeaderIcon = <WonAtomIcon atom={atom} />;
    atomHeaderContent = (
      <div className="ah__right">
        <div className="ah__right__topline">
          <div className="ah__right__topline__title">
            {title ? title : "No Title"}
          </div>
        </div>
        <div className="ah__right__subtitle">
          <span className="ah__right__subtitle__type">
            {holderName ? (
              <span className="ah__right__subtitle__type__holder">
                {holderName}
              </span>
            ) : (
              undefined
            )}
            {isGroupChatEnabled ? (
              <span className="ah__right__subtitle__type__groupchat">
                {isChatEnabled ? "Group Chat enabled" : "Group Chat"}
              </span>
            ) : (
              undefined
            )}
            <span>{atomTypeLabel}</span>
          </span>
          {!hideTimestamp && (
            <div className="ah__right__subtitle__date">{friendlyTimestamp}</div>
          )}
        </div>
      </div>
    );
  }

  return toLink ? (
    <Link
      className={
        "won-atom-header " +
        (atomLoading ? " won-is-loading " : "") +
        (atomToLoad ? " won-to-load " : "") +
        (className ? " " + className + " " : "")
      }
      to={toLink}
    >
      {atomHeaderIcon}
      {atomHeaderContent}
    </Link>
  ) : (
    <won-atom-header
      class={
        (atomLoading ? " won-is-loading " : "") +
        (isAtomFetchNecessary || isHolderFetchNecessary || isOrgFetchNecessary
          ? " won-to-load "
          : "") +
        (onClick ? " clickable " : "") +
        (className ? " " + className + " " : "")
      }
      onClick={onClick}
    >
      {atomHeaderIcon}
      {atomHeaderContent}
    </won-atom-header>
  );
}
WonAtomHeader.propTypes = {
  atom: PropTypes.object,
  hideTimestamp: PropTypes.bool,
  toLink: PropTypes.string,
  onClick: PropTypes.func,
  className: PropTypes.string,
};
