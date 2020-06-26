import React, { useState, useEffect } from "react";

import { get } from "../../../utils.js";
import "~/style/_wikidatapicker.scss";
import PropTypes from "prop-types";
import _ from "lodash";
import WonTitlePicker from "./title-picker";

export default function WikiDataPicker({
  initialValue,
  detail,
  className,
  onUpdate,
}) {
  const [searchText, setSearchText] = useState(
    initialValue === undefined ? "" : initialValue
  );
  const [searchResults, setSearchResults] = useState([]);

  const startSearch = _.debounce(value => {
    const url = `https://www.wikidata.org/w/api.php?action=wbsearchentities&search=${encodeURIComponent(
      value
    )}&format=json&language=en&limit=20&origin=*`;

    fetch(url)
      .then(resp => resp.json())
      .then(results => setSearchResults(get(results, "search") || []));
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
    },
    [searchText]
  );

  return (
    <wikidata-picker class={className ? className : ""}>
      <div className="wikidatap__input">
        <WonTitlePicker
          onUpdate={({ value }) => setSearchText(value)}
          initialValue={searchText}
          detail={{ placeholder: detail && detail.placeholder }}
        />

        <div className="wikidatap__input__results">
          {searchResults.map((result, index) => (
            <div
              className="wikidatap__input__results__result"
              key={index}
              onClick={() => onUpdate({ value: result.concepturi })}
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
      </div>
    </wikidata-picker>
  );
}

WikiDataPicker.propTypes = {
  className: PropTypes.string,
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
