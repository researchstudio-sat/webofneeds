import React from "react";

import "~/style/_petrinet-viewer.scss";
import PropTypes from "prop-types";

export default class WonPetrinetViewer extends React.Component {
  render() {
    const icon = this.props.detail.icon && (
      <svg className="petrinetv__header__icon">
        <use xlinkHref={this.props.detail.icon} href={this.props.detail.icon} />
      </svg>
    );

    const label = this.props.detail.icon && (
      <span className="petrinetv__header__label">
        {this.props.detail.label}
      </span>
    );

    return (
      <won-petrinet-viewer class={this.props.className}>
        <div className="petrinetv__header">
          {icon}
          {label}
        </div>
        <div className="petrinetv__content">{/*TODO: CONTENT*/}</div>
      </won-petrinet-viewer>
    );
  }
}
WonPetrinetViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
