/**
 * Created by ksinger on 01.09.2015.
 */
import GeoPoint from "geopoint";
import Immutable from "immutable";

export function hyphen2Camel(hyphened) {
  return hyphened
    .replace(/^([a-z])/, args => args[0].toUpperCase()) //capitalize first letter
    .replace(/-([a-z])/g, args => args[1].toUpperCase()); //hyphens to camel-case
}

export function camel2Hyphen(cammelled) {
  return cammelled
    .replace(/^([A-Z])/, args => args[0].toLowerCase()) //de-capitalize first letter
    .replace(/(.)([A-Z])/g, args => args[0] + "-" + args[1].toLowerCase()); // camel-case to hyphens
}

export function firstToLowerCase(str) {
  return str.replace(/^([A-Z])/, args => args[0].toLowerCase()); //de-capitalize first letter
}

window.hyphen2Camel = hyphen2Camel;
window.camel2Hyphen = camel2Hyphen;
window.firstToLowerCase = firstToLowerCase;

/**
 * Attaches the contents of `attachments` to `target` using the constiable names from `names`
 * @param target the object
 * @param names array of constiable names
 * @param attachments array of objects/values
 */
export function attach(target, names, attachments) {
  const pairs = zipWith(
    (name, attachment) => [name, attachment],
    names,
    attachments
  );
  for (const [name, attachment] of pairs) {
    target[name] = attachment;
  }
}

