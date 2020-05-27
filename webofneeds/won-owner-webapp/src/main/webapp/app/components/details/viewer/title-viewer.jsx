import React from "react";

import "~/style/_title-viewer.scss";
import PropTypes from "prop-types";

export default function WonTitleViewer({ content, detail, className }) {
  const icon = detail.icon && (
    <svg className="titlev__header__icon">
      <use xlinkHref={detail.icon} href={detail.icon} />
    </svg>
  );

  const label = detail.icon && (
    <span className="titlev__header__label">{detail.label}</span>
  );

  return (
    <won-title-viewer class={className}>
      <div className="titlev__header">
        {icon}
        {label}
      </div>
      <div className="titlev__content">{content}</div>
    </won-title-viewer>
  );
}
WonTitleViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.string,
  className: PropTypes.string,
};
