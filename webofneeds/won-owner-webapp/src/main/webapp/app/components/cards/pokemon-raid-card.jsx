/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { Link } from "react-router-dom";
import { useSelector } from "react-redux";
import { get, generateLink } from "../../utils.js";
import PropTypes from "prop-types";

import WonAtomConnectionsIndicator from "../atom-connections-indicator.jsx";
import WonHolderSnippet from "./snippets/holder-snippet.jsx";
import WonContentSnippet from "./snippets/content-snippet.jsx";

import * as atomUtils from "../../redux/utils/atom-utils.js";
import { relativeTime } from "../../won-label-utils.js";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";

import "~/style/_pokemon-raid-card.scss";

export default function PokemonRaidCard({
  atom,
  showIndicators,
  showHolder,
  currentLocation,
}) {
  const atomUri = get(atom, "uri");
  const holderUri = atomUtils.getHeldByUri(atom);
  const holder = useSelector(generalSelectors.getAtom(holderUri));

  const atomTypeLabel = atomUtils.generateTypeLabel(atom);
  const atomHasHoldableSocket = atomUtils.hasHoldableSocket(atom);
  const isGroupChatEnabled = atomUtils.hasGroupSocket(atom);
  const isChatEnabled = atomUtils.hasChatSocket(atom);
  const externalDataState = useSelector(generalSelectors.getExternalDataState);
  const globalLastUpdateTime = useSelector(
    generalSelectors.selectLastUpdateTime
  );
  const friendlyTimestamp =
    atom && relativeTime(globalLastUpdateTime, get(atom, "lastUpdateDate"));

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

  //FIXME currently we always show the icon, we just need to implement a way where we only show it when we have swipeable-content (see content-snippet, maybe with local state)
  const showsContent = true;

  function createCardMainIcon() {
    if (showsContent) {
      const useCaseIcon = atomUtils.getMatchedUseCaseIcon(atom);
      const identiconSvg = !useCaseIcon
        ? atomUtils.getIdenticonSvg(atom)
        : undefined;

      const iconBackground = atomUtils.getBackground(atom);
      const style = iconBackground
        ? {
            backgroundColor: iconBackground,
          }
        : undefined;

      const icon = useCaseIcon ? (
        <div className="card__main__icon__usecaseimage">
          <svg>
            <use xlinkHref={useCaseIcon} href={useCaseIcon} />
          </svg>
        </div>
      ) : (
        <img
          className="card__main__icon__identicon"
          alt="Auto-generated title image"
          src={"data:image/svg+xml;base64," + identiconSvg}
        />
      );

      return (
        <div className="card__main__icon" style={style}>
          {icon}
        </div>
      );
    }
    return undefined;
  }

  const cardMain = (
    <Link
      className={
        "card__main clickable " + (showsContent ? "card__main--showIcon" : "")
      }
      to={location =>
        generateLink(
          location,
          { postUri: atomUri, connectionUri: undefined, tab: undefined },
          "/post"
        )
      }
    >
      {createCardMainIcon()}
      {createCardMainTopline()}
      {createCardMainSubtitle()}
    </Link>
  );

  const cardSuggestionIndicators = showIndicators ? (
    <div className="card__indicators">
      <WonAtomConnectionsIndicator atom={atom} />
    </div>
  ) : (
    undefined
  );

  return (
    <pokemon-raid-card>
      <WonContentSnippet atom={atom} currentLocation={currentLocation} />
      {cardMain}
      {showHolder &&
        holder &&
        atomHasHoldableSocket && (
          <WonHolderSnippet holder={holder} heldAtom={atom} />
        )}
      {cardSuggestionIndicators}
    </pokemon-raid-card>
  );
}

PokemonRaidCard.propTypes = {
  atom: PropTypes.object.isRequired,
  showHolder: PropTypes.bool,
  showIndicators: PropTypes.bool,
  currentLocation: PropTypes.object,
};
