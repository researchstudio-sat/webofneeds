/**
 * Created by ksinger on 11.08.2016.
 */

import L from "./leaflet-bundleable.js";
import {
  generateIdString,
  getRandomString,
  findAllFieldOccurancesRecursively,
  is,
  isValidNumber,
  endOfDateStrInterval,
  getFromJsonLd,
  toAbsoluteURL,
} from "./utils.js";

import { ownerBaseUrl } from "~/config/default.js";
import qr from "qr-image";
import jsonld from "jsonld/dist/jsonld.js";
window.jsonld4dbg = jsonld;

import Immutable from "immutable";
import { get, parseDatetimeStrictly, isValidDate } from "./utils.js";
import * as useCaseUtils from "./usecase-utils.js";

import won from "./won-es6.js";

export function initLeaflet(mapMount, overrideOptions) {
  if (!L) {
    throw new Error(
      "Tried to initialize a leaflet widget while leaflet wasn't loaded."
    );
  }
  Error;

  const baseMaps = initLeafletBaseMaps();

  const map = L.map(
    mapMount,
    Object.assign(
      {
        center: [37.44, -42.89], //centered on north-west africa
        zoom: 1, //world-map
        layers: [baseMaps["Detailed default map"]], //initially visible layers
      },
      overrideOptions
    )
  ); //.setView([51.505, -0.09], 13);

  //map.fitWorld() // shows every continent twice :|
  map.fitBounds([[-80, -190], [80, 190]]); // fitWorld without repetition

  // Force it to adapt to actual size
  // for some reason this doesn't happen by default
  // when the map is within a tag.
  // this.map.invalidateSize();
  // ^ doesn't work (needs to be done manually atm);

  return map;
}

function initLeafletBaseMaps() {
  if (!L) {
    throw new Error(
      "Tried to initialize leaflet map-sources while leaflet wasn't loaded."
    );
  }

  //const secureOsmSource = "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"; // secure osm.org
  const secureOsmSource = "https://www.matchat.org/tile/{z}/{x}/{y}.png"; // TODO: use own tile server instead of proxy
  const secureOsm = L.tileLayer(secureOsmSource, {
    attribution:
      '&copy; <a href="http://osm.org/copyright" target="_blank">OpenStreetMap</a> contributors',
  });

  const baseMaps = {
    "Detailed default map": secureOsm,
  };

  return baseMaps;
}

export function leafletBounds(location) {
  if (location && location.nwCorner && location.seCorner) {
    return new L.latLngBounds(
      new L.LatLng(location.nwCorner.lat, location.nwCorner.lng),
      new L.latLng(location.seCorner.lat, location.seCorner.lng)
    );
  }
}

/**
 * Generates a privateId of `[usernameFragment]-[password]`
 * @returns {string}
 */
export function generatePrivateId() {
  return generateIdString(8) + "-" + generateIdString(8); //<usernameFragment>-<password>
}

/**
 * Parses a given privateId into a fake email address and a password.
 * @param privateId
 * @returns {{email: string, password: *}}
 */
export function privateId2Credentials(privateId) {
  const [usernameFragment, password] = privateId.split("-");
  const email = usernameFragment + "@matchat.org";
  return {
    email,
    password,
    privateId,
  };
}

/**
 * @param credentials either {email, password} or {privateId}
 * @returns {email, password}
 */
export function parseCredentials(credentials) {
  return credentials.privateId
    ? privateId2Credentials(credentials.privateId)
    : credentials;
}

export function getRandomWonId() {
  // needs to start with a letter, so N3 doesn't run into
  // problems when serializing, see
  // https://github.com/RubenVerborgh/N3.js/issues/121
  return (
    getRandomString(1, "abcdefghijklmnopqrstuvwxyz") +
    getRandomString(11, "abcdefghijklmnopqrstuvwxyz0123456789")
  );
}

