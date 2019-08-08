import React from "react";

import "~/style/_title-viewer.scss";
import PropTypes from "prop-types";

export default class WonTitleViewer extends React.Component {
  render() {
    const icon = this.props.detail.icon && (
      <svg className="titlev__header__icon">
        <use xlinkHref={this.props.detail.icon} href={this.props.detail.icon} />
      </svg>
    );

    const label = this.props.detail.icon && (
      <span className="titlev__header__label">{this.props.detail.label}</span>
    );

    return (
      <won-title-viewer>
        <div className="titlev__header">
          {icon}
          {label}
        </div>
        <div className="titlev__content">{this.props.content}</div>
      </won-title-viewer>
    );
  }
}
WonTitleViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
};
