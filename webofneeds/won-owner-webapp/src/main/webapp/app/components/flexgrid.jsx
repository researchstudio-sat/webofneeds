import React, { useState } from "react";
import PropTypes from "prop-types";
import WonLabelledHr from "./labelled-hr.jsx";

import "~/style/_flexgrid.scss";
import ico16_arrow_up from "~/images/won-icons/ico16_arrow_up.svg";
import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";

export default function WonFlexGrid({ items, className }) {
  const [selectedIdx, setSelectedIdx] = useState(undefined);

  function openElement(index) {
    setSelectedIdx(selectedIdx ? undefined : index);
  }

  return (
    <won-flex-grid class={className ? className : ""}>
      {items.map((item, index) => {
        const detail = item.detail;
        const imageSrc = item.imageSrc;
        const svgSrc = item.svgSrc;
        const text = item.text;
        const text2 = item.text2;
        const separatorText = item.separatorText;

        return (
          <div className="flexgrid__item" key={index}>
            <div
              className={
                "fgi__block " + (detail !== undefined ? " clickable " : "")
              }
              onClick={() => openElement(index)}
            >
              {imageSrc && <img className="fgi__image" src={imageSrc} />}
              {svgSrc && (
                <svg className="fgi__image">
                  <use href={svgSrc} />
                </svg>
              )}
              {!text2 &&
                !separatorText && <span className="fgi__text">{text}</span>}
              {text2 &&
                separatorText && (
                  <span className="fgi__text">
                    {text}
                    <WonLabelledHr label={separatorText} />
                    {text2}
                  </span>
                )}
              {detail ? (
                index === selectedIdx ? (
                  <svg className="fgi__arrow">
                    <use xlinkHref={ico16_arrow_up} href={ico16_arrow_up} />
                  </svg>
                ) : (
                  <svg className="fgi__arrow">
                    <use xlinkHref={ico16_arrow_down} href={ico16_arrow_down} />
                  </svg>
                )
              ) : (
                undefined
              )}
            </div>
            {detail &&
              index === selectedIdx && (
                <span className="fgi__additionaltext">{detail}</span>
              )}
          </div>
        );
      })}
    </won-flex-grid>
  );
}
WonFlexGrid.propTypes = {
  items: PropTypes.arrayOf(PropTypes.object).isRequired,
  className: PropTypes.string,
};
