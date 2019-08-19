import React from "react";
import { generateSimpleTransitionLabel, get, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";

import PropTypes from "prop-types";

import "~/style/_petrinet-state.scss";

export default class WonPetrinetState extends React.Component {
  componentDidMount() {
    this.processUri = this.props.processUri;
    this.disconnect = this.props.ngRedux.connect(
      this.selectFromState.bind(this),
      actionCreators
    )(state => {
      this.setState(state);
    });
  }

  componentWillUnmount() {
    this.disconnect();
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    this.processUri = nextProps.processUri;
    this.setState(this.selectFromState(this.props.ngRedux.getState()));
  }

  selectFromState(state) {
    const connectionUri = generalSelectors.getConnectionUriFromRoute(state); //TODO: create selector that returns the correct connectionUri without looking up the open one
    const atom =
      connectionUri &&
      generalSelectors.getOwnedAtomByConnectionUri(state, connectionUri);
    const connection = atom && atom.getIn(["connections", connectionUri]);

    const petriNetData = get(connection, "petriNetData");

    const process = this.processUri && get(petriNetData, this.processUri);

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
      connectionUri: connectionUri,
      multiSelectType: get(connection, "multiSelectType"),
      petriNetData: petriNetData,
      process: process,
      hasEnabledTransitions: enabledTransitionsSize > 0,
      hasMarkedPlaces: markedPlacesSize > 0,
      enabledTransitionsArray:
        enabledTransitions && enabledTransitions.toArray(),
      markedPlacesArray: markedPlaces && markedPlaces.toArray(),
      petriNetDataDirty: petriNetDataDirty,
      petriNetDataLoading: petriNetDataLoading,
      petriNetDataLoaded: petriNetDataLoaded,
    };
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div />;
    }

    const petrinetStateLoadingElement = this.state.petriNetDataLoaded &&
      (this.state.petriNetDataLoading || this.state.petriNetDataDirty) && (
        <div className="ps__loading">
          <svg className="ps__loading__spinner">
            <use xlinkHref="#ico_loading_anim" href="#ico_loading_anim" />
          </svg>
          <div className="ps__loading__label">
            The PetriNet-State, is currently being calculated
          </div>
        </div>
      );

    const petrinetInactiveElement = !this.state.process &&
      !this.state.petriNetDataLoading &&
      this.state.petriNetDataLoaded && (
        <div className="ps__inactive">This PetriNet, is not active (yet).</div>
      );

    let petrinetActiveElement;

    if (
      this.state.process &&
      (this.state.petriNetDataLoaded || !this.state.petriNetDataLoading)
    ) {
      let markedPlacesElement;

      if (this.state.hasMarkedPlaces) {
        markedPlacesElement = this.state.markedPlacesArray.map(
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

      if (this.state.hasEnabledTransitions) {
        enabledTransitionsElement = this.state.enabledTransitionsArray.map(
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
                  this.state.multiSelectType || this.state.petriNetDataDirty
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
    if (transitionUri && this.processUri && this.state.connectionUri) {
      console.debug(
        "send transition 'claim' ",
        transitionUri,
        " for processUri: ",
        this.processUri
      );

      this.props.ngRedux.dispatch(
        actionCreators.connections__sendChatMessageClaimOnSuccess(
          undefined,
          new Map().set("petriNetTransition", {
            petriNetUri: this.processUri,
            transitionUri: transitionUri,
          }),
          this.state.connectionUri
        )
      );
    }
  }
}
WonPetrinetState.propTypes = {
  processUri: PropTypes.string.isRequired,
  ngRedux: PropTypes.object.isRequired,
  className: PropTypes.string,
};
