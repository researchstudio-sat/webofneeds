import React from "react";

import "~/style/_datetime-viewer.scss";
import PropTypes from "prop-types";

export default class WonDateTimeViewer extends React.Component {
  render() {
    const icon = this.props.detail.icon && (
      <svg className="datetimev__header__icon">
        <use xlinkHref={this.props.detail.icon} href={this.props.detail.icon} />
      </svg>
    );

    const label = this.props.detail.icon && (
      <span className="datetimev__header__label">
        {this.props.detail.label}
      </span>
    );

    return (
      <won-datetime-viewer class={this.props.className}>
        <div className="datetimev__header">
          {icon}
          {label}
        </div>
        <div className="datetimev__content">
          {this.props.content && this.props.content.toLocaleString()}
        </div>
      </won-datetime-viewer>
    );
  }
}
WonDateTimeViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
