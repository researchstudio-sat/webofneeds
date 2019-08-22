import React from "react";

import "~/style/_pokemonraidbosspicker.scss";
import PropTypes from "prop-types";
import WonDatetimePicker from "./datetime-picker.jsx";
import WonTitlePicker from "./title-picker.jsx";

export default class PokemonRaidbossPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      pokemonRaidBoss: props.initialValue || { level: 1, hatched: false },
      pokemonFilter: "",
      pokemonList: [],
    };
  }

  render() {
    const pokemonListEntries = this.state.pokemonList
      .filter(pokemon =>
        this.filterPokemon(
          pokemon,
          this.state.pokemonFilter,
          this.state.pokemonRaidBoss.id,
          this.state.pokemonRaidBoss.form
        )
      )
      .map((pokemon, index) => (
        <div
          key={pokemon.id + "-" + index}
          className={
            "prbp__pokemonlist__pokemon " +
            (this.state.pokemonRaidBoss.id == pokemon.id &&
            (!this.state.pokemonRaidBoss.form ||
              this.state.pokemonRaidBoss.form == pokemon.form)
              ? " prbp__pokemonlist__pokemon--selected "
              : "")
          }
          onClick={() => this.updatePokemon(pokemon.id, pokemon.form)}
        >
          <img
            className="prbp__pokemonlist__pokemon__image"
            src={pokemon.imageUrl}
          />
          <div className="prbp__pokemonlist__pokemon__id">
            {"#" + pokemon.id}
          </div>
          <div className="prbp__pokemonlist__pokemon__name">
            {pokemon.name}
            {pokemon.form ? (
              <span className="prbp__pokemonlist__pokemon__name__form">
                {"(" + pokemon.form + ")"}
              </span>
            ) : (
              undefined
            )}
          </div>
        </div>
      ));

    return (
      <pokemon-raidboss-picker>
        <div className="prbp__level">
          <input
            id="prbp__level__1"
            type="radio"
            className="prbp__level__option"
            value="1"
            checked={this.isLevelChecked(1)}
            readOnly={true}
          />
          <label htmlFor="prbp__level__1" onClick={() => this.updateLevel(1)}>
            {this.props.detail.getLevelLabel(1)}
          </label>
          <input
            id="prbp__level__2"
            type="radio"
            className="prbp__level__option"
            value="2"
            checked={this.isLevelChecked(2)}
            readOnly={true}
          />
          <label htmlFor="prbp__level__2" onClick={() => this.updateLevel(2)}>
            {this.props.detail.getLevelLabel(2)}
          </label>
          <input
            id="prbp__level__3"
            type="radio"
            className="prbp__level__option"
            value="3"
            checked={this.isLevelChecked(3)}
            readOnly={true}
          />
          <label htmlFor="prbp__level__3" onClick={() => this.updateLevel(3)}>
            {this.props.detail.getLevelLabel(3)}
          </label>
          <input
            id="prbp__level__4"
            type="radio"
            className="prbp__level__option"
            value="4"
            checked={this.isLevelChecked(4)}
            readOnly={true}
          />
          <label htmlFor="prbp__level__4" onClick={() => this.updateLevel(4)}>
            {this.props.detail.getLevelLabel(4)}
          </label>
          <input
            id="prbp__level__5"
            type="radio"
            className="prbp__level__option"
            value="5"
            checked={this.isLevelChecked(5)}
            readOnly={true}
          />
          <label htmlFor="prbp__level__5" onClick={() => this.updateLevel(5)}>
            {this.props.detail.getLevelLabel(5)}
          </label>
        </div>

        <label htmlFor="prbp__hatched">Hatched</label>
        <input
          type="checkbox"
          id="prbp__hatched"
          className="prbp__hatched"
          value={this.state.pokemonRaidBoss.hatched}
          onChange={this.updateHatched.bind(this)}
        />

        <label
          className={
            "prbp__label " +
            (this.state.pokemonRaidBoss.hatched ? "prbp__label--disabled" : "")
          }
        >
          Hatches at
        </label>
        <WonDatetimePicker
          className={
            "prbp__hatches " +
            (this.state.pokemonRaidBoss.hatched
              ? "prbp__hatches--disabled"
              : "")
          }
          onUpdate={this.updateHatches.bind(this)}
          detail={this.props.detail && this.props.detail.hatches}
          initialValue={
            this.props.initialValue && this.props.initialValue.hatches
          }
        />

        <label className="prbp__label">Expires at</label>
        <WonDatetimePicker
          className="prbp__expires"
          onUpdate={this.updateExpires.bind(this)}
          detail={this.props.detail && this.props.detail.expires}
          initialValue={
            this.props.initialValue && this.props.initialValue.expires
          }
        />

        <label
          className={
            "prbp__label " +
            (!this.state.pokemonRaidBoss.hatched ? "prbp__label--disabled" : "")
          }
        >
          Pokemon
        </label>
        <WonTitlePicker
          className={
            "prbp__pokemon " +
            (!this.state.pokemonRaidBoss.hatched
              ? "prbp__pokemon--disabled"
              : "")
          }
          onUpdate={this.updatePokemonFilter.bind(this)}
          detail={this.props.detail && this.props.detail.filterDetail}
          initialValue={this.state.pokemonFilter}
        />
        <div
          className={
            "prbp__pokemonlist " +
            (!this.state.pokemonRaidBoss.hatched
              ? "prbp__pokemonlist--disabled"
              : "")
          }
        >
          {pokemonListEntries}
        </div>
      </pokemon-raidboss-picker>
    );
  }

  filterPokemon(pokemon, pokemonFilter, selectedId, selectedForm) {
    if (pokemonFilter) {
      const filterArray =
        pokemonFilter &&
        pokemonFilter
          .trim()
          .toLowerCase()
          .split(" ");
      if (filterArray && filterArray.length > 0) {
        for (const idx in filterArray) {
          if (pokemon.id == filterArray[idx]) return true;
          if ("#" + pokemon.id === filterArray[idx]) return true;
          if (
            pokemon.form &&
            pokemon.form.toLowerCase().includes(filterArray[idx])
          )
            return true;
          if (pokemon.name.toLowerCase().includes(filterArray[idx]))
            return true;
        }
        return false;
      }
    } else if (selectedId) {
      if (
        (!selectedForm && selectedId == pokemon.id && !pokemon.form) ||
        (selectedForm &&
          selectedId == pokemon.id &&
          selectedForm === pokemon.form)
      ) {
        return true;
      }
      return false;
    }

    return true;
  }

  /**
   * Checks validity and uses callback method
   */
  update() {
    if (this.props.detail.isValid(this.state.pokemonRaidBoss)) {
      this.props.onUpdate({
        value: {
          id: this.state.pokemonRaidBoss.hatched
            ? this.state.pokemonRaidBoss.id
            : undefined,
          form: this.state.pokemonRaidBoss.hatched
            ? this.state.pokemonRaidBoss.form
            : undefined,
          level: this.state.pokemonRaidBoss.level,
          hatched: this.state.pokemonRaidBoss.hatched,
          hatches: !this.state.pokemonRaidBoss.hatched
            ? this.state.pokemonRaidBoss.hatches
            : undefined,
          expires: this.state.pokemonRaidBoss.expires,
        },
      });
    } else {
      this.props.onUpdate({ value: undefined });
    }
  }

  componentDidMount() {
    this.getCurrentPokemon().then(pokemonList => {
      this.setState({ pokemonList: pokemonList });
    });
  }

  getCurrentPokemon() {
    // TODO: change this URL for any live system
    const url = "https://pokemon.socialmicrolearning.com/current";

    return fetch(url)
      .then(response => {
        if (response.ok) {
          return response.json();
        }
        throw new Error("HTTP Error: ", response.status);
      })
      .then(jsonResponse => this.formatPokemonJson(jsonResponse))
      .catch(error => {
        console.warn("Pokemon Data could not be fetched, using fallback list.");
        console.debug(error);
        return this.props.detail.fullPokemonList;
      });
  }

  formatPokemonJson(jsonList) {
    if (!jsonList || jsonList.length == 0) {
      console.debug("jsonList is: " + jsonList);
      return this.props.detail.fullPokemonList;
    }
    let formattedList = { array: [] };
    for (let entry of jsonList) {
      let pokemon = {};
      pokemon.id = entry.PokemonId;
      pokemon.name = entry.Pokemons[0].Name;
      pokemon.imageUrl = entry.Pokemons[0].PokemonPictureFileNameLink;
      pokemon.isShiny = entry.Pokemons[0].isShiny;
      if (
        entry.Pokemons[0].HasForm &&
        entry.Pokemons[0].Form !== "Standardform"
      ) {
        pokemon.form = entry.Pokemons[0].Form;
      }

      formattedList["array"].push(pokemon);
    }
    return formattedList["array"];
  }

  updateHatched(event) {
    const _pokemonRaidBoss = this.state.pokemonRaidBoss;
    _pokemonRaidBoss.hatched = event.target.checked;

    this.setState(
      {
        pokemonRaidBoss: _pokemonRaidBoss,
      },
      this.update.bind(this)
    );
  }

  updateHatches({ value }) {
    const _pokemonRaidBoss = this.state.pokemonRaidBoss;
    _pokemonRaidBoss.hatches = value;

    this.setState(
      {
        pokemonRaidBoss: _pokemonRaidBoss,
      },
      this.update.bind(this)
    );
  }

  updateExpires({ value }) {
    const _pokemonRaidBoss = this.state.pokemonRaidBoss;
    _pokemonRaidBoss.expires = value;

    this.setState(
      {
        pokemonRaidBoss: _pokemonRaidBoss,
      },
      this.update.bind(this)
    );
  }

  updateLevel(level) {
    const _pokemonRaidBoss = this.state.pokemonRaidBoss;
    _pokemonRaidBoss.level = level;

    this.setState(
      {
        pokemonRaidBoss: _pokemonRaidBoss,
      },
      this.update.bind(this)
    );
  }

  isLevelChecked(option) {
    return (
      this.state.pokemonRaidBoss && this.state.pokemonRaidBoss.level === option
    );
  }

  updatePokemonFilter({ value }) {
    this.setState({
      pokemonFilter: value ? value.trim() : "",
    });
  }

  updatePokemon(id, form) {
    const _pokemonRaidBoss = this.state.pokemonRaidBoss;
    if (_pokemonRaidBoss.id == id && _pokemonRaidBoss.form === form) {
      _pokemonRaidBoss.id = undefined;
      _pokemonRaidBoss.form = undefined;
    } else {
      _pokemonRaidBoss.id = id;
      _pokemonRaidBoss.form = form;
    }

    console.debug(
      "Selected Pokemon:",
      this.pokemonRaidBoss,
      "based on (",
      id,
      ",",
      form,
      ")"
    );
    this.setState(
      {
        pokemonRaidBoss: _pokemonRaidBoss,
      },
      this.update.bind(this)
    );
    this.update(this.pokemonRaidBoss);
  }
}
PokemonRaidbossPicker.propTypes = {
  initialValue: PropTypes.object,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
