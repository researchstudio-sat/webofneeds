import {
  getHeldByUri,
  getTitle,
  getFakePersonaName,
} from "./redux/utils/atom-utils.js";
import { getTargetAtomUri } from "./redux/utils/connection-utils.js";
import fakeNames from "./fakeNames.json";

/**
 * Created by ksinger on 01.09.2015.
 */

export function getPathname(location) {
  return location && location.pathname;
}

/*
Currently Used/possible Parameters:
  {
    connectionUri: undefined,
    requireLogin: undefined,
    postUri: undefined,
    useCase: undefined,
    useCaseGroup: undefined,
    token: undefined,
    privateId: undefined,
    fromAtomUri: undefined,
    senderSocketType: undefined,
    targetSocketType: undefined,
    mode: undefined,
    tab: undefined,
  }
*/
export function getQueryParams(location) {
  return getParamsObject(location.search.slice(1));
}

/**
 * generates a json object out of the paramsString of a url (everything after the ?)
 * @param paramsString
 * @returns {any}
 */
function getParamsObject(paramsString) {
  let pairs = paramsString.split("&");
  let result = {};
  pairs.forEach(function(pair) {
    pair = pair.split("=");
    result[pair[0]] = pair[1] ? decodeURIComponent(pair[1]) : undefined;
  });

  return JSON.parse(JSON.stringify(result));
}

/**
 * Generates A Link String for React Router
 * @param currentLocation current location object (e.g. this.props.history.location - injected in the component by withRouter)
 * @param newParams - parameters that should be set
 * @param path - path to go to -> if left empty link will use the current pathname of the location
 * @param keepExistingParams - default true, if set to false, only newParams will be added to the link
 * @returns {*}
 */
export function generateLink(
  currentLocation,
  newParams,
  path,
  keepExistingParams = true
) {
  const previousParams = keepExistingParams
    ? getQueryParams(currentLocation)
    : {};
  const mergedParams = { ...previousParams, ...newParams };

  return generateQueryString(
    path ? path : getPathname(currentLocation),
    mergedParams
  );
}

function generateQueryString(path, params = {}) {
  const queryParamsString = generateQueryParamsString(params);

  if (queryParamsString) {
    return path + queryParamsString;
  } else {
    return path;
  }
}

function generateQueryParamsString(params) {
  if (params) {
    const keyValueArray = [];

    for (const key in params) {
      const value = params[key];

      if (value) {
        keyValueArray.push(key + "=" + encodeURIComponent(value));
      }
    }

    if (keyValueArray.length > 0) {
      return "?" + keyValueArray.join("&");
    }
  }
  return undefined;
}
/*
 * Freezes an object recursively.
 *
 * Taken from:
 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/freeze
 */
export function deepFreeze(obj) {
  // Retrieve the property names defined on obj
  const propNames = Object.getOwnPropertyNames(obj);

  // Freeze properties before freezing self
  propNames.forEach(function(name) {
    const prop = obj[name];

    // Freeze prop if it is an object
    if (typeof prop == "object" && !Object.isFrozen(prop)) deepFreeze(prop);
  });

  // Freeze self
  return Object.freeze(obj);
}

export function delay(milliseconds) {
  return new Promise(resolve =>
    window.setTimeout(() => resolve(), milliseconds)
  );
}

/**
 * Generate string of [a-z0-9] with specified length
 * @param length
 * @returns {*}
 */
export function generateIdString(length) {
  const characters = "abcdefghijklmnopqrstuvwxyz0123456789";

  const arrayOfRandoms = length =>
    Array.apply(null, Array(length)).map(() => Math.random());

  return arrayOfRandoms(length)
    .map(randomFloat => Math.floor(randomFloat * characters.length))
    .map(randomPosition => characters.charAt(randomPosition))
    .join("");
}

export function readAsDataURL(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();

    reader.onload = () => resolve(reader.result);
    reader.onerror = () => reject("Something exploded in the FileReader");

    reader.readAsDataURL(file);
  });
}

/**
 * Stable method of determining the type
 * taken from http://bonsaiden.github.io/JavaScript-Garden/
 *
 * NOTE: `NaN` has a type of "Number". Use `isValidNumber` to
 * catch those as well.
 *
 * @param type
 * @param obj
 * @return {boolean}
 */
export function is(type, obj) {
  const clas = Object.prototype.toString.call(obj).slice(8, -1);
  return obj !== undefined && obj !== null && clas === type;
}

export function isValidNumber(num) {
  // isNaN accepts string-numbers, `is` catches that
  return !isNaN(num) && is("Number", num);
}

export function msStringToDate(ts) {
  if (is("String", ts)) {
    ts = Number.parseInt(ts);
  }
  return new Date(ts);
}

