import { generateIdString, get } from "../../app/utils.js";
import Immutable from "immutable";

export const files = {
  identifier: "files",
  label: "Files",
  icon: "#ico36_detail_files",
  placeholder: "",
  accepts: "",
  multiSelect: true,
  component: "won-file-picker",
  viewerComponent: "won-file-viewer",
  messageEnabled: true,
  parseToRDF: function({ value, identifier, contentUri }) {
    if (!value) {
      return { "won:hasFile": undefined };
    }
    let payload = [];
    value.forEach(file => {
      //TODO: SAVE CORRECT RDF THIS METHOD
      if (file.name && file.type && file.data) {
        let f = {
          "@id":
            contentUri && identifier
              ? contentUri + "/" + identifier + "/" + generateIdString(10)
              : undefined,
          "@type": "s:FileObject",
          "s:name": file.name,
          "s:type": file.type,
          "s:data": file.data,
        };

        payload.push(f);
      }
    });
    if (payload.length > 0) {
      return { "won:hasFile": payload };
    }
    return { "won:hasFile": undefined };
  },
  parseFromRDF: function(jsonLDImm) {
    const files = jsonLDImm && jsonLDImm.get("won:hasFile");
    let parsedFiles = [];

    if (Immutable.List.isList(files)) {
      files &&
        files.forEach(file => {
          //TODO: RETRIEVE FROM CORRECT RDF THIS METHOD
          let f = {
            name: get(file, "s:name"),
            type: get(file, "s:type"),
            data: get(file, "s:data"),
          };
          if (f.name && f.type && f.data) {
            parsedFiles.push(f);
          }
        });
    } else {
      let f = {
        name: get(files, "s:name"),
        type: get(files, "s:type"),
        data: get(files, "s:data"),
      };
      if (f.name && f.type && f.data) {
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
  component: "won-file-picker",
  viewerComponent: "won-file-viewer",
  messageEnabled: true,
  parseToRDF: function({ value, identifier, contentUri }) {
    if (!value) {
      return { "won:hasImage": undefined };
    }
    let payload = [];
    value.forEach(image => {
      //TODO: SAVE CORRECT RDF THIS METHOD
      if (image.name && image.type && image.data) {
        let img = {
          "@id":
            contentUri && identifier
              ? contentUri + "/" + identifier + "/" + generateIdString(10)
              : undefined,
          "@type": "s:ImageObject",
          "s:name": image.name,
          "s:type": image.type,
          "s:data": image.data,
        };

        payload.push(img);
      }
    });
    if (payload.length > 0) {
      return { "won:hasImage": payload };
    }
    return { "won:hasImage": undefined };
  },
  parseFromRDF: function(jsonLDImm) {
    const images = jsonLDImm && jsonLDImm.get("won:hasImage");
    let imgs = [];

    if (Immutable.List.isList(images)) {
      images &&
        images.forEach(image => {
          //TODO: RETRIEVE FROM CORRECT RDF THIS METHOD
          let img = {
            name: get(image, "s:name"),
            type: get(image, "s:type"),
            data: get(image, "s:data"),
          };
          if (img.name && img.type && img.data) {
            imgs.push(img);
          }
        });
    } else {
      let img = {
        name: get(images, "s:name"),
        type: get(images, "s:type"),
        data: get(images, "s:data"),
      };
      if (img.name && img.type && img.data) {
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
