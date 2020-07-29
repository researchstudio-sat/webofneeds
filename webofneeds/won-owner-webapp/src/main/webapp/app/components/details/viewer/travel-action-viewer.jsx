import React, { useState } from "react";

import "~/style/_travel-action-viewer.scss";
import ico_filter_map from "~/images/won-icons/ico-filter_map.svg";
import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";
import PropTypes from "prop-types";
import { get } from "../../../utils.js";
import WonAtomMap from "../../atom-map";

export default function WonTravelActionViewer({ detail, content, className }) {
  const [locationExpanded, toggleLocationExpanded] = useState(false);

  const icon = detail.icon && (
    <svg className="rv__header__icon">
      <use xlinkHref={detail.icon} href={detail.icon} />
    </svg>
  );

  const label = detail.icon && (
    <span className="rv__header__label">{detail.label}</span>
  );

  const fromAddress = get(content, "fromAddress");
  const toAddress = get(content, "toAddress");
  const addressElement =
    fromAddress || toAddress ? (
      <div
        className="rv__content__text clickable"
        onClick={() => toggleLocationExpanded(!locationExpanded)}
      >
        <div>
          {fromAddress ? (
            <span>
              <strong>From: </strong>
              {fromAddress}
            </span>
          ) : (
            undefined
          )}
          <br />
          {toAddress ? (
            <span>
              <strong>To: </strong>
              {toAddress}
            </span>
          ) : (
            undefined
          )}
        </div>
        <svg className="rv__content__text__carret">
          <use xlinkHref={ico_filter_map} href={ico_filter_map} />
        </svg>
        <svg
          className={
            "rv__content__text__carret " +
            (locationExpanded
              ? " rv__content__text__carret--expanded "
              : " rv__content__text__carret--collapsed ")
          }
        >
          <use xlinkHref={ico16_arrow_down} href={ico16_arrow_down} />
        </svg>
      </div>
    ) : (
      undefined
    );

  const map =
    content &&
    locationExpanded &&
    (get(content, "fromLocation") || get(content, "toLocation")) ? (
      <WonAtomMap
        locations={[get(content, "fromLocation"), get(content, "toLocation")]}
      />
    ) : (
      undefined
    );

  return (
    <won-travel-action-viewer class={className}>
      <div className="rv__header">
        {icon}
        {label}
      </div>
      <div className="rv__content">
        {addressElement}
        {map}
      </div>
    </won-travel-action-viewer>
  );
}
WonTravelActionViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