/**
 * Deep clone. Don't feed it recurrent structures!
 * Thanks to A. Levy at <http://stackoverflow.com/questions/728360/how-do-i-correctly-clone-a-javascript-object>
 * @param obj
 * @return {*}
 */
export function clone(obj) {
  let copy;

  // Handle the 3 simple types, and null or undefined
  if (null == obj || "object" != typeof obj) return obj;

  // Handle Date
  if (obj instanceof Date) {
    copy = new Date();
    copy.setTime(obj.getTime());
    return copy;
  }

  // Handle Array
  if (obj instanceof Array) {
    copy = [];
    obj.map(subobj => clone(subobj));
    return copy;
  }

  // Handle Object
  if (obj instanceof Object) {
    copy = {};
    for (const attr in obj) {
      if (obj.hasOwnProperty(attr)) copy[attr] = clone(obj[attr]);
    }
    return copy;
  }

  throw new Error("Unable to copy obj! Its type isn't supported.");
}

/**
 * Returns a property of a given object, no matter whether
 * it's a normal or an immutable-js object.
 * @param obj
 * @param property
 */
export function get(obj, property) {
  if (!obj) {
    return undefined;
  } else if (obj.get) {
    /* obj is an immutabljs-object
           * NOTE: the canonical check atm would be `Immutable.Iterable.isIterable(obj)`
           * but that would require including immutable as dependency her and it'd be better
           * to keep this library independent of anything.
           */
    return obj.get(property);
  } else {
    /* obj is a vanilla object */
    return obj[property];
  }
}

/**
 * Shorthand for get(obj, "uri")
 * it's a normal or an immutable-js object.
 * @param obj
 * @param property
 */
export function getUri(obj) {
  return get(obj, "uri");
}

/**
 * Tries to look up a property-path on a nested object-structure.
 * Where `obj.x.y` would throw an error if `x` wasn't defined
 * `get(obj, ['x','y'])` would return undefined.
 * @param obj
 * @param path
 * @return {*}
 */
export function getIn(obj, path) {
  if (!path || !obj || path.length === 0) {
    return undefined;
  } else {
    const child = get(obj, path[0]);
    // let child;
    // if (obj.toJS && obj.get) {
    //   /* obj is an immutabljs-object
    //          * NOTE: the canonical check atm would be `Immutable.Iterable.isIterable(obj)`
    //          * but that would require including immutable as dependency her and it'd be better
    //          * to keep this library independent of anything.
    //          */
    //   child = obj.get(path[0]);
    // } else {
    //   /* obj is a vanilla object */
    //   child = obj[path[0]];
    // }
    if (path.length === 1) {
      /* end of the path */
      return child;
    } else {
      /* recurse */
      return getIn(child, path.slice(1));
    }
  }
}

/**
 * Filters given Map of connections by occurence of a given searchText Object { value: searchString }
 * check only looks into the humanReadable of the atom(s)
 * @param connectionsImm connections map to apply filter on
 * @param allAtomsImm all stored Atoms in the state
 * @param value searchValue
 * @param externalDataState title might be composed by externalData
 * @param includeSenderAtom if true, also searches in senderAtom
 * @param includePersonas if true, also searches the attached Personas or the fakePersonaName within the atom
 * @returns {*}
 */
export function filterConnectionsBySearchValue(
  connectionsImm,
  allAtomsImm,
  { value },
  externalDataState,
  includeSenderAtom = false,
  includePersonas = false
) {
  const tempSearchText = value.trim();

  if (connectionsImm && tempSearchText.length > 0) {
    const regExp = new RegExp(tempSearchText, "i");

    return connectionsImm.filter(conn => {
      const targetAtom = get(allAtomsImm, getTargetAtomUri(conn));
      const targetAtomTitle = getTitle(targetAtom, externalDataState) || "";

      let found = false;

      found = targetAtomTitle.search(regExp) !== -1;
      if (found) {
        return true;
      }

      if (includePersonas) {
        const targetPersona = get(allAtomsImm, getHeldByUri(targetAtom));
        const targetPersonaTitle =
          getTitle(targetPersona, externalDataState) ||
          getFakePersonaName(targetAtom) ||
          "";

        found = targetPersonaTitle.search(regExp) !== -1;

        if (found) {
          return true;
        }
      }

      if (includeSenderAtom) {
        const senderAtom = get(
          allAtomsImm,
          extractAtomUriFromConnectionUri(getUri(conn))
        );
        const senderAtomTitle = getTitle(senderAtom, externalDataState) || "";

        found = senderAtomTitle.search(regExp) !== -1;
        if (found) {
          return true;
        }

        if (includePersonas) {
          const senderPersona = get(allAtomsImm, getHeldByUri(senderAtom));
          const senderPersonaTitle =
            getTitle(senderPersona, externalDataState) ||
            getFakePersonaName(senderAtom) ||
            "";

          found = senderPersonaTitle.search(regExp) !== -1;
          if (found) {
            return true;
          }
        }
      }

      return false;
    });
  }
  return connectionsImm;
}

