import React from "react";

import { get } from "../../../utils.js";
import "~/style/_image-viewer.scss";
import PropTypes from "prop-types";

export default class WonImageViewer extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      selectedIndex: 0,
    };
  }

  render() {
    const icon = this.props.detail.icon && (
      <svg className="imagev__header__icon">
        <use xlinkHref={this.props.detail.icon} href={this.props.detail.icon} />
      </svg>
    );

    const label = this.props.detail.icon && (
      <span className="imagev__header__label">{this.props.detail.label}</span>
    );

    const imagesArray = this.props.content && this.props.content.toArray();

    const thumbNails =
      imagesArray &&
      imagesArray.map((file, index) => {
        if (this.isImage(file)) {
          return (
            <div
              className={
                "imagev__content__thumbnails__thumbnail " +
                (this.state.selectedIndex == index
                  ? "imagev__content__thumbnails__thumbnail--selected"
                  : "")
              }
              key={index}
            >
              <img
                className="imagev__content__thumbnails__thumbnail__image"
                onClick={() => this.changeSelectedIndex(index)}
                alt={get(file, "name")}
                src={
                  "data:" + get(file, "type") + ";base64," + get(file, "data")
                }
              />
            </div>
          );
        }
      });

    return (
      <won-image-viewer class={this.props.className}>
        <div className="imagev__header">
          {icon}
          {label}
        </div>
        <div className="imagev__content">
          <div className="imagev__content__selected">
            <img
              className="imagev__content__selected__image"
              onClick={() => this.openImageInNewTab(this.getSelectedImage())}
              alt={this.getSelectedImage().get("name")}
              src={
                "data:" +
                this.getSelectedImage().get("type") +
                ";base64," +
                this.getSelectedImage().get("data")
              }
            />
          </div>
          {this.props.content.size > 1 && (
            <div className="imagev__content__thumbnails">{thumbNails}</div>
          )}
        </div>
      </won-image-viewer>
    );
  }

  isImage(file) {
    return file && /^image\//.test(file.get("type"));
  }

  changeSelectedIndex(index) {
    this.setState({ selectedIndex: index });
  }

  getSelectedImage() {
    return (
      this.props.content &&
      this.props.content.toArray()[this.state.selectedIndex]
    );
  }

  openImageInNewTab(file) {
    if (file) {
      let image = new Image();
      image.src = "data:" + get(file, "type") + ";base64," + get(file, "data");

      let w = window.open("");
      w.document.write(image.outerHTML);
    }
  }
}
WonImageViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
