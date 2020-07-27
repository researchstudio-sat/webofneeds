import React from "react";
import {
  generateSimpleTransitionLabel,
  get,
  getQueryParams,
} from "../utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";
import { actionCreators } from "../actions/actions.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import { useSelector, useDispatch } from "react-redux";

import PropTypes from "prop-types";

import "~/style/_petrinet-state.scss";
import ico_loading_anim from "~/images/won-icons/ico_loading_anim.svg";
import { useHistory } from "react-router-dom";
import * as connectionUtils from "~/app/redux/utils/connection-utils";

export default function WonPetrinetState({ processUri, className }) {
  const history = useHistory();
  const dispatch = useDispatch();

  const { connectionUri } = getQueryParams(history.location); //TODO: create selector that returns the correct connectionUri without looking up the open one
  const atom = useSelector(
    generalSelectors.getOwnedAtomByConnectionUri(connectionUri)
  );
  const processState = useSelector(generalSelectors.getProcessState);
  const connection = atomUtils.getConnection(atom, connectionUri);

  const petriNetData = connectionUtils.getPetriNetData(connection);

  const process = processUri && get(petriNetData, processUri);

  const petriNetDataLoading = processUtils.isConnectionPetriNetDataLoading(
    processState,
    connectionUri
  );

  const petriNetDataLoaded = processUtils.isConnectionPetriNetDataLoaded(
    processState,
    connectionUri
  );
  const petriNetDataDirty = processUtils.isConnectionPetriNetDataDirty(
    processState,
    connectionUri
  );

  const markedPlaces = get(process, "markedPlaces");
  const enabledTransitions = get(process, "enabledTransitions");

  const markedPlacesSize = markedPlaces ? markedPlaces.size : 0;
  const enabledTransitionsSize = enabledTransitions
    ? enabledTransitions.size
    : 0;

  const multiSelectType = connectionUtils.getMultiSelectType(connection);
  const hasEnabledTransitions = enabledTransitionsSize > 0;
  const hasMarkedPlaces = markedPlacesSize > 0;
  const enabledTransitionsArray =
    enabledTransitions && enabledTransitions.toArray();
  const markedPlacesArray = markedPlaces && markedPlaces.toArray();

  function sendClaim(transitionUri) {
    if (transitionUri && processUri && connectionUri) {
      console.debug(
        "send transition 'claim' ",
        transitionUri,
        " for processUri: ",
        processUri
      );

      const senderSocketUri = connectionUtils.getSocketUri(connection);
      const targetSocketUri = connectionUtils.getTargetSocketUri(connection);

      dispatch(
        actionCreators.connections__sendChatMessageClaimOnSuccess(
          undefined,
          new Map().set("petriNetTransition", {
            petriNetUri: processUri,
            transitionUri: transitionUri,
          }),
          senderSocketUri,
          targetSocketUri,
          connectionUri
        )
      );
    }
  }

  const petrinetStateLoadingElement = petriNetDataLoaded &&
    (petriNetDataLoading || petriNetDataDirty) && (
      <div className="ps__loading">
        <svg className="ps__loading__spinner">
          <use xlinkHref={ico_loading_anim} href={ico_loading_anim} />
        </svg>
        <div className="ps__loading__label">
          The PetriNet-State, is currently being calculated
        </div>
      </div>
    );

  const petrinetInactiveElement = !process &&
    !petriNetDataLoading &&
    petriNetDataLoaded && (
      <div className="ps__inactive">This PetriNet, is not active (yet).</div>
    );

  let petrinetActiveElement;

  if (process && (petriNetDataLoaded || !petriNetDataLoading)) {
    let markedPlacesElement;

    if (hasMarkedPlaces) {
      markedPlacesElement = markedPlacesArray.map((markedPlace, index) => {
        return (
          <div
            className="ps__active__markedPlace"
            key={index + "-" + markedPlace}
          >
            {generateSimpleTransitionLabel(markedPlace)}
          </div>
        );
      });
    } else {
      markedPlacesElement = (
        <div className="ps__active__noMarkedPlace">
          No Marked Places in PetriNet
        </div>
      );
    }

    let enabledTransitionsElement;

    if (hasEnabledTransitions) {
      enabledTransitionsElement = enabledTransitionsArray.map(
        (enabledTransition, index) => {
          return (
            <div
              className="ps__active__enabledTransition"
              key={index + "-" + enabledTransition}
            >
              <div className="ps__active__enabledTransition__label">
                {generateSimpleTransitionLabel(enabledTransition)}
              </div>
              {/*The button is labelled 'send' at the moment because we jsut send the transition but not claim it right away*/}
              <button
                className="ps__active__enabledTransition__button won-button--filled thin secondary"
                disabled={multiSelectType || petriNetDataDirty}
                onClick={() => {
                  sendClaim(enabledTransition);
                }}
              >
                Claim
              </button>
            </div>
          );
        }
      );
    } else {
      enabledTransitionsElement = (
        <div className="ps__active__noEnabledTransition">
          No Enabled Transitions in PetriNet
        </div>
      );
    }

    petrinetActiveElement = (
      <div className="ps__active">
        <div className="ps__active__header">Marked Places</div>
        {markedPlacesElement}
        <div className="ps__active__header">Enabled Transitions</div>
        {enabledTransitionsElement}
      </div>
    );
  }

  return (
    <won-petrinet-state class={className ? className : ""}>
      {petrinetActiveElement}
      {petrinetInactiveElement}
      {petrinetStateLoadingElement}
    </won-petrinet-state>
  );
}
WonPetrinetState.propTypes = {
  processUri: PropTypes.string.isRequired,
  className: PropTypes.string,
};
