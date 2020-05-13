/**
 * Created by fsuda on 21.08.2017.
 */

import React from "react";

import "~/style/_labelledhr.scss";
import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";
import PropTypes from "prop-types";

export default function WonLabelledHr({ arrow, label, onClick, className }) {
  return (
    <won-labelled-hr class={className ? className : ""}>
      <div className="wlh__label">
        <span className="wlh__label__text">{label}</span>
        {arrow ? (
          <svg
            onClick={onClick}
            className={
              "wlh__label__carret clickable " +
              (arrow === "up"
                ? " wlh__label__carret--expanded "
                : " wlh__label__carret--collapsed ")
            }
          >
            <use xlinkHref={ico16_arrow_down} href={ico16_arrow_down} />
          </svg>
        ) : (
          undefined
        )}
      </div>
    </won-labelled-hr>
  );
}

WonLabelledHr.propTypes = {
  label: PropTypes.string.isRequired,
  arrow: PropTypes.string,
  onClick: PropTypes.func,
  className: PropTypes.string,
};
