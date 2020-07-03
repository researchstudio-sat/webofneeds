import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import rdfFetch from "@rdfjs/fetch";
import cf from "clownface";

import PropTypes from "prop-types";
import Immutable from "immutable";
import * as useCaseUtils from "../../../usecase-utils";
import * as generalSelectors from "../../../redux/selectors/general-selectors.js";
import { get } from "../../../utils";

import ico_loading_anim from "~/images/won-icons/ico_loading_anim.svg";
import rdf_logo_1 from "~/images/won-icons/rdf_logo_1.svg";
import "~/style/_wikidata-viewer.scss";
import "~/style/_rdflink.scss";
import { actionCreators } from "~/app/actions/actions";
import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";

export default function WikiDataViewer({ content, detail, className }) {
  const dispatch = useDispatch();
  const entityUri = content && content.replace(/\/$/, "");
  const wikiDataContent = useSelector(
    generalSelectors.getExternalData(entityUri)
  );

  const [showAdditionalData, toggleAdditionalData] = useState(false);
  const detailsToParse = useCaseUtils.getAllDetails();
  const allDetailsImm = useCaseUtils.getAllDetailsImm();

  const icon = detail.icon && (
    <svg className="wikidatav__header__icon">
      <use xlinkHref={detail.icon} href={detail.icon} />
    </svg>
  );

  const label = detail.icon && (
    <span className="wikidatav__header__label">{detail.label}</span>
  );

  function generateContentFromCF(cfEntityData, detailsToParse) {
    let content = {};
    if (cfEntityData && detailsToParse) {
      for (const detailKey in detailsToParse) {
        const detailToParse = detailsToParse[detailKey];
        const detailIdentifier = detailToParse && detailToParse.identifier;
        const detailValue =
          detailToParse &&
          detailToParse.parseFromCF &&
          detailToParse.parseFromCF(cfEntityData);

        if (detailIdentifier && detailValue) {
          content[detailIdentifier] = detailValue;
        }
      }
    }

    return content;
  }

  useEffect(
    () => {
      if (entityUri && !wikiDataContent) {
        const entityId = entityUri.substr(entityUri.lastIndexOf("/") + 1);
        const specialDataUrl = `https://www.wikidata.org/wiki/Special:EntityData/${entityId}.ttl`;

        rdfFetch(specialDataUrl)
          .then(response => response.dataset())
          .then(dataset => {
            const cfData = cf({ dataset });

            const cfEntity = cfData.namedNode(entityUri);

            return generateContentFromCF(cfEntity, detailsToParse);
          })
          .then(parsedContent => {
            dispatch(
              actionCreators.externalData__store(
                Immutable.fromJS({ [entityUri]: parsedContent })
              )
            );
          });
      }
    },
    [entityUri]
  );

  const title = get(wikiDataContent, "title");
  const personaName = get(wikiDataContent, "personaName");
  const imageUrl = get(wikiDataContent, "imageUrl");

  const dataElement = (
    <div className="wikidatav__content__data">
      {imageUrl ? (
        <a
          className="wikidatav__content__data__image clickable"
          href={imageUrl}
          target="_blank"
          rel="noopener noreferrer"
        >
          <img src={imageUrl} />
        </a>
      ) : (
        <div />
      )}
      {(title || personaName) && (
        <div className="wikidatav__content__data__title">
          {title || personaName}
        </div>
      )}
    </div>
  );

  const additionalContentDetailsMap =
    wikiDataContent &&
    wikiDataContent
      .filter(
        (_, contentDetailKey) =>
          contentDetailKey !== "imageUrl" &&
          //if there was a title, we add the personaName to the additional Details so we do not lose information
          (title || contentDetailKey !== "personaName") &&
          contentDetailKey !== "title"
      )
      .map((contentDetail, contentDetailKey) => {
        const detailDefinitionImm = get(allDetailsImm, contentDetailKey);
        if (detailDefinitionImm) {
          const detailDefinition = detailDefinitionImm.toJS();
          const ReactViewerComponent =
            detailDefinition && detailDefinition.viewerComponent;

          if (ReactViewerComponent) {
            return (
              <div
                key={contentDetailKey}
                className="wikidatav__content__additionalData__content__detail"
              >
                <ReactViewerComponent
                  detail={detailDefinition}
                  content={contentDetail}
                />
              </div>
            );
          }
        }

        return undefined;
      });

  return (
    <wikidata-viewer class={className}>
      <div className="wikidatav__header">
        {icon}
        {label}
      </div>
      <div className="wikidatav__content">
        {wikiDataContent ? (
          <React.Fragment>
            {dataElement}
            {additionalContentDetailsMap && (
              <div className="wikidatav__content__additionalData">
                <div
                  className={
                    "wikidatav__content__additionalData__header clickable " +
                    (showAdditionalData
                      ? " wikidatav__content__additionalData__header--expanded "
                      : " wikidatav__content__additionalData__header--collapsed ")
                  }
                  onClick={() => toggleAdditionalData(!showAdditionalData)}
                >
                  <svg className="wikidatav__content__additionalData__header__carret">
                    <use xlinkHref={ico16_arrow_down} href={ico16_arrow_down} />
                  </svg>
                  <div className="wikidatav__content__additionalData__header__title">
                    Additional Data
                  </div>
                </div>
                {showAdditionalData && (
                  <React.Fragment>
                    <div className="wikidatav__content__additionalData__content">
                      {additionalContentDetailsMap.toArray()}
                    </div>
                    <a
                      className="wikidatav__content__additionalData__content__link rdflink clickable"
                      target="_blank"
                      rel="noopener noreferrer"
                      href={content}
                    >
                      <svg className="rdflink__small">
                        <use xlinkHref={rdf_logo_1} href={rdf_logo_1} />
                      </svg>
                      <span className="rdflink__label">WikiData Link</span>
                    </a>
                  </React.Fragment>
                )}
              </div>
            )}
          </React.Fragment>
        ) : (
          <div className="wikidatav__content__loading">
            <svg className="wikidatav__content__loading__spinner hspinner">
              <use xlinkHref={ico_loading_anim} href={ico_loading_anim} />
            </svg>
            <span className="wikidatav__content__loading__label">
              Loading Data...
            </span>
          </div>
        )}
      </div>
    </wikidata-viewer>
  );
}
WikiDataViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.string,
  className: PropTypes.string,
};
