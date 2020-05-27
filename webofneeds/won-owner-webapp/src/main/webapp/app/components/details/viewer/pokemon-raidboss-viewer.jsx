import React from "react";

import "~/style/_pokemon-raidboss-viewer.scss";
import PropTypes from "prop-types";
import { get } from "../../../utils.js";
import * as generalSelectors from "../../../redux/selectors/general-selectors.js";
import { relativeTime } from "../../../won-label-utils.js";
import { useSelector } from "react-redux";

export default function PokemonRaidbossViewer({ content, detail, className }) {
  const id = get(content, "id");
  const form = get(content, "form");
  const level = get(content, "level");
  const hatches = get(content, "hatches");
  const expires = get(content, "expires");
  const hatched = get(content, "hatched");

  const globalLastUpdateTime = useSelector(
    generalSelectors.selectLastUpdateTime
  );
  const pokemon =
    id && detail && detail.findPokemonById && detail.findPokemonById(id, form);
  const levelLabel =
    level && detail && detail.getLevelLabel && detail.getLevelLabel(level);

  const shouldHaveHatched = hatches && globalLastUpdateTime > hatches;
  const hasExpired = expires && globalLastUpdateTime > expires;
  const friendlyHatchesTime =
    hatches && relativeTime(globalLastUpdateTime, hatches);
  const friendlyExpiresTime =
    expires && relativeTime(globalLastUpdateTime, expires);
  const hatchesLocaleString = hatches && hatches.toLocaleString();
  const expiresLocaleString = expires && expires.toLocaleString();

  const icon = detail.icon && (
    <svg className="prbv__header__icon">
      <use xlinkHref={detail.icon} href={detail.icon} />
    </svg>
  );

  const label = detail.label && (
    <span className="prbv__header__label">{detail.label}</span>
  );

  let levelClass;

  switch (level) {
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

  return (
    <pokemon-raidboss-viewer class={className}>
      <div className="prbv__header">
        {icon}
        {label}
      </div>
      <div className="prbv__content">
        {level ? (
          <div className={"prbv__content__level " + levelClass}>
            {levelLabel}
          </div>
        ) : (
          undefined
        )}
        {hatched && pokemon ? (
          <div className="prbv__content__pokemon">
            <img
              className="prbv__content__pokemon__image"
              src={pokemon.imageUrl}
            />
            <div className="prbv__content__pokemon__id">{"#" + pokemon.id}</div>
            <div className="prbv__content__pokemon__name">
              {pokemon.name}
              {form ? (
                <span className="prbv__content__pokemon__name__form">
                  {"(" + form + ")"}
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
              src={detail.fullPokemonList[0].imageUrl}
            />
            <div className="prbv__content__pokemon__id">?</div>
            {shouldHaveHatched ? (
              <div className="prbv__content__pokemon__name">
                {"Should have hatched " +
                  friendlyHatchesTime +
                  " (" +
                  hatchesLocaleString +
                  ")"}
              </div>
            ) : (
              <div className="prbv__content__pokemon__name">
                {"Hatches " +
                  friendlyHatchesTime +
                  " (" +
                  hatchesLocaleString +
                  ")"}
              </div>
            )}
          </div>
        )}
        {hasExpired ? (
          <div className="prbv__content__expires prbv__content__expires--expired">
            {"Has expired " +
              friendlyExpiresTime +
              " (" +
              expiresLocaleString +
              ")"}
          </div>
        ) : (
          <div className="prbv__content__expires">
            {"Expires " +
              friendlyExpiresTime +
              " (" +
              expiresLocaleString +
              ")"}
          </div>
        )}
      </div>
    </pokemon-raidboss-viewer>
  );
}
PokemonRaidbossViewer.propTypes = {
  content: PropTypes.object,
  detail: PropTypes.object,
  className: PropTypes.string,
};
