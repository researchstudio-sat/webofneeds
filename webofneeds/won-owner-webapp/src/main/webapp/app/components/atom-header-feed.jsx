/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { Link } from "react-router-dom";
import { getUri } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";

import WonAtomIcon from "./atom-icon.jsx";
import VisibilitySensor from "react-visibility-sensor";
import PropTypes from "prop-types";
import { useDispatch, useSelector } from "react-redux";

import "~/style/_atom-header-feed.scss";

export default function WonAtomHeaderFeed({
  atom,
  toLink,
  onClick,
  className,
}) {
  const dispatch = useDispatch();
  const atomUri = getUri(atom);

  const externalDataState = useSelector(generalSelectors.getExternalDataState);
  const processState = useSelector(generalSelectors.getProcessState);

  const atomLoading =
    !atom || processUtils.isAtomLoading(processState, atomUri);
  const atomToLoad = !atom || processUtils.isAtomToLoad(processState, atomUri);
  const atomFailedToLoad =
    atom && processUtils.hasAtomFailedToLoad(processState, atomUri);

  const title = atomUtils.getTitle(atom, externalDataState);

  function ensureAtomIsFetched() {
    if (isAtomFetchNecessary) {
      console.debug("fetch atomUri, ", atomUri);
      dispatch(actionCreators.atoms__fetchUnloadedAtom(atomUri));
    }
  }

  function onChange(isVisible) {
    if (isVisible) {
      ensureAtomIsFetched();
    }
  }

  let atomHeaderContent;
  let atomHeaderIcon;

  const isAtomFetchNecessary = processUtils.isAtomFetchNecessary(
    processState,
    atomUri,
    atom
  );

  if (isAtomFetchNecessary || atomLoading) {
    //Loading View

    atomHeaderIcon = <div className="ahf__icon__skeleton" />;
    atomHeaderContent = (
      <VisibilitySensor
        onChange={onChange}
        intervalDelay={200}
        partialVisibility={true}
        offset={{ top: -300, bottom: -300 }}
      >
        <div className="ahf__right">
          <div className="ahf__right__topline">
            <div className="ahf__right__topline__title" />
          </div>
        </div>
      </VisibilitySensor>
    );
  } else if (atomUtils.isBeingCreated(atom)) {
    //In Creation View
    atomHeaderIcon = <WonAtomIcon atom={atom} />;
    atomHeaderContent = (
      <div className="ahf__right">
        <div className="ahf__right__topline">
          <div className="ahf__right__topline__notitle">Creating...</div>
        </div>
      </div>
    );
  } else if (atomFailedToLoad) {
    const isAtomDeleted = processUtils.isAtomDeleted(processState, atomUri);

    //FailedToLoad View
    atomHeaderIcon = <WonAtomIcon atom={atom} />;
    atomHeaderContent = (
      <div className="ahf__right">
        <div className="ahf__right__topline">
          <div className="ahf__right__topline__notitle">
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
      </div>
    );
  } else {
    //Normal View
    atomHeaderIcon = <WonAtomIcon atom={atom} />;
    atomHeaderContent = (
      <div className="ahf__right">
        <div className="ahf__right__topline">
          <div className="ahf__right__topline__title">
            {title ? title : "No Title"}
          </div>
        </div>
      </div>
    );
  }

  return toLink ? (
    <Link
      className={
        "won-atom-header-feed " +
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
    <won-atom-header-feed
      class={
        (atomLoading ? " won-is-loading " : "") +
        (isAtomFetchNecessary ? " won-to-load " : "") +
        (onClick ? " clickable " : "") +
        (className ? " " + className + " " : "")
      }
      onClick={onClick}
    >
      {atomHeaderIcon}
      {atomHeaderContent}
    </won-atom-header-feed>
  );
}
WonAtomHeaderFeed.propTypes = {
  atom: PropTypes.object,
  toLink: PropTypes.string,
  onClick: PropTypes.func,
  className: PropTypes.string,
};
