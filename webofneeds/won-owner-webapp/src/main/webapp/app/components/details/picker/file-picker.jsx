import React from "react";

import "~/style/_filepicker.scss";
import PropTypes from "prop-types";
import WonFileDropzone from "../../file-dropzone";

export default class WonFilePicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      addedFiles: props.initialValue || [],
    };
  }

  render() {
    let filePreviewContainer;

    if (this.state.addedFiles && this.state.addedFiles.length > 0) {
      const filePreviewElementArray = this.state.addedFiles.map(
        (file, index) => (
          <div className="filep__preview__item" key={file.name + "-" + index}>
            {this.isImage(file) ? (
              <img
                className="filep__preview__item__image"
                alt={file.name}
                src={"data:" + file.encodingFormat + ";base64," + file.encoding}
              />
            ) : (
              <svg className="filep__preview__item__typeicon">
                <use
                  xlinkHref="#ico36_uc_transport_demand"
                  href="#ico36_uc_transport_demand"
                />
              </svg>
            )}

            <div className="filep__preview__item__label">{file.name}</div>
            <svg
              className="filep__preview__item__remove"
              onClick={() => this.removeFile(file)}
            >
              <use xlinkHref="#ico36_close" href="#ico36_close" />
            </svg>
          </div>
        )
      );

      filePreviewContainer = (
        <React.Fragment>
          <div className="filep__header">
            {"Chosen File" + (this.state.addedFiles.length > 1 ? "s:" : ":")}
          </div>
          <div className="filep__preview">{filePreviewElementArray}</div>
        </React.Fragment>
      );
    }

    return (
      <won-file-picker>
        {(this.props.detail.multiSelect ||
          !this.state.addedFiles ||
          this.state.addedFiles.length == 0) && (
          <WonFileDropzone
            onFilePicked={this.updateFiles.bind(this)}
            accepts={this.props.detail.accepts}
            multiSelect={this.props.detail.multiSelect}
          />
        )}
        {filePreviewContainer}
      </won-file-picker>
    );
  }

  isImage(file) {
    return file && /^image\//.test(file.encodingFormat);
  }

  removeFile(fileToRemove) {
    this.setState(
      {
        addedFiles: this.state.addedFiles
          ? this.state.addedFiles.filter(file => fileToRemove !== file)
          : [],
      },
      this.update.bind(this)
    );
  }

  /**
   * Checks validity and uses callback method
   */
  update() {
    if (this.state.addedFiles && this.state.addedFiles.length > 0) {
      this.props.onUpdate({ value: this.state.addedFiles });
    } else {
      this.props.onUpdate({ value: undefined });
    }
  }

  updateFiles({ file }) {
    const _addedFiles = this.state.addedFiles || [];
    _addedFiles.push(file);
    this.setState(
      {
        addedFiles: _addedFiles,
      },
      this.update.bind(this)
    );
  }
}
WonFilePicker.propTypes = {
  initialValue: PropTypes.array,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
