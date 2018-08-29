// TODO: import detail-picker components here, not in create-post
// TODO: each detail picker should know it's own rdf template
// --> both for parsing to and from rdf
// --> templates are used in need-builder (toRDF) and in parse-need (from RDF)
import {
  get,
  getIn,
  isValidDate,
  parseDatetimeStrictly,
  toLocalISODateString,
} from "../app/utils.js";
import Immutable from "immutable";
import won from "../app/won-es6.js";

/**
 * Defines a set of details that will only be visible within a specific 'implementation'
 * you will need to alter the identifier, label, icon, parseToRDF, and parseFromRDF if
 * you want to use it.
 */
export const abstractDetails = {
  range: {
    identifier: function() {
      throw "abstract Detail does not override necessary identifier";
    },
    label: function() {
      throw "abstract Detail does not override necessary label";
    },
    minLabel: function() {
      throw "abstract Detail does not override necessary minLabel";
    },
    maxLabel: function() {
      throw "abstract Detail does not override necessary maxLabel";
    },
    minPlaceholder: undefined,
    maxPlaceholder: undefined,
    icon: undefined,
    component: "won-range-picker",
    viewerComponent: "won-range-viewer",
    parseToRDF: function() {
      throw "abstract Detail does not override necessary function";
    },
    parseFromRDF: function() {
      throw "abstract Detail does not override necessary function";
    },
    generateHumanReadable: function() {
      throw "abstract Detail does not override necessary function";
    },
  },
  number: {
    identifier: function() {
      throw "abstract Detail does not override necessary identifier";
    },
    label: function() {
      throw "abstract Detail does not override necessary label";
    },
    icon: undefined,
    component: "won-number-picker",
    viewerComponent: "won-number-viewer",
    parseToRDF: function() {
      throw "abstract Detail does not override necessary function";
    },
    parseFromRDF: function() {
      throw "abstract Detail does not override necessary function";
    },
    generateHumanReadable: function() {
      throw "abstract Detail does not override necessary function";
    },
  },
  select: {
    identifier: function() {
      throw "abstract Detail does not override necessary identifier";
    },
    label: function() {
      throw "abstract Detail does not override necessary label";
    },
    icon: undefined,
    component: "won-select-picker",
    viewerComponent: "won-select-viewer",
    multiSelect: false,
    options: function() {
      throw 'abstract Detail does not override necessary options array(structure: [{value: val, label: "labeltext"}...]';
      /**
       * e.g. number of rooms ....
       [
        {value: "1", label: "one"},
        {value: "2", label: "two"},
        {value: "3", label: "three"},
        {value: "4", label: "four"},
        {value: "5+", label: "more"},
       ]
       */
    },
    parseToRDF: function() {
      throw "abstract Detail does not override necessary function";
    },
    parseFromRDF: function() {
      throw "abstract Detail does not override necessary function";
    },
    generateHumanReadable: function() {
      throw "abstract Detail does not override necessary function";
    },
  },
  dropdown: {
    identifier: function() {
      throw "abstract Detail does not override necessary identifier";
    },
    label: function() {
      throw "abstract Detail does not override necessary label";
    },
    icon: undefined,
    component: "won-dropdown-picker",
    viewerComponent: "won-dropdown-viewer",
    options: function() {
      throw 'abstract Detail does not override necessary options array(structure: [{value: val, label: "labeltext"}...]';
      /**
       * e.g. relationship status....
        [
         {value: "single", label: "single"},
         {value: "married", label: "married"},
         {value: "complicated", label: "it's complicated"},
         {value: "divorced", label: "divorced"},
         {value: "free", label: "free for all"},
        ]
       */
    },
    parseToRDF: function() {
      throw "abstract Detail does not override necessary function";
    },
    parseFromRDF: function() {
      throw "abstract Detail does not override necessary function";
    },
    generateHumanReadable: function() {
      throw "abstract Detail does not override necessary function";
    },
  },
};