export function findLatestIntervallEndInJsonLdOrNowAndAddMillis(
  draft,
  jsonld,
  timeToLiveMillisDefault = 1000 * 60 * 30,
  timeToLiveMillisAfterDate = 1000 * 60 * 30
) {
  const findLatestIntervallEndInJsonLdAsDate = (draft, jsonld) => {
    // get all occurrances of `xsd:dateTime`
    const allTimes = Array.concat(
      findAllFieldOccurancesRecursively("xsd:dateTime", jsonld)
    );

    // filter for string-literals (just in case)
    const allTimesStrs = allTimes.filter(s => {
      if (is("String", s)) {
        return true;
      } else {
        console.error(
          "Found a non-string date. Ignoring it for calculating `doNotMatchAfter`. ",
          s
        );
      }
    });

    if (allTimesStrs.length === 0) return undefined; // no time strings found

    // determine if any of them are intervals (e.g. days or months) and if so calculate the end of these
    const endDatetimes = allTimesStrs.map(str => endOfDateStrInterval(str));

    // find the latest mentioned point in time
    const sorted = endDatetimes.sort((a, b) => b - a); // sort descending
    const latest = sorted[0];

    // convert to an `xsd:datetime`/ISO-8601 string and return
    return latest;
  };

  const date = findLatestIntervallEndInJsonLdAsDate(draft, jsonld);
  if (date) {
    return new Date(date.getTime() + timeToLiveMillisAfterDate).toISOString();
  }
  return new Date(new Date().getTime() + timeToLiveMillisDefault).toISOString();
}

/**
 * Behaves like `parseJsonldLeaf`, but can also handle
 * Arrays and Lists and turns those into ImmutableJS
 * Lists if they get passed. Otherwise takes and returns
 * flat values like compactValues.
 *
 * e.g.:
 * ```
 * parseJsonldLeafsImm("123.1") => 123.1
 * parseJsonldLeafsImm([
 *   {"@value": "123.1", "@type": "xsd:float"},
 *   {"@value": "7", "@type": "xsd:float"}
 * ]) // => Immutable.List([123.1, 7]);
 * ```
 * @param {*} val
 * @param {*} type if specified, guides parsing
 *  (see `compactValues`). Note that only one type can
 *   be specified atm, even if passing an list/array atm.
 *
 */

export function parseJsonldLeafsImm(val, type) {
  if (is("Array", val)) {
    const parsed = val.map(item => parseJsonldLeaf(item, type));
    return Immutable.List(parsed);
  } else if (Immutable.List.isList(val)) {
    return val.map(item => parseJsonldLeaf(item, type));
  } else {
    const parsed = parseJsonldLeaf(val, type);
    if (parsed) {
      // got a non-list; make it a 1-item list...
      return Immutable.List([parsed]);
    } else {
      // ... unless that one item would be `undefined` because lookup has failed.
      return undefined;
    }
  }
}

/**
 * Parses a json-ld value, in whatever way it's serialized, to a
 * corresponding javascript-value.
 *
 * Will traverse into `s:value` of `s:PropertyValue` and
 * `s:QuantiativeValue`s. Note that you'll need to pass the
 * inner type in that case though (e.g. `s:Float` or `s:Text`)
 *
 * @param {*} val
 *  * already parsed
 *  * `{"@value": "<someval>", "@type": "<sometype>"}`, where `<sometype>` is one of:
 *    * `s:Number`
 *    * `s:Float`
 *    * `s:Integer`
 *    * `xsd:int`
 *    * `xsd:float`
 *    * `xsd:dateTime`
 *    * `xsd:dateTime`
 *    * `xsd:string`
 *    * `s:Text`
 *    * `http://www.bigdata.com/rdf/geospatial/literals/v1#lat-lon`?, e.g. `"48.225073#16.358398"`
 *  * anything, that _strictly_ parses to a number or date or is a string
 * @param {*} type passing `val` and `type` is equivalent to passing an object with `@value` and `@type`
 *
 */
