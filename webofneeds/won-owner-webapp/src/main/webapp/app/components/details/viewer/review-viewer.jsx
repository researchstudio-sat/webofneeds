import React from "react";
import WonDescriptionViewer from "./description-viewer.jsx";
import { get } from "../../../utils.js";

import "~/style/_review-viewer.scss";
import PropTypes from "prop-types";

export default function WonReviewViewer({ detail, content, className }) {
  const reviewRating = get(content, "rating");
  const reviewText = get(content, "text");

  const icon = detail.icon && (
    <svg className="reviewv__header__icon">
      <use xlinkHref={detail.icon} href={detail.icon} />
    </svg>
  );

  const label = detail.icon && (
    <span className="reviewv__header__label">{detail.label}</span>
  );

  function getRatingLabel() {
    let ratingLabel;
    detail.rating &&
      detail.rating.forEach(rating => {
        if (rating.value === reviewRating) {
          ratingLabel = rating.label;
        }
      });
    return ratingLabel || reviewRating;
  }

  return (
    <won-review-viewer class={className}>
      <div className="reviewv__header">
        {icon}
        {label}
      </div>
      <div className="reviewv__content">
        <div className="reviewv__content__rating">{getRatingLabel()}</div>
        {reviewText ? (
          <WonDescriptionViewer content={reviewText} detail={{}} />
        ) : (
          undefined
        )}
      </div>
    </won-review-viewer>
  );
}
WonReviewViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
