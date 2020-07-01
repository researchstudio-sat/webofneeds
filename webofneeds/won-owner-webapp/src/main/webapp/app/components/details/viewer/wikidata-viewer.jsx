import React, { useState, useEffect } from "react";
import rdfFetch from "@rdfjs/fetch";
import cf from "clownface";

import PropTypes from "prop-types";
import Immutable from "immutable";
import * as useCaseUtils from "../../../usecase-utils";
import { get } from "../../../utils";

import ico_loading_anim from "~/images/won-icons/ico_loading_anim.svg";
import rdf_logo_1 from "~/images/won-icons/rdf_logo_1.svg";
import "~/style/_wikidata-viewer.scss";
import "~/style/_rdflink.scss";

export default function WikiDataViewer({ content, detail, className }) {
  const [wikiDataContent, setWikiDataContent] = useState(undefined);
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
      if (content) {
        const entityUri = content.replace(/\/$/, "");
        const entityId = entityUri.substr(entityUri.lastIndexOf("/") + 1);
        const specialDataUrl = `https://www.wikidata.org/wiki/Special:EntityData/${entityId}.jsonld`;

        rdfFetch(specialDataUrl)
          .then(response => response.dataset())
          .then(dataset => {
            const cfData = cf({ dataset });

            const cfEntity = cfData.namedNode(entityUri);

            return generateContentFromCF(cfEntity, detailsToParse);
          })
          .then(parsedContent =>
            setWikiDataContent(Immutable.fromJS(parsedContent))
          );
      }
    },
    [content]
  );

  const contentDetailsMap =
    wikiDataContent &&
    wikiDataContent.map((contentDetail, contentDetailKey) => {
      const detailDefinitionImm = get(allDetailsImm, contentDetailKey);
      if (detailDefinitionImm) {
        const detailDefinition = detailDefinitionImm.toJS();
        const ReactViewerComponent =
          detailDefinition && detailDefinition.viewerComponent;

        if (ReactViewerComponent) {
          return (
            <div key={contentDetailKey} className="pis__component">
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
        {contentDetailsMap ? (
          <div className="wikidatav__content__data">
            {contentDetailsMap.toArray()}
          </div>
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
        <a
          className="wikidatav__content__link rdflink clickable"
          target="_blank"
          rel="noopener noreferrer"
          href={content}
        >
          <svg className="rdflink__small">
            <use xlinkHref={rdf_logo_1} href={rdf_logo_1} />
          </svg>
          <span className="rdflink__label">WikiData Link</span>
        </a>
      </div>
    </wikidata-viewer>
  );
}
WikiDataViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.string,
  className: PropTypes.string,
};
