import React, { useCallback } from "react";

import "~/style/_file-dropzone.scss";
import PropTypes from "prop-types";
import { useDropzone } from "react-dropzone";
import { readAsDataURL } from "../utils.js";

import ico36_close from "~/images/won-icons/ico36_close.svg";
import illu_drag_here from "~/images/won-icons/illu_drag_here.svg";

export default function WonFileDropzone(props) {
  const onDrop = useCallback(acceptedFiles => {
    //Do something with the files
    console.debug("acceptedFiles: ", acceptedFiles);

    acceptedFiles.map(file => {
      readAsDataURL(file)
        .then(dataUrl => {
          const b64data = dataUrl.split("base64,")[1];
          const fileData = {
            name: file.name,
            encodingFormat: file.type,
            encoding: b64data,
          };
          return fileData;
        })
        .then(fileData => {
          props.onFilePicked({ file: fileData });
        });
    });
  }, []);
  const {
    getRootProps,
    getInputProps,
    isDragActive,
    isDragReject,
    isDragAccept,
  } = useDropzone({ onDrop });

  const icon = isDragReject ? ico36_close : illu_drag_here;

  return (
    <won-file-dropzone>
      <div
        {...getRootProps()}
        className={
          "wfd__dropzone " +
          (isDragAccept ? " valid " : "") +
          (isDragReject ? " invalid " : "")
        }
      >
        <input
          {...getInputProps()}
          accept={props.accepts}
          multiple={props.multiSelect}
        />

        <React.Fragment>
          <svg className="wfd__dropzone__icon">
            <use xlinkHref={icon} href={icon} />
          </svg>
          {isDragActive ? (
            <p className="wfd__dropzone__label">
              {"Drop the file" +
                (props.multiSelect ? "s" : "") +
                " here..." +
                props.accepts}
            </p>
          ) : (
            <p className="wfd__dropzone__label">
              {"Drag some file" +
                (props.multiSelect ? "s" : "") +
                " here, or click to select " +
                (props.multiSelect ? "files" : "a file")}
            </p>
          )}
        </React.Fragment>
      </div>
    </won-file-dropzone>
  );
}
WonFileDropzone.propTypes = {
  onFilePicked: PropTypes.func.isRequired,
  accepts: PropTypes.string,
  multiSelect: PropTypes.bool,
};
