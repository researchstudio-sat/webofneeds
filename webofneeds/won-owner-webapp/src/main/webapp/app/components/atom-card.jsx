/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { get, getIn } from "../utils.js";

import * as atomUtils from "../redux/utils/atom-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";
import WonOtherCard from "./cards/other-card.jsx";
import WonSkeletonCard from "./cards/skeleton-card.jsx";
import WonPersonaCard from "./cards/persona-card.jsx";
import PropTypes from "prop-types";
import { connect } from "react-redux";

import "~/style/_atom-card.scss";

const mapStateToProps = (state, ownProps) => {
  const atom = getIn(state, ["atoms", ownProps.atomUri]);
  const isPersona = atomUtils.isPersona(atom);
  const process = get(state, "process");
  const isSkeleton =
    !(
      processUtils.isAtomLoaded(process, ownProps.atomUri) &&
      !get(atom, "isBeingCreated")
    ) ||
    get(atom, "isBeingCreated") ||
    processUtils.hasAtomFailedToLoad(process, ownProps.atomUri) ||
    processUtils.isAtomLoading(process, ownProps.atomUri) ||
    processUtils.isAtomToLoad(process, ownProps.atomUri);

  if (isSkeleton) {
    return {
      atomUri: ownProps.atomUri,
      showPersona: ownProps.showPersona,
      showSuggestions: ownProps.showSuggestions,
      currentLocation: ownProps.currentLocation,
      onAtomClick: ownProps.onAtomClick,
      isPersona: false,
      isOtherAtom: false,
      isSkeleton: true,
    };
  } else if (isPersona) {
    return {
      atomUri: ownProps.atomUri,
      showPersona: ownProps.showPersona,
      showSuggestions: ownProps.showSuggestions,
      currentLocation: ownProps.currentLocation,
      onAtomClick: ownProps.onAtomClick,
      isPersona: true,
      isOtherAtom: false,
      isSkeleton: false,
    };
  } else {
    return {
      atomUri: ownProps.atomUri,
      showPersona: ownProps.showPersona,
      showSuggestions: ownProps.showSuggestions,
      currentLocation: ownProps.currentLocation,
      onAtomClick: ownProps.onAtomClick,
      isPersona: false,
      isOtherAtom: true,
      isSkeleton: false,
    };
  }
};

class WonAtomCard extends React.Component {
  render() {
    if (this.props.isSkeleton) {
      return (
        <won-atom-card>
          <WonSkeletonCard
            atomUri={this.props.atomUri}
            showSuggestions={this.props.showSuggestions}
            showPersona={this.props.showPersona}
          />
        </won-atom-card>
      );
    } else if (this.props.isPersona) {
      return (
        <won-atom-card>
          <WonPersonaCard
            atomUri={this.props.atomUri}
            onAtomClick={this.props.onAtomClick}
          />
        </won-atom-card>
      );
    } else {
      return (
        <won-atom-card>
          <WonOtherCard
            atomUri={this.props.atomUri}
            showSuggestions={this.props.showSuggestions}
            showPersona={this.props.showPersona}
            onAtomClick={this.props.onAtomClick}
            currentLocation={this.props.currentLocation}
          />
        </won-atom-card>
      );
    }
  }
}

WonAtomCard.propTypes = {
  atomUri: PropTypes.string.isRequired,
  showPersona: PropTypes.bool,
  showSuggestions: PropTypes.bool,
  currentLocation: PropTypes.object,
  onAtomClick: PropTypes.func,
  isPersona: PropTypes.bool,
  isOtherAtom: PropTypes.bool,
  isSkeleton: PropTypes.bool,
};
export default connect(mapStateToProps)(WonAtomCard);
