import React from "react";
import { useSelector } from "react-redux";
import PropTypes from "prop-types";
import { Link } from "react-router-dom";
import { generateLink, get } from "~/app/utils";
import * as atomUtils from "~/app/redux/utils/atom-utils";
import * as generalSelectors from "~/app/redux/selectors/general-selectors";
import { relativeTime } from "~/app/won-label-utils";

import "~/style/_main-snippet.scss";

export default function WonMainSnippet({ atom, showIcon, externalDataState }) {
  const atomUri = get(atom, "uri");

  const isGroupChatEnabled = atomUtils.hasGroupSocket(atom);
  const isChatEnabled = atomUtils.hasChatSocket(atom);

  const atomTypeLabel = atomUtils.generateTypeLabel(atom);
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

  function createCardMainIcon() {
    if (showIcon) {
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

  return (
    <Link
      className={
        "card__main clickable " + (showIcon ? "card__main--showIcon" : "")
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
}
WonMainSnippet.propTypes = {
  atom: PropTypes.object.isRequired,
  externalDataState: PropTypes.object.isRequired,
  showIcon: PropTypes.bool,
};