/**
 * Filters given Map of atoms by occurence of a given searchText Object { value: searchString }
 * check only looks into the humanReadable of the atom(s)
 * @param allAtomsImm atoms to filter in the state
 * @param value searchValue
 * @param externalDataState title might be composed by externalData
 * @returns {*}
 */
export function filterAtomsBySearchValue(
  allAtomsImm,
  { value },
  externalDataState
) {
  const tempSearchText = value.trim();

  if (allAtomsImm && tempSearchText.length > 0) {
    const regExp = new RegExp(tempSearchText, "i");

    return allAtomsImm.filter(
      atom => (getTitle(atom, externalDataState) || "").search(regExp) !== -1
    );
  }
  return allAtomsImm;
}

export function generateHexColor(text) {
  let hash = 0;

  if (text) {
    for (const char of text.split("")) {
      hash = char.charCodeAt(0) + ((hash << 5) - hash);
    }
  }

  const c = (hash & 0x00ffffff).toString(16);

  return "#" + ("00000".substring(0, 6 - c.length) + c);
}

export function generateRgbColorArray(text) {
  if (!text) return [0, 0, 0];
  const hexStr = generateHexColor(text);
  return [
    // ignore first char, as that's the `#`
    hexStr.slice(1, 3),
    hexStr.slice(3, 5),
    hexStr.slice(5, 7),
  ].map(hexColor => parseInt(hexColor, 16));
}

/**
 * Returns the URL if it's already absolute,
 * otherwise prepends window.location.origin.
 *
 * e.g.
 * ```
 * toAbsoluteURL("/somepath/")
 * // => URL("http://example.org/somepath/")
 *
 * toAbsoluteURL("http://example.org/anotherpath/")
 * // => URL("http://example.org/anotherpath/")
 * ```
 * @param {*} pathOrURL
 */
