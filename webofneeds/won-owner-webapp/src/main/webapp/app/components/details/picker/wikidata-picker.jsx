import React, { useState, useEffect } from "react";
import Immutable from "immutable";

import { get } from "../../../utils.js";
import "~/style/_wikidatapicker.scss";
import PropTypes from "prop-types";
import _ from "lodash";
import WonTitlePicker from "./title-picker";
import WikiDataViewer from "~/app/components/details/viewer/wikidata-viewer";
import { searchWikiData } from "~/app/api/wikidata-api";

export default function WikiDataPicker({
  initialValue,
  detail,
  className,
  onUpdate,
}) {
  const [searchText, setSearchText] = useState("");
  const [wikiDataUris, setWikiDataUris] = useState(
    initialValue === undefined ? Immutable.Set() : Immutable.Set(initialValue)
  );
  const [showSearchResults, toggleShowSearchResults] = useState(
    searchText && searchText.length > 0
  );
  const [searchResults, setSearchResults] = useState([]);

  const startSearch = _.debounce(value => {
    searchWikiData(value).then(results =>
      setSearchResults(get(results, "search") || [])
    );
  }, 700);

  function resetClassifiedAsSearchInput() {
    setSearchText("");
    setSearchResults([]);
  }

  useEffect(
    () => {
      const classifiedAsInputValue = searchText && searchText.trim();

      if (!!classifiedAsInputValue && classifiedAsInputValue.length > 0) {
        classifiedAsInputValue.length > 2 &&
          startSearch(classifiedAsInputValue);
      } else {
        resetClassifiedAsSearchInput();
      }
      toggleShowSearchResults(true);
    },
    [searchText]
  );

  function selectWikiDataUri(conceptUri) {
    const newWikiDataUris = wikiDataUris.add(conceptUri);
    onUpdate({ value: newWikiDataUris.toArray() });
    setWikiDataUris(newWikiDataUris);
    setSearchText("");
    toggleShowSearchResults(false);
  }

  function removeWikiDataUri(conceptUri) {
    const newWikiDataUris = wikiDataUris.remove(conceptUri);
    onUpdate({ value: newWikiDataUris.toArray() });
    setWikiDataUris(newWikiDataUris);
  }

  return (
    <wikidata-picker class={className ? className : ""}>
      <div className="wikidatap__input">
        <WonTitlePicker
          onUpdate={({ value }) => setSearchText(value)}
          initialValue={searchText}
          detail={{ placeholder: detail && detail.placeholder }}
        />

        {showSearchResults && (
          <div className="wikidatap__input__results">
            {searchResults.map((result, index) => (
              <div
                className="wikidatap__input__results__result"
                key={index}
                onClick={() => selectWikiDataUri(result.concepturi)}
              >
                <span className="wikidatap__input__results__result__label">
                  {result.label}
                </span>
                <span className="wikidatap__input__results__result__concepturi">
                  {result.concepturi}
                </span>
              </div>
            ))}
          </div>
        )}
        {wikiDataUris &&
          wikiDataUris.map(wikiDataUri => (
            <div className="wikidatap__input__selected" key={wikiDataUri}>
              <div className="wikidatap__input__selected__header">
                <span className="wikidatap__input__selected__header__title">
                  Selected
                </span>
                <button
                  className="wikidatap__input__selected__header__clearButton won-button--filled red"
                  onClick={() => removeWikiDataUri(wikiDataUri)}
                >
                  Remove
                </button>
              </div>
              <WikiDataViewer
                className="wikidatap__input__selected__content"
                content={Immutable.fromJS([wikiDataUri])}
                detail={{}}
              />
            </div>
          ))}
      </div>
    </wikidata-picker>
  );
}

WikiDataPicker.propTypes = {
  className: PropTypes.string,
  initialValue: PropTypes.oneOfType([
    PropTypes.arrayOf(PropTypes.string),
    PropTypes.object,
  ]),
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
