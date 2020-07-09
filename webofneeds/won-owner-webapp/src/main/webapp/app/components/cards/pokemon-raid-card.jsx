/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { useSelector } from "react-redux";
import PropTypes from "prop-types";

import WonAtomConnectionsIndicator from "../atom-connections-indicator.jsx";
import WonHolderSnippet from "./snippets/holder-snippet.jsx";
import WonMainSnippet from "./snippets/main-snippet.jsx";
import WonContentSnippet, {
  generateSwipeableContent,
} from "./snippets/content-snippet.jsx";

import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";

import "~/style/_pokemon-raid-card.scss";

export default function PokemonRaidCard({
  atom,
  showIndicators,
  showHolder,
  currentLocation,
}) {
  const holderUri = atomUtils.getHeldByUri(atom);
  const holder = useSelector(generalSelectors.getAtom(holderUri));
  const atomHasHoldableSocket = atomUtils.hasHoldableSocket(atom);
  const externalDataState = useSelector(generalSelectors.getExternalDataState);

  const swipeableContent = generateSwipeableContent(
    atom,
    externalDataState,
    currentLocation
  );

  const cardConnectionIndicators = showIndicators ? (
    <div className="card__indicators">
      <WonAtomConnectionsIndicator atom={atom} />
    </div>
  ) : (
    undefined
  );

  return (
    <pokemon-raid-card>
      <WonContentSnippet atom={atom} swipeableContent={swipeableContent} />
      <WonMainSnippet
        atom={atom}
        externalDataState={externalDataState}
        showIcon={swipeableContent && swipeableContent.length > 0}
      />
      {showHolder &&
        holder &&
        atomHasHoldableSocket && (
          <WonHolderSnippet holder={holder} heldAtom={atom} />
        )}
      {cardConnectionIndicators}
    </pokemon-raid-card>
  );
}

PokemonRaidCard.propTypes = {
  atom: PropTypes.object.isRequired,
  showHolder: PropTypes.bool,
  showIndicators: PropTypes.bool,
  currentLocation: PropTypes.object,
};
