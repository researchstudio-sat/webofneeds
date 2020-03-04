import { generateIdString, get } from "../../app/utils.js";
import Immutable from "immutable";
import WonFileViewer from "../../app/components/details/viewer/file-viewer.jsx";
import WonImageViewer from "../../app/components/details/viewer/image-viewer.jsx";

import WonFilePicker from "../../app/components/details/picker/file-picker.jsx";
import WonImagePicker from "../../app/components/details/picker/image-picker.jsx";

export const files = {
  identifier: "files",
  label: "Files",
  icon: "#ico36_detail_files",
  placeholder: "",
  accepts: "",
  multiSelect: true,
  component: WonFilePicker,
  viewerComponent: WonFileViewer,
  messageEnabled: true,
  parseToRDF: function({ value, identifier, contentUri }) {
    if (!value) {
      return { "con:file": undefined };
    }
    let payload = [];
    value.forEach(file => {
      if (file.name && file.encodingFormat && file.encoding) {
        let f = {
          "@id":
            contentUri && identifier
              ? contentUri + "#" + identifier + "-" + generateIdString(10)
              : undefined,
          "@type": "s:MediaObject",
          "s:name": file.name,
          "s:encodingFormat": file.encodingFormat,
          "s:encoding": file.encoding,
        };

        payload.push(f);
      }
    });
    if (payload.length > 0) {
      return { "con:file": payload };
    }
    return { "con:file": undefined };
  },
  parseFromRDF: function(jsonLDImm) {
    const files = jsonLDImm && jsonLDImm.get("con:file");
    let parsedFiles = [];

    if (Immutable.List.isList(files)) {
      files &&
        files.forEach(file => {
          let f = {
            name: get(file, "s:name"),
            encodingFormat: get(file, "s:encodingFormat"),
            encoding: get(file, "s:encoding"),
          };
          if (f.name && f.encodingFormat && f.encoding) {
            parsedFiles.push(f);
          }
        });
    } else {
      let f = {
        name: get(files, "s:name"),
        encodingFormat: get(files, "s:encodingFormat"),
        encoding: get(files, "s:encoding"),
      };
      if (f.name && f.encodingFormat && f.encoding) {
        return Immutable.fromJS([f]);
      }
    }
    if (parsedFiles.length > 0) {
      return Immutable.fromJS(parsedFiles);
    }
    return undefined;
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      let humanReadable = "";
      if (value.length > 1) {
        humanReadable = value.length + " Files";
      } else {
        humanReadable = value[0].name;
      }

      return includeLabel ? this.label + ": " + humanReadable : humanReadable;
    }
    return undefined;
  },
};
export const images = {
  identifier: "images",
  label: "Images",
  icon: "#ico36_detail_media",
  placeholder: "",
  accepts: "image/*",
  multiSelect: true,
  component: WonImagePicker,
  viewerComponent: WonImageViewer,
  messageEnabled: true,
  parseToRDF: function({ value, identifier, contentUri }) {
    if (!value) {
      return { "won:image": undefined };
    }
    let payload = [];
    value.forEach(image => {
      if (
        image.name &&
        image.encodingFormat &&
        image.encoding &&
        /^image\//.test(image.encodingFormat)
      ) {
        let img = {
          "@id":
            contentUri && identifier
              ? contentUri + "#" + identifier + "-" + generateIdString(10)
              : undefined,
          "@type": "s:MediaObject",
          "s:name": image.name,
          "s:encodingFormat": image.encodingFormat,
          "s:encoding": image.encoding,
          "s:representativeOfPage": image.default,
        };

        payload.push(img);
      }
    });
    if (payload.length > 0) {
      return { "won:image": payload };
    }
    return { "won:image": undefined };
  },
  parseFromRDF: function(jsonLDImm) {
    const images = jsonLDImm && jsonLDImm.get("won:image");
    let imgs = [];

    if (Immutable.List.isList(images)) {
      images &&
        images.forEach(image => {
          let img = {
            name: get(image, "s:name"),
            encodingFormat: get(image, "s:encodingFormat"),
            encoding: get(image, "s:encoding"),
            default: JSON.parse(get(image, "s:representativeOfPage")),
          };
          if (
            img.name &&
            img.encodingFormat &&
            img.encoding &&
            /^image\//.test(img.encodingFormat)
          ) {
            imgs.push(img);
          }
        });
    } else {
      let img = {
        name: get(images, "s:name"),
        encodingFormat: get(images, "s:encodingFormat"),
        encoding: get(images, "s:encoding"),
        default: get(images, "s:representativeOfPage"),
      };
      if (
        img.name &&
        img.encodingFormat &&
        img.encoding &&
        /^image\//.test(img.encodingFormat)
      ) {
        return Immutable.fromJS([img]);
      }
    }
    if (imgs.length > 0) {
      return Immutable.fromJS(imgs);
    }
    return undefined;
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      let humanReadable = "";
      if (value.length > 1) {
        humanReadable = value.length + " Images";
      } else {
        humanReadable = value[0].name;
      }

      return includeLabel ? this.label + ": " + humanReadable : humanReadable;
    }
    return undefined;
  },
};
