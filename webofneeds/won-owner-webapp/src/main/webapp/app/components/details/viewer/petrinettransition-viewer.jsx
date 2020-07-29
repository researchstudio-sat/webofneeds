import React from "react";

import "~/style/_petrinettransition-viewer.scss";
import PropTypes from "prop-types";

export default function WonPetrinetTransitionViewer({
  detail,
  content,
  className,
}) {
  const icon = detail.icon && (
    <svg className="petrinettransitionv__header__icon">
      <use xlinkHref={detail.icon} href={detail.icon} />
    </svg>
  );

  const label = detail.icon && (
    <span className="petrinettransitionv__header__label">{detail.label}</span>
  );

  return (
    <won-petrinettransition-viewer class={className}>
      <div className="petrinettransitionv__header">
        {icon}
        {label}
      </div>
      <div className="petrinettransitionv__content">
        {detail.generateHumanReadable({
          value: content.toJS(),
          includeLabel: false,
        })}
      </div>
    </won-petrinettransition-viewer>
  );
}
WonPetrinetTransitionViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
