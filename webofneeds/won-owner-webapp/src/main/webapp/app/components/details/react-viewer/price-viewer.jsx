import React from "react";

import "~/style/_price-viewer.scss";
import PropTypes from "prop-types";

export default class WonPriceViewer extends React.Component {
  render() {
    const icon = this.props.detail.icon && (
      <svg className="pricev__header__icon">
        <use xlinkHref={this.props.detail.icon} href={this.props.detail.icon} />
      </svg>
    );

    const label = this.props.detail.icon && (
      <span className="pricev__header__label">{this.props.detail.label}</span>
    );

    return (
      <won-price-viewer>
        <div className="pricev__header">
          {icon}
          {label}
        </div>
        <div className="pricev__content">
          {this.props.detail.generateHumanReadable({
            value: this.props.content.toJS(),
            includeLabel: false,
          })}
        </div>
      </won-price-viewer>
    );
  }
}
WonPriceViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
};
