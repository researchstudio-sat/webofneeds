import React from "react";

import "~/style/_petrinet-viewer.scss";
import ico36_uc_transport_demand from "~/images/won-icons/ico36_uc_transport_demand.svg";
import PropTypes from "prop-types";
import WonPetrinetState from "../../petrinet-state";
import { get } from "../../../utils.js";

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

    const petrinetStateElement = get(this.props.content, "processURI") && (
      <WonPetrinetState
        className="petrinetv__content__state"
        processUri={get(this.props.content, "processURI")}
      />
    );

    const petrinetDownloadElement = this.props.content && (
      <a
        className="petrinetv__content__download"
        href={
          "data:" +
          get(this.props.content, "type") +
          ";base64," +
          get(this.props.content, "data")
        }
        download={get(this.props.content, "name")}
      >
        <svg className="petrinetv__content__download__typeicon">
          <use
            xlinkHref={ico36_uc_transport_demand}
            href={ico36_uc_transport_demand}
          />
        </svg>
        <div className="petrinetv__content__download__label clickable">
          {"Download '" + get(this.props.content, "name") + "'"}
        </div>
      </a>
    );

    return (
      <won-petrinet-viewer class={this.props.className}>
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
}
WonPetrinetViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
