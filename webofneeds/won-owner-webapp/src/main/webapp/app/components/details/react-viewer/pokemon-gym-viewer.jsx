import React from "react";

import "~/style/_pokemon-gym-viewer.scss";
import { get } from "../../../utils.js";
import PropTypes from "prop-types";

export default class PokemonGymViewer extends React.Component {
  render() {
    const icon = this.props.detail.icon && (
      <svg className="pgv__header__icon">
        <use xlinkHref={this.props.detail.icon} href={this.props.detail.icon} />
      </svg>
    );

    const label = this.props.detail.icon && (
      <span className="pgv__header__label">{this.props.detail.label}</span>
    );
    const exGym = get(this.props.content, "ex") && (
      <div className="pgv__content__ex">This is an Ex Gym!</div>
    );

    return (
      <pokemon-gym-viewer class={this.props.className}>
        <div className="pgv__header">
          {icon}
          {label}
        </div>
        {exGym}
      </pokemon-gym-viewer>
    );
  }
}
PokemonGymViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
