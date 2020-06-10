import _ from "lodash";

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
 * @returns {*}
 */
export function filterConnectionsBySearchValue(
  connectionsImm,
  allAtomsImm,
  { value },
  includeSenderAtom = false
) {
  const tempSearchText = value.trim();

  if (connectionsImm && tempSearchText.length > 0) {
    const regExp = new RegExp(tempSearchText, "i");

    return connectionsImm.filter(conn => {
      const targetAtom = get(allAtomsImm, get(conn, "targetAtomUri"));
      const targetAtomTitle = get(targetAtom, "humanReadable") || "";

      if (includeSenderAtom) {
        const senderAtom = get(allAtomsImm, get(conn, "uri").split("/c")[0]);
        const senderAtomTitle = get(senderAtom, "humanReadable") || "";

        return (
          targetAtomTitle.search(regExp) !== -1 ||
          senderAtomTitle.search(regExp) !== -1
        );
      }
      return targetAtomTitle.search(regExp) !== -1;
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

export function generateFakePersonaName(atomUri) {
  let hash = 0;

  if (atomUri) {
    for (const char of atomUri.split("")) {
      hash = char.charCodeAt(0) + ((hash << 5) - hash);
    }
  }

  return (
    adjectives[hash % adjectives.length] + " " + animals[hash % animals.length]
  );
}

const animals = [
  "canidae",
  "felidae",
  "cat",
  "cattle",
  "dog",
  "donkey",
  "goat",
  "horse",
  "pig",
  "rabbit",
  "aardvark",
  "aardwolf",
  "albatross",
  "alligator",
  "alpaca",
  "amphibian",
  "anaconda",
  "angelfish",
  "anglerfish",
  "ant",
  "anteater",
  "antelope",
  "antlion",
  "ape",
  "aphid",
  "armadillo",
  "asp",
  "baboon",
  "badger",
  "bandicoot",
  "barnacle",
  "barracuda",
  "basilisk",
  "bass",
  "bat",
  "bear",
  "beaver",
  "bedbug",
  "bee",
  "beetle",
  "bird",
  "bison",
  "blackbird",
  "boa",
  "boar",
  "bobcat",
  "bobolink",
  "bonobo",
  "booby",
  "bovid",
  "bug",
  "butterfly",
  "buzzard",
  "camel",
  "canid",
  "capybara",
  "cardinal",
  "caribou",
  "carp",
  "cat",
  "catshark",
  "caterpillar",
  "catfish",
  "cattle",
  "centipede",
  "cephalopod",
  "chameleon",
  "cheetah",
  "chickadee",
  "chicken",
  "chimpanzee",
  "chinchilla",
  "chipmunk",
  "clam",
  "clownfish",
  "cobra",
  "cockroach",
  "cod",
  "condor",
  "constrictor",
  "coral",
  "cougar",
  "cow",
  "coyote",
  "crab",
  "crane",
  "crawdad",
  "crayfish",
  "cricket",
  "crocodile",
  "crow",
  "cuckoo",
  "cicada",
  "damselfly",
  "deer",
  "dingo",
  "dinosaur",
  "dog",
  "dolphin",
  "donkey",
  "dormouse",
  "dove",
  "dragonfly",
  "dragon",
  "duck",
  "eagle",
  "earthworm",
  "earwig",
  "echidna",
  "eel",
  "egret",
  "elephant",
  "elk",
  "emu",
  "ermine",
  "falcon",
  "ferret",
  "finch",
  "firefly",
  "fish",
  "flamingo",
  "flea",
  "fly",
  "flyingfish",
  "fowl",
  "fox",
  "frog",
  "gamefowl",
  "galliform",
  "gazelle",
  "gecko",
  "gerbil",
  "gibbon",
  "giraffe",
  "goat",
  "goldfish",
  "goose",
  "gopher",
  "gorilla",
  "grasshopper",
  "grouse",
  "guan",
  "guanaco",
  "guineafowl",
  "gull",
  "guppy",
  "haddock",
  "halibut",
  "hamster",
  "hare",
  "harrier",
  "hawk",
  "hedgehog",
  "heron",
  "herring",
  "hippopotamus",
  "hookworm",
  "hornet",
  "horse",
  "hoverfly",
  "hummingbird",
  "hyena",
  "iguana",
  "impala",
  "jackal",
  "jaguar",
  "jay",
  "jellyfish",
  "junglefowl",
  "kangaroo",
  "kingfisher",
  "kite",
  "kiwi",
  "koala",
  "koi",
  "krill",
  "ladybug",
  "lamprey",
  "landfowl",
  "lark",
  "leech",
  "lemming",
  "lemur",
  "leopard",
  "leopon",
  "limpet",
  "lion",
  "lizard",
  "llama",
  "lobster",
  "locust",
  "loon",
  "louse",
  "lungfish",
  "lynx",
  "macaw",
  "mackerel",
  "magpie",
  "mammal",
  "manatee",
  "mandrill",
  "marlin",
  "marmoset",
  "marmot",
  "marsupial",
  "marten",
  "mastodon",
  "meadowlark",
  "meerkat",
  "mink",
  "minnow",
  "mite",
  "mockingbird",
  "mole",
  "mollusk",
  "mongoose",
  "monkey",
  "moose",
  "mosquito",
  "moth",
  "mouse",
  "mule",
  "muskox",
  "narwhal",
  "newt",
  "nightingale",
  "ocelot",
  "octopus",
  "opossum",
  "orangutan",
  "orca",
  "ostrich",
  "otter",
  "owl",
  "ox",
  "panda",
  "panther",
  "parakeet",
  "parrot",
  "parrotfish",
  "partridge",
  "peacock",
  "peafowl",
  "pelican",
  "penguin",
  "perch",
  "pheasant",
  "pig",
  "pigeon",
  "pike",
  "pinniped",
  "piranha",
  "planarian",
  "platypus",
  "pony",
  "porcupine",
  "porpoise",
  "possum",
  "prawn",
  "primate",
  "ptarmigan",
  "puffin",
  "puma",
  "python",
  "quail",
  "quelea",
  "quokka",
  "rabbit",
  "raccoon",
  "rat",
  "rattlesnake",
  "raven",
  "reindeer",
  "reptile",
  "rhinoceros",
  "roadrunner",
  "rodent",
  "rook",
  "rooster",
  "roundworm",
  "sailfish",
  "salamander",
  "salmon",
  "sawfish",
  "scallop",
  "scorpion",
  "seahorse",
  "shark",
  "sheep",
  "shrew",
  "shrimp",
  "silkworm",
  "silverfish",
  "skink",
  "skunk",
  "sloth",
  "slug",
  "smelt",
  "snail",
  "snake",
  "snipe",
  "sole",
  "sparrow",
  "spider",
  "spoonbill",
  "squid",
  "squirrel",
  "starfish",
  "stingray",
  "stoat",
  "stork",
  "sturgeon",
  "swallow",
  "swan",
  "swift",
  "swordfish",
  "swordtail",
  "tahr",
  "takin",
  "tapir",
  "tarantula",
  "tarsier",
  "termite",
  "tern",
  "thrush",
  "tick",
  "tiger",
  "tiglon",
  "toad",
  "tortoise",
  "toucan",
  "trout",
  "tuna",
  "turkey",
  "turtle",
  "tyrannosaurus",
  "urial",
  "vicuna",
  "viper",
  "vole",
  "vulture",
  "wallaby",
  "walrus",
  "wasp",
  "warbler",
  "weasel",
  "whale",
  "whippet",
  "whitefish",
  "wildcat",
  "wildebeest",
  "wildfowl",
  "wolf",
  "wolverine",
  "wombat",
  "woodpecker",
  "worm",
  "wren",
  "xerinae",
  "yak",
  "zebra",
  "alpaca",
  "cat",
  "cattle",
  "chicken",
  "dog",
  "donkey",
  "ferret",
  "gayal",
  "goldfish",
  "guppy",
  "horse",
  "koi",
  "llama",
  "sheep",
  "yak",
  "unicorn",
];
const adjectives = [
  "average",
  "big",
  "colossal",
  "fat",
  "giant",
  "gigantic",
  "great",
  "huge",
  "immense",
  "large",
  "little",
  "long",
  "mammoth",
  "massive",
  "miniature",
  "petite",
  "puny",
  "short",
  "small",
  "tall",
  "tiny",
  "boiling",
  "breezy",
  "broken",
  "bumpy",
  "chilly",
  "cold",
  "cool",
  "creepy",
  "crooked",
  "cuddly",
  "curly",
  "damaged",
  "damp",
  "dirty",
  "dry",
  "dusty",
  "filthy",
  "flaky",
  "fluffy",
  "wet",
  "broad",
  "chubby",
  "crooked",
  "curved",
  "deep",
  "flat",
  "high",
  "hollow",
  "low",
  "narrow",
  "round",
  "shallow",
  "skinny",
  "square",
  "steep",
  "straight",
  "wide",
  "ancient",
  "brief",
  "early",
  "fast",
  "late",
  "long",
  "modern",
  "old",
  "quick",
  "rapid",
  "short",
  "slow",
  "swift",
  "young",
  "abundant",
  "empty",
  "few",
  "heavy",
  "light",
  "many",
  "numerous",
  "Sound",
  "cooing",
  "deafening",
  "faint",
  "harsh",
  "hissing",
  "hushed",
  "husky",
  "loud",
  "melodic",
  "moaning",
  "mute",
  "noisy",
  "purring",
  "quiet",
  "raspy",
  "resonant",
  "screeching",
  "shrill",
  "silent",
  "soft",
  "squealing",
  "thundering",
  "voiceless",
  "whispering",
  "bitter",
  "delicious",
  "fresh",
  "juicy",
  "ripe",
  "rotten",
  "salty",
  "sour",
  "spicy",
  "stale",
  "sticky",
  "strong",
  "sweet",
  "tasteless",
  "tasty",
  "thirsty",
  "fluttering",
  "fuzzy",
  "greasy",
  "grubby",
  "hard",
  "hot",
  "icy",
  "loose",
  "melted",
  "plastic",
  "prickly",
  "rainy",
  "rough",
  "scattered",
  "shaggy",
  "shaky",
  "sharp",
  "shivering",
  "silky",
  "slimy",
  "slippery",
  "smooth",
  "soft",
  "solid",
  "steady",
  "sticky",
  "tender",
  "tight",
  "uneven",
  "weak",
  "wet",
  "wooden",
  "afraid",
  "angry",
  "annoyed",
  "anxious",
  "arrogant",
  "ashamed",
  "awful",
  "bad",
  "bewildered",
  "bored",
  "combative",
  "condemned",
  "confused",
  "creepy",
  "cruel",
  "dangerous",
  "defeated",
  "defiant",
  "depressed",
  "disgusted",
  "disturbed",
  "eerie",
  "embarrassed",
  "envious",
  "evil",
  "fierce",
  "foolish",
  "frantic",
  "frightened",
  "grieving",
  "helpless",
  "homeless",
  "hungry",
  "hurt",
  "ill",
  "jealous",
  "lonely",
  "mysterious",
  "naughty",
  "nervous",
  "obnoxious",
  "outrageous",
  "panicky",
  "repulsive",
  "scary",
  "scornful",
  "selfish",
  "sore",
  "tense",
  "terrible",
  "thoughtless",
  "tired",
  "troubled",
  "upset",
  "uptight",
  "weary",
  "wicked",
  "worried",
  "agreeable",
  "amused",
  "brave",
  "calm",
  "charming",
  "cheerful",
  "comfortable",
  "cooperative",
  "courageous",
  "delightful",
  "determined",
  "eager",
  "elated",
  "enchanting",
  "encouraging",
  "energetic",
  "enthusiastic",
  "excited",
  "exuberant",
  "fair",
  "faithful",
  "fantastic",
  "fine",
  "friendly",
  "funny",
  "gentle",
  "glorious",
  "good",
  "happy",
  "healthy",
  "helpful",
  "hilarious",
  "jolly",
  "joyous",
  "kind",
  "lively",
  "lovely",
  "lucky",
  "obedient",
  "perfect",
  "pleasant",
  "proud",
  "relieved",
  "silly",
  "smiling",
  "splendid",
  "successful",
  "thoughtful",
  "victorious",
  "vivacious",
  "witty",
  "wonderful",
  "zealous",
  "zany",
  "other",
  "good",
  "new",
  "old",
  "great",
  "high",
  "small",
  "different",
  "large",
  "local",
  "social",
  "important",
  "long",
  "young",
  "national",
  "british",
  "right",
  "early",
  "possible",
  "big",
  "little",
  "political",
  "able",
  "late",
  "general",
  "full",
  "far",
  "low",
  "public",
  "available",
  "bad",
  "main",
  "sure",
  "clear",
  "major",
  "economic",
  "only",
  "likely",
  "real",
  "black",
  "particular",
  "international",
  "special",
  "difficult",
  "certain",
  "open",
  "whole",
  "white",
  "free",
  "short",
  "easy",
  "strong",
  "european",
  "central",
  "similar",
  "human",
  "common",
  "necessary",
  "single",
  "personal",
  "hard",
  "private",
  "poor",
  "financial",
  "wide",
  "foreign",
  "simple",
  "recent",
  "concerned",
  "american",
  "various",
  "close",
  "fine",
  "english",
  "wrong",
  "present",
  "royal",
  "natural",
  "individual",
  "nice",
  "french",
  "following",
  "current",
  "modern",
  "labour",
  "legal",
  "happy",
  "final",
  "red",
  "normal",
  "serious",
  "previous",
  "total",
  "prime",
  "significant",
  "industrial",
  "sorry",
  "dead",
  "specific",
  "appropriate",
  "top",
  "soviet",
  "basic",
  "military",
  "original",
  "successful",
  "aware",
  "hon",
  "popular",
  "heavy",
  "professional",
  "direct",
  "dark",
  "cold",
  "ready",
  "green",
  "useful",
  "effective",
  "western",
  "traditional",
  "scottish",
  "german",
  "independent",
  "deep",
  "interesting",
  "considerable",
  "involved",
  "physical",
  "left",
  "hot",
  "existing",
  "responsible",
  "complete",
  "medical",
  "blue",
  "extra",
  "past",
  "male",
  "interested",
  "fair",
  "essential",
  "beautiful",
  "civil",
  "primary",
  "obvious",
  "future",
  "environmental",
  "positive",
  "senior",
  "nuclear",
  "annual",
  "relevant",
  "huge",
  "rich",
  "commercial",
  "safe",
  "regional",
  "practical",
  "official",
  "separate",
  "key",
  "chief",
  "regular",
  "due",
  "additional",
  "active",
  "powerful",
  "complex",
  "standard",
  "impossible",
  "light",
  "warm",
  "middle",
  "fresh",
  "sexual",
  "front",
  "domestic",
  "actual",
  "united",
  "technical",
  "ordinary",
  "cheap",
  "strange",
  "internal",
  "excellent",
  "quiet",
  "soft",
  "potential",
  "northern",
  "religious",
  "quick",
  "very",
  "famous",
  "cultural",
  "proper",
  "broad",
  "joint",
  "formal",
  "limited",
  "conservative",
  "lovely",
  "usual",
  "ltd",
  "unable",
  "rural",
  "initial",
  "substantial",
  "christian",
  "bright",
  "average",
  "leading",
  "reasonable",
  "immediate",
  "suitable",
  "equal",
  "detailed",
  "working",
  "overall",
  "female",
  "afraid",
  "democratic",
  "growing",
  "sufficient",
  "scientific",
  "eastern",
  "correct",
  "inc",
  "irish",
  "expensive",
  "educational",
  "mental",
  "dangerous",
  "critical",
  "increased",
  "familiar",
  "unlikely",
  "double",
  "perfect",
  "slow",
  "tiny",
  "dry",
  "historical",
  "thin",
  "daily",
  "southern",
  "increasing",
  "wild",
  "alone",
  "urban",
  "empty",
  "married",
  "narrow",
  "liberal",
  "supposed",
  "upper",
  "apparent",
  "tall",
  "busy",
  "bloody",
  "prepared",
  "russian",
  "moral",
  "careful",
  "clean",
  "attractive",
  "japanese",
  "vital",
  "thick",
  "alternative",
  "fast",
  "ancient",
  "elderly",
  "rare",
  "external",
  "capable",
  "brief",
  "wonderful",
  "grand",
  "typical",
  "entire",
  "grey",
  "constant",
  "vast",
  "surprised",
  "ideal",
  "terrible",
  "academic",
  "funny",
  "minor",
  "pleased",
  "severe",
  "ill",
  "corporate",
  "negative",
  "permanent",
  "weak",
  "brown",
  "fundamental",
  "odd",
  "crucial",
  "inner",
  "used",
  "criminal",
  "contemporary",
  "sharp",
  "sick",
  "near",
  "roman",
  "massive",
  "unique",
  "secondary",
  "parliamentary",
  "african",
  "unknown",
  "subsequent",
  "angry",
  "alive",
  "guilty",
  "lucky",
  "enormous",
  "well",
  "communist",
  "yellow",
  "unusual",
  "net",
  "tough",
  "dear",
  "extensive",
  "glad",
  "remaining",
  "agricultural",
  "alright",
  "healthy",
  "italian",
  "principal",
  "tired",
  "efficient",
  "comfortable",
  "chinese",
  "relative",
  "friendly",
  "conventional",
  "willing",
  "sudden",
  "proposed",
  "voluntary",
  "slight",
  "valuable",
  "dramatic",
  "golden",
  "temporary",
  "federal",
  "keen",
  "flat",
  "silent",
  "indian",
  "worried",
  "pale",
  "statutory",
  "welsh",
  "dependent",
  "firm",
  "wet",
  "competitive",
  "armed",
  "radical",
  "outside",
  "acceptable",
  "sensitive",
  "living",
  "pure",
  "global",
  "emotional",
  "sad",
  "secret",
  "rapid",
  "adequate",
  "fixed",
  "sweet",
  "administrative",
  "wooden",
  "remarkable",
  "comprehensive",
  "surprising",
  "solid",
  "rough",
  "mere",
  "mass",
  "brilliant",
  "maximum",
  "absolute",
  "tory",
  "electronic",
  "visual",
  "electric",
  "cool",
  "spanish",
  "literary",
  "continuing",
  "supreme",
  "chemical",
  "genuine",
  "exciting",
  "written",
  "stupid",
  "advanced",
  "extreme",
  "classical",
  "fit",
  "favourite",
  "socialist",
  "widespread",
  "confident",
  "straight",
  "catholic",
  "proud",
  "numerous",
  "opposite",
  "distinct",
  "mad",
  "helpful",
  "given",
  "disabled",
  "consistent",
  "anxious",
  "nervous",
  "awful",
  "stable",
  "constitutional",
  "satisfied",
  "conscious",
  "developing",
  "strategic",
  "holy",
  "smooth",
  "dominant",
  "remote",
  "theoretical",
  "outstanding",
  "pink",
  "pretty",
  "clinical",
  "minimum",
  "honest",
  "impressive",
  "related",
  "residential",
  "extraordinary",
  "plain",
  "visible",
  "accurate",
  "distant",
  "still",
  "greek",
  "complicated",
  "musical",
  "precise",
  "gentle",
  "broken",
  "live",
  "silly",
  "fat",
  "tight",
  "monetary",
  "round",
  "psychological",
  "violent",
  "unemployed",
  "inevitable",
  "junior",
  "sensible",
  "grateful",
  "pleasant",
  "dirty",
  "structural",
  "welcome",
  "deaf",
  "above",
  "continuous",
  "blind",
  "overseas",
  "mean",
  "entitled",
  "delighted",
  "loose",
  "occasional",
  "evident",
  "desperate",
  "fellow",
  "universal",
  "square",
  "steady",
  "classic",
  "equivalent",
  "intellectual",
  "victorian",
  "level",
  "ultimate",
  "creative",
  "lost",
  "medieval",
  "clever",
  "linguistic",
  "convinced",
  "judicial",
  "raw",
  "sophisticated",
  "asleep",
  "vulnerable",
  "illegal",
  "outer",
  "revolutionary",
  "bitter",
  "changing",
  "australian",
  "native",
  "imperial",
  "strict",
  "wise",
  "informal",
  "flexible",
  "collective",
  "frequent",
  "experimental",
  "spiritual",
  "intense",
  "rational",
  "ethnic",
  "generous",
  "inadequate",
  "prominent",
  "logical",
  "bare",
  "historic",
  "modest",
  "dutch",
  "acute",
  "electrical",
  "valid",
  "weekly",
  "gross",
  "automatic",
  "loud",
  "reliable",
  "mutual",
  "liable",
  "multiple",
  "ruling",
  "curious",
  "arab",
  "sole",
  "jewish",
  "managing",
  "pregnant",
  "latin",
  "nearby",
  "exact",
  "underlying",
  "identical",
  "satisfactory",
  "marginal",
  "distinctive",
  "electoral",
  "urgent",
  "presidential",
  "controversial",
  "oral",
  "everyday",
  "encouraging",
  "organic",
  "continued",
  "expected",
  "statistical",
  "desirable",
  "innocent",
  "improved",
  "exclusive",
  "marked",
  "experienced",
  "unexpected",
  "superb",
  "sheer",
  "disappointed",
  "frightened",
  "gastric",
  "capitalist",
  "romantic",
  "naked",
  "reluctant",
  "magnificent",
  "convenient",
  "established",
  "closed",
  "uncertain",
  "artificial",
  "diplomatic",
  "tremendous",
  "marine",
  "mechanical",
  "retail",
  "institutional",
  "mixed",
  "required",
  "biological",
  "known",
  "functional",
  "straightforward",
  "superior",
  "digital",
  "spectacular",
  "unhappy",
  "confused",
  "unfair",
  "aggressive",
  "spare",
  "painful",
  "abstract",
  "asian",
  "associated",
  "legislative",
  "monthly",
  "intelligent",
  "hungry",
  "explicit",
  "nasty",
  "just",
  "faint",
  "coloured",
  "ridiculous",
  "amazing",
  "comparable",
  "successive",
  "realistic",
  "back",
  "decent",
  "unnecessary",
  "flying",
  "random",
  "influential",
  "dull",
  "genetic",
  "neat",
  "marvellous",
  "crazy",
  "damp",
  "giant",
  "secure",
  "bottom",
  "skilled",
  "subtle",
  "elegant",
  "brave",
  "lesser",
  "parallel",
  "steep",
  "intensive",
  "casual",
  "tropical",
  "lonely",
  "partial",
  "preliminary",
  "concrete",
  "alleged",
  "assistant",
  "vertical",
  "upset",
  "delicate",
  "mild",
  "occupational",
  "excessive",
  "progressive",
  "iraqi",
  "exceptional",
  "integrated",
  "striking",
  "continental",
  "okay",
  "harsh",
  "combined",
  "fierce",
  "handsome",
  "characteristic",
  "chronic",
  "compulsory",
  "interim",
  "objective",
  "splendid",
  "magic",
  "systematic",
  "obliged",
  "payable",
  "fun",
  "horrible",
  "primitive",
  "fascinating",
  "ideological",
  "metropolitan",
  "surrounding",
  "estimated",
  "peaceful",
  "premier",
  "operational",
  "technological",
  "kind",
  "advisory",
  "hostile",
  "precious",
  "gay",
  "accessible",
  "determined",
  "excited",
  "impressed",
  "provincial",
  "smart",
  "endless",
  "isolated",
  "drunk",
  "geographical",
  "like",
  "dynamic",
  "boring",
  "forthcoming",
  "unfortunate",
  "definite",
  "super",
  "notable",
  "indirect",
  "stiff",
  "wealthy",
  "awkward",
  "lively",
  "neutral",
  "artistic",
  "content",
  "mature",
  "colonial",
  "ambitious",
  "evil",
  "magnetic",
  "verbal",
  "legitimate",
  "sympathetic",
  "empirical",
  "head",
  "shallow",
  "vague",
  "naval",
  "depressed",
  "shared",
  "added",
  "shocked",
  "mid",
  "worthwhile",
  "qualified",
  "missing",
  "blank",
  "absent",
  "favourable",
  "polish",
  "israeli",
  "developed",
  "profound",
  "representative",
  "enthusiastic",
  "dreadful",
  "rigid",
  "reduced",
  "cruel",
  "coastal",
  "peculiar",
  "racial",
  "ugly",
  "swiss",
  "crude",
  "extended",
  "selected",
  "eager",
  "feminist",
  "canadian",
  "bold",
  "relaxed",
  "corresponding",
  "running",
  "planned",
  "applicable",
  "immense",
  "allied",
  "comparative",
  "uncomfortable",
  "conservation",
  "productive",
  "beneficial",
  "bored",
  "charming",
  "minimal",
  "mobile",
  "turkish",
  "orange",
  "rear",
  "passive",
  "suspicious",
  "overwhelming",
  "fatal",
  "resulting",
  "symbolic",
  "registered",
  "neighbouring",
  "calm",
  "irrelevant",
  "patient",
  "compact",
  "profitable",
  "rival",
  "loyal",
  "moderate",
  "distinguished",
  "interior",
  "noble",
  "insufficient",
  "eligible",
  "mysterious",
  "varying",
  "managerial",
  "molecular",
  "olympic",
  "linear",
  "prospective",
  "printed",
  "parental",
  "diverse",
  "elaborate",
  "furious",
  "fiscal",
  "burning",
  "useless",
  "semantic",
  "embarrassed",
  "inherent",
  "philosophical",
  "deliberate",
  "awake",
  "variable",
  "promising",
  "unpleasant",
  "varied",
  "sacred",
  "selective",
  "inclined",
  "tender",
  "hidden",
  "worthy",
  "intermediate",
  "sound",
  "protective",
  "fortunate",
  "slim",
  "islamic",
  "defensive",
  "divine",
  "stuck",
  "driving",
  "invisible",
  "misleading",
  "circular",
  "mathematical",
  "inappropriate",
  "liquid",
  "persistent",
  "solar",
  "doubtful",
  "manual",
  "architectural",
  "intact",
  "incredible",
  "devoted",
  "prior",
  "tragic",
  "respectable",
  "optimistic",
  "convincing",
  "unacceptable",
  "decisive",
  "competent",
  "spatial",
  "respective",
  "binding",
  "relieved",
  "nursing",
  "toxic",
  "select",
  "redundant",
  "integral",
  "then",
  "probable",
  "amateur",
  "fond",
  "passing",
  "specified",
  "territorial",
  "horizontal",
  "inland",
  "cognitive",
  "regulatory",
  "miserable",
  "resident",
  "polite",
  "scared",
  "marxist",
  "gothic",
  "civilian",
  "instant",
  "lengthy",
  "adverse",
  "korean",
  "unconscious",
  "anonymous",
  "aesthetic",
  "orthodox",
  "static",
  "unaware",
  "costly",
  "fantastic",
  "foolish",
  "fashionable",
  "causal",
  "compatible",
  "wee",
  "implicit",
  "dual",
  "ok",
  "cheerful",
  "subjective",
  "forward",
  "surviving",
  "exotic",
  "purple",
  "cautious",
  "visiting",
  "aggregate",
  "ethical",
  "protestant",
  "teenage",
  "dying",
  "disastrous",
  "delicious",
  "confidential",
  "underground",
  "thorough",
  "grim",
  "autonomous",
  "atomic",
  "frozen",
  "colourful",
  "injured",
  "uniform",
  "ashamed",
  "glorious",
  "wicked",
  "coherent",
  "rising",
  "shy",
  "novel",
  "balanced",
  "delightful",
  "arbitrary",
  "adjacent",
  "psychiatric",
  "worrying",
  "weird",
  "unchanged",
  "rolling",
  "evolutionary",
  "intimate",
  "sporting",
  "disciplinary",
  "formidable",
  "lexical",
  "noisy",
  "gradual",
  "accused",
  "homeless",
  "supporting",
  "coming",
  "renewed",
  "excess",
  "retired",
  "rubber",
  "chosen",
  "outdoor",
  "embarrassing",
  "preferred",
  "bizarre",
  "appalling",
  "agreed",
  "imaginative",
  "governing",
  "accepted",
  "vocational",
  "palestinian",
  "mighty",
  "puzzled",
  "worldwide",
  "handicapped",
  "organisational",
  "sunny",
  "eldest",
  "eventual",
  "spontaneous",
  "vivid",
  "rude",
  "faithful",
  "ministerial",
  "innovative",
  "controlled",
  "conceptual",
  "unwilling",
  "civic",
  "meaningful",
  "disturbing",
  "alive",
  "brainy",
  "breakable",
  "busy",
  "careful",
  "cautious",
  "clever",
  "concerned",
  "crazy",
  "curious",
  "dead",
  "different",
  "difficult",
  "doubtful",
  "easy",
  "famous",
  "fragile",
  "helpful",
  "helpless",
  "important",
  "impossible",
  "innocent",
  "inquisitive",
  "modern",
  "open",
  "outstanding",
  "poor",
  "powerful",
  "puzzled",
  "real",
  "rich",
  "shy",
  "sleepy",
  "stupid",
  "super",
  "tame",
  "uninterested",
  "wandering",
  "wild",
  "wrong",
  "adorable",
  "alert",
  "average",
  "beautiful",
  "blonde",
  "bloody",
  "blushing",
  "bright",
  "clean",
  "clear",
  "cloudy",
  "colorful",
  "crowded",
  "cute",
  "dark",
  "drab",
  "distinct",
  "dull",
  "elegant",
  "fancy",
  "filthy",
  "glamorous",
  "gleaming",
  "graceful",
  "grotesque",
  "homely",
  "light",
  "misty",
  "motionless",
  "muddy",
  "plain",
  "poised",
  "quaint",
  "shiny",
  "smoggy",
  "sparkling",
  "spotless",
  "stormy",
  "strange",
  "ugly",
  "unsightly",
  "unusual",
  "bad",
  "better",
  "beautiful",
  "big",
  "black",
  "blue",
  "bright",
  "clumsy",
  "crazy",
  "dizzy",
  "dull",
  "fat",
  "frail",
  "friendly",
  "funny",
  "great",
  "green",
  "gigantic",
  "gorgeous",
  "grumpy",
  "handsome",
  "happy",
  "horrible",
  "itchy",
  "jittery",
  "jolly",
  "kind",
  "long",
  "lazy",
  "magnificent",
  "magenta",
  "many",
  "mighty",
  "mushy",
  "nasty",
  "new",
  "nice",
  "nosy",
  "nutty",
  "nutritious",
  "odd",
  "orange",
  "ordinary",
  "pretty",
  "precious",
  "prickly",
  "purple",
  "quaint",
  "quiet",
  "quick",
  "quickest",
  "rainy",
  "rare",
  "ratty",
  "red",
  "roasted",
  "robust",
  "round",
  "sad",
  "scary",
  "scrawny",
  "short",
  "silly",
  "stingy",
  "strange",
  "striped",
  "spotty",
  "tart",
  "tall",
  "tame",
  "tan",
  "tender",
  "testy",
  "tricky",
  "tough",
  "ugly",
  "ugliest",
  "vast",
  "watery",
  "wasteful",
  "wonderful",
  "yellow",
  "yummy",
  "zany",
];
