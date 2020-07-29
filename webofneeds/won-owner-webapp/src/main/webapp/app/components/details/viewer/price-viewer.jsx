import React from "react";

import "~/style/_price-viewer.scss";
import PropTypes from "prop-types";

export default function WonPriceViewer({ detail, content, className }) {
  const icon = detail.icon && (
    <svg className="pricev__header__icon">
      <use xlinkHref={detail.icon} href={detail.icon} />
    </svg>
  );

  const label = detail.icon && (
    <span className="pricev__header__label">{detail.label}</span>
  );

  return (
    <won-price-viewer class={className}>
      <div className="pricev__header">
        {icon}
        {label}
      </div>
      <div className="pricev__content">
        {detail.generateHumanReadable({
          value: content.toJS(),
          includeLabel: false,
        })}
      </div>
    </won-price-viewer>
  );
}
WonPriceViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
