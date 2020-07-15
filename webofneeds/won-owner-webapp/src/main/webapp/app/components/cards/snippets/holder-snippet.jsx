import React from "react";
import { useDispatch, useSelector } from "react-redux";
import PropTypes from "prop-types";
import { generateLink, get } from "~/app/utils";
import { Link } from "react-router-dom";
import * as generalSelectors from "~/app/redux/selectors/general-selectors";
import * as atomUtils from "~/app/redux/utils/atom-utils";
import * as processUtils from "~/app/redux/utils/process-utils";

import "~/style/_holder-snippet.scss";
import { actionCreators } from "~/app/actions/actions";
import VisibilitySensor from "react-visibility-sensor";

export default function WonHolderSnippet({
  holderUri,
  holder,
  heldAtom,
  processState,
}) {
  const dispatch = useDispatch();
  const externalDataState = useSelector(generalSelectors.getExternalDataState);
  const holderName = atomUtils.getTitle(holder, externalDataState);

  const isHolderFetchNecessary = processUtils.isAtomFetchNecessary(
    processState,
    holderUri,
    holder
  );

  function ensureHolderIsFetched() {
    if (isHolderFetchNecessary) {
      console.debug("fetch holderUri, ", holderUri);
      dispatch(actionCreators.atoms__fetchUnloadedAtom(holderUri));
    }
  }

  function onChange(isVisible) {
    if (isVisible) {
      ensureHolderIsFetched();
    }
  }

  function createHolderInfoIcon() {
    const isHolderPersona = atomUtils.isPersona(holder);
    const personaImage = atomUtils.getDefaultPersonaImage(holder);
    const showPersonaImage = isHolderPersona && !!personaImage;
    const showHolderIcon = !isHolderPersona;
    const personaIdenticonSvg = atomUtils.getIdenticonSvg(holder);
    const showPersonaIdenticon =
      isHolderPersona && !personaImage && !!personaIdenticonSvg;

    if (showHolderIcon) {
      const holderUseCaseIconBackground = !isHolderPersona
        ? atomUtils.getBackground(holder)
        : undefined;

      const style = {
        backgroundColor: holderUseCaseIconBackground,
      };

      const holderUseCaseIcon = !isHolderPersona
        ? atomUtils.getMatchedUseCaseIcon(holder)
        : undefined;

      return (
        <div style={style} className="card__holder__icon holderUseCaseIcon">
          <svg className="si__serviceatomicon">
            <use xlinkHref={holderUseCaseIcon} href={holderUseCaseIcon} />
          </svg>
        </div>
      );
    } else if (showPersonaIdenticon) {
      return (
        <img
          className="card__holder__icon"
          alt="Auto-generated title image for persona that holds the atom"
          src={"data:image/svg+xml;base64," + personaIdenticonSvg}
        />
      );
    }
    if (showPersonaImage) {
      return (
        <img
          className="card__holder__icon"
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

  const holderSnippetContent = (
    <Link
      className="card__holder clickable"
      to={location =>
        generateLink(
          location,
          {
            postUri: get(holder, "uri"),
            tab: undefined,
            connectionUri: undefined,
          },
          "/post"
        )
      }
    >
      {createHolderInfoIcon()}
      {holderName ? (
        <div className="card__holder__name">
          <span className="card__holder__name__label">{holderName}</span>
          {atomUtils.isHolderVerified(heldAtom, holder) ? (
            <span
              className="card__holder__name__verification card__holder__name__verification--verified"
              title="The Persona-Relation of this Post is verified by the Persona"
            >
              Verified
            </span>
          ) : (
            <span
              className="card__holder__name__verification card__holder__name__verification--unverified"
              title="The Persona-Relation of this Post is NOT verified by the Persona"
            >
              Unverified!
            </span>
          )}
        </div>
      ) : (
        undefined
      )}
    </Link>
  );

  return isHolderFetchNecessary ? (
    <VisibilitySensor
      onChange={onChange}
      intervalDelay={200}
      partialVisibility={true}
      delayedCall={true}
      offset={{ top: -300, bottom: -300 }}
    >
      {holderSnippetContent}
    </VisibilitySensor>
  ) : (
    holderSnippetContent
  );
}
WonHolderSnippet.propTypes = {
  holderUri: PropTypes.string.isRequired,
  holder: PropTypes.object,
  processState: PropTypes.object.isRequired,
  heldAtom: PropTypes.object.isRequired,
};
