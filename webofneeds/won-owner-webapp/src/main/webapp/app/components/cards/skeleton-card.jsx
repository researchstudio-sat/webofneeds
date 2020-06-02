/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React, { useState } from "react";
import VisibilitySensor from "react-visibility-sensor";
import { get } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import { useDispatch, useSelector } from "react-redux";

import "~/style/_skeleton-card.scss";
import * as processUtils from "../../redux/utils/process-utils.js";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import WonAtomConnectionsIndicator from "../atom-connections-indicator.jsx";
import PropTypes from "prop-types";

export default function WonSkeletonCard({
  atomUri,
  showHolder,
  showSuggestions,
}) {
  const dispatch = useDispatch();
  const [localFetchInitiated, setLocalFetchInitiated] = useState(false);
  const atom = useSelector(generalSelectors.getAtom(atomUri));
  const process = useSelector(generalSelectors.getProcessState);

  const atomInCreation = get(atom, "isBeingCreated");
  const atomLoaded =
    processUtils.isAtomLoaded(process, atomUri) && !atomInCreation;
  const atomLoading = processUtils.isAtomLoading(process, atomUri);
  const atomFailedToLoad = processUtils.hasAtomFailedToLoad(process, atomUri);
  const atomToLoad = processUtils.isAtomToLoad(process, atomUri) || !atom;

  function ensureAtomIsLoaded() {
    if (
      atomUri &&
      !atomLoaded &&
      !atomLoading &&
      !atomInCreation &&
      atomToLoad
    ) {
      dispatch(actionCreators.atoms__fetchUnloadedAtom(atomUri));
    }
  }

  function onChange(isVisible) {
    if (isVisible && !localFetchInitiated) {
      ensureAtomIsLoaded();
      setLocalFetchInitiated(true);
    }
  }

  const cardIconSkeleton = !atomLoaded ? (
    <VisibilitySensor
      onChange={isVisible => {
        onChange(isVisible);
      }}
      intervalDelay={200}
      partialVisibility={true}
      delayedCall={true}
      offset={{ top: -300, bottom: -300 }}
    >
      <div className="card__icon__skeleton" />
    </VisibilitySensor>
  ) : (
    undefined
  );

  const cardMainFailed = atomFailedToLoad ? (
    <div className="card__main">
      <div className="card__main__topline">
        <div className="card__main__topline__notitle">Atom Loading failed</div>
      </div>
      <div className="card__main__subtitle">
        <span className="card__main__subtitle__type">
          Atom might have been deleted.
        </span>
      </div>
    </div>
  ) : (
    undefined
  );

  const cardMain =
    atomLoading || atomToLoad || atomInCreation ? (
      <div className="card__main">
        <div className="card__main__topline">
          <div className="card__main__topline__title" />
        </div>
        <div className="card__main__subtitle">
          <span className="card__main__subtitle__type" />
        </div>
      </div>
    ) : (
      undefined
    );

  const cardPersona =
    showHolder && !atomLoaded ? <div className="card__nopersona" /> : undefined;

  const cardSuggestions = showSuggestions ? (
    <WonAtomConnectionsIndicator />
  ) : (
    undefined
  );

  return (
    <won-skeleton-card
      class={
        (atomLoading || atomInCreation ? " won-is-loading " : "") +
        (atomToLoad ? "won-is-toload" : "")
      }
    >
      {cardIconSkeleton}
      {cardMainFailed}
      {cardMain}
      {cardPersona}
      {cardSuggestions}
    </won-skeleton-card>
  );
}
WonSkeletonCard.propTypes = {
  atomUri: PropTypes.string,
  showHolder: PropTypes.bool,
  showSuggestions: PropTypes.bool,
};
