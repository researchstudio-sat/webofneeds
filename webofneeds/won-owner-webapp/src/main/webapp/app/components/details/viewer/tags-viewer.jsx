import React from "react";

import "~/style/_tags-viewer.scss";
import PropTypes from "prop-types";

export default function WonTagsViewer({ content, detail, className }) {
  const icon = detail.icon && (
    <svg className="tv__header__icon">
      <use xlinkHref={detail.icon} href={detail.icon} />
    </svg>
  );

  const label = detail.icon && (
    <span className="tv__header__label">{detail.label}</span>
  );

  const tagsArray = content && content.toArray();

  const tags =
    tagsArray &&
    tagsArray.map((tag, index) => {
      return (
        <div key={index} className="tv__content__tag">
          {tag}
        </div>
      );
    });

  return (
    <won-tags-viewer class={className}>
      <div className="tv__header">
        {icon}
        {label}
      </div>
      <div className="tv__content">{tags}</div>
    </won-tags-viewer>
  );
}
WonTagsViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
