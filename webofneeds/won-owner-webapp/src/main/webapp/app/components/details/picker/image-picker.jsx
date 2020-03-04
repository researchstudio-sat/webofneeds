import React from "react";

import "~/style/_imagepicker.scss";
import PropTypes from "prop-types";
import WonFileDropzone from "../../file-dropzone";

export default class WonImagePicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      addedImages: props.initialValue || [],
    };
  }

  render() {
    let imagePreviewContainer;

    if (this.state.addedImages && this.state.addedImages.length > 0) {
      const imagePreviewElementArray = this.state.addedImages.map(
        (image, index) => {
          if (this.isImage(image)) {
            return (
              <div
                key={image.name + "-" + index}
                className={
                  "imagep__preview__item " +
                  (image.default ? "imagep__preview__item--default" : "")
                }
              >
                <div
                  className="imagep__preview__item__label"
                  onClick={() => this.setImageAsDefault(image)}
                >
                  {image.name}
                </div>
                <svg
                  className="imagep__preview__item__remove"
                  onClick={() => this.removeImage(image)}
                >
                  <use xlinkHref="#ico36_close" href="#ico36_close" />
                </svg>
                <img
                  className="imagep__preview__item__image"
                  onClick={() => this.setImageAsDefault(image)}
                  alt={image.name}
                  src={
                    "data:" + image.escodingFormat + ";base64," + image.encoding
                  }
                />
                <div
                  className="imagep__preview__item__default"
                  onClick={() => this.setImageAsDefault(image)}
                >
                  {image.default ? (
                    <span>Default Image</span>
                  ) : (
                    <span className="imagep__preview__item__default__set">
                      Click to set Image as Default
                    </span>
                  )}
                </div>
              </div>
            );
          }
        }
      );

      imagePreviewContainer = (
        <React.Fragment>
          <div className="imagep__header">
            {"Chosen Image" + (this.state.addedImages.length > 1 ? "s:" : ":")}
          </div>
          <div className="imagep__preview">{imagePreviewElementArray}</div>
        </React.Fragment>
      );
    }

    return (
      <won-image-picker>
        {(this.props.detail.multiSelect ||
          !this.state.addedImages ||
          this.state.addedImages.length == 0) && (
          <WonFileDropzone
            onFilePicked={this.updateImages.bind(this)}
            accepts={this.props.detail.accepts}
            multiSelect={this.props.detail.multiSelect}
          />
        )}
        {imagePreviewContainer}
      </won-image-picker>
    );
  }

  isImage(file) {
    return file && /^image\//.test(file.encodingFormat);
  }

  removeImage(imageToRemove) {
    let _addedImages = this.state.addedImages || [];

    _addedImages = _addedImages.filter(image => imageToRemove !== image);

    const hasDefaultImage = !!_addedImages.find(image => image.default);

    if (!hasDefaultImage && _addedImages.length > 0) {
      _addedImages[0].default = true;
    }

    this.setState(
      {
        addedImages: _addedImages,
      },
      this.update.bind(this)
    );
  }

  setImageAsDefault(imageToSetAsDefault) {
    if (imageToSetAsDefault) {
      let _addedImages = this.state.addedImages;

      _addedImages = _addedImages.map(image => {
        image.default = image === imageToSetAsDefault;
        return image;
      });

      this.setState(
        {
          addedImages: _addedImages,
        },
        this.update.bind(this)
      );
    }
  }

  /**
   * Checks validity and uses callback method
   */
  update() {
    if (this.state.addedImages && this.state.addedImages.length > 0) {
      this.props.onUpdate({ value: this.state.addedImages });
    } else {
      this.props.onUpdate({ value: undefined });
    }
  }

  updateImages({ file }) {
    if (this.isImage(file)) {
      const _addedImages = this.state.addedImages || [];
      if (_addedImages.length == 0) {
        file.default = true;
      }
      _addedImages.push(file);

      this.setState(
        {
          addedImages: _addedImages,
        },
        this.update.bind(this)
      );
    }
  }
}
WonImagePicker.propTypes = {
  initialValue: PropTypes.array,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
