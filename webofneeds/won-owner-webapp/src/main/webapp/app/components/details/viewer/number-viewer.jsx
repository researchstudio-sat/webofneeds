import React from "react";

import "~/style/_number-viewer.scss";
import PropTypes from "prop-types";

export default function WonNumberViewer({ detail, content, className }) {
  const icon = detail.icon && (
    <svg className="numberv__header__icon">
      <use xlinkHref={detail.icon} href={detail.icon} />
    </svg>
  );

  const label = detail.icon && (
    <span className="numberv__header__label">{detail.label}</span>
  );

  return (
    <won-number-viewer class={className}>
      <div className="numberv__header">
        {icon}
        {label}
      </div>
      <div className="numberv__content">{content}</div>
    </won-number-viewer>
  );
}
WonNumberViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
