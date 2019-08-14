import React from "react";

import "~/style/_pokemongympicker.scss";
import PropTypes from "prop-types";

export default class PokemonGymPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    return <pokemon-gym-picker>TODO: IMPL</pokemon-gym-picker>;
  }
}
PokemonGymPicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
