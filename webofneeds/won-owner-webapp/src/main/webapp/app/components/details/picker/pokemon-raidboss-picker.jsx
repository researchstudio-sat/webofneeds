import React from "react";

import "~/style/_pokemonraidbosspicker.scss";
import PropTypes from "prop-types";

export default class PokemonRaidbossPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    return <pokemon-raidboss-picker>TODO: IMPL</pokemon-raidboss-picker>;
  }
}
PokemonRaidbossPicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
