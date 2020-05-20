/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { get, getIn } from "../utils.js";

import * as processUtils from "../redux/utils/process-utils.js";
import * as atomUtils from "../redux/utils/atom-utils";
import PropTypes from "prop-types";
import { useSelector } from "react-redux";

export default function WonAtomIcon({ atom, className, onClick }) {
  const isPersona = atomUtils.isPersona(atom);
  const image = isPersona ? atomUtils.getDefaultPersonaImage(atom) : undefined;

  const useCaseIcon = !isPersona
    ? atomUtils.getMatchedUseCaseIcon(atom)
    : undefined;
  const useCaseIconBackground = !isPersona
    ? atomUtils.getBackground(atom)
    : undefined;

  const identiconSvg = !useCaseIcon
    ? atomUtils.getIdenticonSvg(atom)
    : undefined;

  // Icons/Images of the AtomHolder
  const holderUri = atomUtils.getHeldByUri(atom);
  const holder = useSelector(
    state => holderUri && getIn(state, ["atoms", holderUri])
  );
  const holderImage = atomUtils.getDefaultPersonaImage(holder);
  const holderIdenticonSvg = atomUtils.getIdenticonSvg(holder);
  const isHolderPersona = atomUtils.isPersona(holder);
  const showHolderIdenticon =
    isHolderPersona && !holderImage && !!holderIdenticonSvg;
  const showHolderImage = isHolderPersona && holderImage;
  const showHolderIcon = !isHolderPersona;

  const holderUseCaseIcon = !isHolderPersona
    ? atomUtils.getMatchedUseCaseIcon(holder)
    : undefined;
  const holderUseCaseIconBackground = !isHolderPersona
    ? atomUtils.getBackground(holder)
    : undefined;

  const process = useSelector(state => get(state, "process"));
  const atomInactive = atomUtils.isInactive(atom);
  const atomFailedToLoad =
    atom && processUtils.hasAtomFailedToLoad(process, get(atom, "uri"));
  const showIdenticon = !image && !!identiconSvg;
  const showImage = !!image;

  let holderIcon = undefined;
  let atomIcon = undefined;

  if (showHolderIdenticon) {
    holderIcon = (
      <img
        className="holderIcon"
        alt="Auto-generated title image for persona that holds the atom"
        src={"data:image/svg+xml;base64," + holderIdenticonSvg}
      />
    );
  } else if (showHolderImage) {
    holderIcon = (
      <img
        className="holderIcon"
        alt={get(holderImage, "name")}
        src={
          "data:" +
          get(holderImage, "encodingFormat") +
          ";base64," +
          get(holderImage, "encoding")
        }
      />
    );
  } else if (showHolderIcon) {
    const style = {
      backgroundColor: holderUseCaseIconBackground,
    };

    holderIcon = (
      <div style={style} className="holderIcon holderUseCaseIcon">
        <svg className="si__serviceatomicon">
          <use xlinkHref={holderUseCaseIcon} href={holderUseCaseIcon} />
        </svg>
      </div>
    );
  }

  if (showIdenticon) {
    atomIcon = (
      <img
        className="image"
        alt="Auto-generated title icon"
        src={"data:image/svg+xml;base64," + identiconSvg}
      />
    );
  } else if (showImage) {
    atomIcon = (
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
  } else if (useCaseIcon) {
    const style = {
      backgroundColor: useCaseIconBackground,
    };

    atomIcon = (
      <div className="image usecaseimage" style={style}>
        <svg className="si__usecaseicon">
          <use xlinkHref={useCaseIcon} href={useCaseIcon} />
        </svg>
      </div>
    );
  }

  return (
    <won-atom-icon
      class={
        (className ? className : "") +
        " " +
        (isPersona ? " won-is-persona " : "") +
        (atomFailedToLoad ? " won-failed-to-load " : "") +
        (atomInactive ? " inactive " : "") +
        (onClick ? " clickable " : "")
      }
      onClick={onClick}
    >
      {atomIcon}
      {holderIcon}
    </won-atom-icon>
  );
}
WonAtomIcon.propTypes = {
  atom: PropTypes.object.isRequired,
  className: PropTypes.string,
  onClick: PropTypes.func,
};
