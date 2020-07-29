import React from "react";

import "~/style/_person-viewer.scss";
import { get } from "../../../utils.js";
import PropTypes from "prop-types";

export default function WonPersonViewer({ detail, content, className }) {
  const icon = detail.icon && (
    <svg className="pv__header__icon">
      <use xlinkHref={detail.icon} href={detail.icon} />
    </svg>
  );

  const label = detail.icon && (
    <span className="pv__header__label">{detail.label}</span>
  );

  const title = get(content, "title") && (
    <React.Fragment>
      <div className="pv__content__label">Title</div>
      <div className="pv__content__value">{get(content, "title")}</div>
    </React.Fragment>
  );

  const name = get(content, "name") && (
    <React.Fragment>
      <div className="pv__content__label">Name</div>
      <div className="pv__content__value">{get(content, "name")}</div>
    </React.Fragment>
  );

  const position = get(content, "position") && (
    <React.Fragment>
      <div className="pv__content__label">Position</div>
      <div className="pv__content__value">{get(content, "position")}</div>
    </React.Fragment>
  );

  const company = get(content, "company") && (
    <React.Fragment>
      <div className="pv__content__label">Company</div>
      <div className="pv__content__value">{get(content, "company")}</div>
    </React.Fragment>
  );

  return (
    <won-person-viewer class={className}>
      <div className="pv__header">
        {icon}
        {label}
      </div>
      <div className="pv__content">
        {title}
        {name}
        {position}
        {company}
      </div>
    </won-person-viewer>
  );
}
WonPersonViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
