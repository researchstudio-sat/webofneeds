import React, { useEffect } from "react";

import PropTypes from "prop-types";
import WonAtomCard from "../../atom-card.jsx";
import { useSelector, useDispatch } from "react-redux";
import * as generalSelectors from "../../../redux/selectors/general-selectors.js";
import * as atomUtils from "../../../redux/utils/atom-utils.js";
import { actionCreators } from "../../../actions/actions.js";

import "~/style/_suggest-atom-viewer.scss";
import * as processUtils from "../../../redux/utils/process-utils.js";

export default function WonSuggestAtomViewer({ content, detail, className }) {
  const dispatch = useDispatch();

  const suggestedAtomUri = content;
  const suggestedAtom = useSelector(generalSelectors.getAtom(suggestedAtomUri));

  const processState = useSelector(generalSelectors.getProcessState);

  const isLoading = processUtils.isAtomLoading(processState, suggestedAtomUri);
  const toLoad = processUtils.isAtomToLoad(processState, suggestedAtomUri);
  const failedToLoad = processUtils.hasAtomFailedToLoad(
    processState,
    suggestedAtomUri
  );

  const currentLocation = useSelector(generalSelectors.getCurrentLocation);

  useEffect(() => {
    if (suggestedAtomUri && ((toLoad && !isLoading) || !suggestedAtom)) {
      dispatch(actionCreators.atoms__fetchUnloadedAtom(suggestedAtomUri));
    }
  });

  function getInfoText() {
    if (isLoading || toLoad) {
      return "Loading Atom...";
    } else if (failedToLoad) {
      return "Failed to load Atom";
    } else if (atomUtils.isInactive(suggestedAtom)) {
      return "This Atom is inactive";
    }

    return "Click on the Icon to view Details";
  }

  const icon = detail.icon && (
    <svg className="suggestatomv__header__icon">
      <use xlinkHref={detail.icon} href={detail.icon} />
    </svg>
  );

  const label = detail.icon && (
    <span className="suggestatomv__header__label">{detail.label}</span>
  );

  return (
    <won-suggest-atom-viewer class={className}>
      <div className="suggestatomv__header">
        {icon}
        {label}
      </div>
      <div className="suggestatomv__content">
        <div className="suggestatomv__content__element">
          <div className="suggestatomv__content__element__post">
            <WonAtomCard
              atom={suggestedAtom}
              currentLocation={currentLocation}
              showHolder={true}
              showIndicators={false}
            />
          </div>
          <div className="suggestatomv__content__element__info">
            {getInfoText()}
          </div>
        </div>
      </div>
    </won-suggest-atom-viewer>
  );
}
WonSuggestAtomViewer.propTypes = {
  content: PropTypes.string, //atomUri in this case
  detail: PropTypes.object,
  className: PropTypes.string,
};
