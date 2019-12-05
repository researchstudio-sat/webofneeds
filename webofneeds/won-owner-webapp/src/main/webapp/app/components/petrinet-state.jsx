import React from "react";
import { generateSimpleTransitionLabel, get, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import { connect } from "react-redux";

import PropTypes from "prop-types";

import "~/style/_petrinet-state.scss";

const mapStateToProps = (state, ownProps) => {
  const connectionUri = generalSelectors.getConnectionUriFromRoute(state); //TODO: create selector that returns the correct connectionUri without looking up the open one
  const atom =
    connectionUri &&
    generalSelectors.getOwnedAtomByConnectionUri(state, connectionUri);
  const connection = atom && atom.getIn(["connections", connectionUri]);

  const petriNetData = get(connection, "petriNetData");

  const process = ownProps.processUri && get(petriNetData, ownProps.processUri);

  const petriNetDataLoading =
    connection &&
    getIn(state, [
      "process",
      "connections",
      connectionUri,
      "petriNetData",
      "loading",
    ]);
  const petriNetDataLoaded =
    petriNetData &&
    getIn(state, [
      "process",
      "connections",
      connectionUri,
      "petriNetData",
      "loaded",
    ]);
  const petriNetDataDirty =
    petriNetData &&
    getIn(state, [
      "process",
      "connections",
      connectionUri,
      "petriNetData",
      "dirty",
    ]);
  const markedPlaces = get(process, "markedPlaces");
  const enabledTransitions = get(process, "enabledTransitions");

  const markedPlacesSize = markedPlaces ? markedPlaces.size : 0;
  const enabledTransitionsSize = enabledTransitions
    ? enabledTransitions.size
    : 0;

  return {
    className: ownProps.className,
    processUri: ownProps.processUri,
    connection: connection,
    connectionUri: connectionUri,
    multiSelectType: get(connection, "multiSelectType"),
    petriNetData: petriNetData,
    process: process,
    hasEnabledTransitions: enabledTransitionsSize > 0,
    hasMarkedPlaces: markedPlacesSize > 0,
    enabledTransitionsArray: enabledTransitions && enabledTransitions.toArray(),
    markedPlacesArray: markedPlaces && markedPlaces.toArray(),
    petriNetDataDirty: petriNetDataDirty,
    petriNetDataLoading: petriNetDataLoading,
    petriNetDataLoaded: petriNetDataLoaded,
  };
};

const mapDispatchToProps = dispatch => {
  return {
    sendChatMessageClaimOnSuccess: (
      processUri,
      transitionUri,
      senderSocketUri,
      targetSocketUri
    ) => {
      dispatch(
        actionCreators.connections__sendChatMessageClaimOnSuccess(
          undefined,
          new Map().set("petriNetTransition", {
            petriNetUri: processUri,
            transitionUri: transitionUri,
          }),
          senderSocketUri,
          targetSocketUri
        )
      );
    },
  };
};

class WonPetrinetState extends React.Component {
  render() {
    const petrinetStateLoadingElement = this.props.petriNetDataLoaded &&
      (this.props.petriNetDataLoading || this.props.petriNetDataDirty) && (
        <div className="ps__loading">
          <svg className="ps__loading__spinner">
            <use xlinkHref="#ico_loading_anim" href="#ico_loading_anim" />
          </svg>
          <div className="ps__loading__label">
            The PetriNet-State, is currently being calculated
          </div>
        </div>
      );

    const petrinetInactiveElement = !this.props.process &&
      !this.props.petriNetDataLoading &&
      this.props.petriNetDataLoaded && (
        <div className="ps__inactive">This PetriNet, is not active (yet).</div>
      );

    let petrinetActiveElement;

    if (
      this.props.process &&
      (this.props.petriNetDataLoaded || !this.props.petriNetDataLoading)
    ) {
      let markedPlacesElement;

      if (this.props.hasMarkedPlaces) {
        markedPlacesElement = this.props.markedPlacesArray.map(
          (markedPlace, index) => {
            return (
              <div
                className="ps__active__markedPlace"
                key={index + "-" + markedPlace}
              >
                {generateSimpleTransitionLabel(markedPlace)}
              </div>
            );
          }
        );
      } else {
        markedPlacesElement = (
          <div className="ps__active__noMarkedPlace">
            No Marked Places in PetriNet
          </div>
        );
      }

      let enabledTransitionsElement;

      if (this.props.hasEnabledTransitions) {
        enabledTransitionsElement = this.props.enabledTransitionsArray.map(
          (enabledTransition, index) => {
            <div
              className="ps__active__enabledTransition"
              key={index + "-" + enabledTransition}
            >
              <div className="ps__active__enabledTransition__label">
                {generateSimpleTransitionLabel(enabledTransition)}
              </div>
              {/*The button is labelled 'send' at the moment because we jsut send the transition but not claim it right away*/}
              <button
                className="ps__active__enabledTransition__button won-button--filled thin red"
                disabled={
                  this.props.multiSelectType || this.props.petriNetDataDirty
                }
                onClick={() => {
                  this.sendClaim(enabledTransition);
                }}
              >
                Claim
              </button>
            </div>;
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
      <won-petrinet-state
        class={this.props.className ? this.props.className : ""}
      >
        {petrinetActiveElement}
        {petrinetInactiveElement}
        {petrinetStateLoadingElement}
      </won-petrinet-state>
    );
  }

  sendClaim(transitionUri) {
    if (transitionUri && this.props.processUri && this.props.connectionUri) {
      console.debug(
        "send transition 'claim' ",
        transitionUri,
        " for processUri: ",
        this.props.processUri
      );

      const senderSocketUri = get(this.props.connection, "socketUri");
      const targetSocketUri = get(this.props.connection, "targetSocketUri");

      this.props.sendChatMessageClaimOnSuccess(
        this.props.processUri,
        transitionUri,
        senderSocketUri,
        targetSocketUri
      );
    }
  }
}
WonPetrinetState.propTypes = {
  processUri: PropTypes.string.isRequired,
  className: PropTypes.string,
  connection: PropTypes.object,
  connectionUri: PropTypes.string,
  multiSelectType: PropTypes.string,
  petriNetData: PropTypes.object,
  process: PropTypes.object,
  hasEnabledTransitions: PropTypes.bool,
  hasMarkedPlaces: PropTypes.bool,
  enabledTransitionsArray: PropTypes.array,
  markedPlacesArray: PropTypes.array,
  petriNetDataDirty: PropTypes.bool,
  petriNetDataLoading: PropTypes.bool,
  petriNetDataLoaded: PropTypes.bool,
  sendChatMessageClaimOnSuccess: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonPetrinetState);
