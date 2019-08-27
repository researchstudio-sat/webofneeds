import React from "react";

import "~/style/_pokemongympicker.scss";
import PropTypes from "prop-types";

export default class PokemonGymPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = this.initialValue || {};
  }

  render() {
    return (
      <pokemon-gym-picker>
        <label htmlFor="pgp__ex" className="pgp__label">
          Gym Ex:
        </label>
        <input
          type="checkbox"
          id="pgp__ex"
          className="pgp__ex"
          value={this.state.ex}
          onChange={this.updateEx.bind(this)}
        />
      </pokemon-gym-picker>
    );
  }

  updateEx(event) {
    this.setState({ ex: event.target.checked }, this.update.bind(this));
  }
  /**
   * Checks validity and uses callback method
   */
  update() {
    if (this.props.detail.isValid(this.state)) {
      this.props.onUpdate({
        value: this.state,
      });
    } else {
      this.props.onUpdate({ value: undefined });
    }
  }
}
PokemonGymPicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
