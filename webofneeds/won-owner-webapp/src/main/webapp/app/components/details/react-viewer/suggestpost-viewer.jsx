import React from "react";

import "~/style/_suggestpost-viewer.scss";
import PropTypes from "prop-types";

export default class WonSuggestPostViewer extends React.Component {
  render() {
    const icon = this.props.detail.icon && (
      <svg className="suggestpostv__header__icon">
        <use xlinkHref={this.props.detail.icon} href={this.props.detail.icon} />
      </svg>
    );

    const label = this.props.detail.icon && (
      <span className="suggestpostv__header__label">
        {this.props.detail.label}
      </span>
    );

    return (
      <won-suggestpost-viewer class={this.props.className}>
        <div className="suggestpostv__header">
          {icon}
          {label}
        </div>
        <div className="suggestpostv__content">{/*TODO: CONTENT*/}</div>
      </won-suggestpost-viewer>
    );
  }
}
WonSuggestPostViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
