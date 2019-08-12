import React from "react";

import "~/style/_location-viewer.scss";
import { get } from "../../../utils.js";
import WonAtomMap from "../../atom-map.jsx";

import PropTypes from "prop-types";

export default class WonLocationViewer extends React.Component {
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
      <svg className="locationv__header__icon">
        <use xlinkHref={this.props.detail.icon} href={this.props.detail.icon} />
      </svg>
    );

    const label = this.props.detail.icon && (
      <span className="locationv__header__label">
        {this.props.detail.label}
      </span>
    );

    const address = get(this.props.content, "address");
    const addressElement = address ? (
      <div
        className="lv__content__text clickable"
        onClick={() => this.toggleLocation()}
      >
        {address}
        <svg className="lv__content__text__carret">
          <use xlinkHref="#ico-filter_map" href="#ico-filter_map" />
        </svg>
        <svg className="lv__content__text__carret">
          {this.props.content && this.state.locationExanded ? (
            <use xlinkHref="#ico16_arrow_up" href="#ico16_arrow_up" />
          ) : (
            <use xlinkHref="#ico16_arrow_down" href="#ico16_arrow_down" />
          )}
        </svg>
      </div>
    ) : (
      undefined
    );

    const map = this.state.locationExpanded ? (
      <WonAtomMap locations={[this.props.content]} />
    ) : (
      undefined
    );

    return (
      <won-location-viewer>
        <div className="locationv__header">
          {icon}
          {label}
        </div>
        <div className="locationv__content">
          {addressElement}
          {map}
        </div>
      </won-location-viewer>
    );
  }
}
WonLocationViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
};
