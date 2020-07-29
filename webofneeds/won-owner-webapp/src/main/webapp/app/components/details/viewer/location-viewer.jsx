import React, { useState } from "react";

import "~/style/_location-viewer.scss";
import ico_filter_map from "~/images/won-icons/ico-filter_map.svg";
import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";
import { get } from "../../../utils.js";
import WonAtomMap from "../../atom-map.jsx";

import PropTypes from "prop-types";

export default function WonLocationViewer({ detail, content, className }) {
  const [locationExpanded, toggleLocationExpanded] = useState(false);

  const icon = detail.icon && (
    <svg className="lv__header__icon">
      <use xlinkHref={detail.icon} href={detail.icon} />
    </svg>
  );

  const label = detail.icon && (
    <span className="lv__header__label">{detail.label}</span>
  );

  const address = get(content, "address");
  const addressElement = address ? (
    <div
      className="lv__content__text clickable"
      onClick={() => toggleLocationExpanded(!locationExpanded)}
    >
      {address}
      <svg className="lv__content__text__carret">
        <use xlinkHref={ico_filter_map} href={ico_filter_map} />
      </svg>
      <svg
        className={
          "lv__content__text__carret " +
          (locationExpanded
            ? " lv__content__text__carret--expanded "
            : " lv__content__text__carret--collapsed ")
        }
      >
        <use xlinkHref={ico16_arrow_down} href={ico16_arrow_down} />
      </svg>
    </div>
  ) : (
    undefined
  );

  const map =
    content && locationExpanded ? (
      <WonAtomMap locations={[content]} />
    ) : (
      undefined
    );

  return (
    <won-location-viewer class={className}>
      <div className="lv__header">
        {icon}
        {label}
      </div>
      <div className="lv__content">
        {addressElement}
        {map}
      </div>
    </won-location-viewer>
  );
}
WonLocationViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
