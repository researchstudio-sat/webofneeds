import React from "react";
import PropTypes from "prop-types";
import Immutable from "immutable";
import { generateLink, get, getIn } from "~/app/utils";
import { Link } from "react-router-dom";
import * as atomUtils from "~/app/redux/utils/atom-utils";
import WonAtomMap from "~/app/components/atom-map";
import SwipeableViews from "react-swipeable-views";
import { autoPlay } from "react-swipeable-views-utils";
import { details } from "~/config/detail-definitions";

import "~/style/_content-snippet.scss";

const AutoPlaySwipeableViews = autoPlay(SwipeableViews);

export function generateSwipeableContent(
  atom,
  externalDataState,
  currentLocation
) {
  const swipeableContent = [];

  const eventObjectAboutUris = getIn(atom, ["content", "eventObjectAboutUris"]);
  const classifiedAs = getIn(atom, ["content", "classifiedAs"]);
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

  const generateWikiDataImages = uris => {
    const externalDataMap =
      uris &&
      uris.map(uri => get(externalDataState, uri)).filter(data => !!data);

    externalDataMap &&
      externalDataMap.map(data => {
        const wikiDataImageUrl = get(data, "imageUrl");
        wikiDataImageUrl && wikiDataImageUrls.push(wikiDataImageUrl);
      });
  };

  generateWikiDataImages(eventObjectAboutUris);
  generateWikiDataImages(classifiedAs);
  wikiDataImageUrls.map((url, index) =>
    swipeableContent.push(
      <img key={url + "-" + index} className="image" src={url} />
    )
  );

  const pokemonId = getIn(atom, ["content", "pokemonRaid", "id"]);
  const pokemonForm = getIn(atom, ["content", "pokemonRaid", "form"]);
  const pokemon =
    pokemonId &&
    details.pokemonRaid &&
    details.pokemonRaid.findPokemonById &&
    details.pokemonRaid.findPokemonById(pokemonId, pokemonForm);
  const pokemonImageUrl = pokemon && pokemon.imageUrl;
  pokemonImageUrl &&
    swipeableContent.push(
      <img
        key={"pkm"}
        className="image"
        alt={pokemonImageUrl}
        src={pokemonImageUrl}
      />
    );

  const atomImageUrl = atomUtils.getImageUrl(atom);
  atomImageUrl &&
    swipeableContent.push(
      <img
        key={"imgUrl"}
        className="image"
        alt={atomImageUrl}
        src={atomImageUrl}
      />
    );

  const contentImages = atomUtils.getImages(atom);
  contentImages &&
    contentImages.map(image => {
      swipeableContent.push(
        <img
          className="image"
          alt={get(image, "name")}
          src={
            "data:" +
            get(image, "encodingFormat") +
            ";base64," +
            get(image, "encoding")
          }
        />
      );
    });

  const seeksImages = atomUtils.getSeeksImages(atom);
  seeksImages &&
    seeksImages.map(image => {
      swipeableContent.push(
        <img
          className="image"
          alt={get(image, "name")}
          src={
            "data:" +
            get(image, "encodingFormat") +
            ";base64," +
            get(image, "encoding")
          }
        />
      );
    });

  const atomLocation = atomUtils.getLocation(atom);
  atomLocation &&
    swipeableContent.push(
      <WonAtomMap
        key="location"
        className="location"
        locations={[atomLocation]}
        currentLocation={currentLocation}
        disableControls={true}
      />
    );

  return swipeableContent;
}

export default function WonContentSnippet({ atom, swipeableContent }) {
  const atomUri = get(atom, "uri");
  const isInactive = atomUtils.isInactive(atom);
  const iconBackground = atomUtils.getBackground(atom);
  const useCaseIcon = atomUtils.getMatchedUseCaseIcon(atom);
  const identiconSvg = !useCaseIcon
    ? atomUtils.getIdenticonSvg(atom)
    : undefined;

  const style = iconBackground
    ? {
        backgroundColor: iconBackground,
      }
    : undefined;

  let iconContent;
  if (swipeableContent.length === 0) {
    iconContent = useCaseIcon ? (
      <div className="identicon usecaseimage">
        <svg>
          <use xlinkHref={useCaseIcon} href={useCaseIcon} />
        </svg>
      </div>
    ) : (
      <img
        className="identicon"
        alt="Auto-generated title image"
        src={"data:image/svg+xml;base64," + identiconSvg}
      />
    );
  } else if (swipeableContent.length === 1) {
    iconContent = swipeableContent;
  } else {
    iconContent = (
      <AutoPlaySwipeableViews>{swipeableContent}</AutoPlaySwipeableViews>
    );
  }

  return (
    <Link
      className={
        "card__detailinfo " +
        (isInactive ? " inactive " : "") +
        (swipeableContent.length > 0 ? " card__detailinfo--map " : "")
      }
      to={location =>
        generateLink(
          location,
          { postUri: atomUri, tab: undefined, connectionUri: undefined },
          "/post"
        )
      }
      style={swipeableContent.length === 0 ? style : undefined}
    >
      {iconContent}
    </Link>
  );
}
WonContentSnippet.propTypes = {
  atom: PropTypes.object.isRequired,
  swipeableContent: PropTypes.array.isRequired,
};
