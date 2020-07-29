import React from "react";

import "~/style/_pokemon-gym-viewer.scss";
import { get } from "../../../utils.js";
import PropTypes from "prop-types";

export default function PokemonGymViewer({ detail, content, className }) {
  const icon = detail.icon && (
    <svg className="pgv__header__icon">
      <use xlinkHref={detail.icon} href={detail.icon} />
    </svg>
  );

  const label = detail.icon && (
    <span className="pgv__header__label">{detail.label}</span>
  );
  const exGym = get(content, "ex") && (
    <div className="pgv__content__ex">This is an Ex Gym!</div>
  );

  return (
    <pokemon-gym-viewer class={className}>
      <div className="pgv__header">
        {icon}
        {label}
      </div>
      {exGym}
    </pokemon-gym-viewer>
  );
}
PokemonGymViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
