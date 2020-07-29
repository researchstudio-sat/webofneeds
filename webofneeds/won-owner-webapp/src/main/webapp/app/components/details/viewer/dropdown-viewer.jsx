import React from "react";

import "~/style/_dropdown-viewer.scss";
import PropTypes from "prop-types";

export default function WonDropdownViewer({ detail, content, className }) {
  const icon = detail.icon && (
    <svg className="dropdownv__header__icon">
      <use xlinkHref={detail.icon} href={detail.icon} />
    </svg>
  );

  const label = detail.icon && (
    <span className="dropdownv__header__label">{detail.label}</span>
  );

  return (
    <won-dropdown-viewer class={className}>
      <div className="dropdownv__header">
        {icon}
        {label}
      </div>
      <div className="dropdownv__content">{content}</div>
    </won-dropdown-viewer>
  );
}
WonDropdownViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
