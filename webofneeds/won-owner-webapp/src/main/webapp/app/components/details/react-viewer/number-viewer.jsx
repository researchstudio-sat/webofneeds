import React from "react";

import "~/style/_number-viewer.scss";
import PropTypes from "prop-types";

export default class WonNumberViewer extends React.Component {
  render() {
    const icon = this.props.detail.icon && (
      <svg className="numberv__header__icon">
        <use xlinkHref={this.props.detail.icon} href={this.props.detail.icon} />
      </svg>
    );

    const label = this.props.detail.icon && (
      <span className="numberv__header__label">{this.props.detail.label}</span>
    );

    return (
      <won-number-viewer>
        <div className="numberv__header">
          {icon}
          {label}
        </div>
        <div className="numberv__content">{this.props.content}</div>
      </won-number-viewer>
    );
  }
}
WonNumberViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
};
