import React from "react";

import { get } from "../../../utils.js";
import "~/style/_file-viewer.scss";
import PropTypes from "prop-types";

export default class WonFileViewer extends React.Component {
  render() {
    const icon = this.props.detail.icon && (
      <svg className="titlev__header__icon">
        <use xlinkHref={this.props.detail.icon} href={this.props.detail.icon} />
      </svg>
    );

    const label = this.props.detail.icon && (
      <span className="titlev__header__label">{this.props.detail.label}</span>
    );

    const filesArray = this.props.content && this.props.content.toArray();

    const files =
      filesArray &&
      filesArray.map((file, index) => {
        return (
          <div className="filev__content__item" key={index}>
            {this.isImage(file) ? (
              <a
                className="filev__content__item__inner"
                href={
                  "data:" + get(file, "type") + ";base64," + get(file, "data")
                }
                download={get(file, "name")}
              >
                <svg className="filev__content__item__inner__typeicon">
                  <use
                    xlinkHref="#ico36_uc_transport_demand"
                    href="#ico36_uc_transport_demand"
                  />
                </svg>
                <div className="filev__content__item__inner__label">
                  {get(file, "name")}
                </div>
              </a>
            ) : (
              <a
                className="filev__content__item__inner"
                onClick={() => this.openImageInNewTab(file)}
              >
                <img
                  className="filev__content__item__inner__image"
                  alt={get(file, "name")}
                  src={
                    "data:" + get(file, "type") + ";base64," + get(file, "data")
                  }
                />
                <div className="filev__content__item__inner__label">
                  {get(file, "name")}
                </div>
              </a>
            )}
          </div>
        );
      });

    return (
      <won-file-viewer class={this.props.className}>
        <div className="titlev__header">
          {icon}
          {label}
        </div>
        <div className="titlev__content">{files}</div>
      </won-file-viewer>
    );
  }

  isImage(file) {
    return file && /^image\//.test(file.get("type"));
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
WonFileViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
