import React from "react";

import "~/style/_pokemon-raidboss-viewer.scss";
import PropTypes from "prop-types";
import { actionCreators } from "../../../actions/actions";
import { get } from "../../../utils.js";
import { selectLastUpdateTime } from "../../../redux/selectors/general-selectors.js";
import { relativeTime } from "../../../won-label-utils.js";

export default class PokemonRaidbossViewer extends React.Component {
  componentDidMount() {
    this.disconnect = this.props.ngRedux.connect(
      this.selectFromState.bind(this),
      actionCreators
    )(state => {
      this.setState(state);
    });
  }

  componentWillUnmount() {
    this.disconnect();
  }

  UNSAFE_componentWillReceiveProps() {
    this.setState(this.selectFromState(this.props.ngRedux.getState()));
  }

  selectFromState(state) {
    const id = get(this.props.content, "id");
    const form = get(this.props.content, "form");
    const level = get(this.props.content, "level");
    const hatches = get(this.props.content, "hatches");
    const expires = get(this.props.content, "expires");
    const hatched = get(this.props.content, "hatched");

    return {
      pokemon:
        id &&
        this.props.detail &&
        this.props.detail.findPokemonById &&
        this.props.detail.findPokemonById(id, form),
      form,
      level,
      levelLabel:
        level &&
        this.props.detail &&
        this.props.detail.getLevelLabel &&
        this.props.detail.getLevelLabel(level),
      hatched,
      shouldHaveHatched: hatches && selectLastUpdateTime(state) > hatches,
      hasExpired: expires && selectLastUpdateTime(state) > expires,
      friendlyHatchesTime:
        hatches && relativeTime(selectLastUpdateTime(state), hatches),
      friendlyExpiresTime:
        expires && relativeTime(selectLastUpdateTime(state), expires),
      hatchesLocaleString: hatches && hatches.toLocaleString(),
      expiresLocaleString: expires && expires.toLocaleString(),
    };
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div />;
    }

    const icon = this.props.detail.icon && (
      <svg className="prbv__header__icon">
        <use xlinkHref={this.props.detail.icon} href={this.props.detail.icon} />
      </svg>
    );

    const label = this.props.detail.icon && (
      <span className="prbv__header__label">{this.props.detail.label}</span>
    );

    let levelClass;

    switch (this.state.level) {
      case 1:
      case 2:
        levelClass = "prbv__content__level--normal";
        break;
      case 3:
      case 4:
        levelClass = "prbv__content__level--rare";
        break;
      case 5:
        levelClass = "prbv__content__level--legendary";
        break;
      default:
        levelClass = "prbv__content__level--normal";
        break;
    }

    const level = this.state.level && (
      <div className={"prbv__content__level " + levelClass}>
        {this.state.levelLabel}
      </div>
    );

    const expiration = this.state.hasExpired ? (
      <div className="prbv__content__expires prbv__content__expires--expired">
        Has expired {this.state.friendlyExpiresTime} (
        {this.state.expiresLocaleString})
      </div>
    ) : (
      <div className="prbv__content__expires">
        Expires {this.state.friendlyExpiresTime} (
        {this.state.expiresLocaleString})
      </div>
    );

    const pokemon =
      this.state.hatched && this.state.pokemon ? (
        <div className="prbv__content__pokemon">
          <img
            className="prbv__content__pokemon__image"
            src={this.state.pokemon.imageUrl}
          />
          <div className="prbv__content__pokemon__id">
            #{this.state.pokemon.id}
          </div>
          <div className="prbv__content__pokemon__name">
            {this.state.pokemon.name}
            (this.state.form ?{" "}
            <span className="prbv__content__pokemon__name__form">
              {"(" + this.state.form + ")"}
            </span>{" "}
            : undefined)
          </div>
        </div>
      ) : (
        <div className="prbv__content__pokemon">
          <img
            className="prbv__content__pokemon__image prbv__content__pokemon__image--unhatched"
            src={this.props.detail.fullPokemonList[0].imageUrl}
          />
          <div className="prbv__content__pokemon__id">?</div>(
          this.state.shouldHaveHatched ? (
          <div className="prbv__content__pokemon__name">
            Should have hatched {this.state.friendlyHatchesTime}{" "}
            {"(" + this.state.hatchesLocaleString + ")"}
          </div>
          ) : (
          <div className="prbv__content__pokemon__name">
            Hatches {this.state.friendlyHatchesTime} (
            {"(" + this.state.hatchesLocaleString + ")"}
          </div>
          ) )
        </div>
      );

    return (
      <pokemon-raidboss-viewer class={this.props.className}>
        <div className="prbv__header">
          {icon}
          {label}
        </div>
        <div className="prbv__content">
          {level}
          {pokemon}
          {expiration}
        </div>
      </pokemon-raidboss-viewer>
    );
  }
}
PokemonRaidbossViewer.propTypes = {
  ngRedux: PropTypes.object.isRequired,
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
