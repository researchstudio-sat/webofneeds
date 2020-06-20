import React from "react";

import "~/style/_description-viewer.scss";
import PropTypes from "prop-types";
import ReactMarkdown from "react-markdown";

export default function WonDescriptionViewer({ detail, content, className }) {
  const icon = detail.icon && (
    <svg className="dv__header__icon">
      <use xlinkHref={detail.icon} href={detail.icon} />
    </svg>
  );

  const label = detail.icon && (
    <span className="dv__header__label">{detail.label}</span>
  );

  return (
    <won-description-viewer class={className}>
      <div className="dv__header">
        {icon}
        {label}
      </div>
      <ReactMarkdown
        className="markdown"
        source={content}
        linkTarget="_blank"
      />
    </won-description-viewer>
  );
}
WonDescriptionViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.string,
  className: PropTypes.string,
};
