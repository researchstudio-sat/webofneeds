import React from "react";

import "~/style/_datetime-viewer.scss";
import PropTypes from "prop-types";

export default function WonDateTimeViewer({ detail, content, className }) {
  const icon = detail.icon && (
    <svg className="datetimev__header__icon">
      <use xlinkHref={detail.icon} href={detail.icon} />
    </svg>
  );

  const label = detail.icon && (
    <span className="datetimev__header__label">{detail.label}</span>
  );

  return (
    <won-datetime-viewer class={className}>
      <div className="datetimev__header">
        {icon}
        {label}
      </div>
      <div className="datetimev__content">
        {content && content.toLocaleString()}
      </div>
    </won-datetime-viewer>
  );
}
WonDateTimeViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
