/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { useSelector } from "react-redux";
import { get, getIn, generateLink } from "../../utils.js";
import PropTypes from "prop-types";

import WonAtomMap from "../atom-map.jsx";
import WonAtomConnectionsIndicator from "../atom-connections-indicator.jsx";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import { relativeTime } from "../../won-label-utils.js";
import { selectLastUpdateTime } from "../../redux/selectors/general-selectors.js";

import "~/style/_other-card.scss";
import { Link } from "react-router-dom";

export default function WonOtherCard({
  atom,
  showSuggestions,
  showHolder,
  currentLocation,
}) {
  const atomUri = get(atom, "uri");
  const useCaseIcon = atomUtils.getMatchedUseCaseIcon(atom);
  const iconBackground = atomUtils.getBackground(atom);
  const identiconSvg = !useCaseIcon
    ? atomUtils.getIdenticonSvg(atom)
    : undefined;
  const isDirectResponse = atomUtils.isDirectResponseAtom(atom);
  const responseToUri =
    isDirectResponse && getIn(atom, ["content", "responseToUri"]);
  const responseToAtom = useSelector(state =>
    getIn(state, ["atoms", responseToUri])
  );
  const atomImage = atomUtils.getDefaultImage(atom);
  const atomLocation = atomUtils.getLocation(atom);
  const holderUri = atomUtils.getHeldByUri(atom);
  const holder = useSelector(state => getIn(state, ["atoms", holderUri]));
  const holderName = get(holder, "humanReadable");
  const holderVerified = atomUtils.isHolderVerified(atom, holder);
  const isHolderPersona = atomUtils.isPersona(holder);
  const personaIdenticonSvg = atomUtils.getIdenticonSvg(holder);
  const personaImage = atomUtils.getDefaultPersonaImage(holder);
  const showHolderIcon = !isHolderPersona;
  const holderUseCaseIcon = !isHolderPersona
    ? atomUtils.getMatchedUseCaseIcon(holder)
    : undefined;
  const holderUseCaseIconBackground = !isHolderPersona
    ? atomUtils.getBackground(holder)
    : undefined;
  const isInactive = atomUtils.isInactive(atom);
  const holderWebsite = getIn(holder, ["content", "website"]);
  const atomTypeLabel = atomUtils.generateTypeLabel(atom);
  const atomHasHoldableSocket = atomUtils.hasHoldableSocket(atom);
  const isGroupChatEnabled = atomUtils.hasGroupSocket(atom);
  const isChatEnabled = atomUtils.hasChatSocket(atom);
  const globalLastUpdateTime = useSelector(selectLastUpdateTime);
  const friendlyTimestamp =
    atom && relativeTime(globalLastUpdateTime, get(atom, "lastUpdateDate"));
  const showPersonaImage = isHolderPersona && !!personaImage;
  const showPersonaIdenticon =
    isHolderPersona && !personaImage && !!personaIdenticonSvg;
  const showMap = false; //!atomImage && atomLocation; //if no image is present but a location is, we display a map instead
  const showDefaultIcon = !atomImage; //&& !atomLocation; //if no image and no location are present we display the defaultIcon in the card__icon area, instead of next to the title

  function createCardMainSubtitle() {
    const createGroupChatLabel = () => {
      if (isGroupChatEnabled) {
        return (
          <span className="card__main__subtitle__type__groupchat">
            {"Group Chat" + (isChatEnabled ? " enabled" : "")}
          </span>
        );
      }
      return undefined;
    };

    return (
      <div className="card__main__subtitle">
        <span className="card__main__subtitle__type">
          {createGroupChatLabel()}
          <span>{atomTypeLabel}</span>
        </span>
        <div className="card__main__subtitle__date">{friendlyTimestamp}</div>
      </div>
    );
  }

  function createCardMainTopline() {
    const hasTitle = () => {
      if (isDirectResponse && responseToAtom) {
        return !!get(responseToAtom, "humanReadable");
      } else {
        return !!get(atom, "humanReadable");
      }
    };

    const generateTitleString = () => {
      if (isDirectResponse && responseToAtom) {
        return "Re: " + get(responseToAtom, "humanReadable");
      } else {
        return get(atom, "humanReadable");
      }
    };

    const generateCardTitle = () => {
      if (hasTitle()) {
        return (
          <div className="card__main__topline__title">
            {generateTitleString()}
          </div>
        );
      } else {
        if (isDirectResponse) {
          return <div className="card__main__topline__notitle">no title</div>;
        } else {
          return (
            <div className="card__main__topline__notitle">Re: no title</div>
          );
        }
      }
    };

    return <div className="card__main__topline">{generateCardTitle()}</div>;
  }

  function createCardMainIcon() {
    if (!showDefaultIcon) {
      const style =
        !atomImage && iconBackground
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
  }

  function createPersonaWebsite() {
    if (holderWebsite) {
      return (
        <React.Fragment>
          <div className="card__persona__websitelabel">Website:</div>,
          <a
            className="card__persona__websitelink"
            target="_blank"
            rel="noopener noreferrer"
            href={holderWebsite}
          >
            {holderWebsite}
          </a>
        </React.Fragment>
      );
    }
  }
  function createVerificationLabel() {
    if (holderVerified) {
      return (
        <span
          className="card__persona__name__verification card__persona__name__verification--verified"
          title="The Persona-Relation of this Post is verified by the Persona"
        >
          Verified
        </span>
      );
    } else {
      return (
        <span
          className="card__persona__name__verification card__persona__name__verification--unverified"
          title="The Persona-Relation of this Post is NOT verified by the Persona"
        >
          Unverified!
        </span>
      );
    }
  }

  function createHolderInfoIcon() {
    if (showHolderIcon) {
      const style = {
        backgroundColor: holderUseCaseIconBackground,
      };

      return (
        <div style={style} className="card__persona__icon holderUseCaseIcon">
          <svg className="si__serviceatomicon">
            <use xlinkHref={holderUseCaseIcon} href={holderUseCaseIcon} />
          </svg>
        </div>
      );
    } else if (showPersonaIdenticon) {
      return (
        <img
          className="card__persona__icon"
          alt="Auto-generated title image for persona that holds the atom"
          src={"data:image/svg+xml;base64," + personaIdenticonSvg}
        />
      );
    }
    if (showPersonaImage) {
      return (
        <img
          className="card__persona__icon"
          alt={get(personaImage, "name")}
          src={
            "data:" +
            get(personaImage, "encodingFormat") +
            ";base64," +
            get(personaImage, "encoding")
          }
        />
      );
    }
  }

  const style =
    showDefaultIcon && iconBackground
      ? {
          backgroundColor: iconBackground,
        }
      : undefined;

  const cardIcon = (
    <Link
      className={
        "card__icon " +
        (isInactive ? " inactive " : "") +
        (showMap ? "card__icon--map" : "")
      }
      to={location =>
        generateLink(location, { postUri: atomUri, tab: "DETAIL" }, "/post")
      }
      style={style}
    >
      {showDefaultIcon && useCaseIcon ? (
        <div className="identicon usecaseimage">
          <svg>
            <use xlinkHref={useCaseIcon} href={useCaseIcon} />
          </svg>
        </div>
      ) : (
        undefined
      )}
      {showDefaultIcon && identiconSvg ? (
        <img
          className="identicon"
          alt="Auto-generated title image"
          src={"data:image/svg+xml;base64," + identiconSvg}
        />
      ) : (
        undefined
      )}
      {atomImage ? (
        <img
          className="image"
          alt={get(atomImage, "name")}
          src={
            "data:" +
            get(atomImage, "encodingFormat") +
            ";base64," +
            get(atomImage, "encoding")
          }
        />
      ) : (
        undefined
      )}
      {showMap ? (
        <WonAtomMap
          className="location"
          locations={[atomLocation]}
          currentLocation={currentLocation}
          disableControls={true}
        />
      ) : (
        undefined
      )}
    </Link>
  );

  const cardMain = (
    <Link
      className={
        "card__main clickable " +
        (!showDefaultIcon ? "card__main--showIcon" : "")
      }
      to={location =>
        generateLink(location, { postUri: atomUri, tab: "DETAIL" }, "/post")
      }
    >
      {createCardMainIcon()}
      {createCardMainTopline()}
      {createCardMainSubtitle()}
    </Link>
  );

  const cardPersonaInfo =
    showHolder && holder && atomHasHoldableSocket ? (
      <Link
        className="card__persona clickable"
        to={location =>
          generateLink(location, { postUri: holderUri, tab: "DETAIL" }, "/post")
        }
      >
        {createHolderInfoIcon()}
        {holderName ? (
          <div className="card__persona__name">
            <span className="card__persona__name__label">{holderName}</span>
            {createVerificationLabel()}
          </div>
        ) : (
          undefined
        )}
        {createPersonaWebsite()}
      </Link>
    ) : (
      undefined
    );

  const cardConnectionIndicators = showSuggestions ? (
    <div className="card__indicators">
      <WonAtomConnectionsIndicator atom={atom} />
    </div>
  ) : (
    undefined
  );

  return (
    <won-other-card>
      {cardIcon}
      {cardMain}
      {cardPersonaInfo}
      {cardConnectionIndicators}
    </won-other-card>
  );
}

WonOtherCard.propTypes = {
  atom: PropTypes.object.isRequired,
  showHolder: PropTypes.bool,
  showSuggestions: PropTypes.bool,
  currentLocation: PropTypes.object,
};
