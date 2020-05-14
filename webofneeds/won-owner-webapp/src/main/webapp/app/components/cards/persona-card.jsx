/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import { get, generateLink } from "../../utils.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";

import "~/style/_persona-card.scss";
import { Link } from "react-router-dom";

export default function WonPersonaCard({ atom }) {
  const identiconSvg = atomUtils.getIdenticonSvg(atom);
  const atomImage = atomUtils.getDefaultPersonaImage(atom);
  const isInactive = atomUtils.isInactive(atom);
  const personaName = get(atom, "humanReadable");
  const showDefaultIcon = !atomImage;

  const personaIdenticon =
    showDefaultIcon && identiconSvg ? (
      <img
        className="identicon"
        alt="Auto-generated title image"
        src={"data:image/svg+xml;base64," + identiconSvg}
      />
    ) : (
      undefined
    );

  const personaImage = atomImage ? (
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
  );

  return (
    <Link
      className="won-persona-card"
      to={location =>
        generateLink(
          location,
          { postUri: get(atom, "uri"), tab: "DETAIL" },
          "/post"
        )
      }
    >
      <div className={"card__icon clickable " + (isInactive ? "inactive" : "")}>
        {personaIdenticon}
        {personaImage}
      </div>
      <div className="card__main clickable">
        <div className="card__main__name">{personaName}</div>
      </div>
    </Link>
  );
}

WonPersonaCard.propTypes = {
  atom: PropTypes.object.isRequired,
};
