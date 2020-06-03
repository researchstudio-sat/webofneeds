import React from "react";

import "~/style/_workflow-viewer.scss";
import ico36_uc_transport_demand from "~/images/won-icons/ico36_uc_transport_demand.svg";
import { get } from "../../../utils.js";

import PropTypes from "prop-types";

export default function WonWorkflowViewer({ content, detail, className }) {
  const icon = detail.icon && (
    <svg className="workflowv__header__icon">
      <use xlinkHref={detail.icon} href={detail.icon} />
    </svg>
  );

  const label = detail.icon && (
    <span className="workflowv__header__label">{detail.label}</span>
  );

  const workflowDownloadElement = content && (
    <a
      className="workflowv__content__download"
      href={"data:" + get(content, "type") + ";base64," + get(content, "data")}
      download={get(content, "name")}
    >
      <svg className="workflowv__content__download__typeicon">
        <use
          xlinkHref={ico36_uc_transport_demand}
          href={ico36_uc_transport_demand}
        />
      </svg>
      <div className="workflowv__content__download__label clickable">
        {"Download '" + get(content, "name") + "'"}
      </div>
    </a>
  );

  return (
    <won-workflow-viewer class={className}>
      <div className="workflowv__header">
        {icon}
        {label}
      </div>
      <div className="workflowv__content">{workflowDownloadElement}</div>
    </won-workflow-viewer>
  );
}
WonWorkflowViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
