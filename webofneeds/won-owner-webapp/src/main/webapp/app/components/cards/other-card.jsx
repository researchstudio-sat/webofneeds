/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { useSelector } from "react-redux";
import { get, generateLink } from "../../utils.js";
import PropTypes from "prop-types";

import WonAtomMap from "../atom-map.jsx";
import WonAtomConnectionsIndicator from "../atom-connections-indicator.jsx";
import WonHolderSnippet from "./snippets/holder-snippet.jsx";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import { relativeTime } from "../../won-label-utils.js";

import "~/style/_other-card.scss";
import { Link } from "react-router-dom";

export default function WonOtherCard({
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
  const atomImage = atomUtils.getDefaultImage(atom);
  const atomLocation = atomUtils.getLocation(atom);
  const holderUri = atomUtils.getHeldByUri(atom);
  const holder = useSelector(generalSelectors.getAtom(holderUri));
  const isInactive = atomUtils.isInactive(atom);
  const atomTypeLabel = atomUtils.generateTypeLabel(atom);
  const atomHasHoldableSocket = atomUtils.hasHoldableSocket(atom);
  const isGroupChatEnabled = atomUtils.hasGroupSocket(atom);
  const isChatEnabled = atomUtils.hasChatSocket(atom);
  const globalLastUpdateTime = useSelector(
    generalSelectors.selectLastUpdateTime
  );
  const friendlyTimestamp =
    atom && relativeTime(globalLastUpdateTime, get(atom, "lastUpdateDate"));

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
    const title = get(atom, "humanReadable");

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
        generateLink(
          location,
          { postUri: atomUri, tab: undefined, connectionUri: undefined },
          "/post"
        )
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
    <won-other-card>
      {cardIcon}
      {cardMain}
      {showHolder &&
        holder &&
        atomHasHoldableSocket && (
          <WonHolderSnippet holder={holder} heldAtom={atom} />
        )}
      {cardConnectionIndicators}
    </won-other-card>
  );
}

WonOtherCard.propTypes = {
  atom: PropTypes.object.isRequired,
  showHolder: PropTypes.bool,
  showIndicators: PropTypes.bool,
  currentLocation: PropTypes.object,
};
