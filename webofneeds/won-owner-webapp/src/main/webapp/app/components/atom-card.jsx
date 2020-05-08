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
import PokemonRaidCard from "./cards/pokemon-raid-card.jsx";
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

  const isPokemonRaid =
    getIn(atom, ["matchedUseCase", "identifier"]) === "pokemonGoRaid";

  if (isSkeleton) {
    return {
      atomUri: ownProps.atomUri,
      showHolder: ownProps.showHolder,
      showSuggestions: ownProps.showSuggestions,
      currentLocation: ownProps.currentLocation,
      isPersona: false,
      isPokemonRaid: false,
      isOtherAtom: false,
      isSkeleton: true,
    };
  } else if (isPersona) {
    return {
      atomUri: ownProps.atomUri,
      showHolder: ownProps.showHolder,
      showSuggestions: ownProps.showSuggestions,
      currentLocation: ownProps.currentLocation,
      isPersona: true,
      isPokemonRaid: false,
      isOtherAtom: false,
      isSkeleton: false,
    };
  } else if (isPokemonRaid) {
    return {
      atomUri: ownProps.atomUri,
      showHolder: ownProps.showHolder,
      showSuggestions: ownProps.showSuggestions,
      currentLocation: ownProps.currentLocation,
      isPersona: false,
      isPokemonRaid: true,
      isOtherAtom: false,
      isSkeleton: false,
    };
  } else {
    return {
      atomUri: ownProps.atomUri,
      showHolder: ownProps.showHolder,
      showSuggestions: ownProps.showSuggestions,
      currentLocation: ownProps.currentLocation,
      isPersona: false,
      isPokemonRaid: false,
      isOtherAtom: true,
      isSkeleton: false,
    };
  }
};

class WonAtomCard extends React.Component {
  render() {
    let cardContent;
    if (this.props.isSkeleton) {
      cardContent = (
        <WonSkeletonCard
          atomUri={this.props.atomUri}
          showSuggestions={this.props.showSuggestions}
          showHolder={this.props.showHolder}
        />
      );
    } else if (this.props.isPersona) {
      cardContent = <WonPersonaCard atomUri={this.props.atomUri} />;
    } else if (this.props.isPokemonRaid) {
      cardContent = (
        <PokemonRaidCard
          atomUri={this.props.atomUri}
          showSuggestions={this.props.showSuggestions}
          showHolder={this.props.showHolder}
          currentLocation={this.props.currentLocation}
        />
      );
    } else {
      cardContent = (
        <WonOtherCard
          atomUri={this.props.atomUri}
          showSuggestions={this.props.showSuggestions}
          showHolder={this.props.showHolder}
          currentLocation={this.props.currentLocation}
        />
      );
    }

    return <won-atom-card>{cardContent}</won-atom-card>;
  }
}

WonAtomCard.propTypes = {
  atomUri: PropTypes.string.isRequired,
  showHolder: PropTypes.bool,
  showSuggestions: PropTypes.bool,
  currentLocation: PropTypes.object,
  isPersona: PropTypes.bool,
  isPokemonRaid: PropTypes.bool,
  isOtherAtom: PropTypes.bool,
  isSkeleton: PropTypes.bool,
};
export default connect(mapStateToProps)(WonAtomCard);
