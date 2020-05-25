import React, { useState } from "react";
import PropTypes from "prop-types";
import ReactMarkdown from "react-markdown";

import "~/style/_accordion.scss";
import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";
import ico16_arrow_up from "~/images/won-icons/ico16_arrow_up.svg";

export default function WonAccordion({ items, className }) {
  const [selectedIdx, setSelectedIdx] = useState(undefined);

  function openElement(index) {
    setSelectedIdx(index === selectedIdx ? undefined : index);
  }

  return (
    <won-accordion className={className ? className : ""}>
      {items.map((item, index) => (
        <div
          key={index}
          className="accordion__element clickable"
          onClick={() => openElement(index)}
        >
          <div className="header clickable">{item.title}</div>
          {index !== selectedIdx ? (
            <svg className="arrow clickable">
              <use xlinkHref={ico16_arrow_down} href={ico16_arrow_down} />
            </svg>
          ) : (
            <React.Fragment>
              <svg className="arrow clickable">
                <use xlinkHref={ico16_arrow_up} href={ico16_arrow_up} />
              </svg>
              <ReactMarkdown
                className="detail markdown"
                source={item.detail}
                linkTarget="_blank"
              />
            </React.Fragment>
          )}
        </div>
      ))}
    </won-accordion>
  );
}
WonAccordion.propTypes = {
  items: PropTypes.arrayOf(PropTypes.object).isRequired,
  className: PropTypes.string,
};
