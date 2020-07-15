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

import "~/style/_interest-card.scss";

export default function WonInterestCard({
  atom,
  processState,
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
    <won-interest-card>
      <WonContentSnippet atom={atom} swipeableContent={swipeableContent} />
      <WonMainSnippet
        atom={atom}
        externalDataState={externalDataState}
        showIcon={swipeableContent && swipeableContent.length > 0}
      />
      {showHolder &&
        holderUri &&
        atomHasHoldableSocket && (
          <WonHolderSnippet
            holderUri={holderUri}
            holder={holder}
            heldAtom={atom}
            processState={processState}
          />
        )}
      {cardConnectionIndicators}
    </won-interest-card>
  );
}

WonInterestCard.propTypes = {
  atom: PropTypes.object.isRequired,
  processState: PropTypes.object.isRequired,
  showHolder: PropTypes.bool,
  showIndicators: PropTypes.bool,
  currentLocation: PropTypes.object,
};
