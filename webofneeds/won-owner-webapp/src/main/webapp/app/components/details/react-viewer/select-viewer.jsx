import React from "react";

import "~/style/_select-viewer.scss";
import PropTypes from "prop-types";

export default class WonSelectViewer extends React.Component {
  render() {
    const icon = this.props.detail.icon && (
      <svg className="selectv__header__icon">
        <use xlinkHref={this.props.detail.icon} href={this.props.detail.icon} />
      </svg>
    );

    const label = this.props.detail.icon && (
      <span className="selectv__header__label">{this.props.detail.label}</span>
    );

    const options =
      this.props.detail.options &&
      this.props.detail.options.map(option => {
        <label className="selectv__input__inner">
          <input
            className="selectv__input__inner__select"
            type={
              this.props.detail && this.props.detail.multiSelect
                ? "checkbox"
                : "radio"
            }
            value={option.value}
            disabled="true"
            checked={this.isChecked(option)}
          />
          {option.label}
        </label>;
      });

    return (
      <won-select-viewer>
        <div className="selectv__header">
          {icon}
          {label}
        </div>
        <div className="selectv__content">{options}</div>
      </won-select-viewer>
    );
  }

  isChecked(option) {
    return this.content && !!this.content.find(elem => elem === option.value);
  }
}
WonSelectViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
};