export function parseJsonldLeaf(val, type) {
  const sval = getFromJsonLd(val, "s:value", won.defaultContext);
  if (sval) {
    /* in schema.org's `s:PropertyValue`s and `s:QuantitativeValue`s
     * can be nested. We need to go in a level deeper.
     */
    return parseJsonldLeaf(sval, type);
  }
  const unwrappedVal = get(val, "@value") || val;
  if (unwrappedVal === undefined || unwrappedVal === null) {
    return undefined;
  }

  const atType = get(val, "@type");
  if (type && atType && type !== atType) {
    throwParsingError(val, type, "Conflicting types.");
  }
  const type_ = get(val, "@type") || type;
  if (isValidNumber(unwrappedVal) || is("Date", unwrappedVal)) {
    // already parsed
    return unwrappedVal;
  }
  const throwErr = msg => throwParsingError(val, type, msg);
  switch (type_) {
    case "s:Text":
    case "xsd:string":
      return unwrappedVal + ""; // everything can be parsed to a string in js

    case "xsd:boolean": {
      const value = unwrappedVal + "";
      if (value === "true") {
        return true;
      } else if (value === "false") {
        return false;
      } else {
        throwErr(`Annotated \`${type_}\` is not valid.`);
      }
      break;
    }

    case "s:Number":
    case "s:Float":
    case "s:Integer":
    case "xsd:int":
    case "xsd:float": {
      const parsedVal = Number(unwrappedVal);
      if (!isValidNumber(parsedVal)) {
        throwErr(
          `Annotated value of type \`${type_}\` isn't parsable to a \`Number\`.`
        );
      } else {
        return parsedVal;
      }
      break;
    }

    case "s:DateTime":
    case "xsd:dateTime": {
      const parsedDateTime = parseDatetimeStrictly(unwrappedVal);
      if (isValidDate(parsedDateTime)) {
        return parsedDateTime;
      } else {
        throwErr(`Annotated \`${type_}\` isn't parsable to a \`Date\`.`);
      }
      break;
    }

    case "xsd:id":
    case "xsd:ID": {
      const id = get(val, "@id");
      if (!id) {
        throwErr(`Could not parse \`${val}\` to an id.`);
      }
      return id;
    }

    // TODO
    // case "http://www.bigdata.com/rdf/geospatial/literals/v1#lat-lon":
    //   break;

    default: {
      if (type_) {
        throwErr("Encountered unexpected type annotation/specification.");
      }

      // try strictly parsing without type information
      const asNum = Number(unwrappedVal);
      if (isValidNumber(asNum)) {
        return asNum;
      }
      const asDateTime = parseDatetimeStrictly(unwrappedVal);
      if (isValidDate(asDateTime)) {
        return asDateTime;
      }

      // and as fallback check if it's at least a string (which it pretty almost will be)
      if (is("String", unwrappedVal)) {
        return unwrappedVal;
      }

      throwErr("Found no `@type`-annotation and also couldn't guess a type.");
    }
  }
}

