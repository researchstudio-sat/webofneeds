import React from "react";
import PropTypes from "prop-types";
import { generateLink, get, getUri } from "~/app/utils";
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

  // Add WikiData Images from eventObjectAboutUris and classifiedAs
  const generateWikiDataImages = uris => {
    const externalDataMap =
      uris &&
      uris.map(uri => get(externalDataState, uri)).filter(data => !!data);

    externalDataMap &&
      externalDataMap.map((data, key) => {
        const wikiDataImageUrl = get(data, "imageUrl");
        wikiDataImageUrl &&
          swipeableContent.push(
            <img
              key={key + "-" + wikiDataImageUrl}
              className="image"
              src={wikiDataImageUrl}
            />
          );
      });
  };
  const atomContent = atomUtils.getContent(atom);
  generateWikiDataImages(get(atomContent, "eventObjectAboutUris"));
  generateWikiDataImages(get(atomContent, "classifiedAs"));

  // Add Pokemon Image if pokemonRaid detail is set
  const pokemonRaid = get(atomContent, "pokemonRaid");
  const pokemonId = get(pokemonRaid, "id");
  const pokemon =
    pokemonId &&
    details.pokemonRaid &&
    details.pokemonRaid.findPokemonById &&
    details.pokemonRaid.findPokemonById(pokemonId, get(pokemonRaid, "form"));
  const pokemonImageUrl = pokemon && pokemon.imageUrl;
  pokemonImageUrl &&
    swipeableContent.push(
      <img
        key={"pkm"}
        className="image pkmimg"
        alt={pokemonImageUrl}
        src={pokemonImageUrl}
      />
    );

  // Add Image from ImageUrl if ImageUrl is in content
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

  // Add Images if Images are present in content
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

  // Add Images if Images are present in seeks
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

  // Add Map if location is present
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
  const atomUri = getUri(atom);
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
