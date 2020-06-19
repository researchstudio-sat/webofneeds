import React, { useState, useEffect } from "react";

import PropTypes from "prop-types";
import WonDescriptionPicker from "./description-picker.jsx";

import "~/style/_reviewpicker.scss";

export default function WonReviewPicker({ initialValue, detail, onUpdate }) {
  const [state, setState] = useState({
    text: initialValue && initialValue.text,
    rating: (initialValue && initialValue.rating) || getDefaultRating(),
  });

  useEffect(
    () => {
      if (state.rating) {
        onUpdate({
          value: state,
        });
      } else {
        onUpdate({ value: undefined });
      }
    },
    [state]
  );

  function getDefaultRating() {
    let defaultRating;

    detail &&
      detail.rating.forEach(rating => {
        if (rating.default) defaultRating = rating.value;
      });

    return defaultRating;
  }

  return (
    <won-review-picker>
      <div className="reviewp__input">
        <select
          className="reviewp__input__rating"
          onChange={event => setState({ ...state, rating: event.target.value })}
          value={state.rating}
          disabled={!detail.rating || detail.rating.length <= 1}
        >
          {detail.rating &&
            detail.rating.map((r, index) => (
              <option key={r.value + "-" + index} value={r.value}>
                {r.label}
              </option>
            ))}
        </select>
        <WonDescriptionPicker
          onUpdate={({ value }) => setState({ ...state, text: value })}
          initialValue={initialValue && initialValue.text}
          detail={{ placeholder: detail.placeholder }}
        />
      </div>
    </won-review-picker>
  );
}
WonReviewPicker.propTypes = {
  initialValue: PropTypes.object,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
