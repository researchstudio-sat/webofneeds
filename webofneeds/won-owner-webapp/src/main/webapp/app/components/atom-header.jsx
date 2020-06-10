/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { Link } from "react-router-dom";
import { get, getIn } from "../utils.js";
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

export default function WonAtomHeader({
  atom,
  hideTimestamp,
  toLink,
  onClick,
  className,
}) {
  const dispatch = useDispatch();
  const atomUri = get(atom, "uri");
  const isDirectResponse = atomUtils.isDirectResponseAtom(atom);
  const responseToUri =
    isDirectResponse && getIn(atom, ["content", "responseToUri"]);
  const responseToAtom = useSelector(generalSelectors.getAtom(responseToUri));

  const personaUri = atomUtils.getHeldByUri(atom);
  const persona = useSelector(generalSelectors.getAtom(personaUri));
  const personaName = get(persona, "humanReadable");

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
    relativeTime(globalUpdateTimestamp, get(atom, "lastUpdateDate"));

  function ensureAtomIsLoaded() {
    if (atomUri && (!atom || (atomToLoad && !atomLoading))) {
      dispatch(actionCreators.atoms__fetchUnloadedAtom(atomUri));
    }
  }

  function hasTitle() {
    if (isDirectResponse && responseToAtom) {
      return !!get(responseToAtom, "humanReadable");
    } else {
      return !!get(atom, "humanReadable");
    }
  }

  function generateTitle() {
    if (isDirectResponse && responseToAtom) {
      return "Re: " + get(responseToAtom, "humanReadable");
    } else {
      return get(atom, "humanReadable");
    }
  }

  function onChange(isVisible) {
    if (isVisible) {
      ensureAtomIsLoaded();
    }
  }

  let atomHeaderContent;
  let atomHeaderIcon;

  if (atomLoading) {
    //Loading View

    atomHeaderIcon = <div className="ph__icon__skeleton" />;
    atomHeaderContent = (
      <div className="ph__right">
        <div className="ph__right__topline">
          <div className="ph__right__topline__title" />
        </div>
        <div className="ph__right__subtitle">
          <span className="ph__right__subtitle__type" />
        </div>
      </div>
    );
  } else if (get(atom, "isBeingCreated")) {
    //In Creation View
    atomHeaderIcon = <WonAtomIcon atom={atom} />;
    atomHeaderContent = (
      <div className="ph__right">
        <div className="ph__right__topline">
          <div className="ph__right__topline__notitle">Creating...</div>
        </div>
        <div className="ph__right__subtitle">
          <span className="ph__right__subtitle__type">
            {personaName ? (
              <span className="ph__right__subtitle__type__persona">
                {personaName}
              </span>
            ) : (
              undefined
            )}
            {isGroupChatEnabled ? (
              <span className="ph__right__subtitle__type__groupchat">
                {isChatEnabled ? "Group Chat enabled" : "Group Chat"}
              </span>
            ) : (
              undefined
            )}
            <span className="ph__right__subtitle__type">{atomTypeLabel}</span>
          </span>
        </div>
      </div>
    );
  } else if (atomFailedToLoad) {
    //FailedToLoad View
    atomHeaderIcon = <WonAtomIcon atom={atom} />;
    atomHeaderContent = (
      <div className="ph__right">
        <div className="ph__right__topline">
          <div className="ph__right__topline__notitle">Atom Loading failed</div>
        </div>
        <div className="ph__right__subtitle">
          <span className="ph__right__subtitle__type">
            Atom might have been deleted.
          </span>
        </div>
      </div>
    );
  } else {
    //Normal View
    atomHeaderIcon = <WonAtomIcon atom={atom} />;
    atomHeaderContent = (
      <div className="ph__right">
        <div className="ph__right__topline">
          <div className="ph__right__topline__title">
            {hasTitle()
              ? generateTitle()
              : isDirectResponse
                ? "Re: No Title"
                : "No Title"}
          </div>
        </div>
        <div className="ph__right__subtitle">
          <span className="ph__right__subtitle__type">
            {personaName ? (
              <span className="ph__right__subtitle__type__persona">
                {personaName}
              </span>
            ) : (
              undefined
            )}
            {isGroupChatEnabled ? (
              <span className="ph__right__subtitle__type__groupchat">
                {isChatEnabled ? "Group Chat enabled" : "Group Chat"}
              </span>
            ) : (
              undefined
            )}
            <span>{atomTypeLabel}</span>
          </span>
          {!hideTimestamp && (
            <div className="ph__right__subtitle__date">{friendlyTimestamp}</div>
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
      <VisibilitySensor
        onChange={onChange}
        intervalDelay={200}
        partialVisibility={true}
        offset={{ top: -300, bottom: -300 }}
      >
        {atomHeaderContent}
      </VisibilitySensor>
    </Link>
  ) : (
    <won-atom-header
      className={
        (atomLoading ? " won-is-loading " : "") +
        (atomToLoad ? " won-to-load " : "") +
        (onClick ? " clickable " : "") +
        (className ? " " + className + " " : "")
      }
      onClick={onClick}
    >
      {atomHeaderIcon}
      <VisibilitySensor
        onChange={onChange}
        intervalDelay={200}
        partialVisibility={true}
        offset={{ top: -300, bottom: -300 }}
      >
        {atomHeaderContent}
      </VisibilitySensor>
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
