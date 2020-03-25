import React from "react";

import "~/style/_travel-action-viewer.scss";
import ico_filter_map from "~/images/won-icons/ico-filter_map.svg";
import ico16_arrow_up from "~/images/won-icons/ico-ico16_arrow_up.svg";
import ico16_arrow_down from "~/images/won-icons/ico-ico16_arrow_down.svg";
import PropTypes from "prop-types";
import { get } from "../../../utils.js";
import WonAtomMap from "../../atom-map";

export default class WonTravelActionViewer extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      locationExpanded: false,
    };
  }

  toggleLocation() {
    this.setState({ locationExpanded: !this.state.locationExpanded });
  }

  render() {
    const icon = this.props.detail.icon && (
      <svg className="rv__header__icon">
        <use xlinkHref={this.props.detail.icon} href={this.props.detail.icon} />
      </svg>
    );

    const label = this.props.detail.icon && (
      <span className="rv__header__label">{this.props.detail.label}</span>
    );

    const fromAddress = get(this.props.content, "fromAddress");
    const toAddress = get(this.props.content, "toAddress");
    const addressElement =
      fromAddress || toAddress ? (
        <div
          className="rv__content__text clickable"
          onClick={() => this.toggleLocation()}
        >
          <div>
            {fromAddress ? (
              <span>
                <strong>From: </strong>
                {fromAddress}
              </span>
            ) : (
              undefined
            )}
            <br />
            {toAddress ? (
              <span>
                <strong>To: </strong>
                {toAddress}
              </span>
            ) : (
              undefined
            )}
          </div>
          <svg className="rv__content__text__carret">
            <use xlinkHref={ico_filter_map} href={ico_filter_map} />
          </svg>
          <svg className="rv__content__text__carret">
            {this.state.locationExpanded ? (
              <use xlinkHref={ico16_arrow_up} href={ico16_arrow_up} />
            ) : (
              <use xlinkHref={ico16_arrow_down} href={ico16_arrow_down} />
            )}
          </svg>
        </div>
      ) : (
        undefined
      );

    const map =
      this.props.content &&
      this.state.locationExpanded &&
      (get(this.props.content, "fromLocation") ||
        get(this.props.content, "toLocation")) ? (
        <WonAtomMap
          locations={[
            get(this.props.content, "fromLocation"),
            get(this.props.content, "toLocation"),
          ]}
        />
      ) : (
        undefined
      );

    return (
      <won-travel-action-viewer class={this.props.className}>
        <div className="rv__header">
          {icon}
          {label}
        </div>
        <div className="rv__content">
          {addressElement}
          {map}
        </div>
      </won-travel-action-viewer>
    );
  }
}
WonTravelActionViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
