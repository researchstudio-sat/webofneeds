/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import VisibilitySensor from "react-visibility-sensor";
import WonAtomConnectionsIndicator from "../atom-connections-indicator.jsx";
import * as processUtils from "../../redux/utils/process-utils.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import { actionCreators } from "../../actions/actions.js";
import { useDispatch } from "react-redux";

import "~/style/_skeleton-card.scss";

export default function WonSkeletonCard({
  atomUri,
  processState,
  atom,
  showHolder,
  showIndicators,
}) {
  const dispatch = useDispatch();

  const atomInCreation = atomUtils.isBeingCreated(atom);
  const atomFailedToLoad = processUtils.hasAtomFailedToLoad(
    processState,
    atomUri
  );
  const atomToLoad = processUtils.isAtomToLoad(processState, atomUri) || !atom;

  const isAtomFetchNecessary =
    !atomInCreation &&
    processUtils.isAtomFetchNecessary(processState, atomUri, atom);

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

  const cardIconSkeleton = isAtomFetchNecessary ? (
    <VisibilitySensor
      onChange={onChange}
      intervalDelay={200}
      partialVisibility={true}
      delayedCall={true}
      offset={{ top: -300, bottom: -300 }}
    >
      <div className="card__detailinfo__skeleton" />
    </VisibilitySensor>
  ) : (
    <div className="card__detailinfo__skeleton" />
  );

  const cardMain = atomFailedToLoad ? (
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
    <div className="card__main">
      <div className="card__main__topline">
        <div className="card__main__topline__title" />
      </div>
      <div className="card__main__subtitle">
        <span className="card__main__subtitle__type" />
      </div>
    </div>
  );

  const cardPersona = showHolder ? (
    <div className="card__noholder" />
  ) : (
    undefined
  );

  const cardIndicators = showIndicators ? (
    <WonAtomConnectionsIndicator />
  ) : (
    undefined
  );

  return (
    <won-skeleton-card
      class={
        (isAtomFetchNecessary || atomInCreation ? " won-is-loading " : "") +
        (atomToLoad ? "won-is-toload" : "")
      }
    >
      {cardIconSkeleton}
      {cardMain}
      {cardPersona}
      {cardIndicators}
    </won-skeleton-card>
  );
}
WonSkeletonCard.propTypes = {
  atomUri: PropTypes.string.isRequired,
  processState: PropTypes.object.isRequired,
  atom: PropTypes.object,
  showHolder: PropTypes.bool,
  showIndicators: PropTypes.bool,
};
