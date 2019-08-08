import React from "react";

import "~/style/_review-viewer.scss";
import PropTypes from "prop-types";

export default class WonReviewViewer extends React.Component {
  render() {
    const icon = this.props.detail.icon && (
      <svg className="reviewv__header__icon">
        <use xlinkHref={this.props.detail.icon} href={this.props.detail.icon} />
      </svg>
    );

    const label = this.props.detail.icon && (
      <span className="reviewv__header__label">{this.props.detail.label}</span>
    );

    return (
      <won-review-viewer>
        <div className="reviewv__header">
          {icon}
          {label}
        </div>
        <div className="reviewv__content">
          {this.props.detail.generateHumanReadable({
            value: this.props.content.toJS(),
            includeLabel: false,
          })}
        </div>
      </won-review-viewer>
    );
  }
}
WonReviewViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
};
