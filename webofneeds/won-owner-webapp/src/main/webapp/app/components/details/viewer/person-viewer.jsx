import React from "react";

import "~/style/_person-viewer.scss";
import { get } from "../../../utils.js";
import PropTypes from "prop-types";

export default class WonPersonViewer extends React.Component {
  render() {
    const icon = this.props.detail.icon && (
      <svg className="pv__header__icon">
        <use xlinkHref={this.props.detail.icon} href={this.props.detail.icon} />
      </svg>
    );

    const label = this.props.detail.icon && (
      <span className="pv__header__label">{this.props.detail.label}</span>
    );

    const title = get(this.props.content, "title") && (
      <React.Fragment>
        <div className="pv__content__label">Title</div>
        <div className="pv__content__value">
          {this.props.content.get("title")}
        </div>
      </React.Fragment>
    );

    const name = get(this.props.content, "name") && (
      <React.Fragment>
        <div className="pv__content__label">Name</div>
        <div className="pv__content__value">
          {this.props.content.get("name")}
        </div>
      </React.Fragment>
    );

    const position = get(this.props.content, "position") && (
      <React.Fragment>
        <div className="pv__content__label">Position</div>
        <div className="pv__content__value">
          {this.props.content.get("position")}
        </div>
      </React.Fragment>
    );

    const company = get(this.props.content, "company") && (
      <React.Fragment>
        <div className="pv__content__label">Company</div>
        <div className="pv__content__value">
          {this.props.content.get("company")}
        </div>
      </React.Fragment>
    );

    return (
      <won-person-viewer class={this.props.className}>
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
}
WonPersonViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