export function dispatchEvent(elem, eventName, eventData) {
  let event = undefined;
  if (eventData) {
    event = new CustomEvent(eventName, { detail: eventData });
  } else {
    event = new Event(eventName);
  }
  elem.dispatchEvent(event);
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

/*
 * @param obj an object-tree.
 *
 * @param prefix add a custom prefix to all generated constants.
 *
 * @returns a tree using the same structure as `o` but with
 *          all leaves being strings equal to their lookup path.
 * e.g.:
 * tree2constants({foo: null}) -> {foo: 'foo'}
 * tree2constants{{foo: {bar: null}}) -> {foo: {bar: 'foo.bar'}}
 * tree2constants{foo: null}, 'pfx') -> {foo: 'pfx.foo'}
 */
export function tree2constants(obj, prefix = "") {
  //wrap prefix in array
  prefix = prefix === "" ? [] : [prefix];

  return deepFreeze(
    reduceAndMapTreeKeys(
      (acc, k) => acc.concat(k),
      acc => acc.join("."),
      prefix,
      obj
    )
  );
}

/**
 * Traverses down an object, reducing the keys with the reducer
 * and then applying the mapper once it reaches the leaves.
 * The function doesn't modify the input-object.
 * @param obj
 * @param acc the initial accumulator
 * @param reducer (acc, key) => newAcc
 * @param mapper (acc) => newAcc
 * @returns {*}
 */

export function reduceAndMapTreeKeys(reducer, mapper, acc, obj) {
  if (typeof obj === "object" && obj !== null) {
    const accObj = {};
    for (let k of Object.keys(obj)) {
      accObj[k] = reduceAndMapTreeKeys(
        reducer,
        mapper,
        reducer(acc, k),
        obj[k]
      );
    }
    return accObj;
  } else {
    return mapper(acc);
  }
}

/**
 * Traverses an object-tree and produces an object
 * that is just one level deep but concatenating the
 * traversal path.
 *
 * ```
 * flattenTree({
 *   myInt: 1,
 *   myObj: {
 *      myProp: 2,
 *      myStr: 'asdf',
 *      foo: {
 *        bar: 3
 *      }
 *   }
 * });
 * // result:
 * // {
 * //   'myInt': 1,
 * //   'myObj__myProp' : 2,
 * //   'myObj__myStr' : 'asdf',
 * //   'myObj__foo__bar' : 3
 * // }
 * ```
 *
 * @param tree {object} the object-tree
 * @param delimiter {string} will be used to join the path. by default `__`
 * @returns {object} the flattened object
 */
export function flattenTree(tree, delimiter = "__") {
  const accObj = {}; //the accumulator accObject
  function _flattenTree(node, pathAcc = []) {
    for (let k of Object.keys(node)) {
      const pathAccUpd = pathAcc.concat(k);
      if (typeof node[k] === "object" && node[k] !== null) {
        _flattenTree(node[k], pathAccUpd);
      } else {
        const propertyName = pathAccUpd.join(delimiter);
        accObj[propertyName] = node[k];
      }
    }
  }
  _flattenTree(tree);
  return accObj;
}

export function delay(milliseconds) {
  return new Promise(resolve =>
    window.setTimeout(() => resolve(), milliseconds)
  );
}

/**
 * `subscribe`s and watches the output of `select` for changes,
 * calling `callback` if those happen.
 * @param subscribe {function} used to subscribe
 * @param select {function} a clojure that's called to get the
 *                          value to be watched
 * @param callback {function}
 * @return {function} the unsubscribe function generated by `subscribe`
 */
export function watch(subscribe, select, callback) {
  let unsubscribe = null;

  /*
     * creating this function (and instantly executing it)
     * allows attaching individual previousValue to it
     */
  (function() {
    let previousValue = select();
    unsubscribe = subscribe(() => {
      const currentValue = select();
      if (currentValue !== previousValue) callback(currentValue, previousValue);
      previousValue = currentValue;
    });
  })();

  return unsubscribe;
}

/**
 * An oppinioned constiant of the generic watch that
 * for usage with redux-stores containing immutablejs-objects
 * @param redux {object} should provide `.subscribe` and `.getState`
 *                       (with the latter yielding an immutablejs-object)
 * @param path {array} an array of strings for usage with store.getIn
 * @param callback
 */
export function watchImmutableRdxState(redux, path, callback) {
  return watch(redux.subscribe, () => getIn(redux.getState(), path), callback);
}

/**
 * generates a string of random characters
 *
 * @param {*} length the length of the string to be generated. e.g. in the example below: 5
 * @param {*} chars the allowed characters, e.g. "abc123" to generate strings like "a3cba"
 */
export function getRandomString(
  length,
  chars = "abcdefghijklmnopqrstuvwxyz0123456789"
) {
  const randomChar = () => chars[Math.floor(Math.random() * chars.length)];
  return Array.from(
    {
      length: length,
    },
    randomChar
  ).join("");
}

export function isString(o) {
  return (
    typeof o == "string" || (typeof o == "object" && o.constructor === String)
  );
}

/**
 * Generate string of [a-z0-9] with specified length
 * @param length
 * @returns {*}
 */
export function generateIdString(length) {
  const characters = "abcdefghijklmnopqrstuvwxyz0123456789";
  return arrayOfRandoms(length)
    .map(randomFloat => Math.floor(randomFloat * characters.length))
    .map(randomPosition => characters.charAt(randomPosition))
    .join("");
}

/**
 * Generate array of random numbers.
 * @param length
 * @returns {*}
 */
export function arrayOfRandoms(length) {
  return Array.apply(null, Array(length)).map(() => Math.random());
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
 * Throws an error if this isn't a good http-response
 * @param response
 * @returns {*}
 */
export function checkHttpStatus(response) {
  if (response.status >= 200 && response.status < 300) {
    return response;
  } else {
    const error = new Error(response.statusText);
    error.response = response;
    error.status = response.status;
    throw error;
  }
}

/**
 * taken from: https://esdiscuss.org/topic/es6-iteration-over-object-values
 *
 * example usage:
 *
 * ```javascript
 * for (let [key, value] of entries(o)) {
 *   console.log(key, ' --> ', value)
 * }
 * ```
 * @param obj the object to generate a (key,value)-pair iterator for
 */
export function* entries(obj) {
  for (let key of Object.keys(obj)) {
    yield [key, obj[key]];
  }
}

/**
 * Maps over the (value,key)-pairs of the object and produces
 * a new object with the same keys but the function's result
 * as values.
 * @param obj
 * @param f  a function `(value, key) => result` or `value => result`
 */
export function mapObj(obj, f) {
  const accumulator = {};
  for (let [key, value] of entries(obj)) {
    accumulator[key] = f(value, key);
  }
  return accumulator;
}

/**
 * @param listOfLists e.g. [ [1,2], [3], [], [3,4,5] ]
 * @return {*} e.g. [1,2,3,3,4,5]
 */
export function flatten(listOfLists) {
  return listOfLists.reduce(
    (flattendList, innerList) =>
      innerList ? flattendList.concat(innerList) : [], //not concatenating `undefined`s
    [] //concat onto empty list as start
  );
}

/**
 * @param objOfObj e.g. { a: { x: 1, y: 2}, b: {z: 3}, c: {} }
 * @return {*} e.g. {x: 1, y: 2, z: 3}
 */
export function flattenObj(objOfObj) {
  let flattened = {};
  for (const [, innerObjects] of entries(objOfObj)) {
    flattened = Object.assign(flattened, innerObjects);
  }
  return flattened;
}

/**
 * Takes a single uri or an array of uris, performs the lookup function on each
 * of them seperately, collects the results and builds an map/object
 * with the uris as keys and the results as values.
 * If any call to the asyncLookupFunction fails, the corresponding
 * key-value-pair will not be contained in the result.
 * @param uris
 * @param asyncLookupFunction
 * @param excludeUris uris to exclude from lookup
 * @param abortOnError -> abort the whole crawl by breaking the promisechain instead of ignoring the failures
 * @return {*}
 */
export function urisToLookupMap(
  uris,
  asyncLookupFunction,
  excludeUris = [],
  abortOnError = false
) {
  //make sure we have an array and not a single uri.
  const urisAsArray = is("Array", uris) ? uris : [uris];
  const excludeUrisAsArray = is("Array", excludeUris)
    ? excludeUris
    : [excludeUris];

  const urisAsArrayWithoutExcludes = urisAsArray.filter(uri => {
    const exclude = excludeUrisAsArray.indexOf(uri) < 0;
    if (exclude) {
      return true;
    } else {
      return false;
    }
  });

  const asyncLookups = urisAsArrayWithoutExcludes.map(uri =>
    asyncLookupFunction(uri).catch(error => {
      if (abortOnError) {
        throw new Error(error);
      } else {
        console.error(
          `failed lookup for ${uri} in utils.js:urisToLookupMap ` +
            error.message,
          "\n\n",
          error.stack,
          "\n\n",
          urisAsArrayWithoutExcludes,
          "\n\n",
          uris,
          "\n\n",
          error
        );
        return undefined;
      }
    })
  );
  return Promise.all(asyncLookups).then(dataObjects => {
    const lookupMap = {};
    //make sure there's the same
    uris.forEach((uri, i) => {
      if (dataObjects[i]) {
        lookupMap[uri] = dataObjects[i];
      }
    });
    return lookupMap;
  });
}

/**
 * Takes a single uri or an array of uris, performs the lookup function on each
 * of them seperately, collects the results and builds an map/object
 * with the uris as keys and the results as values.
 * If any call to the asyncLookupFunction fails, the corresponding
 * key-value-pair will not be contained in the success-result but rather in the failed-results.
 * @param uris
 * @param asyncLookupFunction
 * @param excludeUris uris to exclude from lookup
 * @return {*}
 */
export function urisToLookupSuccessAndFailedMap(
  uris,
  asyncLookupFunction,
  excludeUris = []
) {
  //make sure we have an array and not a single uri.
  const urisAsArray = is("Array", uris) ? uris : [uris];
  const excludeUrisAsArray = is("Array", excludeUris)
    ? excludeUris
    : [excludeUris];

  const urisAsArrayWithoutExcludes = urisAsArray.filter(uri => {
    const exclude = excludeUrisAsArray.indexOf(uri) < 0;
    if (exclude) {
      return true;
    } else {
      return false;
    }
  });

  const asyncLookups = urisAsArrayWithoutExcludes.map(uri =>
    asyncLookupFunction(uri).catch(error => {
      return error;
    })
  );
  return Promise.all(asyncLookups).then(dataObjects => {
    const lookupMap = { success: {}, failed: {} };
    //make sure there's the same
    uris.forEach((uri, i) => {
      if (dataObjects[i] instanceof Error) {
        lookupMap["failed"][uri] = dataObjects[i];
      } else if (dataObjects[i]) {
        lookupMap["success"][uri] = dataObjects[i];
      }
    });
    return lookupMap;
  });
}

/**
 * Maps an asynchronous function over the values of an object or
 * the elements of an array. It returns a promise with the result,
 * when all applications of the asyncFunction have finished.
 * @param object
 * @param asyncFunction
 * @return {*}
 */
export function mapJoin(object, asyncFunction) {
  if (is("Array", object)) {
    const promises = object.map(el => asyncFunction(el));
    return Promise.all(promises);
  } else if (is("Object", object)) {
    const keys = Object.keys(object);
    const promises = keys.map(k => asyncFunction(object[k]));
    return Promise.all(promises).then(results => {
      const acc = {};
      results.forEach((result, i) => {
        acc[keys[i]] = result;
      });
      return acc;
    });
  } else {
    return undefined;
  }
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

export function decodeUriComponentProperly(encodedUri) {
  if (!encodedUri) return undefined;
  //for some reason decodeUri(undefined) yields "undefined"
  else return decodeURIComponent(encodedUri);
}

export function msStringToDate(ts) {
  if (is("String", ts)) {
    ts = Number.parseInt(ts);
  }
  return new Date(ts);
}

/**
 * Searches the nominatim address-lookup service and
 * returns a list with the search results.
 */
export function searchNominatim(searchStr) {
  const url =
    "https://nominatim.openstreetmap.org/search" +
    "?q=" +
    encodeURIComponent(searchStr) +
    "&format=json";
  return fetchJSON(url);
}

export function reverseSearchNominatim(lat, lon, zoom) {
  let url =
    "https://nominatim.openstreetmap.org/reverse" +
    "?lat=" +
    lat +
    "&lon=" +
    lon +
    "&format=json";

  if (isValidNumber(zoom)) {
    url += "&zoom=" + Math.max(0, Math.min(zoom, 18));
  }

  let json = fetchJSON(url).catch(function() {
    const distance = 0.2;
    const gp = new GeoPoint(lat, lon);
    const bBox = gp.boundingCoordinates(distance, true);
    return {
      display_name: "-",
      lat: lat,
      lon: lon,
      boundingbox: [
        bBox[0]._degLat,
        bBox[1]._degLat,
        bBox[0]._degLon,
        bBox[1]._degLon,
      ],
    };
  });
  return json;
}

/**
 * drop info not stored in rdf, thus info that we
 * couldn't restore for previously used locations
 */
export function nominatim2draftLocation(searchResult) {
  const b = searchResult.boundingbox;
  return {
    name: searchResult.display_name,
    lng: Number.parseFloat(searchResult.lon),
    lat: Number.parseFloat(searchResult.lat),
    //importance: searchResult.importance,
    nwCorner: {
      lat: Number.parseFloat(b[0]),
      lng: Number.parseFloat(b[2]),
    },
    seCorner: {
      lat: Number.parseFloat(b[1]),
      lng: Number.parseFloat(b[3]),
    },
    //bounds: [
    //    [ Number.parseFloat(b[0]), Number.parseFloat(b[2]) ], //north-western point
    //    [ Number.parseFloat(b[1]), Number.parseFloat(b[3]) ] //south-eastern point
    //],
  };
}

function fetchJSON(url) {
  return fetch(url, {
    method: "get",
    //credentials: "same-origin",
    headers: { Accept: "application/json" },
  }).then(resp => {
    /*
             * handle errors and read json-data
             */
    const errorMsg =
      "GET to " +
      url +
      " failed with (" +
      resp.status +
      "): " +
      resp.statusText +
      "\n" +
      resp;
    if (resp.status !== 200) {
      throw new Error(errorMsg);
    } else {
      try {
        return resp.json();
      } catch (jsonParseError) {
        // nominatim responded with an HTTP-200 with an error html-page m(
        const e = new Error(errorMsg);
        e.originalErr = jsonParseError;
        throw e;
      }
    }
  });
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
window.getIn4dbg = getIn;

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
export function getInFromJsonLd(obj, path, context) {
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
window.getInFromJsonLd4dbg = getInFromJsonLd;

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
export function getFromJsonLd(obj, predicate, context) {
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

export function contains(arr, el) {
  return arr.indexOf(el) > 0;
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

export function all(boolArr) {
  return boolArr.reduce((b1, b2) => b1 && b2, true);
}

/**
 * compares two arrays and checks if their contents are equal
 */
export function arrEq(xs, ys) {
  return (
    xs.length === ys.length &&
    all(
      //elementwise comparison
      zipWith((x, y) => x === y, xs, ys)
    )
  );
}

/**
 *
 * Adapted from https://stackoverflow.com/questions/901115/how-can-i-get-query-string-values-in-javascript
 *
 * Usage:
 * ```
 * // query string: ?foo=lorem&bar=&baz
 * const foo = getParameterByName('foo'); // "lorem"
 * const bar = getParameterByName('bar'); // "" (present with empty value)
 * const baz = getParameterByName('baz'); // "" (present with no value)
 * const qux = getParameterByName('qux'); // null (absent)
 * ```
 * @param name
 * @param url
 * @returns {*}
 */
export function getParameterByName(name, url) {
  if (!url) url = window.location.href;
  name = name.replace(/[[\]]/g, "\\$&");
  const regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)");
  const results = regex.exec(url);
  if (!results) return null;
  if (!results[2]) return "";
  return decodeURIComponent(results[2].replace(/\+/g, " "));
}

/**
 * Retrieves parameters from the url-bar or parses them from a passed url.
 * @param url
 * @returns {{}}
 */
export function getParameters(url) {
  const url_ = url ? url : window.location.href; // e.g. url_ = "http://example.org/?privateId=5kpskm09-ocri63&foo=bar&asdf"
  const [, paramsString] = url_.split("?"); // e.g. paramsString = "privateId=5kpskm09-ocri63&foo=bar&asdf"

  if (!paramsString) {
    // no parameters present
    return {};
  }

  const paramsKconstray = paramsString
    .split("&") // e.g. ["privateId=5kpskm09-ocri63", "foo=bar", "asdf"]
    .map(p => p.split("=")) // e.g. [["privateId", "5kpskm09-ocri63"], ["foo", "bar"], ["asdf"]]
    .filter(p => p.length === 2); // filter out parameter that's not a proper key-value pair, e.g. "asdf"

  // create object from kv-pairs
  const params = {};
  paramsKconstray.forEach(kv => (params[kv[0]] = kv[1]));

  return params;
}

// from https://github.com/gagan-bansal/parse-svg/blob/master/index.js
export function parseSVG(xmlString) {
  const div = document.createElementNS("http://www.w3.org/1999/xhtml", "div");
  div.innerHTML =
    '<svg xmlns="http://www.w3.org/2000/svg">' + xmlString + "</svg>";
  const frag = document.createDocumentFragment();
  while (div.firstChild.firstChild) frag.appendChild(div.firstChild.firstChild);
  return frag;
}

/**
 * Fetch and inline an icon-spritemap so it can be colored using css-constiables.
 */
export function inlineSVGSpritesheet(path, id) {
  return fetch(path)
    .then(res => res.text())
    .then(xmlString => parseSVG(xmlString))
    .then(svgDocumentFragment => {
      if (!svgDocumentFragment)
        throw new Error("Couldn't parse icon-spritesheet.");
      document.body.appendChild(svgDocumentFragment);
      const svgNode = document.body.lastChild; // the node resulting from the fragment we just appended
      if (svgNode && svgNode.style) {
        svgNode.style.display = "none";
      }
      if (id) {
        svgNode.id = id;
      }
      //svgNode.style.display = "none"; // don't want it to show up in full, only via the fragment-references to it.
      //window.svgNode4dbg = svgNode;
      //window.foo4dbg = document.body.appendChild(svgNode);
    });
}

/**
 * Optionally prepends a string, and then throws
 * whatever it gets as proper javascript error.
 * Note, that throwing an error will also
 * reject in a `Promise`-constructor-callback.
 * @param {*} e
 * @param {*} prependedMsg
 */
export function rethrow(e, prependedMsg = "") {
  prependedMsg = prependedMsg ? prependedMsg + "\n" : "";

  if (is("String", e)) {
    throw new Error(prependedMsg + e);
  } else if (e.stack && e.message) {
    // a class defined
    const g = new Error(prependedMsg + e.message);
    g.stack = e.stack;
    g.response = e.response; //we add the response so we can look up why a request threw an error

    throw g;
  } else {
    throw new Error(prependedMsg + JSON.stringify(e));
  }
}

/**
 * Parses an rdf-uri and gets the base-uri, i.e.
 * the part before and including the fragment identifier
 * ("#") or last slash ("/").
 * @param {*} uri
 */
export function prefixOfUri(uri) {
  // if there's hash-tags, the first of these
  // is the fragment identifier and everything
  // after is the id. remove everything following it.
  let prefix = uri.replace(/#.*/, "#");

  // if there's no fragment-identifier, the
  // everything after the last slash is removed.
  if (!prefix.endsWith("#")) {
    prefix = prefix.replace(/\/([^/]*)$/, "/");
  }

  return prefix;
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

export function clamp(value, lower, upper) {
  if (lower > value) return lower;
  if (upper < value) return upper;
  return value;
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
 * Splits a string of TRIG into prefixes and body (e.g. to allow hiding either)
 * Also adds some empty lines to the body, so it stays readable when
 * the lines wrap.
 * @param {*} trigString
 */
export function trigPrefixesAndBody(trigString) {
  const lines = (trigString || "").split("\n");

  //seperating off header/@prefix-statements, so they can be folded in
  const prefixes = lines.filter(line => line.startsWith("@prefix")).join("\n");

  const body = lines
    .filter(line => !line.startsWith("@prefix"))
    .map(line =>
      line
        // add some extra white-space between statements, so they stay readable even when they wrap.
        .replace(/\.$/, ".\n")
        .replace(/;$/, ";\n")
        .replace(/\{$/, "{\n")
        .replace(/^\}$/, "\n}")
    )
    .join("\n")
    .trim();

  return { trigPrefixes: prefixes, trigBody: body };
}

export function callBuffer(fn, delay = 1000) {
  let timeout = undefined;
  const buffer = (...args) => {
    if (timeout) {
      clearTimeout(timeout);
    }
    timeout = setTimeout(() => fn(...args), delay);
  };
  return buffer;
}

export function scrubSearchResults(searchResults) {
  return (
    Immutable.fromJS(searchResults.map(nominatim2draftLocation))
      /*
       * filter "duplicate" results (e.g. "Wien"
       *  -> 1x waterway, 1x boundary, 1x place)
       */
      .groupBy(r => r.get("name"))
      .map(sameNamedResults => sameNamedResults.first())
      .toList()
      .toJS()
  );
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
 * Takes an ISO-8601 string and returns a `Date` that marks
 * the exact time or the end of the year, month or day
 * e.g. xsd:dateTime: "2011-04-11T10:20:30Z"
 *
 * @param {*} dateStr
 */
export function endOfDateStrInterval(dateStr) {
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
 * Method that checks if the given element is already an array, if so return it, if not
 * return the element as a single element array, if element is undefined return undefined
 * @param elements
 * @returns {*}
 */
export function createArray(elements) {
  return !elements || Array.isArray(elements) ? elements : [elements];
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

window.toLocalISODateString4dbg = toLocalISODateString;

/**
 * Calculates distance between two locations in meters
 * If any of the locations or lat, lng of the location are undefined/null, return undefined
 * @param locationA json {lat, lng]
 * @param locationB json {lat, lng]
 * @returns {number} distance between these two coordinates in meters
 */
export function calculateDistance(locationA, locationB) {
  const locationAImm = locationA && Immutable.fromJS(locationA);
  const locationBImm = locationB && Immutable.fromJS(locationB);

  if (
    !locationAImm ||
    !locationAImm.get("lat") ||
    !locationAImm.get("lng") ||
    !locationBImm ||
    !locationBImm.get("lat") ||
    !locationBImm.get("lng")
  ) {
    return;
  }

  const earthRadius = 6371000; // earth radius in meters
  const dLat =
    ((locationBImm.get("lat") - locationAImm.get("lat")) * Math.PI) / 180;
  const dLon =
    ((locationBImm.get("lng") - locationAImm.get("lng")) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((locationAImm.get("lat") * Math.PI) / 180) *
      Math.cos((locationBImm.get("lat") * Math.PI) / 180) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  const d = earthRadius * c;

  return Math.round(d);
}
