import React from "react";

import "~/style/_dropdown-viewer.scss";
import PropTypes from "prop-types";

export default class WonDropdownViewer extends React.Component {
  render() {
    const icon = this.props.detail.icon && (
      <svg className="dropdownv__header__icon">
        <use xlinkHref={this.props.detail.icon} href={this.props.detail.icon} />
      </svg>
    );

    const label = this.props.detail.icon && (
      <span className="dropdownv__header__label">
        {this.props.detail.label}
      </span>
    );

    return (
      <won-dropdown-viewer>
        <div className="dropdownv__header">
          {icon}
          {label}
        </div>
        <div className="dropdownv__content">{this.props.content}</div>
      </won-dropdown-viewer>
    );
  }
}
WonDropdownViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
};
