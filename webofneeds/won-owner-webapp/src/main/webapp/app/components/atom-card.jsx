/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { get, getIn } from "../utils.js";

import * as processUtils from "../redux/utils/process-utils.js";
import WonOtherCard from "./cards/other-card.jsx";
import WonSkeletonCard from "./cards/skeleton-card.jsx";
import WonPersonaCard from "./cards/persona-card.jsx";
import PokemonRaidCard from "./cards/pokemon-raid-card.jsx";
import PropTypes from "prop-types";
import { useSelector } from "react-redux";

import "~/style/_atom-card.scss";

export default function WonAtomCard({
  atomUri,
  showHolder,
  showSuggestions,
  currentLocation,
}) {
  const atom = useSelector(state => getIn(state, ["atoms", atomUri]));
  const processState = useSelector(state => get(state, "process"));

  const isSkeleton =
    !(
      processUtils.isAtomLoaded(processState, atomUri) &&
      !get(atom, "isBeingCreated")
    ) ||
    get(atom, "isBeingCreated") ||
    processUtils.hasAtomFailedToLoad(processState, atomUri) ||
    processUtils.isAtomLoading(processState, atomUri) ||
    processUtils.isAtomToLoad(processState, atomUri);

  const matchedUseCase = getIn(atom, ["matchedUseCase", "identifier"]);

  let cardContent;

  if (isSkeleton) {
    cardContent = (
      <WonSkeletonCard
        atomUri={atomUri}
        showSuggestions={showSuggestions}
        showHolder={showHolder}
      />
    );
  } else {
    // We already know that we have the atom thats why we can push it directly into the card-components
    switch (matchedUseCase) {
      case "persona":
        cardContent = <WonPersonaCard atomUri={atomUri} atom={atom} />;
        break;
      case "pokemonGoRaid":
        cardContent = (
          <PokemonRaidCard
            atom={atom}
            showSuggestions={showSuggestions}
            showHolder={showHolder}
            currentLocation={currentLocation}
          />
        );
        break;
      default:
        cardContent = (
          <WonOtherCard
            atom={atom}
            showSuggestions={showSuggestions}
            showHolder={showHolder}
            currentLocation={currentLocation}
          />
        );
        break;
    }
  }

  return <won-atom-card>{cardContent}</won-atom-card>;
}
WonAtomCard.propTypes = {
  atomUri: PropTypes.string.isRequired,
  showHolder: PropTypes.bool,
  showSuggestions: PropTypes.bool,
  currentLocation: PropTypes.object,
};
