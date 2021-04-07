import React, { useState } from "react";

import { get } from "../../../utils.js";
import "~/style/_image-viewer.scss";
import PropTypes from "prop-types";
import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";

export default function WonImageViewer({ detail, content, className }) {
  const [expanded, toggleExpanded] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(0);

  const icon = detail.icon && (
    <svg className="imagev__header__icon">
      <use xlinkHref={detail.icon} href={detail.icon} />
    </svg>
  );

  const imagesArray = content && content.toArray();

  const label = detail.icon && (
    <span className="imagev__header__label">
      {detail.label + (imagesArray ? " (" + imagesArray.length + ")" : "")}
    </span>
  );

  function isImage(file) {
    return file && /^image\//.test(file.get("encodingFormat"));
  }

  function getSelectedImage() {
    return content && content.toArray()[selectedIndex];
  }

  function openImageInNewTab(file) {
    if (file) {
      let image = new Image();
      image.src =
        "data:" +
        get(file, "encodingFormat") +
        ";base64," +
        get(file, "encoding");

      let w = window.open("");
      w.document.write(image.outerHTML);
    }
  }

  const thumbNails =
    imagesArray &&
    imagesArray.map((file, index) => {
      if (isImage(file)) {
        return (
          <div
            className={
              "imagev__content__thumbnails__thumbnail " +
              (selectedIndex === index
                ? "imagev__content__thumbnails__thumbnail--selected"
                : "")
            }
            key={index}
          >
            <img
              className="imagev__content__thumbnails__thumbnail__image"
              onClick={() => setSelectedIndex(index)}
              alt={get(file, "name")}
              src={
                "data:" +
                get(file, "encodingFormat") +
                ";base64," +
                get(file, "encoding")
              }
            />
          </div>
        );
      }
    });

  return (
    <won-image-viewer class={className}>
      <div
        className="imagev__header clickable"
        onClick={() => toggleExpanded(!expanded)}
      >
        {icon}
        {label}
        <svg
          className={
            "imagev__header__carret " +
            (expanded
              ? " imagev__header__carret--expanded "
              : " imagev__header__carret--collapsed ")
          }
        >
          <use xlinkHref={ico16_arrow_down} href={ico16_arrow_down} />
        </svg>
      </div>
      {expanded ? (
        <div className="imagev__content">
          <div className="imagev__content__selected">
            <img
              className="imagev__content__selected__image"
              onClick={() => openImageInNewTab(getSelectedImage())}
              alt={getSelectedImage().get("name")}
              src={
                "data:" +
                getSelectedImage().get("encodingFormat") +
                ";base64," +
                getSelectedImage().get("encoding")
              }
            />
          </div>
          {content.size > 1 && (
            <div className="imagev__content__thumbnails">{thumbNails}</div>
          )}
        </div>
      ) : (
        undefined
      )}
    </won-image-viewer>
  );
}
WonImageViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
