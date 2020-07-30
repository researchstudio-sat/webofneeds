/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { get, getUri } from "../utils.js";

import * as processUtils from "../redux/utils/process-utils.js";
import * as atomUtils from "../redux/utils/atom-utils";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import PropTypes from "prop-types";
import { useSelector } from "react-redux";
import { generateHexColor } from "../utils";

import ico36_person_anon from "~/images/won-icons/ico36_person_anon.svg";
import shajs from "sha.js";

export default function WonAtomIcon({ atom, className, onClick, flipIcons }) {
  const atomUri = getUri(atom);
  const holder = useSelector(
    generalSelectors.getAtom(atomUtils.getHeldByUri(atom))
  );

  let mainIconAtom;
  let subIconAtom;

  if (atomUtils.hasHolderSocket(atom)) {
    mainIconAtom = atom;
    subIconAtom = undefined;
  } else {
    mainIconAtom = flipIcons ? holder : atom;
    subIconAtom = flipIcons ? atom : holder;
  }

  const processState = useSelector(generalSelectors.getProcessState);

  function generateSubIcon(atom) {
    const image = atomUtils.getDefaultImage(atom);

    if (image) {
      return (
        <img
          className={
            "holderIcon " +
            (atomUtils.isPersona(atom) ? "holderIcon--isPersona" : "")
          }
          alt={get(image, "name")}
          src={
            "data:" +
            get(image, "encodingFormat") +
            ";base64," +
            get(image, "encoding")
          }
        />
      );
    } else {
      const isPersona = atomUtils.isPersona(atom);

      const useCaseIcon = !isPersona
        ? atomUtils.getMatchedUseCaseIcon(atom)
        : undefined;

      if (useCaseIcon) {
        return (
          <div
            style={{
              backgroundColor: atomUtils.getBackground(atom),
            }}
            className={
              "holderIcon holderUseCaseIcon " +
              (atomUtils.isPersona(atom) ? "holderIcon--isPersona" : "")
            }
          >
            <svg className="si__serviceatomicon">
              <use xlinkHref={useCaseIcon} href={useCaseIcon} />
            </svg>
          </div>
        );
      } else {
        const identiconSvg = atomUtils.getIdenticonSvg(atom);

        return identiconSvg ? (
          <img
            className={
              "holderIcon " +
              (atomUtils.isPersona(atom) ? "holderIcon--isPersona" : "")
            }
            alt="Auto-generated title icon"
            src={"data:image/svg+xml;base64," + identiconSvg}
          />
        ) : (
          <div
            className="holderIcon holderUseCaseIcon holderIcon--isPersona"
            style={{
              backgroundColor: atomUri
                ? generateHexColor(
                    new shajs.sha512().update(atomUri).digest("hex")
                  )
                : "black",
            }}
          >
            <svg className="si__serviceatomicon">
              <use xlinkHref={ico36_person_anon} href={ico36_person_anon} />
            </svg>
          </div>
        );
      }
    }
  }

  function generateAtomIcon(atom) {
    const image = atomUtils.getDefaultImage(atom);

    if (image) {
      return (
        <img
          className={
            "image " + (atomUtils.isPersona(atom) ? "image--isPersona" : "")
          }
          alt={get(image, "name")}
          src={
            "data:" +
            get(image, "encodingFormat") +
            ";base64," +
            get(image, "encoding")
          }
        />
      );
    } else {
      const isPersona = atomUtils.isPersona(atom);
      const useCaseIcon = !isPersona
        ? atomUtils.getMatchedUseCaseIcon(atom)
        : undefined;

      if (useCaseIcon) {
        return (
          <div
            className={
              "image usecaseimage " +
              (atomUtils.isPersona(atom) ? "image--isPersona" : "")
            }
            style={{
              backgroundColor: atomUtils.getBackground(atom),
            }}
          >
            <svg className="si__usecaseicon">
              <use xlinkHref={useCaseIcon} href={useCaseIcon} />
            </svg>
          </div>
        );
      } else {
        const identiconSvg = atomUtils.getIdenticonSvg(atom);

        return identiconSvg ? (
          <img
            className={
              "image " + (atomUtils.isPersona(atom) ? "image--isPersona" : "")
            }
            alt="Auto-generated title icon"
            src={"data:image/svg+xml;base64," + identiconSvg}
          />
        ) : (
          <div
            className="image usecaseimage image--isPersona"
            style={{
              backgroundColor: atomUri
                ? generateHexColor(
                    new shajs.sha512().update(atomUri).digest("hex")
                  )
                : "black",
            }}
          >
            <svg className="si__usecaseicon">
              <use xlinkHref={ico36_person_anon} href={ico36_person_anon} />
            </svg>
          </div>
        );
      }
    }
  }

  return (
    <won-atom-icon
      class={
        (className ? className : "") +
        " " +
        (processUtils.hasAtomFailedToLoad(processState, getUri(atom))
          ? " won-failed-to-load "
          : "") +
        (atomUtils.isInactive(atom) ? " inactive " : "") +
        (onClick ? " clickable " : "")
      }
      onClick={onClick}
    >
      {generateAtomIcon(mainIconAtom)}
      {atomUtils.hasHoldableSocket(atom) && !atomUtils.hasGroupSocket(atom)
        ? generateSubIcon(subIconAtom)
        : undefined}
    </won-atom-icon>
  );
}
WonAtomIcon.propTypes = {
  atom: PropTypes.object.isRequired,
  className: PropTypes.string,
  onClick: PropTypes.func,
  flipIcons: PropTypes.bool,
};
