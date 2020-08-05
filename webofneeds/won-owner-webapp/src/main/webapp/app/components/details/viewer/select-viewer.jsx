import React from "react";

import "~/style/_select-viewer.scss";
import PropTypes from "prop-types";

export default function WonSelectViewer({ content, detail, className }) {
  function isChecked(option) {
    return content && !!content.find(elem => elem === option.value);
  }
  const icon = detail.icon && (
    <svg className="selectv__header__icon">
      <use xlinkHref={detail.icon} href={detail.icon} />
    </svg>
  );

  const label = detail.icon && (
    <span className="selectv__header__label">{detail.label}</span>
  );

  const options =
    detail.options &&
    detail.options.map(option => (
      <label className="selectv__input__inner" key={option}>
        <input
          className="selectv__input__inner__select"
          type={detail && detail.multiSelect ? "checkbox" : "radio"}
          value={option.value}
          disabled={true}
          checked={isChecked(option)}
        />
        {option.label}
      </label>
    ));

  return (
    <won-select-viewer class={className}>
      <div className="selectv__header">
        {icon}
        {label}
      </div>
      <div className="selectv__content">{options}</div>
    </won-select-viewer>
  );
}
WonSelectViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