export function createDocumentDefinitionFromPost(post) {
  if (!post) return;

  let title = { text: post.get("humanReadable"), style: "title" };
  let contentHeader = { text: "Description", style: "branchHeader" };
  let seeksHeader = { text: "Looking For", style: "branchHeader" };

  let content = [];
  content.push(title);

  const allDetails = useCaseUtils.getAllDetails();

  const postContent = post.get("content");
  if (postContent) {
    content.push(contentHeader);
    postContent.map((detailValue, detailKey) => {
      const detailJS =
        detailValue && Immutable.Iterable.isIterable(detailValue)
          ? detailValue.toJS()
          : detailValue;

      const detailDefinition = allDetails[detailKey];
      if (detailDefinition && detailJS) {
        content.push({ text: detailDefinition.label, style: "detailHeader" });
        content.push({
          text: detailDefinition.generateHumanReadable({
            value: detailJS,
            includeLabel: false,
          }),
          style: "detailText",
        });
      }
    });
  }

  const seeksBranch = post.get("seeks");
  if (seeksBranch) {
    content.push(seeksHeader);
    seeksBranch.map((detailValue, detailKey) => {
      const detailJS =
        detailValue && Immutable.Iterable.isIterable(detailValue)
          ? detailValue.toJS()
          : detailValue;

      const detailDefinition = allDetails[detailKey];
      if (detailDefinition && detailJS) {
        content.push({ text: detailDefinition.label, style: "detailHeader" });
        content.push({
          text: detailDefinition.generateHumanReadable({
            value: detailJS,
            includeLabel: false,
          }),
          style: "detailText",
        });
      }
    });
  }

  if (ownerBaseUrl && post) {
    const path = "#!post/" + `?postUri=${encodeURI(post.get("uri"))}`;
    const linkToPost = toAbsoluteURL(ownerBaseUrl).toString() + path;

    if (linkToPost) {
      content.push({ text: linkToPost, style: "postLink" });
      const base64PngQrCode = generateBase64PngQrCode(linkToPost);
      if (base64PngQrCode) {
        content.push({
          image: "data:image/png;base64," + base64PngQrCode,
          width: 200,
          height: 200,
        });
      }
    }
  }

  let styles = {
    title: {
      fontSize: 20,
      bold: true,
    },
    branchHeader: {
      fontSize: 18,
      bold: true,
    },
    detailHeader: {
      fontSize: 12,
      bold: true,
    },
    detailText: {
      fontSize: 12,
    },
    postLink: {
      fontSize: 10,
    },
  };
  return {
    content: content /*[title, 'This is an sample PDF printed with pdfMake '+this.linkToPost]*/,
    styles: styles,
  };
}

function throwParsingError(val, type, prependedMsg = "") {
  const fullMsg =
    prependedMsg +
    ` Failed to parse jsonld value of type \`${type}\`:\n` +
    JSON.stringify(val);
  throw new Error(fullMsg.trim());
}

export function generateSvgQrCode(link) {
  return link && qr.imageSync(link, { type: "svg" });
}

function generatePngQrCode(link) {
  return link && qr.imageSync(link, { type: "png" });
}

function generateBase64PngQrCode(link) {
  const pngQrCode = generatePngQrCode(link);
  return pngQrCode && btoa(String.fromCharCode.apply(null, pngQrCode));
}

export function parseRestErrorMessage(error) {
  if (error && error.get("code") === won.RESPONSECODE.PRIVATEID_NOT_FOUND) {
    return "Sorry, we couldn't find the private ID (the one in your url-bar). If you copied this address make sure you **copied everything** and try **reloading the page**. If this doesn't work you can try [removing it](#) to start fresh.";
  } else if (
    error &&
    error.get("code") === won.RESPONSECODE.USER_NOT_VERIFIED
  ) {
    return "You haven't verified your email addres yet. Please do so now by clicking the link in the email we sent you.";
  } else if (error) {
    //return the message FIXME: once the localization is implemented use the correct localization
    return error.get("message");
  }

  return error;
}

/**
 * Parses json-ld of an `s:Place` in a best-effort kinda style.
 * Any data missing in the RDF will be missing in the result object.
 * @param {*} jsonldLocation
 */
