/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { get, getIn } from "../utils.js";

import * as processUtils from "../redux/utils/process-utils.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import WonOtherCard from "./cards/other-card.jsx";
import WonSkeletonCard from "./cards/skeleton-card.jsx";
import WonPersonaCard from "./cards/persona-card.jsx";
import WonInterestCard from "./cards/interest-card.jsx";
import PokemonRaidCard from "./cards/pokemon-raid-card.jsx";
import PropTypes from "prop-types";
import { useSelector } from "react-redux";

import "~/style/_atom-card.scss";

export default function WonAtomCard({
  atom,
  showHolder,
  showIndicators,
  currentLocation,
}) {
  const processState = useSelector(generalSelectors.getProcessState);
  const atomUri = get(atom, "uri");
  const isSkeleton =
    get(atom, "isBeingCreated") ||
    processUtils.hasAtomFailedToLoad(processState, atomUri) ||
    processUtils.isAtomFetchNecessary(processState, atomUri, atom);

  const matchedUseCase = getIn(atom, ["matchedUseCase", "identifier"]);

  let cardContent;

  if (isSkeleton) {
    cardContent = (
      <WonSkeletonCard
        atom={atom}
        atomUri={atomUri}
        processState={processState}
        showIndicators={showIndicators}
        showHolder={showHolder}
      />
    );
  } else {
    // We already know that we have the atom thats why we can push it directly into the card-components
    switch (matchedUseCase) {
      case "persona":
        cardContent = <WonPersonaCard atom={atom} />;
        break;
      case "pokemonGoRaid":
        cardContent = (
          <PokemonRaidCard
            atom={atom}
            showIndicators={showIndicators}
            showHolder={showHolder}
            currentLocation={currentLocation}
          />
        );
        break;
      case "lunchInterest":
      case "cyclingInterest":
      case "pokemonInterest":
      case "genericInterest":
        cardContent = (
          <WonInterestCard
            atom={atom}
            showIndicators={showIndicators}
            showHolder={showHolder}
          />
        );
        break;
      default:
        cardContent = (
          <WonOtherCard
            atom={atom}
            showIndicators={showIndicators}
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
  atom: PropTypes.object,
  showHolder: PropTypes.bool,
  showIndicators: PropTypes.bool,
  currentLocation: PropTypes.object,
};
