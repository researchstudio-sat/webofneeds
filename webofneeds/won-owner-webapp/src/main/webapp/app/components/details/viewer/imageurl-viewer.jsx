import React from "react";

import "~/style/_imageurl-viewer.scss";
import PropTypes from "prop-types";

export default function WonImageUrlViewer({ content, detail, className }) {
  const icon = detail.icon && (
    <svg className="imageurlv__header__icon">
      <use xlinkHref={detail.icon} href={detail.icon} />
    </svg>
  );

  const label = detail.icon && (
    <span className="imageurlv__header__label">{detail.label}</span>
  );

  return (
    <won-imageurl-viewer class={className}>
      <div className="imageurlv__header">
        {icon}
        {label}
      </div>
      <div className="imageurlv__content">
        <img className="imageurlv__content__image" src={content} />
      </div>
    </won-imageurl-viewer>
  );
}
WonImageUrlViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.string,
  className: PropTypes.string,
};
