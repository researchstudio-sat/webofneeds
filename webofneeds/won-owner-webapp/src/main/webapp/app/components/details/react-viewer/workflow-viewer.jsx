import React from "react";

import "~/style/_workflow-viewer.scss";
import PropTypes from "prop-types";

export default class WonWorkflowViewer extends React.Component {
  render() {
    const icon = this.props.detail.icon && (
      <svg className="workflowv__header__icon">
        <use xlinkHref={this.props.detail.icon} href={this.props.detail.icon} />
      </svg>
    );

    const label = this.props.detail.icon && (
      <span className="workflowv__header__label">
        {this.props.detail.label}
      </span>
    );

    return (
      <won-workflow-viewer class={this.props.className}>
        <div className="workflowv__header">
          {icon}
          {label}
        </div>
        <div className="workflowv__content">{/*TODO: CONTENT*/}</div>
      </won-workflow-viewer>
    );
  }
}
WonWorkflowViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
