import React from "react";

import { get } from "../../../utils.js";
import "~/style/_file-viewer.scss";
import ico36_uc_transport_demand from "~/images/won-icons/ico36_uc_transport_demand.svg";
import PropTypes from "prop-types";

export default class WonFileViewer extends React.Component {
  render() {
    const icon = this.props.detail.icon && (
      <svg className="filev__header__icon">
        <use xlinkHref={this.props.detail.icon} href={this.props.detail.icon} />
      </svg>
    );

    const label = this.props.detail.icon && (
      <span className="filev__header__label">{this.props.detail.label}</span>
    );

    const filesArray = this.props.content && this.props.content.toArray();

    const files =
      filesArray &&
      filesArray.map((file, index) => {
        return (
          <div className="filev__content__item" key={index}>
            <a
              className="filev__content__item__inner"
              href={
                "data:" +
                get(file, "encodingFormat") +
                ";base64," +
                get(file, "encoding")
              }
              download={get(file, "name")}
            >
              <svg className="filev__content__item__inner__typeicon">
                <use
                  xlinkHref={ico36_uc_transport_demand}
                  href={ico36_uc_transport_demand}
                />
              </svg>
              <div className="filev__content__item__inner__label">
                {get(file, "name")}
              </div>
            </a>
          </div>
        );
      });

    return (
      <won-file-viewer class={this.props.className}>
        <div className="filev__header">
          {icon}
          {label}
        </div>
        <div className="filev__content">{files}</div>
      </won-file-viewer>
    );
  }

  isImage(file) {
    return file && /^image\//.test(file.get("encodingFormat"));
  }

  openImageInNewTab(file) {
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
}
WonFileViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
