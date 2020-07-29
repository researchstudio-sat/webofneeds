import React from "react";

import "~/style/_range-viewer.scss";
import PropTypes from "prop-types";

export default function WonRangeViewer({ detail, content, className }) {
  const icon = detail.icon && (
    <svg className="rangev__header__icon">
      <use xlinkHref={detail.icon} href={detail.icon} />
    </svg>
  );

  const label = detail.icon && (
    <span className="rangev__header__label">{detail.label}</span>
  );

  return (
    <won-range-viewer class={className}>
      <div className="rangev__header">
        {icon}
        {label}
      </div>
      <div className="rangev__content">
        {detail.generateHumanReadable({
          value: content.toJS(),
          includeLabel: false,
        })}
      </div>
    </won-range-viewer>
  );
}
WonRangeViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
