import React from "react";

import "~/style/_range-viewer.scss";
import PropTypes from "prop-types";

export default class WonRangeViewer extends React.Component {
  render() {
    const icon = this.props.detail.icon && (
      <svg className="rangev__header__icon">
        <use xlinkHref={this.props.detail.icon} href={this.props.detail.icon} />
      </svg>
    );

    const label = this.props.detail.icon && (
      <span className="rangev__header__label">{this.props.detail.label}</span>
    );

    return (
      <won-range-viewer>
        <div className="rangev__header">
          {icon}
          {label}
        </div>
        <div className="rangev__content">
          {this.props.detail.generateHumanReadable({
            value: this.props.content.toJS(),
            includeLabel: false,
          })}
        </div>
      </won-range-viewer>
    );
  }
}
WonRangeViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
};