export const details = {
  title: {
    identifier: "title",
    label: "Title",
    icon: "#ico36_detail_title",
    placeholder: "What? (Short title shown in lists)",
    component: "won-title-picker",
    viewerComponent: "won-title-viewer",
    parseToRDF: function({ value }) {
      if (!value) {
        return { "dc:title": undefined };
      }
      return { "dc:title": value };
    },
    parseFromRDF: function(jsonLDImm) {
      return won.parseFrom(jsonLDImm, ["dc:title"], "xsd:string");
    },
    generateHumanReadable: function({ value, includeLabel }) {
      if (value) {
        return includeLabel ? this.label + ": " + value : value;
      }
      return undefined;
    },
  },
  description: {
    identifier: "description",
    label: "Description",
    icon: "#ico36_detail_description",
    placeholder: "Enter Description...",
    component: "won-description-picker",
    viewerComponent: "won-description-viewer",
    parseToRDF: function({ value }) {
      if (!value) {
        return { "dc:description": undefined };
      }
      return { "dc:description": value };
    },
    parseFromRDF: function(jsonLDImm) {
      return won.parseFrom(jsonLDImm, ["dc:description"], "xsd:string");
    },
    generateHumanReadable: function({ value, includeLabel }) {
      if (value) {
        return includeLabel ? this.label + ": " + value : value;
      }
      return undefined;
    },
  },
  fromDatetime: {
    identifier: "fromDatetime",
    label: "Starting at",
    icon: "#ico36_detail_datetime",
    placeholder: "Enter Date and Time...",
    component: "won-datetime-picker",
    viewerComponent: "won-datetime-viewer",
    parseToRDF: function({ value }) {
      // value can be an xsd:datetime-string or a javascript date object
      const datetime = parseDatetimeStrictly(value);
      if (!isValidDate(datetime)) {
        return { "s:validFrom": undefined };
      } else {
        const datetimeString = toLocalISODateString(datetime);
        return {
          "s:validFrom": { "@value": datetimeString, "@type": "s:DateTime" },
        };
      }
    },
    parseFromRDF: function(jsonLDImm) {
      return won.parseFrom(jsonLDImm, ["s:validFrom"], "s:DateTime");
    },
    generateHumanReadable: function({ value, includeLabel }) {
      if (value) {
        const maybeLabel = includeLabel ? this.label + ": " : "";
        const datetime = parseDatetimeStrictly(value);
        const timestring = isValidDate(datetime)
          ? datetime.toLocaleString()
          : "";
        return maybeLabel + timestring;
      }
      return undefined;
    },
  },
  throughDatetime: {
    identifier: "throughDatetime",
    label: "Ending at",
    icon: "#ico36_detail_datetime",
    placeholder: "Enter Date and Time...",
    component: "won-datetime-picker",
    viewerComponent: "won-datetime-viewer",
    parseToRDF: function({ value }) {
      // value can be an xsd:datetime-string or a javascript date object
      const datetime = parseDatetimeStrictly(value);
      if (!isValidDate(datetime)) {
        return { "s:validThrough": undefined };
      } else {
        const datetimeString = toLocalISODateString(datetime);
        return {
          "s:validThrough": { "@value": datetimeString, "@type": "s:DateTime" },
        };
      }
    },
    parseFromRDF: function(jsonLDImm) {
      return won.parseFrom(jsonLDImm, ["s:validThrough"], "s:DateTime");
    },
    generateHumanReadable: function({ value, includeLabel }) {
      if (value) {
        const maybeLabel = includeLabel ? this.label + ": " : "";
        const datetime = parseDatetimeStrictly(value);
        const timestring = isValidDate(datetime)
          ? datetime.toLocaleString()
          : "";
        return maybeLabel + timestring;
      }
      return undefined;
    },
  },
  location: {
    identifier: "location",
    label: "Location",
    icon: "#ico36_detail_location",
    placeholder: "Search for location",
    component: "won-location-picker",
    viewerComponent: "won-location-viewer",
    parseToRDF: function({ value, identifier }) {
      if (!value) {
        // TODO: this should happen in need-builder
        return { "won:hasLocation": undefined };
      }
      return {
        "won:hasLocation": {
          "@type": "s:Place",
          "s:geo": {
            "@id": "_:" + identifier + "-location",
            "@type": "s:GeoCoordinates",
            "s:latitude": value.lat.toFixed(6),
            "s:longitude": value.lng.toFixed(6),
            "won:geoSpatial": {
              "@type":
                "http://www.bigdata.com/rdf/geospatial/literals/v1#lat-lon",
              "@value": `${value.lat.toFixed(6)}#${value.lng.toFixed(6)}`,
            },
          },
          "s:name": value.name,
          "won:hasBoundingBox":
            !value.nwCorner || !value.seCorner
              ? undefined
              : {
                  "won:hasNorthWestCorner": {
                    "@id": "_:" + identifier + "BoundsNW",
                    "@type": "s:GeoCoordinates",
                    "s:latitude": value.nwCorner.lat.toFixed(6),
                    "s:longitude": value.nwCorner.lng.toFixed(6),
                  },
                  "won:hasSouthEastCorner": {
                    "@id": "_:" + identifier + "BoundsSE",
                    "@type": "s:GeoCoordinates",
                    "s:latitude": value.seCorner.lat.toFixed(6),
                    "s:longitude": value.seCorner.lng.toFixed(6),
                  },
                },
        },
      };
    },
    parseFromRDF: function(jsonLDImm) {
      const jsonldLocation = jsonLDImm && jsonLDImm.get("won:hasLocation");
      if (!jsonldLocation) return undefined; // NO LOCATION PRESENT

      const jsonldLocationImm = Immutable.fromJS(jsonldLocation);

      let location = {
        address: undefined,
        lat: undefined,
        lng: undefined,
        nwCorner: {
          lat: undefined,
          lng: undefined,
        },
        seCorner: {
          lat: undefined,
          lng: undefined,
        },
      };

      location.address = won.parseFrom(
        jsonldLocationImm,
        ["s:name"],
        "xsd:string"
      );

      const parseFloatFromLocation = path =>
        won.parseFrom(jsonldLocationImm, path, "xsd:float");

      location.lat = parseFloatFromLocation(["s:geo", "s:latitude"]);
      location.lng = parseFloatFromLocation(["s:geo", "s:longitude"]);
      location.nwCorner.lat = parseFloatFromLocation([
        "won:hasBoundingBox",
        "won:hasNorthWestCorner",
        "s:latitude",
      ]);
      location.nwCorner.lng = parseFloatFromLocation([
        "won:hasBoundingBox",
        "won:hasNorthWestCorner",
        "s:longitude",
      ]);
      location.seCorner.lat = parseFloatFromLocation([
        "won:hasBoundingBox",
        "won:hasSouthEastCorner",
        "s:latitude",
      ]);
      location.seCorner.lng = parseFloatFromLocation([
        "won:hasBoundingBox",
        "won:hasSouthEastCorner",
        "s:longitude",
      ]);

      if (
        location.address &&
        location.lat &&
        location.lng &&
        location.nwCorner.lat &&
        location.nwCorner.lng &&
        location.seCorner.lat &&
        location.seCorner.lng
      ) {
        return Immutable.fromJS(location);
      }

      console.error(
        "Cant parse location, data is an invalid location-object: ",
        jsonldLocationImm.toJS()
      );
      return undefined;
    },
    generateHumanReadable: function({ value, includeLabel }) {
      if (value) {
        let humanReadable;
        if (value.name) {
          humanReadable = value.name;
        } else if (value.address) {
          humanReadable = value.address;
        } else {
          const locationLat = value.lat && value.lat.toFixed(6);
          const locationLng = value.lng && value.lng.toFixed(6);
          if (locationLat && locationLng) {
            humanReadable = "@(" + locationLat + " , " + locationLng + ")";
          }
        }
        if (humanReadable) {
          return includeLabel
            ? this.label + ": " + humanReadable.trim()
            : humanReadable.trim();
        }
      }
      return undefined;
    },
  },
  person: {
    identifier: "person",
    label: "Person",
    icon: "#ico36_detail_person",
    placeholder: undefined,
    component: "won-person-picker",
    viewerComponent: "won-person-viewer",
    parseToRDF: function({ value }) {
      if (!value) {
        return { "foaf:person": undefined };
      }
      return {
        "foaf:title": getIn(value, ["title"])
          ? getIn(value, ["title"])
          : undefined,
        "foaf:name": getIn(value, ["name"])
          ? getIn(value, ["name"])
          : undefined,
        "s:worksFor": getIn(value, ["company"])
          ? {
              "@type": "s:Organization",
              "s:name": getIn(value, ["company"]),
            }
          : undefined,
        "s:jobTitle": getIn(value, ["position"])
          ? getIn(value, ["position"])
          : undefined,
      };
    },
    parseFromRDF: function(jsonLDImm) {
      if (!jsonLDImm) return undefined;

      let person = {
        name: undefined,
        title: undefined,
        company: undefined,
        position: undefined,
        // bio: undefined,
      };

      person.name = won.parseFrom(jsonLDImm, ["foaf:name"], "xsd:string");
      person.title = won.parseFrom(jsonLDImm, ["foaf:title"], "xsd:string");
      person.company = won.parseFrom(
        jsonLDImm,
        ["s:worksFor", "s:name"],
        "xsd:string"
      );
      person.position = won.parseFrom(jsonLDImm, ["s:jobTitle"], "xsd:string");

      // if there's anything, use it
      if (person.name || person.title || person.company || person.position) {
        return Immutable.fromJS(person);
      }
      return undefined;
    },
    generateHumanReadable: function({ value, includeLabel }) {
      const title = getIn(value, ["title"]);
      const name = getIn(value, ["name"]);
      const company = getIn(value, ["company"]);
      const position = getIn(value, ["position"]);

      let humanReadable;
      if (title) {
        humanReadable = title + " ";
      }
      if (name) {
        if (humanReadable) {
          humanReadable += name + " ";
        } else {
          humanReadable = name + " ";
        }
      }
      if (company) {
        if (humanReadable) {
          humanReadable += "works at " + company + " ";
        } else {
          humanReadable = company + " ";
        }
      }
      if (position) {
        if (humanReadable) {
          if (company) {
            humanReadable += "as a " + position;
          } else {
            humanReadable += "is a " + position;
          }
        } else {
          humanReadable = position;
        }
      }

      if (humanReadable) {
        return includeLabel
          ? this.label + ": " + humanReadable.trim()
          : humanReadable.trim();
      }
      return undefined;
    },
  },
  travelAction: {
    identifier: "travelAction",
    label: "Route (From - To)",
    icon: "#ico36_detail_travelaction",
    placeholder: {
      departure: "Start location",
      destination: "Destination",
    },
    component: "won-travel-action-picker",
    viewerComponent: "won-travel-action-viewer",
    parseToRDF: function({ value }) {
      if (!value) {
        return { "won:travelAction": undefined };
      }
      return {
        "won:travelAction": {
          "@type": "s:TravelAction",
          "s:fromLocation": !value.fromLocation
            ? undefined
            : {
                "@type": "s:Place",
                "s:geo": {
                  "@type": "s:GeoCoordinates",
                  "s:latitude": value.fromLocation.lat.toFixed(6),
                  "s:longitude": value.fromLocation.lng.toFixed(6),
                },
                "s:name": value.fromLocation.name,
              },
          "s:toLocation": !value.toLocation
            ? undefined
            : {
                "@type": "s:Place",
                "s:geo": {
                  "@type": "s:GeoCoordinates",
                  "s:latitude": value.toLocation.lat.toFixed(6),
                  "s:longitude": value.toLocation.lng.toFixed(6),
                },
                "s:name": value.toLocation.name,
              },
        },
      };
    },
    parseFromRDF: function(jsonLDImm) {
      const jsonTravelAction = jsonLDImm && jsonLDImm.get("won:travelAction");
      if (!jsonTravelAction) return undefined;

      const travelActionImm = Immutable.fromJS(jsonTravelAction);

      let travelAction = {
        fromAddress: undefined,
        fromLocation: {
          lat: undefined,
          lng: undefined,
        },
        toAddress: undefined,
        toLocation: {
          lat: undefined,
          lng: undefined,
        },
      };

      travelAction.fromAddress = won.parseFrom(
        travelActionImm,
        ["s:fromLocation", "s:name"],
        "xsd:string"
      );

      const parseFloatFromTravelAction = path =>
        won.parseFrom(travelActionImm, path, "xsd:float");

      travelAction.fromLocation.lat = parseFloatFromTravelAction([
        "s:fromLocation",
        "s:geo",
        "s:latitude",
      ]);
      travelAction.fromLocation.lng = parseFloatFromTravelAction([
        "s:fromLocation",
        "s:geo",
        "s:longitude",
      ]);

      travelAction.toAddress = won.parseFrom(
        travelActionImm,
        ["s:toLocation", "s:name"],
        "xsd:string"
      );

      travelAction.toLocation.lat = parseFloatFromTravelAction([
        "s:toLocation",
        "s:geo",
        "s:latitude",
      ]);
      travelAction.toLocation.lng = parseFloatFromTravelAction([
        "s:toLocation",
        "s:geo",
        "s:longitude",
      ]);

      if (
        (travelAction.fromAddress &&
          travelAction.fromLocation.lat &&
          travelAction.fromLocation.lng) ||
        (travelAction.toAddress &&
          travelAction.toLocation.lat &&
          travelAction.toLocation.lng)
      ) {
        return Immutable.fromJS(travelAction);
      } else {
        console.error(
          "Cant parse travelAction, data is an invalid travelAction-object: ",
          travelActionImm.toJS()
        );
        return undefined;
      }
    },
    generateHumanReadable: function({ value, includeLabel }) {
      if (value && (value.fromLocation || value.toLocation)) {
        const fromLocation = value.fromLocation;
        const toLocation = value.toLocation;

        let fromLocationName;
        let toLocationName;

        if (fromLocation.name) {
          fromLocationName = fromLocation.name;
        } else if (value && value.fromAddress) {
          fromLocationName = value.fromAddress;
        }

        if (toLocation.name) {
          toLocationName = toLocation.name;
        } else if (value && value.toAddress) {
          toLocationName = value.toAddress;
        }

        let humanReadable;

        if (fromLocationName) {
          humanReadable = "from: " + fromLocationName + " ";
        } else {
          const fromLocationLat =
            fromLocation.lat && fromLocation.lat.toFixed(6);
          const fromLocationLng =
            fromLocation.lng && fromLocation.lng.toFixed(6);
          if (fromLocationLat && fromLocationLng) {
            humanReadable =
              "from: @(" + fromLocationLat + " , " + fromLocationLng + ") ";
          }
        }

        if (toLocationName) {
          if (humanReadable) {
            humanReadable += "to: " + toLocationName + " ";
          } else {
            humanReadable = "to: " + toLocationName + " ";
          }
        } else {
          const toLocationLat = toLocation.lat && toLocation.lat.toFixed(6);
          const toLocationLng = toLocation.lng && toLocation.lng.toFixed(6);
          if (toLocationLat && toLocationLng) {
            if (humanReadable) {
              humanReadable +=
                "to: @(" + toLocationLat + " , " + toLocationLng + ") ";
            } else {
              humanReadable +=
                "to: @(" + toLocationLat + " , " + toLocationLng + ") ";
            }
          }
        }

        if (humanReadable) {
          return includeLabel
            ? this.label + ": " + humanReadable.trim()
            : humanReadable.trim();
        }
      }
      return undefined;
    },
  },
  tags: {
    identifier: "tags",
    label: "Tags",
    icon: "#ico36_detail_tags",
    placeholder: "e.g. couch, free",
    component: "won-tags-picker",
    viewerComponent: "won-tags-viewer",
    parseToRDF: function({ value }) {
      if (!value) {
        return { "won:hasTag": undefined };
      }
      return { "won:hasTag": value };
    },
    parseFromRDF: function(jsonLDImm) {
      return won.parseListFrom(jsonLDImm, ["won:hasTag"], "xsd:string");
    },
    generateHumanReadable: function({ value, includeLabel }) {
      if (value) {
        let humanReadable = "";

        for (const key in value) {
          humanReadable += value[key] + ", ";
        }
        humanReadable = humanReadable.trim();

        if (humanReadable.length > 0) {
          humanReadable = humanReadable.substr(0, humanReadable.length - 1);
          return includeLabel
            ? this.label + ": " + humanReadable
            : humanReadable;
        }
      }
      return undefined;
    },
  },
  files: {
    identifier: "files",
    label: "Files",
    icon: "#ico36_detail_files",
    placeholder: "",
    accepts: "",
    multiSelect: true,
    component: "won-file-picker",
    viewerComponent: "won-file-viewer",
    parseToRDF: function({ value }) {
      if (!value) {
        return { "won:hasFile": undefined };
      }
      let payload = [];
      value.forEach(file => {
        //TODO: SAVE CORRECT RDF THIS METHOD
        if (file.name && file.type && file.data) {
          let f = {
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
  },
  images: {
    identifier: "images",
    label: "Images",
    icon: "#ico36_detail_media",
    placeholder: "",
    accepts: "image/*",
    multiSelect: false,
    component: "won-file-picker",
    viewerComponent: "won-file-viewer",
    parseToRDF: function({ value }) {
      if (!value) {
        return { "won:hasImage": undefined };
      }
      let payload = [];
      value.forEach(image => {
        //TODO: SAVE CORRECT RDF THIS METHOD
        if (image.name && image.type && image.data) {
          let img = {
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
  },
  workflow: {
    identifier: "workflow",
    label: "Workflow",
    icon: "#ico36_detail_workflow",
    placeholder: "",
    //accepts: "application/octet-stream",
    accepts: "",
    component: "won-workflow-picker",
    viewerComponent: "won-workflow-viewer",
    parseToRDF: function({ value }) {
      if (value && value.name && value.data) {
        //do not check for value.type might not be present on some systems
        let workflow = {
          "@type": "s:FileObject",
          "s:name": value.name,
          "s:type": value.type,
          "s:data": value.data,
        };

        return { "won:hasWorkflow": workflow };
      }
      return { "won:hasWorkflow": undefined };
    },
    parseFromRDF: function(jsonLDImm) {
      const wflw = jsonLDImm && jsonLDImm.get("won:hasWorkflow");

      let workflow = {
        name: get(wflw, "s:name"),
        type: get(wflw, "s:type"),
        data: get(wflw, "s:data"),
      };
      if (workflow.name && workflow.data) {
        //do not check for value.type might not be present on some systems
        return Immutable.fromJS(workflow);
      }

      return undefined;
    },
    generateHumanReadable: function({ value, includeLabel }) {
      if (value && value.name) {
        return includeLabel ? this.label + ": " + value.name : value.name;
      }
      return undefined;
    },
  },
  ttl: {
    identifier: "ttl",
    label: "Turtle (TTL)",
    icon: "#ico36_detail_ttl",
    placeholder: "Enter TTL...",
    component: "won-ttl-picker",
    viewerComponent: undefined,
    parseToRDF: function({ value }) {
      if (!value) {
        return undefined;
      }
      return undefined;
      // TODO: return value
    },
    parseFromRDF: function(jsonLDImm) {
      //console.error("IMPLEMENT ME", jsonLDImm);
      if (!jsonLDImm) {
        return undefined;
      }
      return undefined;
      // TODO: return value
    },
    generateHumanReadable: function({ value, includeLabel }) {
      //TODO: implement generateHumanReadable
      if (!value || includeLabel) {
        return undefined;
      }
      return undefined;
    },
  },
};
