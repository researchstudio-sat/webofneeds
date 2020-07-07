/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { useSelector } from "react-redux";
import { get, getIn, generateLink } from "../../utils.js";
import PropTypes from "prop-types";
import Immutable from "immutable";

import WonAtomConnectionsIndicator from "../atom-connections-indicator.jsx";
import WonHolderSnippet from "./snippets/holder-snippet.jsx";
import WonAtomMap from "../atom-map.jsx";
import SwipeableViews from "react-swipeable-views";
import { autoPlay } from "react-swipeable-views-utils";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import { relativeTime } from "../../won-label-utils.js";

import "~/style/_interest-card.scss";
import { Link } from "react-router-dom";

const AutoPlaySwipeableViews = autoPlay(SwipeableViews);

export default function WonInterestCard({
  atom,
  showIndicators,
  showHolder,
  currentLocation,
}) {
  const atomUri = get(atom, "uri");
  const useCaseIcon = atomUtils.getMatchedUseCaseIcon(atom);
  const iconBackground = atomUtils.getBackground(atom);
  const identiconSvg = !useCaseIcon
    ? atomUtils.getIdenticonSvg(atom)
    : undefined;

  const atomLocation = atomUtils.getLocation(atom);
  const holderUri = atomUtils.getHeldByUri(atom);
  const holder = useSelector(generalSelectors.getAtom(holderUri));
  const isInactive = atomUtils.isInactive(atom);
  const atomTypeLabel = atomUtils.generateTypeLabel(atom);
  const atomHasHoldableSocket = atomUtils.hasHoldableSocket(atom);
  const externalDataState = useSelector(generalSelectors.getExternalDataState);
  const eventObjectAboutUris = getIn(atom, ["content", "eventObjectAboutUris"]);

  const externalDataMap = eventObjectAboutUris
    ? eventObjectAboutUris
        .map(uri => get(externalDataState, uri))
        .filter(data => !!data)
    : Immutable.Map();

  const wikiDataImageUrls = [];
  externalDataMap.map(data => {
    const wikiDataImageUrl = get(data, "imageUrl");
    wikiDataImageUrl && wikiDataImageUrls.push(wikiDataImageUrl);
  });

  const globalLastUpdateTime = useSelector(
    generalSelectors.selectLastUpdateTime
  );
  const friendlyTimestamp =
    atom && relativeTime(globalLastUpdateTime, get(atom, "lastUpdateDate"));

  function createCardMainSubtitle() {
    return (
      <div className="card__main__subtitle">
        <span className="card__main__subtitle__type">
          <span>{atomTypeLabel}</span>
        </span>
        <div className="card__main__subtitle__date">{friendlyTimestamp}</div>
      </div>
    );
  }

  function createCardMainTopline() {
    const title = atomUtils.getTitle(atom, externalDataState);

    return (
      <div className="card__main__topline">
        {title ? (
          <div className="card__main__topline__title">{title}</div>
        ) : (
          <div className="card__main__topline__notitle">No Title</div>
        )}
      </div>
    );
  }

  function createCardMainIcon() {
    const style = iconBackground
      ? {
          backgroundColor: iconBackground,
        }
      : undefined;

    return (
      <div className="card__main__icon" style={style}>
        {useCaseIcon ? (
          <div className="card__main__icon__usecaseimage">
            <svg>
              <use xlinkHref={useCaseIcon} href={useCaseIcon} />
            </svg>
          </div>
        ) : (
          undefined
        )}
        {identiconSvg ? (
          <img
            className="card__main__icon__identicon"
            alt="Auto-generated title image"
            src={"data:image/svg+xml;base64," + identiconSvg}
          />
        ) : (
          undefined
        )}
      </div>
    );
  }

  const cardIcon = (
    <Link
      className={
        "card__icon card__icon--map " + (isInactive ? " inactive " : "")
      }
      to={location =>
        generateLink(
          location,
          { postUri: atomUri, tab: undefined, connectionUri: undefined },
          "/post"
        )
      }
    >
      <AutoPlaySwipeableViews>
        {wikiDataImageUrls.map((url, index) => (
          <img key={url + "-" + index} className="image" src={url} />
        ))}
        {atomLocation && (
          <WonAtomMap
            className="location"
            locations={[atomLocation]}
            currentLocation={currentLocation}
            disableControls={true}
          />
        )}
      </AutoPlaySwipeableViews>
    </Link>
  );

  const cardMain = (
    <Link
      className="card__main clickable card__main--showIcon"
      to={location =>
        generateLink(
          location,
          { postUri: atomUri, tab: undefined, connectionUri: undefined },
          "/post"
        )
      }
    >
      {createCardMainIcon()}
      {createCardMainTopline()}
      {createCardMainSubtitle()}
    </Link>
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
      {cardIcon}
      {cardMain}
      {showHolder &&
        holder &&
        atomHasHoldableSocket && (
          <WonHolderSnippet holder={holder} heldAtom={atom} />
        )}
      {cardConnectionIndicators}
    </won-interest-card>
  );
}

WonInterestCard.propTypes = {
  atom: PropTypes.object.isRequired,
  showHolder: PropTypes.bool,
  showIndicators: PropTypes.bool,
  currentLocation: PropTypes.object,
};
