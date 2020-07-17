import React, { useState, useEffect } from "react";
import PropTypes from "prop-types";

import { get } from "../utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import WonAtomHeader from "./atom-header.jsx";
import WonAtomIcon from "./atom-icon.jsx";

import "~/style/_publish-button.scss";
import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";
import ico36_person_anon from "~/images/won-icons/ico36_person_anon.svg";

export default function WonPublishButton({
  buttonEnabled,
  label = "Publish",
  showPersonas,
  ownedPersonas,
  presetHolderUri,
  onPublish,
}) {
  const [selectedHolderUri, setSelectedHolderUri] = useState(presetHolderUri);
  const [showHolderPicker, toggleHolderPicker] = useState(false);

  const holderPickerEnabled = !!(
    showPersonas &&
    ownedPersonas &&
    ownedPersonas.size > 0
  );

  useEffect(
    () => {
      if (holderPickerEnabled && presetHolderUri) {
        setSelectedHolderUri(presetHolderUri);
      }
    },
    [presetHolderUri, ownedPersonas, holderPickerEnabled]
  );

  const publishLabel = () => {
    if (holderPickerEnabled) {
      const icon = selectedHolderUri ? (
        <WonAtomIcon
          className="submit-button__icon"
          atom={get(ownedPersonas, selectedHolderUri)}
        />
      ) : (
        <svg className="submit-button__icon anon">
          <use href={ico36_person_anon} xlinkHref={ico36_person_anon} />
        </svg>
      );

      return (
        <React.Fragment>
          <span className="submit-button__label">{`${label} as`}</span>
          {icon}
        </React.Fragment>
      );
    }

    return <span className="submit-button__label">{label}</span>;
  };

  const generateHolderPicker = () => {
    const selectHolderUri = holderUri => {
      setSelectedHolderUri(holderUri);
      toggleHolderPicker(false);
    };

    const sortedOwnedPersonas = ownedPersonas
      .toOrderedMap()
      .sortBy(persona => atomUtils.getTitle(persona))
      .map((persona, personaUri) => (
        <WonAtomHeader
          key={personaUri}
          onClick={() => selectHolderUri(personaUri)}
          hideTimestamp={true}
          atom={persona}
        />
      ));

    return (
      <div className="holder-picker">
        <div className="holder-picker__content">
          {sortedOwnedPersonas.toSet()}
          <div
            className="holder-picker__content__anon"
            onClick={() => selectHolderUri(undefined)}
          >
            <svg className="holder-picker__content__anon__icon">
              <use href={ico36_person_anon} xlinkHref={ico36_person_anon} />
            </svg>
            <span className="holder-picker__content__anon__label">
              Anonymous
            </span>
          </div>
        </div>
      </div>
    );
  };

  return (
    <won-publish-button class="won-publish-button">
      <div
        className={
          "submit-button " +
          (!buttonEnabled
            ? " submit-button--disabled "
            : " submit-button--enabled ")
        }
        onClick={() =>
          buttonEnabled && onPublish({ personaId: selectedHolderUri })
        }
      >
        {publishLabel()}
      </div>
      {holderPickerEnabled ? (
        <div
          onClick={() => toggleHolderPicker(!showHolderPicker)}
          className={
            " holder-indicator " +
            (showHolderPicker
              ? " holder-indicator--expanded "
              : " holder-indicator--collapsed ")
          }
        >
          <svg className="holder-indicator__carret">
            <use href={ico16_arrow_down} xlinkHref={ico16_arrow_down} />
          </svg>
        </div>
      ) : (
        undefined
      )}
      {showHolderPicker && generateHolderPicker()}
    </won-publish-button>
  );
}
WonPublishButton.propTypes = {
  buttonEnabled: PropTypes.bool.isRequired,
  showPersonas: PropTypes.bool.isRequired,
  onPublish: PropTypes.func.isRequired,
  ownedPersonas: PropTypes.object,
  presetHolderUri: PropTypes.string,
  label: PropTypes.string,
};
