import _ from "lodash";
import { getHeldByUri } from "./redux/utils/atom-utils.js";

/**
 * Created by ksinger on 01.09.2015.
 */

export function dispatchEvent(elem, eventName, eventData) {
  let event = undefined;
  if (eventData) {
    event = new CustomEvent(eventName, { detail: eventData });
  } else {
    event = new Event(eventName);
  }
  elem.dispatchEvent(event);
}

export function getPathname(location) {
  return location && location.pathname;
}

export function parseHeaderLinks(linkHeaderString) {
  return (
    linkHeaderString &&
    _.chain(linkHeaderString)
      .split(",")
      .map(link => {
        return {
          ref: link
            .split(";")[1]
            .replace(/rel="(.*)"/, "$1")
            .trim(),
          url: link
            .split(";")[0]
            .replace(/<(.*)>/, "$1")
            .trim(),
        };
      })
      .keyBy("ref")
      .mapValues("url")
      .value()
  );
}

/*
Currently Used/possible Parameters:
  {
    connectionUri: undefined,
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
 * parses a json object out of a url, that puts the url/query params within a json object
 * @param url
 * @returns {{params: {}, url: (*|string)}|{params: any, url: (*|string)}|undefined}
 */
export function getLinkAndParams(url) {
  const array = url && url.split("?");

  if (array) {
    if (array.length == 1) {
      return {
        url: array[0],
        params: {},
      };
    } else if (array.length == 2) {
      return {
        url: array[0],
        params: getParamsObject(array[1]),
      };
    }
  }
  return undefined;
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

export function generateQueryParamsString(params) {
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
 * zipWith :: (a -> b -> c) -> [a] -> [b] -> [c]
 * e.g. zipWith((x,y)=>x-y, [8,9,3], [3,2]) // => [5,7]
 * @param f
 * @param xs
 * @param ys
 */
export function zipWith(f, xs, ys) {
  return Array.from(
    {
      length: Math.min(xs.length, ys.length),
    },
    (_, i) => f(xs[i], ys[i])
  );
}

/**
 * Sorts the elements by Date (default order is descending)
 * @param elementsImm elements from state that need to be returned as a sorted array
 * @param selector selector for the date that will be used to sort the elements (default is "lastUpdateDate")
 * @param order if "ASC" then the order will be ascending, everything else resorts to the default sort of descending order
 * @returns {*} sorted Elements array
 */
export function sortByDate(
  elementsImm,
  selector = "lastUpdateDate",
  order = "DESC"
) {
  let sortedElements = elementsImm && elementsImm.toArray();

  if (sortedElements) {
    sortedElements.sort(function(a, b) {
      const bDate = b.get(selector) && b.get(selector).getTime();
      const aDate = a.get(selector) && a.get(selector).getTime();

      if (order === "ASC") {
        return aDate - bDate;
      } else {
        return bDate - aDate;
      }
    });
  }

  return sortedElements;
}

/**
 * Filters given Map of connections by occurence of a given searchText Object { value: searchString }
 * check only looks into the humanReadable of the atom(s)
 * @param connectionsImm connections map to apply filter on
 * @param allAtomsImm all stored Atoms in the state
 * @param value searchValue
 * @param includeSenderAtom if true, also searches in senderAtom
 * @param includePersonas if true, also searches the attached Personas or the fakePersonaName within the atom
 * @returns {*}
 */
export function filterConnectionsBySearchValue(
  connectionsImm,
  allAtomsImm,
  { value },
  includeSenderAtom = false,
  includePersonas = false
) {
  const tempSearchText = value.trim();

  if (connectionsImm && tempSearchText.length > 0) {
    const regExp = new RegExp(tempSearchText, "i");

    return connectionsImm.filter(conn => {
      const targetAtom = get(allAtomsImm, get(conn, "targetAtomUri"));
      const targetAtomTitle = get(targetAtom, "humanReadable") || "";

      let found = false;

      found = targetAtomTitle.search(regExp) !== -1;
      if (found) {
        return true;
      }

      if (includePersonas) {
        const targetPersona = get(allAtomsImm, getHeldByUri(targetAtom));
        const targetPersonaTitle =
          get(targetPersona, "humanReadable") ||
          get(targetAtom, "fakePersonaName") ||
          "";

        found = targetPersonaTitle.search(regExp) !== -1;

        if (found) {
          return true;
        }
      }

      if (includeSenderAtom) {
        const senderAtom = get(
          allAtomsImm,
          extractAtomUriFromConnectionUri(get(conn, "uri"))
        );
        const senderAtomTitle = get(senderAtom, "humanReadable") || "";

        found = senderAtomTitle.search(regExp) !== -1;
        if (found) {
          return true;
        }

        if (includePersonas) {
          const senderPersona = get(allAtomsImm, getHeldByUri(senderAtom));
          const senderPersonaTitle =
            get(senderPersona, "humanReadable") ||
            get(senderAtom, "fakePersonaName") ||
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
 * @returns {*}
 */
export function filterAtomsBySearchValue(allAtomsImm, { value }) {
  const tempSearchText = value.trim();

  if (allAtomsImm && tempSearchText.length > 0) {
    const regExp = new RegExp(tempSearchText, "i");

    return allAtomsImm.filter(
      atom => (get(atom, "humanReadable") || "").search(regExp) !== -1
    );
  }
  return allAtomsImm;
}

/**
 * Sorts the elements by Selector (default order is ascending)
 * @param elementsImm elements from state that need to be returned as a sorted array
 * @param selector selector for the date that will be used to sort the elements (default is "lastUpdateDate")
 * @param order if "ASC" then the order will be ascending, everything else resorts to the default sort of descending order
 * @returns {*} sorted Elements array
 */
export function sortBy(elementsImm, selector = elem => elem, order = "ASC") {
  let sortedElements = elementsImm && elementsImm.toArray();

  if (sortedElements) {
    sortedElements.sort(function(a, b) {
      const bValue = b && selector(b);
      const aValue = a && selector(a);

      if (order === "ASC") {
        if (aValue < bValue) return -1;
        if (aValue > bValue) return 1;
        return 0;
      } else {
        if (bValue < aValue) return -1;
        if (bValue > aValue) return 1;
        return 0;
      }
    });
  }

  return sortedElements;
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
  const colorArray = [
    // ignore first char, as that's the `#`
    hexStr.slice(1, 3),
    hexStr.slice(3, 5),
    hexStr.slice(5, 7),
  ].map(hexColor => parseInt(hexColor, 16));
  return colorArray;
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
  } else if (timezoneOffsetInMins == 0) {
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

    if (indexOfLastSharp != -1 && indexOfLastSharp + 1 < str.length) {
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

  if (leftArray.length != rightArray.length) return false;

  for (const [index] of leftArray.entries()) {
    if (leftArray[index] != rightArray[index]) {
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
    adjectives[adjectiveHash % adjectives.length] +
    " " +
    animals[animalHash % animals.length]
  );
}

const animals = [
  "Canidae",
  "Felidae",
  "Cat",
  "Cattle",
  "Dog",
  "Donkey",
  "Goat",
  "Horse",
  "Pig",
  "Rabbit",
  "Aardvark",
  "Aardwolf",
  "Albatross",
  "Alligator",
  "Alpaca",
  "Amphibian",
  "Anaconda",
  "Angelfish",
  "Anglerfish",
  "Ant",
  "Anteater",
  "Antelope",
  "Antlion",
  "Ape",
  "Aphid",
  "Armadillo",
  "Asp",
  "Baboon",
  "Badger",
  "Bandicoot",
  "Barnacle",
  "Barracuda",
  "Basilisk",
  "Bass",
  "Bat",
  "Bear",
  "Beaver",
  "Bedbug",
  "Bee",
  "Beetle",
  "Bird",
  "Bison",
  "Blackbird",
  "Boa",
  "Boar",
  "Bobcat",
  "Bobolink",
  "Bonobo",
  "Booby",
  "Bovid",
  "Bug",
  "Butterfly",
  "Buzzard",
  "Camel",
  "Canid",
  "Capybara",
  "Cardinal",
  "Caribou",
  "Carp",
  "Cat",
  "Catshark",
  "Caterpillar",
  "Catfish",
  "Cattle",
  "Centipede",
  "Cephalopod",
  "Chameleon",
  "Cheetah",
  "Chickadee",
  "Chicken",
  "Chimpanzee",
  "Chinchilla",
  "Chipmunk",
  "Clam",
  "Clownfish",
  "Cobra",
  "Cockroach",
  "Cod",
  "Condor",
  "Constrictor",
  "Coral",
  "Cougar",
  "Cow",
  "Coyote",
  "Crab",
  "Crane",
  "Crawdad",
  "Crayfish",
  "Cricket",
  "Crocodile",
  "Crow",
  "Cuckoo",
  "Cicada",
  "Damselfly",
  "Deer",
  "Dingo",
  "Dinosaur",
  "Dog",
  "Dolphin",
  "Donkey",
  "Dormouse",
  "Dove",
  "Dragonfly",
  "Dragon",
  "Duck",
  "Eagle",
  "Earthworm",
  "Earwig",
  "Echidna",
  "Eel",
  "Egret",
  "Elephant",
  "Elk",
  "Emu",
  "Ermine",
  "Falcon",
  "Ferret",
  "Finch",
  "Firefly",
  "Fish",
  "Flamingo",
  "Flea",
  "Fly",
  "Flyingfish",
  "Fowl",
  "Fox",
  "Frog",
  "Gamefowl",
  "Galliform",
  "Gazelle",
  "Gecko",
  "Gerbil",
  "Gibbon",
  "Giraffe",
  "Goat",
  "Goldfish",
  "Goose",
  "Gopher",
  "Gorilla",
  "Grasshopper",
  "Grouse",
  "Guan",
  "Guanaco",
  "Guineafowl",
  "Gull",
  "Guppy",
  "Haddock",
  "Halibut",
  "Hamster",
  "Hare",
  "Harrier",
  "Hawk",
  "Hedgehog",
  "Heron",
  "Herring",
  "Hippopotamus",
  "Hookworm",
  "Hornet",
  "Horse",
  "Hoverfly",
  "Hummingbird",
  "Hyena",
  "Iguana",
  "Impala",
  "Jackal",
  "Jaguar",
  "Jay",
  "Jellyfish",
  "Junglefowl",
  "Kangaroo",
  "Kingfisher",
  "Kite",
  "Kiwi",
  "Koala",
  "Koi",
  "Krill",
  "Ladybug",
  "Lamprey",
  "Landfowl",
  "Lark",
  "Leech",
  "Lemming",
  "Lemur",
  "Leopard",
  "Leopon",
  "Limpet",
  "Lion",
  "Lizard",
  "Llama",
  "Lobster",
  "Locust",
  "Loon",
  "Louse",
  "Lungfish",
  "Lynx",
  "Macaw",
  "Mackerel",
  "Magpie",
  "Mammal",
  "Manatee",
  "Mandrill",
  "Marlin",
  "Marmoset",
  "Marmot",
  "Marsupial",
  "Marten",
  "Mastodon",
  "Meadowlark",
  "Meerkat",
  "Mink",
  "Minnow",
  "Mite",
  "Mockingbird",
  "Mole",
  "Mollusk",
  "Mongoose",
  "Monkey",
  "Moose",
  "Mosquito",
  "Moth",
  "Mouse",
  "Mule",
  "Muskox",
  "Narwhal",
  "Newt",
  "Nightingale",
  "Ocelot",
  "Octopus",
  "Opossum",
  "Orangutan",
  "Orca",
  "Ostrich",
  "Otter",
  "Owl",
  "Ox",
  "Panda",
  "Panther",
  "Parakeet",
  "Parrot",
  "Parrotfish",
  "Partridge",
  "Peacock",
  "Peafowl",
  "Pelican",
  "Penguin",
  "Perch",
  "Pheasant",
  "Pig",
  "Pigeon",
  "Pike",
  "Pinniped",
  "Piranha",
  "Planarian",
  "Platypus",
  "Pony",
  "Porcupine",
  "Porpoise",
  "Possum",
  "Prawn",
  "Primate",
  "Ptarmigan",
  "Puffin",
  "Puma",
  "Python",
  "Quail",
  "Quelea",
  "Quokka",
  "Rabbit",
  "Raccoon",
  "Rat",
  "Rattlesnake",
  "Raven",
  "Reindeer",
  "Reptile",
  "Rhinoceros",
  "Roadrunner",
  "Rodent",
  "Rook",
  "Rooster",
  "Roundworm",
  "Sailfish",
  "Salamander",
  "Salmon",
  "Sawfish",
  "Scallop",
  "Scorpion",
  "Seahorse",
  "Shark",
  "Sheep",
  "Shrew",
  "Shrimp",
  "Silkworm",
  "Silverfish",
  "Skink",
  "Skunk",
  "Sloth",
  "Slug",
  "Smelt",
  "Snail",
  "Snake",
  "Snipe",
  "Sole",
  "Sparrow",
  "Spider",
  "Spoonbill",
  "Squid",
  "Squirrel",
  "Starfish",
  "Stingray",
  "Stoat",
  "Stork",
  "Sturgeon",
  "Swallow",
  "Swan",
  "Swift",
  "Swordfish",
  "Swordtail",
  "Tahr",
  "Takin",
  "Tapir",
  "Tarantula",
  "Tarsier",
  "Termite",
  "Tern",
  "Thrush",
  "Tick",
  "Tiger",
  "Tiglon",
  "Toad",
  "Tortoise",
  "Toucan",
  "Trout",
  "Tuna",
  "Turkey",
  "Turtle",
  "Tyrannosaurus",
  "Urial",
  "Vicuna",
  "Viper",
  "Vole",
  "Vulture",
  "Wallaby",
  "Walrus",
  "Wasp",
  "Warbler",
  "Weasel",
  "Whale",
  "Whippet",
  "Whitefish",
  "Wildcat",
  "Wildebeest",
  "Wildfowl",
  "Wolf",
  "Wolverine",
  "Wombat",
  "Woodpecker",
  "Worm",
  "Wren",
  "Xerinae",
  "Yak",
  "Zebra",
  "Alpaca",
  "Cat",
  "Cattle",
  "Chicken",
  "Dog",
  "Donkey",
  "Ferret",
  "Gayal",
  "Goldfish",
  "Guppy",
  "Horse",
  "Koi",
  "Llama",
  "Sheep",
  "Yak",
  "Unicorn",
];
const adjectives = [
  "Average",
  "Big",
  "Colossal",
  "Fat",
  "Giant",
  "Gigantic",
  "Great",
  "Huge",
  "Immense",
  "Large",
  "Little",
  "Long",
  "Mammoth",
  "Massive",
  "Miniature",
  "Petite",
  "Puny",
  "Short",
  "Small",
  "Tall",
  "Tiny",
  "Boiling",
  "Breezy",
  "Broken",
  "Bumpy",
  "Chilly",
  "Cold",
  "Cool",
  "Creepy",
  "Crooked",
  "Cuddly",
  "Curly",
  "Damaged",
  "Damp",
  "Dirty",
  "Dry",
  "Dusty",
  "Filthy",
  "Flaky",
  "Fluffy",
  "Wet",
  "Broad",
  "Chubby",
  "Crooked",
  "Curved",
  "Deep",
  "Flat",
  "High",
  "Hollow",
  "Low",
  "Narrow",
  "Round",
  "Shallow",
  "Skinny",
  "Square",
  "Steep",
  "Straight",
  "Wide",
  "Ancient",
  "Brief",
  "Early",
  "Fast",
  "Late",
  "Long",
  "Modern",
  "Old",
  "Quick",
  "Rapid",
  "Short",
  "Slow",
  "Swift",
  "Young",
  "Abundant",
  "Empty",
  "Few",
  "Heavy",
  "Light",
  "Many",
  "Numerous",
  "Sound",
  "Cooing",
  "Deafening",
  "Faint",
  "Harsh",
  "Hissing",
  "Hushed",
  "Husky",
  "Loud",
  "Melodic",
  "Moaning",
  "Mute",
  "Noisy",
  "Purring",
  "Quiet",
  "Raspy",
  "Resonant",
  "Screeching",
  "Shrill",
  "Silent",
  "Soft",
  "Squealing",
  "Thundering",
  "Voiceless",
  "Whispering",
  "Bitter",
  "Delicious",
  "Fresh",
  "Juicy",
  "Ripe",
  "Rotten",
  "Salty",
  "Sour",
  "Spicy",
  "Stale",
  "Sticky",
  "Strong",
  "Sweet",
  "Tasteless",
  "Tasty",
  "Thirsty",
  "Fluttering",
  "Fuzzy",
  "Greasy",
  "Grubby",
  "Hard",
  "Hot",
  "Icy",
  "Loose",
  "Melted",
  "Plastic",
  "Prickly",
  "Rainy",
  "Rough",
  "Scattered",
  "Shaggy",
  "Shaky",
  "Sharp",
  "Shivering",
  "Silky",
  "Slimy",
  "Slippery",
  "Smooth",
  "Soft",
  "Solid",
  "Steady",
  "Sticky",
  "Tender",
  "Tight",
  "Uneven",
  "Weak",
  "Wet",
  "Wooden",
  "Afraid",
  "Angry",
  "Annoyed",
  "Anxious",
  "Arrogant",
  "Ashamed",
  "Awful",
  "Bad",
  "Bewildered",
  "Bored",
  "Combative",
  "Condemned",
  "Confused",
  "Creepy",
  "Cruel",
  "Dangerous",
  "Defeated",
  "Defiant",
  "Depressed",
  "Disgusted",
  "Disturbed",
  "Eerie",
  "Embarrassed",
  "Envious",
  "Evil",
  "Fierce",
  "Foolish",
  "Frantic",
  "Frightened",
  "Grieving",
  "Helpless",
  "Homeless",
  "Hungry",
  "Hurt",
  "Ill",
  "Jealous",
  "Lonely",
  "Mysterious",
  "Naughty",
  "Nervous",
  "Obnoxious",
  "Outrageous",
  "Panicky",
  "Repulsive",
  "Scary",
  "Scornful",
  "Selfish",
  "Sore",
  "Tense",
  "Terrible",
  "Thoughtless",
  "Tired",
  "Troubled",
  "Upset",
  "Uptight",
  "Weary",
  "Wicked",
  "Worried",
  "Agreeable",
  "Amused",
  "Brave",
  "Calm",
  "Charming",
  "Cheerful",
  "Comfortable",
  "Cooperative",
  "Courageous",
  "Delightful",
  "Determined",
  "Eager",
  "Elated",
  "Enchanting",
  "Encouraging",
  "Energetic",
  "Enthusiastic",
  "Excited",
  "Exuberant",
  "Fair",
  "Faithful",
  "Fantastic",
  "Fine",
  "Friendly",
  "Funny",
  "Gentle",
  "Glorious",
  "Good",
  "Happy",
  "Healthy",
  "Helpful",
  "Hilarious",
  "Jolly",
  "Joyous",
  "Kind",
  "Lively",
  "Lovely",
  "Lucky",
  "Obedient",
  "Perfect",
  "Pleasant",
  "Proud",
  "Relieved",
  "Silly",
  "Smiling",
  "Splendid",
  "Successful",
  "Thoughtful",
  "Victorious",
  "Vivacious",
  "Witty",
  "Wonderful",
  "Zealous",
  "Zany",
  "Other",
  "Good",
  "New",
  "Old",
  "Great",
  "High",
  "Small",
  "Different",
  "Large",
  "Local",
  "Social",
  "Important",
  "Long",
  "Young",
  "National",
  "British",
  "Right",
  "Early",
  "Possible",
  "Big",
  "Little",
  "Political",
  "Able",
  "Late",
  "General",
  "Full",
  "Far",
  "Low",
  "Public",
  "Available",
  "Bad",
  "Main",
  "Sure",
  "Clear",
  "Major",
  "Economic",
  "Only",
  "Likely",
  "Real",
  "Black",
  "Particular",
  "International",
  "Special",
  "Difficult",
  "Certain",
  "Open",
  "Whole",
  "White",
  "Free",
  "Short",
  "Easy",
  "Strong",
  "European",
  "Central",
  "Similar",
  "Human",
  "Common",
  "Necessary",
  "Single",
  "Personal",
  "Hard",
  "Private",
  "Poor",
  "Financial",
  "Wide",
  "Foreign",
  "Simple",
  "Recent",
  "Concerned",
  "American",
  "Various",
  "Close",
  "Fine",
  "English",
  "Wrong",
  "Present",
  "Royal",
  "Natural",
  "Individual",
  "Nice",
  "French",
  "Following",
  "Current",
  "Modern",
  "Labour",
  "Legal",
  "Happy",
  "Final",
  "Red",
  "Normal",
  "Serious",
  "Previous",
  "Total",
  "Prime",
  "Significant",
  "Industrial",
  "Sorry",
  "Dead",
  "Specific",
  "Appropriate",
  "Top",
  "Soviet",
  "Basic",
  "Military",
  "Original",
  "Successful",
  "Aware",
  "Hon",
  "Popular",
  "Heavy",
  "Professional",
  "Direct",
  "Dark",
  "Cold",
  "Ready",
  "Green",
  "Useful",
  "Effective",
  "Western",
  "Traditional",
  "Scottish",
  "German",
  "Independent",
  "Deep",
  "Interesting",
  "Considerable",
  "Involved",
  "Physical",
  "Left",
  "Hot",
  "Existing",
  "Responsible",
  "Complete",
  "Medical",
  "Blue",
  "Extra",
  "Past",
  "Male",
  "Interested",
  "Fair",
  "Essential",
  "Beautiful",
  "Civil",
  "Primary",
  "Obvious",
  "Future",
  "Environmental",
  "Positive",
  "Senior",
  "Nuclear",
  "Annual",
  "Relevant",
  "Huge",
  "Rich",
  "Commercial",
  "Safe",
  "Regional",
  "Practical",
  "Official",
  "Separate",
  "Key",
  "Chief",
  "Regular",
  "Due",
  "Additional",
  "Active",
  "Powerful",
  "Complex",
  "Standard",
  "Impossible",
  "Light",
  "Warm",
  "Middle",
  "Fresh",
  "Sexual",
  "Front",
  "Domestic",
  "Actual",
  "United",
  "Technical",
  "Ordinary",
  "Cheap",
  "Strange",
  "Internal",
  "Excellent",
  "Quiet",
  "Soft",
  "Potential",
  "Northern",
  "Religious",
  "Quick",
  "Very",
  "Famous",
  "Cultural",
  "Proper",
  "Broad",
  "Joint",
  "Formal",
  "Limited",
  "Conservative",
  "Lovely",
  "Usual",
  "Ltd",
  "Unable",
  "Rural",
  "Initial",
  "Substantial",
  "Christian",
  "Bright",
  "Average",
  "Leading",
  "Reasonable",
  "Immediate",
  "Suitable",
  "Equal",
  "Detailed",
  "Working",
  "Overall",
  "Female",
  "Afraid",
  "Democratic",
  "Growing",
  "Sufficient",
  "Scientific",
  "Eastern",
  "Correct",
  "Inc",
  "Irish",
  "Expensive",
  "Educational",
  "Mental",
  "Dangerous",
  "Critical",
  "Increased",
  "Familiar",
  "Unlikely",
  "Double",
  "Perfect",
  "Slow",
  "Tiny",
  "Dry",
  "Historical",
  "Thin",
  "Daily",
  "Southern",
  "Increasing",
  "Wild",
  "Alone",
  "Urban",
  "Empty",
  "Married",
  "Narrow",
  "Liberal",
  "Supposed",
  "Upper",
  "Apparent",
  "Tall",
  "Busy",
  "Bloody",
  "Prepared",
  "Russian",
  "Moral",
  "Careful",
  "Clean",
  "Attractive",
  "Japanese",
  "Vital",
  "Thick",
  "Alternative",
  "Fast",
  "Ancient",
  "Elderly",
  "Rare",
  "External",
  "Capable",
  "Brief",
  "Wonderful",
  "Grand",
  "Typical",
  "Entire",
  "Grey",
  "Constant",
  "Vast",
  "Surprised",
  "Ideal",
  "Terrible",
  "Academic",
  "Funny",
  "Minor",
  "Pleased",
  "Severe",
  "Ill",
  "Corporate",
  "Negative",
  "Permanent",
  "Weak",
  "Brown",
  "Fundamental",
  "Odd",
  "Crucial",
  "Inner",
  "Used",
  "Criminal",
  "Contemporary",
  "Sharp",
  "Sick",
  "Near",
  "Roman",
  "Massive",
  "Unique",
  "Secondary",
  "Parliamentary",
  "African",
  "Unknown",
  "Subsequent",
  "Angry",
  "Alive",
  "Guilty",
  "Lucky",
  "Enormous",
  "Well",
  "Communist",
  "Yellow",
  "Unusual",
  "Net",
  "Tough",
  "Dear",
  "Extensive",
  "Glad",
  "Remaining",
  "Agricultural",
  "Alright",
  "Healthy",
  "Italian",
  "Principal",
  "Tired",
  "Efficient",
  "Comfortable",
  "Chinese",
  "Relative",
  "Friendly",
  "Conventional",
  "Willing",
  "Sudden",
  "Proposed",
  "Voluntary",
  "Slight",
  "Valuable",
  "Dramatic",
  "Golden",
  "Temporary",
  "Federal",
  "Keen",
  "Flat",
  "Silent",
  "Indian",
  "Worried",
  "Pale",
  "Statutory",
  "Welsh",
  "Dependent",
  "Firm",
  "Wet",
  "Competitive",
  "Armed",
  "Radical",
  "Outside",
  "Acceptable",
  "Sensitive",
  "Living",
  "Pure",
  "Global",
  "Emotional",
  "Sad",
  "Secret",
  "Rapid",
  "Adequate",
  "Fixed",
  "Sweet",
  "Administrative",
  "Wooden",
  "Remarkable",
  "Comprehensive",
  "Surprising",
  "Solid",
  "Rough",
  "Mere",
  "Mass",
  "Brilliant",
  "Maximum",
  "Absolute",
  "Tory",
  "Electronic",
  "Visual",
  "Electric",
  "Cool",
  "Spanish",
  "Literary",
  "Continuing",
  "Supreme",
  "Chemical",
  "Genuine",
  "Exciting",
  "Written",
  "Stupid",
  "Advanced",
  "Extreme",
  "Classical",
  "Fit",
  "Favourite",
  "Socialist",
  "Widespread",
  "Confident",
  "Straight",
  "Catholic",
  "Proud",
  "Numerous",
  "Opposite",
  "Distinct",
  "Mad",
  "Helpful",
  "Given",
  "Disabled",
  "Consistent",
  "Anxious",
  "Nervous",
  "Awful",
  "Stable",
  "Constitutional",
  "Satisfied",
  "Conscious",
  "Developing",
  "Strategic",
  "Holy",
  "Smooth",
  "Dominant",
  "Remote",
  "Theoretical",
  "Outstanding",
  "Pink",
  "Pretty",
  "Clinical",
  "Minimum",
  "Honest",
  "Impressive",
  "Related",
  "Residential",
  "Extraordinary",
  "Plain",
  "Visible",
  "Accurate",
  "Distant",
  "Still",
  "Greek",
  "Complicated",
  "Musical",
  "Precise",
  "Gentle",
  "Broken",
  "Live",
  "Silly",
  "Fat",
  "Tight",
  "Monetary",
  "Round",
  "Psychological",
  "Violent",
  "Unemployed",
  "Inevitable",
  "Junior",
  "Sensible",
  "Grateful",
  "Pleasant",
  "Dirty",
  "Structural",
  "Welcome",
  "Deaf",
  "Above",
  "Continuous",
  "Blind",
  "Overseas",
  "Mean",
  "Entitled",
  "Delighted",
  "Loose",
  "Occasional",
  "Evident",
  "Desperate",
  "Fellow",
  "Universal",
  "Square",
  "Steady",
  "Classic",
  "Equivalent",
  "Intellectual",
  "Victorian",
  "Level",
  "Ultimate",
  "Creative",
  "Lost",
  "Medieval",
  "Clever",
  "Linguistic",
  "Convinced",
  "Judicial",
  "Raw",
  "Sophisticated",
  "Asleep",
  "Vulnerable",
  "Illegal",
  "Outer",
  "Revolutionary",
  "Bitter",
  "Changing",
  "Australian",
  "Native",
  "Imperial",
  "Strict",
  "Wise",
  "Informal",
  "Flexible",
  "Collective",
  "Frequent",
  "Experimental",
  "Spiritual",
  "Intense",
  "Rational",
  "Ethnic",
  "Generous",
  "Inadequate",
  "Prominent",
  "Logical",
  "Bare",
  "Historic",
  "Modest",
  "Dutch",
  "Acute",
  "Electrical",
  "Valid",
  "Weekly",
  "Gross",
  "Automatic",
  "Loud",
  "Reliable",
  "Mutual",
  "Liable",
  "Multiple",
  "Ruling",
  "Curious",
  "Arab",
  "Sole",
  "Jewish",
  "Managing",
  "Pregnant",
  "Latin",
  "Nearby",
  "Exact",
  "Underlying",
  "Identical",
  "Satisfactory",
  "Marginal",
  "Distinctive",
  "Electoral",
  "Urgent",
  "Presidential",
  "Controversial",
  "Oral",
  "Everyday",
  "Encouraging",
  "Organic",
  "Continued",
  "Expected",
  "Statistical",
  "Desirable",
  "Innocent",
  "Improved",
  "Exclusive",
  "Marked",
  "Experienced",
  "Unexpected",
  "Superb",
  "Sheer",
  "Disappointed",
  "Frightened",
  "Gastric",
  "Capitalist",
  "Romantic",
  "Naked",
  "Reluctant",
  "Magnificent",
  "Convenient",
  "Established",
  "Closed",
  "Uncertain",
  "Artificial",
  "Diplomatic",
  "Tremendous",
  "Marine",
  "Mechanical",
  "Retail",
  "Institutional",
  "Mixed",
  "Required",
  "Biological",
  "Known",
  "Functional",
  "Straightforward",
  "Superior",
  "Digital",
  "Spectacular",
  "Unhappy",
  "Confused",
  "Unfair",
  "Aggressive",
  "Spare",
  "Painful",
  "Abstract",
  "Asian",
  "Associated",
  "Legislative",
  "Monthly",
  "Intelligent",
  "Hungry",
  "Explicit",
  "Nasty",
  "Just",
  "Faint",
  "Coloured",
  "Ridiculous",
  "Amazing",
  "Comparable",
  "Successive",
  "Realistic",
  "Back",
  "Decent",
  "Unnecessary",
  "Flying",
  "Random",
  "Influential",
  "Dull",
  "Genetic",
  "Neat",
  "Marvellous",
  "Crazy",
  "Damp",
  "Giant",
  "Secure",
  "Bottom",
  "Skilled",
  "Subtle",
  "Elegant",
  "Brave",
  "Lesser",
  "Parallel",
  "Steep",
  "Intensive",
  "Casual",
  "Tropical",
  "Lonely",
  "Partial",
  "Preliminary",
  "Concrete",
  "Alleged",
  "Assistant",
  "Vertical",
  "Upset",
  "Delicate",
  "Mild",
  "Occupational",
  "Excessive",
  "Progressive",
  "Iraqi",
  "Exceptional",
  "Integrated",
  "Striking",
  "Continental",
  "Okay",
  "Harsh",
  "Combined",
  "Fierce",
  "Handsome",
  "Characteristic",
  "Chronic",
  "Compulsory",
  "Interim",
  "Objective",
  "Splendid",
  "Magic",
  "Systematic",
  "Obliged",
  "Payable",
  "Fun",
  "Horrible",
  "Primitive",
  "Fascinating",
  "Ideological",
  "Metropolitan",
  "Surrounding",
  "Estimated",
  "Peaceful",
  "Premier",
  "Operational",
  "Technological",
  "Kind",
  "Advisory",
  "Hostile",
  "Precious",
  "Gay",
  "Accessible",
  "Determined",
  "Excited",
  "Impressed",
  "Provincial",
  "Smart",
  "Endless",
  "Isolated",
  "Drunk",
  "Geographical",
  "Like",
  "Dynamic",
  "Boring",
  "Forthcoming",
  "Unfortunate",
  "Definite",
  "Super",
  "Notable",
  "Indirect",
  "Stiff",
  "Wealthy",
  "Awkward",
  "Lively",
  "Neutral",
  "Artistic",
  "Content",
  "Mature",
  "Colonial",
  "Ambitious",
  "Evil",
  "Magnetic",
  "Verbal",
  "Legitimate",
  "Sympathetic",
  "Empirical",
  "Head",
  "Shallow",
  "Vague",
  "Naval",
  "Depressed",
  "Shared",
  "Added",
  "Shocked",
  "Mid",
  "Worthwhile",
  "Qualified",
  "Missing",
  "Blank",
  "Absent",
  "Favourable",
  "Polish",
  "Israeli",
  "Developed",
  "Profound",
  "Representative",
  "Enthusiastic",
  "Dreadful",
  "Rigid",
  "Reduced",
  "Cruel",
  "Coastal",
  "Peculiar",
  "Racial",
  "Ugly",
  "Swiss",
  "Crude",
  "Extended",
  "Selected",
  "Eager",
  "Feminist",
  "Canadian",
  "Bold",
  "Relaxed",
  "Corresponding",
  "Running",
  "Planned",
  "Applicable",
  "Immense",
  "Allied",
  "Comparative",
  "Uncomfortable",
  "Conservation",
  "Productive",
  "Beneficial",
  "Bored",
  "Charming",
  "Minimal",
  "Mobile",
  "Turkish",
  "Orange",
  "Rear",
  "Passive",
  "Suspicious",
  "Overwhelming",
  "Fatal",
  "Resulting",
  "Symbolic",
  "Registered",
  "Neighbouring",
  "Calm",
  "Irrelevant",
  "Patient",
  "Compact",
  "Profitable",
  "Rival",
  "Loyal",
  "Moderate",
  "Distinguished",
  "Interior",
  "Noble",
  "Insufficient",
  "Eligible",
  "Mysterious",
  "Varying",
  "Managerial",
  "Molecular",
  "Olympic",
  "Linear",
  "Prospective",
  "Printed",
  "Parental",
  "Diverse",
  "Elaborate",
  "Furious",
  "Fiscal",
  "Burning",
  "Useless",
  "Semantic",
  "Embarrassed",
  "Inherent",
  "Philosophical",
  "Deliberate",
  "Awake",
  "Variable",
  "Promising",
  "Unpleasant",
  "Varied",
  "Sacred",
  "Selective",
  "Inclined",
  "Tender",
  "Hidden",
  "Worthy",
  "Intermediate",
  "Sound",
  "Protective",
  "Fortunate",
  "Slim",
  "Islamic",
  "Defensive",
  "Divine",
  "Stuck",
  "Driving",
  "Invisible",
  "Misleading",
  "Circular",
  "Mathematical",
  "Inappropriate",
  "Liquid",
  "Persistent",
  "Solar",
  "Doubtful",
  "Manual",
  "Architectural",
  "Intact",
  "Incredible",
  "Devoted",
  "Prior",
  "Tragic",
  "Respectable",
  "Optimistic",
  "Convincing",
  "Unacceptable",
  "Decisive",
  "Competent",
  "Spatial",
  "Respective",
  "Binding",
  "Relieved",
  "Nursing",
  "Toxic",
  "Select",
  "Redundant",
  "Integral",
  "Then",
  "Probable",
  "Amateur",
  "Fond",
  "Passing",
  "Specified",
  "Territorial",
  "Horizontal",
  "Inland",
  "Cognitive",
  "Regulatory",
  "Miserable",
  "Resident",
  "Polite",
  "Scared",
  "Marxist",
  "Gothic",
  "Civilian",
  "Instant",
  "Lengthy",
  "Adverse",
  "Korean",
  "Unconscious",
  "Anonymous",
  "Aesthetic",
  "Orthodox",
  "Static",
  "Unaware",
  "Costly",
  "Fantastic",
  "Foolish",
  "Fashionable",
  "Causal",
  "Compatible",
  "Wee",
  "Implicit",
  "Dual",
  "Ok",
  "Cheerful",
  "Subjective",
  "Forward",
  "Surviving",
  "Exotic",
  "Purple",
  "Cautious",
  "Visiting",
  "Aggregate",
  "Ethical",
  "Protestant",
  "Teenage",
  "Dying",
  "Disastrous",
  "Delicious",
  "Confidential",
  "Underground",
  "Thorough",
  "Grim",
  "Autonomous",
  "Atomic",
  "Frozen",
  "Colourful",
  "Injured",
  "Uniform",
  "Ashamed",
  "Glorious",
  "Wicked",
  "Coherent",
  "Rising",
  "Shy",
  "Novel",
  "Balanced",
  "Delightful",
  "Arbitrary",
  "Adjacent",
  "Psychiatric",
  "Worrying",
  "Weird",
  "Unchanged",
  "Rolling",
  "Evolutionary",
  "Intimate",
  "Sporting",
  "Disciplinary",
  "Formidable",
  "Lexical",
  "Noisy",
  "Gradual",
  "Accused",
  "Homeless",
  "Supporting",
  "Coming",
  "Renewed",
  "Excess",
  "Retired",
  "Rubber",
  "Chosen",
  "Outdoor",
  "Embarrassing",
  "Preferred",
  "Bizarre",
  "Appalling",
  "Agreed",
  "Imaginative",
  "Governing",
  "Accepted",
  "Vocational",
  "Palestinian",
  "Mighty",
  "Puzzled",
  "Worldwide",
  "Handicapped",
  "Organisational",
  "Sunny",
  "Eldest",
  "Eventual",
  "Spontaneous",
  "Vivid",
  "Rude",
  "Faithful",
  "Ministerial",
  "Innovative",
  "Controlled",
  "Conceptual",
  "Unwilling",
  "Civic",
  "Meaningful",
  "Disturbing",
  "Alive",
  "Brainy",
  "Breakable",
  "Busy",
  "Careful",
  "Cautious",
  "Clever",
  "Concerned",
  "Crazy",
  "Curious",
  "Dead",
  "Different",
  "Difficult",
  "Doubtful",
  "Easy",
  "Famous",
  "Fragile",
  "Helpful",
  "Helpless",
  "Important",
  "Impossible",
  "Innocent",
  "Inquisitive",
  "Modern",
  "Open",
  "Outstanding",
  "Poor",
  "Powerful",
  "Puzzled",
  "Real",
  "Rich",
  "Shy",
  "Sleepy",
  "Stupid",
  "Super",
  "Tame",
  "Uninterested",
  "Wandering",
  "Wild",
  "Wrong",
  "Adorable",
  "Alert",
  "Average",
  "Beautiful",
  "Blonde",
  "Bloody",
  "Blushing",
  "Bright",
  "Clean",
  "Clear",
  "Cloudy",
  "Colorful",
  "Crowded",
  "Cute",
  "Dark",
  "Drab",
  "Distinct",
  "Dull",
  "Elegant",
  "Fancy",
  "Filthy",
  "Glamorous",
  "Gleaming",
  "Graceful",
  "Grotesque",
  "Homely",
  "Light",
  "Misty",
  "Motionless",
  "Muddy",
  "Plain",
  "Poised",
  "Quaint",
  "Shiny",
  "Smoggy",
  "Sparkling",
  "Spotless",
  "Stormy",
  "Strange",
  "Ugly",
  "Unsightly",
  "Unusual",
  "Bad",
  "Better",
  "Beautiful",
  "Big",
  "Black",
  "Blue",
  "Bright",
  "Clumsy",
  "Crazy",
  "Dizzy",
  "Dull",
  "Fat",
  "Frail",
  "Friendly",
  "Funny",
  "Great",
  "Green",
  "Gigantic",
  "Gorgeous",
  "Grumpy",
  "Handsome",
  "Happy",
  "Horrible",
  "Itchy",
  "Jittery",
  "Jolly",
  "Kind",
  "Long",
  "Lazy",
  "Magnificent",
  "Magenta",
  "Many",
  "Mighty",
  "Mushy",
  "Nasty",
  "New",
  "Nice",
  "Nosy",
  "Nutty",
  "Nutritious",
  "Odd",
  "Orange",
  "Ordinary",
  "Pretty",
  "Precious",
  "Prickly",
  "Purple",
  "Quaint",
  "Quiet",
  "Quick",
  "Quickest",
  "Rainy",
  "Rare",
  "Ratty",
  "Red",
  "Roasted",
  "Robust",
  "Round",
  "Sad",
  "Scary",
  "Scrawny",
  "Short",
  "Silly",
  "Stingy",
  "Strange",
  "Striped",
  "Spotty",
  "Tart",
  "Tall",
  "Tame",
  "Tan",
  "Tender",
  "Testy",
  "Tricky",
  "Tough",
  "Ugly",
  "Ugliest",
  "Vast",
  "Watery",
  "Wasteful",
  "Wonderful",
  "Yellow",
  "Yummy",
  "Zany",
];