export function parsePlaceLeniently(jsonldLocation) {
  if (!jsonldLocation) return undefined; // NO LOCATION PRESENT

  const jsonldLocationImm = Immutable.fromJS(jsonldLocation);

  const parseFloatFromLocation = path =>
    won.parseFrom(jsonldLocationImm, path, "xsd:float");

  const place = {
    address: won.parseFrom(jsonldLocationImm, ["s:name"], "xsd:string"),
    lat: parseFloatFromLocation(["s:geo", "s:latitude"]),
    lng: parseFloatFromLocation(["s:geo", "s:longitude"]),
    // nwCorner if present (see below)
    // seCorner if present (see below)
  };

  const nwCornerLat = parseFloatFromLocation([
    "con:boundingBox",
    "con:northWestCorner",
    "s:latitude",
  ]);
  const nwCornerLng = parseFloatFromLocation([
    "con:boundingBox",
    "con:northWestCorner",
    "s:longitude",
  ]);
  const seCornerLat = parseFloatFromLocation([
    "con:boundingBox",
    "con:southEastCorner",
    "s:latitude",
  ]);
  const seCornerLng = parseFloatFromLocation([
    "con:boundingBox",
    "con:southEastCorner",
    "s:longitude",
  ]);

  if (nwCornerLat || nwCornerLng) {
    place.nwCorner = {
      lat: nwCornerLat,
      lng: nwCornerLng,
    };
  }
  if (seCornerLat || seCornerLng) {
    place.seCorner = {
      lat: seCornerLat,
      lng: seCornerLng,
    };
  }
  return place;
}

/**
 * Generates rdf from given geoData (e.g. location from a location picker)
 * @param geoData
 * @param baseUri
 * @returns {*}
 */
export function genSPlace({ geoData, baseUri }) {
  if (!geoData) {
    return undefined;
  }
  if (!geoData.lat || !geoData.lng || !geoData.name) {
    return undefined;
  }

  return {
    "@id": baseUri,
    "@type": "s:Place",
    "s:name": geoData.name,
    "s:geo": genGeo({ lat: geoData.lat, lng: geoData.lng, baseUri }),
    "con:boundingBox": genBoundingBox({
      nwCorner: geoData.nwCorner,
      seCorner: geoData.seCorner,
      baseUri,
    }),
  };
}

function genGeo({ lat, lng, baseUri }) {
  if (isNaN(lat) || isNaN(lng)) {
    return undefined;
  }
  return {
    "@id": baseUri ? baseUri + "/geo" : undefined,
    "@type": "s:GeoCoordinates",
    "s:latitude": lat.toFixed(6),
    "s:longitude": lng.toFixed(6),
    "con:geoSpatial": {
      "@type": "http://www.bigdata.com/rdf/geospatial/literals/v1#lat-lon",
      "@value": `${lat.toFixed(6)}#${lng.toFixed(6)}`,
    },
  };
}

function genBoundingBox({ nwCorner, seCorner, baseUri }) {
  return !nwCorner || !seCorner
    ? undefined
    : {
        "@id": baseUri ? baseUri + "/bounds" : undefined,
        "con:northWestCorner": {
          "@id": baseUri ? baseUri + "/bounds/nw" : undefined,
          "@type": "s:GeoCoordinates",
          "s:latitude": nwCorner.lat.toFixed(6),
          "s:longitude": nwCorner.lng.toFixed(6),
        },
        "con:southEastCorner": {
          "@id": baseUri ? baseUri + "/bounds/se" : undefined,
          "@type": "s:GeoCoordinates",
          "s:latitude": seCorner.lat.toFixed(6),
          "s:longitude": seCorner.lng.toFixed(6),
        },
      };
}

export function genDetailBaseUri(baseUri, detailIdentifier) {
  if (!baseUri || !detailIdentifier) {
    return undefined;
  }
  const randomId = generateIdString(10);
  return baseUri + "/" + detailIdentifier + "/" + randomId;
}

export function parseSPlace(jsonldLocation) {
  if (!jsonldLocation) return undefined; // NO LOCATION PRESENT

  const location = parsePlaceLeniently(jsonldLocation);

  if (
    location &&
    location.address &&
    location.lat &&
    location.lng &&
    location.nwCorner.lat &&
    location.nwCorner.lng &&
    location.seCorner.lat &&
    location.seCorner.lng
  ) {
    return Immutable.fromJS(location);
  } else {
    console.error(
      "Cant parse location, data is an invalid location-object: ",
      jsonldLocation
    );
    return undefined;
  }
}
