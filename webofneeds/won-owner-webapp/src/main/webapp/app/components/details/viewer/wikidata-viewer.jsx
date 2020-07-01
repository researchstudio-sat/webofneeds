import React, { useState, useEffect } from "react";
import PropTypes from "prop-types";

import jsonld from "jsonld/dist/jsonld.js";
import ico_loading_anim from "~/images/won-icons/ico_loading_anim.svg";
import "~/style/_wikidata-viewer.scss";
import won from "../../../service/won";

export default function WikiDataViewer({ content, detail, className }) {
  const [wikiDataContent, setWikiDataContent] = useState(undefined);

  const icon = detail.icon && (
    <svg className="wikidatav__header__icon">
      <use xlinkHref={detail.icon} href={detail.icon} />
    </svg>
  );

  const label = detail.icon && (
    <span className="wikidatav__header__label">{detail.label}</span>
  );

  useEffect(
    () => {
      if (content) {
        console.debug("TODO: Implement retrieval for wikidata url: ", content);
        const entityUri = content.replace(/\/$/, "");
        const entityId = entityUri.substr(entityUri.lastIndexOf("/") + 1);
        const specialDataUrl = `https://www.wikidata.org/wiki/Special:EntityData/${entityId}.jsonld`;
        fetch(specialDataUrl)
          .then(resp => resp.json())
          .then(jsonLdData =>
            jsonld.frame(jsonLdData, {
              "@id": entityUri, // start the framing from this uri. Otherwise will generate all possible nesting-variants.
              "@context": won.defaultContext,
              "@embed": "@always",
            })
          )
          .then(jsonLdData => setWikiDataContent(jsonLdData));
      }
    },
    [content]
  );

  return (
    <wikidata-viewer class={className}>
      <div className="wikidatav__header">
        {icon}
        {label}
      </div>
      <div className="wikidatav__content__data">
        {wikiDataContent ? (
          <div className="wikidatav__content__data">
            {JSON.stringify(wikiDataContent)}
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
      </div>
    </wikidata-viewer>
  );
}
WikiDataViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.string,
  className: PropTypes.string,
};