export function toAbsoluteURL(pathOrURL) {
  if (pathOrURL.match(/^https?:\/\//)) {
    return new URL(pathOrURL);
  } else {
    return new URL(pathOrURL, window.location.origin);
  }
}

/**
 * NOTE: don't pass recursive structures!
 * NOTE: don't pass immutablejs structures
 * @param {*} fieldName
 * @param {*} obj a (nested) json-structure
 * @param {*} _acc array that's used as accumulator.
 * @returns an array with the values of all occurances of `obj[fieldName]`, `obj[...][fieldNamE]` etc.
 */
export function findAllFieldOccurancesRecursively(fieldName, obj, _acc = []) {
  if (!obj) {
    return _acc;
  }
  if (obj["@type"] === fieldName && obj["@value"]) {
    _acc.push(obj["@value"]);
    console.debug(
      'obj["@type"] is ',
      fieldName,
      " and value present push value to _acc",
      _acc
    );
    return _acc;
  }
  if (obj.toJS && obj.get) {
    /* obj is an immutabljs-object
             * NOTE: the canonical check atm would be `Immutable.Iterable.isIterable(obj)`
             * but that would require including immutable as dependency her and it'd be better
             * to keep this library independent of anything.
             */

    obj.forEach(val => {
      // iterate and recurse down
      findAllFieldOccurancesRecursively(fieldName, val, _acc);
    });
  } else if (is("Array", obj) || is("Object", obj)) {
    // vannilla-js array or object
    Object.values(obj).forEach(val => {
      // iterate and recurse down
      findAllFieldOccurancesRecursively(fieldName, val, _acc);
    });
  }

  return _acc;
}

/**
 * Parses an `xsd:dateTime`/ISO-8601-string strictly.
 * Docs on format: <http://www.datypic.com/sc/xsd/t-xsd_dateTime.html>
 * @param {string} dateTime e.g. "2018-08-21T14:05:27.568Z"
 * @returns a `Date`-object, with `Invalid Date` if parsing failed.
 */
export function parseDatetimeStrictly(dateTime) {
  if (is("Date", dateTime)) {
    // already parsed
    return dateTime;
  } else if (!is("String", dateTime)) {
    // won't be able to parse
    return undefined;
  }
  const validXsdDateTimeString = !!dateTime.match(
    /^-?\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?(([+-]\d{2}:\d{2})|Z)?/
  );
  const asDateTime = new Date(dateTime);
  if (validXsdDateTimeString && isValidNumber(asDateTime.valueOf())) {
    return asDateTime;
  } else {
    return new Date("Invalid Date"); // string here is just any unparsable date-string to get `Invalid Date`
  }
}

export function isValidDate(dateObj) {
  return dateObj && isValidNumber(dateObj.valueOf());
}

/**
 * Renders the javascript-date to an xsd:dateTime / ISO-8601 date-string
 * with the local timezone at the end (in contrast to `dateTime.toISOString()`
 * that normalizes the time to GMT first).
 * adapted from: <http://usefulangle.com/post/30/javascript-get-date-time-with-offset-hours-minutes>
 * @param {Date} dateTime
 */
export function toLocalISODateString(dateTime) {
  /* ensure two digits, i.e. add 0 before 
   * offsetHours, offsetMins,  date, month, hrs, mins or secs if they are less than 10
   */
  const pad = n => (n + "").padStart(2, "0");

  const timezoneOffsetInMins = dateTime.getTimezoneOffset();
  const offsetHours = pad(parseInt(Math.abs(timezoneOffsetInMins / 60)));
  const offsetMins = pad(Math.abs(timezoneOffsetInMins % 60));

  /* 
   * Add an opposite sign to the offset
   * If offset is 0, it means timezone is UTC
   * => timezoneString: Timezone difference in hours and minutes
   * String such as +5:30 or -6:00 or Z
   */
  let timezoneString;
  if (timezoneOffsetInMins < 0) {
    timezoneString = "+" + offsetHours + ":" + offsetMins;
  } else if (timezoneOffsetInMins > 0) {
    timezoneString = "-" + offsetHours + ":" + offsetMins;
  } else if (timezoneOffsetInMins === 0) {
    timezoneString = "Z";
  }

  const year = dateTime.getFullYear();
  const month = pad(dateTime.getMonth() + 1);
  const date = pad(dateTime.getDate());
  const hours = pad(dateTime.getHours());
  const mins = pad(dateTime.getMinutes());
  const secs = pad(dateTime.getSeconds());

  /* Current datetime
   * String such as 2016-07-16T19:20:30
   */
  const currentDatetime =
    year + "-" + month + "-" + date + "T" + hours + ":" + mins + ":" + secs;

  return currentDatetime + timezoneString;
}

/**
 * Method to create a simple Label for a given uri
 * @param str
 * @returns {*}
 */
export function generateSimpleTransitionLabel(str) {
  if (str) {
    const indexOfLastSharp = str.lastIndexOf("#");

    if (indexOfLastSharp !== -1 && indexOfLastSharp + 1 < str.length) {
      return str.substr(indexOfLastSharp + 1);
    }
  }
  return str;
}

export function base64UrlToUint8Array(base64UrlData) {
  const padding = "=".repeat((4 - (base64UrlData.length % 4)) % 4);
  const base64 = (base64UrlData + padding)
    .replace(/-/g, "+")
    .replace(/_/g, "/");

  const rawData = atob(base64);
  const buffer = new Uint8Array(rawData.length);

  for (const i of buffer.keys()) {
    buffer[i] = rawData.charCodeAt(i);
  }
  return buffer.buffer;
}

export function compareArrayBuffers(left, right) {
  const leftArray = new Uint8Array(left);
  const rightArray = new Uint8Array(right);

  if (leftArray.length !== rightArray.length) return false;

  for (const [index] of leftArray.entries()) {
    if (leftArray[index] !== rightArray[index]) {
      return false;
    }
  }

  return true;
}

export function extractAtomUriFromConnectionUri(connectionUri) {
  return (
    connectionUri &&
    connectionUri.substring(0, connectionUri.lastIndexOf("/c/"))
  );
}

export function extractAtomUriBySocketUri(socketUri) {
  return socketUri && socketUri.substring(0, socketUri.lastIndexOf("#"));
}

export function generateFakePersonaName(atomUri) {
  let hash = 0;

  const hashFunc = text => {
    if (text) {
      for (const char of text.split("")) {
        hash = char.charCodeAt(0) + ((hash << 5) - hash);
      }

      return hash < 0 ? hash * -1 : hash;
    }
  };

  const animalHash = hashFunc("animal" + atomUri);
  const adjectiveHash = hashFunc("adj" + atomUri);

  return (
    fakeNames.adjectives[adjectiveHash % fakeNames.adjectives.length] +
    " " +
    fakeNames.animals[animalHash % fakeNames.animals.length]
  );
}

/**
 * This is just seen as a workaround for our fetchWorker since workers can only exchange strings and the error objects are not able to be enriched
 * we store a stringified version within the error message, this method parses the error payload back to the way we wanted it
 * @param error
 * @returns {{}|any}
 */
export function parseWorkerError(error) {
  //FIXME: we somehow lose the status info of the error from the webworker, lets try and parse the message as a json

  try {
    return JSON.parse(error.message);
  } catch {
    console.error("Could not parse error from worker, return nothing: ", error);
    return {};
  }
}

export function parseJwtToken(token) {
  if (token) {
    const base64Url = token.split(".")[1];
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split("")
        .map(function(c) {
          return "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2);
        })
        .join("")
    );
    return JSON.parse(jsonPayload);
  }

  return undefined;
}
