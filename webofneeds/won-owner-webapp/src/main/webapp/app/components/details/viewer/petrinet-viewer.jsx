import React from "react";

import "~/style/_petrinet-viewer.scss";
import ico36_uc_transport_demand from "~/images/won-icons/ico36_uc_transport_demand.svg";
import PropTypes from "prop-types";
import WonPetrinetState from "../../petrinet-state";
import { get } from "../../../utils.js";

export default function WonPetrinetViewer({ detail, content, className }) {
  const icon = detail.icon && (
    <svg className="petrinetv__header__icon">
      <use xlinkHref={detail.icon} href={detail.icon} />
    </svg>
  );

  const label = detail.icon && (
    <span className="petrinetv__header__label">{detail.label}</span>
  );

  const petrinetStateElement = get(content, "processURI") && (
    <WonPetrinetState
      className="petrinetv__content__state"
      processUri={get(content, "processURI")}
    />
  );

  const petrinetDownloadElement = content && (
    <a
      className="petrinetv__content__download"
      href={"data:" + get(content, "type") + ";base64," + get(content, "data")}
      download={get(content, "name")}
    >
      <svg className="petrinetv__content__download__typeicon">
        <use
          xlinkHref={ico36_uc_transport_demand}
          href={ico36_uc_transport_demand}
        />
      </svg>
      <div className="petrinetv__content__download__label clickable">
        {"Download '" + get(content, "name") + "'"}
      </div>
    </a>
  );

  return (
    <won-petrinet-viewer class={className}>
      <div className="petrinetv__header">
        {icon}
        {label}
      </div>
      <div className="petrinetv__content">
        {petrinetStateElement}
        {petrinetDownloadElement}
      </div>
    </won-petrinet-viewer>
  );
}
WonPetrinetViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
