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

import "~/style/_atom-tag-header.scss";
import VisibilitySensor from "react-visibility-sensor";
import PropTypes from "prop-types";
import { useDispatch, useSelector } from "react-redux";

export default function WonAtomTagHeader({ atom, toLink, onClick, className }) {
  const dispatch = useDispatch();
  const atomUri = getUri(atom);

  const holderUri = atomUtils.getHeldByUri(atom);
  const externalDataState = useSelector(generalSelectors.getExternalDataState);
  const holderAtom = useSelector(generalSelectors.getAtom(holderUri));
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

  function ensureHolderIsFetched() {
    if (isHolderFetchNecessary) {
      console.debug("fetch holderUri, ", holderUri);
      dispatch(actionCreators.atoms__fetchUnloadedAtom(holderUri));
    }
  }

  function onChange(isVisible) {
    if (isVisible) {
      ensureAtomIsFetched();
      ensureHolderIsFetched();
    }
  }

  let atomHeaderContent;

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

  if (isAtomFetchNecessary || isHolderFetchNecessary) {
    //Loading View

    atomHeaderContent = (
      <VisibilitySensor
        onChange={onChange}
        intervalDelay={200}
        partialVisibility={true}
        offset={{ top: -300, bottom: -300 }}
      >
        <div className="ath__title" />
      </VisibilitySensor>
    );
  } else if (atomUtils.isBeingCreated(atom)) {
    //In Creation View
    atomHeaderContent = <div className="ath__notitle">Creating...</div>;
  } else if (atomFailedToLoad) {
    //FailedToLoad View
    atomHeaderContent = <div className="ath__notitle">Atom Loading failed</div>;
  } else {
    //Normal View
    atomHeaderContent = (
      <div className="ath__title">{title ? title : "No Title"}</div>
    );
  }

  return toLink ? (
    <Link
      className={
        "won-atom-tag-header " +
        (atomLoading ? " won-is-loading " : "") +
        (atomToLoad ? " won-to-load " : "") +
        (className ? " " + className + " " : "")
      }
      to={toLink}
    >
      {atomHeaderContent}
    </Link>
  ) : (
    <won-atom-tag-header
      class={
        (atomLoading ? " won-is-loading " : "") +
        (isAtomFetchNecessary || isHolderFetchNecessary
          ? " won-to-load "
          : "") +
        (onClick ? " clickable " : "") +
        (className ? " " + className + " " : "")
      }
      onClick={onClick}
    >
      {atomHeaderContent}
    </won-atom-tag-header>
  );
}
WonAtomTagHeader.propTypes = {
  atom: PropTypes.object,
  toLink: PropTypes.string,
  onClick: PropTypes.func,
  className: PropTypes.string,
};
