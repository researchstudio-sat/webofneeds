import {
  findAllFieldOccurancesRecursively,
  get,
  is,
  isValidNumber,
  isValidDate,
  parseDatetimeStrictly,
} from "../utils.js";
import Immutable from "immutable";

import won from "./won.js";

/**
 * Traverses the `path` into the json-ld object and then tries to
 * parse that node as RDF literal object (see `wonUtils.parseJsonldLeaf` for
 * details on that).
 *
 * @param {*} jsonld
 * @param {*} path
 * @param {*} type optional (see getFromJsonLd for details)
 * @param {*} context defaults to `won.defaultContext`
 * @return the value at the path
 */
export function parseFrom(jsonld, path, type, context = won.defaultContext) {
  return parseJsonldLeaf(getInFromJsonLd(jsonld, path, context), type);
}

/**
 * Traverses the `path` into the json-ld object and then tries to
 * parse that node as RDF literal object (see `wonUtils.parseJsonldLeafsImm` for
 * details on that).
 *
 * @param {*} jsonld
 * @param {*} path
 * @param {*} type optional (see getFromJsonLd for details)
 * @param {*} context defaults to `won.defaultContext`
 * @return the list at the path
 */
export function parseListFrom(
  jsonld,
  path,
  type,
  context = won.defaultContext
) {
  try {
    return parseJsonldLeafsImm(getInFromJsonLd(jsonld, path, context), type);
  } catch (err) {
    console.error("Could not parse From list: ", err);
    return undefined;
  }
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
    parseFrom(jsonldLocationImm, path, "xsd:float");

  const place = {
    address: parseFrom(jsonldLocationImm, ["s:name"], "xsd:string"),
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

    /**
     * Takes an ISO-8601 string and returns a `Date` that marks
     * the exact time or the end of the year, month or day
     * e.g. xsd:dateTime: "2011-04-11T10:20:30Z"
     *
     * @param {*} dateStr
     */
    const endOfDateStrInterval = dateStr => {
      // "2011-04-11T10:20:30Z".split(/[-T]/) => ["2011", "04", "11", "10:20:30Z"]
      // "2011-04-11".split(/[-T]/) => ["2011", "04", "11"]
      const split = dateStr.split(/[-T]/);
      if (split.length > 3) {
        // precise datetime
        return new Date(dateStr);
      } else if (split.length === 3) {
        // end of day
        const year = split[0];
        const monthIdx = split[1] - 1;
        const day = split[2];
        return new Date(year, monthIdx, day, 23, 59, 59);
      } else if (split.length === 2) {
        // end of month
        const year = split[0];
        const monthIdx = split[1] - 1;
        const firstOfNextMonth = new Date(year, monthIdx + 1, 1, 23, 59, 59);
        return new Date(firstOfNextMonth - 1000 * 60 * 60 * 24);
      } else if (split.length === 1) {
        // end of year
        const year = split[0];
        const monthIdx = 11;
        return new Date(year, monthIdx, 31, 23, 59, 59);
      } else {
        console.error(
          "Found unexpected date when calculating exact end-datetime of date-string: ",
          dateStr
        );
      }
    };

    const endDatetimes = allTimesStrs.map(str => endOfDateStrInterval(str));

    // find the latest mentioned point in time
    const sorted = endDatetimes.sort((a, b) => b - a); // sort descending
    // convert to an `xsd:datetime`/ISO-8601 string and return
    return sorted[0]; //returns latest
  };

  const date = findLatestIntervallEndInJsonLdAsDate(draft, jsonld);
  if (date) {
    return new Date(date.getTime() + timeToLiveMillisAfterDate).toISOString();
  }
  return new Date(new Date().getTime() + timeToLiveMillisDefault).toISOString();
}

/**
 * Like `get` but allows passing a context
 * that's used for prefix resolution (i.e. it
 * checks both the prefixed and expanded version
 * of that path-step).
 *
 * @param obj a json-ld object
 * @param path an array used for traversal, e.g. `[0, "ex:foo", "ex:bar", "@value"]`. Use
 *   the shortened form of prefixes -- full URLs won't be compacted, only prefixes expanded.
 * @param context a standard json-ld style context. Note, that
 * only prefix-definitions will be minded.
 */
export function getFromJsonLd(obj, predicate, context = won.defaultContext) {
  if (!obj) {
    return undefined;
  } else {
    // e.g. "ex:foo:bar".replace(/([^:]*):.*/, '$1') => "ex"
    const prefixShort = predicate.replace(/^([^:]*):.*/, "$1");
    const prefixLong = context && context[prefixShort];
    let expandedPredicate;

    if (prefixShort && prefixLong && is("String", prefixLong)) {
      // ^ the string-check is because contexts can also have other
      // fields beside prefix-definitions

      // "ex:foo:bar:ex".replace('ex:', 'http://example.org/') => "http://example.org/foo:bar:ex"
      expandedPredicate = predicate.replace(prefixShort + ":", prefixLong);
    }

    return get(obj, predicate) || get(obj, expandedPredicate);
  }
}

/**
 * Like `getIn` but allows passing a context
 * that's used for prefix resolution (i.e. it
 * checks both the prefixed and expanded version
 * of that path-step).
 * ```js
 * getInFromJsonLd(
 *   {'ex:foo': { 'http://example.org/bar' : 42 }},
 *   ['ex:foo', 'ex:bar'],
 *   {'ex': 'http://example.org/'}
 * ) // => 42
 * ```
 * @param obj
 * @param path
 * @param context a standard json-ld style context. Note, that
 * only prefix-definitions will be minded.
 */
export function getInFromJsonLd(obj, path, context = won.defaultContext) {
  if (!path || !obj || path.length === 0) {
    return undefined;
  } else {
    const child = getFromJsonLd(obj, path[0], context);
    if (path.length === 1) {
      /* end of the path */
      return child;
    } else {
      /* recurse */
      return getInFromJsonLd(child, path.slice(1), context);
    }
  }
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

function parseJsonldLeafsImm(val, type) {
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
function parseJsonldLeaf(val, type) {
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
    case "xsd:long":
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

function throwParsingError(val, type, prependedMsg = "") {
  const fullMsg =
    prependedMsg +
    ` Failed to parse jsonld value of type \`${type}\`:\n` +
    JSON.stringify(val);
  console.error(
    "Failed to parse jsonld value of type: ",
    type,
    "for value: ",
    val
  );
  throw new Error(fullMsg.trim());
}

// Helper Method to get Data out of jsonld, since the jsonld update some messages dont have a @graph array wrapped around the content (["@graph"][0])
export function getProperty(jsonld, property) {
  if (jsonld && property) {
    if (jsonld["@graph"] && jsonld["@graph"][0]) {
      return jsonld["@graph"][0][property];
    } else {
      return jsonld[property];
    }
  }
  return undefined;
}

export function setProperty(jsonld, property, val) {
  if (jsonld && property && val) {
    if (jsonld["@graph"] && jsonld["@graph"][0]) {
      jsonld["@graph"][0][property] = val;
    } else {
      jsonld[property] = val;
    }
  }
}
