import React from "react";

import "~/style/_workflow-viewer.scss";
import { get } from "../../../utils.js";

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

    const workflowDownloadElement = this.props.content && (
      <a
        className="workflowv__content__download"
        href={
          "data:" +
          get(this.props.content, "type") +
          ";base64," +
          get(this.props.content, "data")
        }
        download={get(this.props.content, "name")}
      >
        <svg className="workflowv__content__download__typeicon">
          <use
            xlinkHref="#ico36_uc_transport_demand"
            href="#ico36_uc_transport_demand"
          />
        </svg>
        <div className="workflowv__content__download__label clickable">
          {"Download '" + get(this.props.content, "name") + "'"}
        </div>
      </a>
    );

    return (
      <won-workflow-viewer class={this.props.className}>
        <div className="workflowv__header">
          {icon}
          {label}
        </div>
        <div className="workflowv__content">{workflowDownloadElement}</div>
      </won-workflow-viewer>
    );
  }
}
WonWorkflowViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
