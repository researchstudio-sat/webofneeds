import React from "react";

import "~/style/_pokemon-raidboss-viewer.scss";
import PropTypes from "prop-types";
import { get } from "../../../utils.js";
import { selectLastUpdateTime } from "../../../redux/selectors/general-selectors.js";
import { relativeTime } from "../../../won-label-utils.js";
import { connect } from "react-redux";

const mapStateToProps = (state, ownProps) => {
  const id = get(ownProps.content, "id");
  const form = get(ownProps.content, "form");
  const level = get(ownProps.content, "level");
  const hatches = get(ownProps.content, "hatches");
  const expires = get(ownProps.content, "expires");
  const hatched = get(ownProps.content, "hatched");

  return {
    content: ownProps.content,
    detail: ownProps.detail,
    className: ownProps.className,
    pokemon:
      id &&
      ownProps.detail &&
      ownProps.detail.findPokemonById &&
      ownProps.detail.findPokemonById(id, form),
    form,
    level,
    levelLabel:
      level &&
      ownProps.detail &&
      ownProps.detail.getLevelLabel &&
      ownProps.detail.getLevelLabel(level),
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
};

class PokemonRaidbossViewer extends React.Component {
  render() {
    const icon = this.props.detail.icon && (
      <svg className="prbv__header__icon">
        <use xlinkHref={this.props.detail.icon} href={this.props.detail.icon} />
      </svg>
    );

    const label = this.props.detail.icon && (
      <span className="prbv__header__label">{this.props.detail.label}</span>
    );

    let levelClass;

    switch (this.props.level) {
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

    const level = this.props.level && (
      <div className={"prbv__content__level " + levelClass}>
        {this.props.levelLabel}
      </div>
    );

    const expiration = this.props.hasExpired ? (
      <div className="prbv__content__expires prbv__content__expires--expired">
        {"Has expired " +
          this.props.friendlyExpiresTime +
          " (" +
          this.props.expiresLocaleString +
          ")"}
      </div>
    ) : (
      <div className="prbv__content__expires">
        {"Expires " +
          this.props.friendlyExpiresTime +
          " (" +
          this.props.expiresLocaleString +
          ")"}
      </div>
    );

    const pokemon =
      this.props.hatched && this.props.pokemon ? (
        <div className="prbv__content__pokemon">
          <img
            className="prbv__content__pokemon__image"
            src={this.props.pokemon.imageUrl}
          />
          <div className="prbv__content__pokemon__id">
            {"#" + this.props.pokemon.id}
          </div>
          <div className="prbv__content__pokemon__name">
            {this.props.pokemon.name}
            {this.props.form ? (
              <span className="prbv__content__pokemon__name__form">
                {"(" + this.props.form + ")"}
              </span>
            ) : (
              undefined
            )}
          </div>
        </div>
      ) : (
        <div className="prbv__content__pokemon">
          <img
            className="prbv__content__pokemon__image prbv__content__pokemon__image--unhatched"
            src={this.props.detail.fullPokemonList[0].imageUrl}
          />
          <div className="prbv__content__pokemon__id">?</div>
          {this.props.shouldHaveHatched ? (
            <div className="prbv__content__pokemon__name">
              {"Should have hatched " +
                this.props.friendlyHatchesTime +
                " (" +
                this.props.hatchesLocaleString +
                ")"}
            </div>
          ) : (
            <div className="prbv__content__pokemon__name">
              {"Hatches " +
                this.props.friendlyHatchesTime +
                " (" +
                this.props.hatchesLocaleString +
                ")"}
            </div>
          )}
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
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
  pokemon: PropTypes.object,
  form: PropTypes.string,
  level: PropTypes.number,
  levelLabel: PropTypes.string,
  hatched: PropTypes.bool,
  shouldHaveHatched: PropTypes.bool,
  hasExpired: PropTypes.bool,
  friendlyHatchesTime: PropTypes.string,
  friendlyExpiresTime: PropTypes.string,
  hatchesLocaleString: PropTypes.string,
  expiresLocaleString: PropTypes.string,
};
export default connect(mapStateToProps)(PokemonRaidbossViewer);
