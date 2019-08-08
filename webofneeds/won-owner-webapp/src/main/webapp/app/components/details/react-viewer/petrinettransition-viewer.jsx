import React from "react";

import "~/style/_petrinettransition-viewer.scss";
import PropTypes from "prop-types";

export default class WonPetrinetTransitionViewer extends React.Component {
  render() {
    const icon = this.props.detail.icon && (
      <svg className="petrinettransitionv__header__icon">
        <use xlinkHref={this.props.detail.icon} href={this.props.detail.icon} />
      </svg>
    );

    const label = this.props.detail.icon && (
      <span className="petrinettransitionv__header__label">
        {this.props.detail.label}
      </span>
    );

    return (
      <won-petrinettransition-viewer>
        <div className="petrinettransitionv__header">
          {icon}
          {label}
        </div>
        <div className="petrinettransitionv__content">
          {this.props.detail.generateHumanReadable({
            value: this.props.content.toJS(),
            includeLabel: false,
          })}
        </div>
      </won-petrinettransition-viewer>
    );
  }
}
WonPetrinetTransitionViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
};
