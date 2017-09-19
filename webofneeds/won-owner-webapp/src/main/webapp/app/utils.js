/**
 * Created by ksinger on 01.09.2015.
 */

export function hyphen2Camel(hyphened) {
    return hyphened
        .replace(/^([a-z])/, args => args[0].toUpperCase()) //capitalize first letter
        .replace(/-([a-z])/g, args => args[1].toUpperCase()) //hyphens to camel-case
}

export function camel2Hyphen(cammelled) {
    return cammelled
        .replace(/^([A-Z])/, args => args[0].toLowerCase()) //de-capitalize first letter
        .replace(/(.)([A-Z])/g, args => args[0] + '-' + args[1].toLowerCase()) // camel-case to hyphens
}

export function firstToLowerCase(str) {
    return str.replace(/^([A-Z])/, args => args[0].toLowerCase()) //de-capitalize first letter
}

window.hyphen2Camel = hyphen2Camel;
window.camel2Hyphen = camel2Hyphen;
window.firstToLowerCase = firstToLowerCase;


/**
 * Attaches the contents of `attachments` to `target` using the variable names from `names`
 * @param target the object
 * @param names array of variable names
 * @param attachments array of objects/values
 */
export function attach(target, names, attachments) {
    for(let i = 0; i < names.length && i < attachments.length; i++) {
        target[names[i]] = attachments[i];
    }
}

export function dispatchEvent(elem, eventName, eventData) {
    let event = undefined;
    if (eventData) {
        event = new CustomEvent(eventName, {'detail': eventData});
    } else {
        event = new Event(eventName);
    }
    elem.dispatchEvent(event);
    //console.log('dispatching');
}

/*
 * Freezes an object recursively.
 *
 * Taken from:
 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/freeze
 */
export function deepFreeze(obj) {

    // Retrieve the property names defined on obj
    var propNames = Object.getOwnPropertyNames(obj);

    // Freeze properties before freezing self
    propNames.forEach(function(name) {
        var prop = obj[name];

        // Freeze prop if it is an object
        if (typeof prop == 'object' && !Object.isFrozen(prop))
            deepFreeze(prop);
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
export function tree2constants(obj, prefix = '') {
    //wrap prefix in array
    prefix = prefix === ''? [] : [prefix];

    return deepFreeze(reduceAndMapTreeKeys(
        (acc, k) => acc.concat(k),
        (acc) => acc.join('.'),
        prefix,
        obj
    ));
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
    if(typeof obj === 'object' && obj !== null) {

        const accObj = {};
        for(let k of Object.keys(obj)) {
            accObj[k] = reduceAndMapTreeKeys(reducer, mapper,  reducer(acc, k), obj[k]);
        }
        return accObj;

    } else {
        return mapper(acc);
    }
}

/**
 * Generates an array consisting of n times x. e.g.:
 * ```javascript
 * repeatVar('a', 3); // ['a', 'a', 'a']
 * ```
 * @param x
 * @param n
 * @returns {*}
 */
export function repeatVar(x, n) {
   return Array.apply(null, Array(n)).map(() => x);
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
export function flattenTree(tree, delimiter = '__') {
    const accObj = {}; //the accumulator accObject
    function _flattenTree(node, pathAcc = []) {
        for(let k of Object.keys(node)) {
            const pathAccUpd = pathAcc.concat(k);
            if(typeof node[k] === 'object' && node[k] !== null) {
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
    return new Promise((resolve, reject) =>
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
    (function (){
        let previousValue = select();
        unsubscribe = subscribe(() => {
            const currentValue = select();
            if(currentValue !== previousValue)
                callback(currentValue, previousValue);
            previousValue = currentValue;
        });
    })();

    return unsubscribe;
}

/**
 * An oppinioned variant of the generic watch that
 * for usage with redux-stores containing immutablejs-objects
 * @param redux {object} should provide `.subscribe` and `.getState`
 *                       (with the latter yielding an immutablejs-object)
 * @param path {array} an array of strings for usage with store.getIn
 * @param callback
 */
export function watchImmutableRdxState(redux, path, callback) {
    return watch(
        redux.subscribe,
        () => getIn(redux.getState(), path),
        callback
    );
}
export function mapToMatches(connections){
    let needMap = {}
    if(connections){

        Object.keys(connections).forEach(function(key){
            const needUri = connections[key].ownNeed['@id'];

            if(!needMap[needUri]){
                let connectionsArr = [connections[key]]
                needMap[needUri]=connectionsArr
            }else{
                needMap[needUri].push(connections[key])
            }
        }.bind(this))
    }
    return needMap;

}
export function removeAllProperties(obj){
    Object.keys(obj).forEach(function(element,index,array){
        delete obj[element];
    })
}
export function getKeySize(obj) {
    return Object.keys(obj).length;
}
export function getRandomPosInt() {
    return getRandomInt(1,9223372036854775807);
}
export function getRandomInt(min, max){
    return Math.floor(Math.random()*(max-min+1))+min;
}

export function isString(o) {
    return typeof o == "string" || (typeof o == "object" && o.constructor === String);
}

/**
 * Generate string of [a-z0-9] with specified length
 * @param length
 * @returns {*}
 */
export function generateIdString(length) {
    const characters = 'abcdefghijklmnopqrstuvwxyz0123456789';
    return arrayOfRandoms(length)
        .map(randomFloat => Math.floor(randomFloat * characters.length))
        .map(randomPosition => characters.charAt(randomPosition))
        .join('');
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
        var reader = new FileReader();

        reader.onload = () => resolve(reader.result);
        reader.onerror = () => reject(f);

        reader.readAsDataURL(file);
    });
}

export function concatTags(tags) {
    if(tags.length>0){
        var concTags ='';
        for(var i = 0; i < tags.length; i++){
            if(i==0){
                concTags = tags[i].text;
            }else{
                concTags = concTags + ','+ tags[i].text;
            }
        }
        return concTags;
    }
}

// This scrolling function
// is from http://www.itnewb.com/tutorial/Creating-the-Smooth-Scroll-Effect-with-JavaScript
export function scrollTo(eID) {
    console.log("SCROLL TO METHOD");
    var startY = currentYPosition();
    var stopY = elmYPosition(eID);
    var distance = stopY > startY ? stopY - startY : startY - stopY;
    if (distance < 100) {
        scrollTo(0, stopY);
        return;
    }
    var speed = Math.round(distance / 100);
    if (speed >= 20) speed = 20;
    var step = Math.round(distance / 25);
    var leapY = stopY > startY ? startY + step : startY - step;
    var timer = 0;
    if (stopY > startY) {
        for (var i = startY; i < stopY; i += step) {
            setTimeout("window.scrollTo(0, " + leapY + ")", timer * speed);
            leapY += step;
            if (leapY > stopY) leapY = stopY;
            timer++;
        }
        return;
    }
    for (var i = startY; i > stopY; i -= step) {
        setTimeout("window.scrollTo(0, " + leapY + ")", timer * speed);
        leapY -= step;
        if (leapY < stopY) leapY = stopY;
        timer++;
    }

    function currentYPosition() {
        // Firefox, Chrome, Opera, Safari
        if (self.pageYOffset) return self.pageYOffset;
        // Internet Explorer 6 - standards mode
        if (document.documentElement && document.documentElement.scrollTop)
            return document.documentElement.scrollTop;
        // Internet Explorer 6, 7 and 8
        if (document.body.scrollTop) return document.body.scrollTop;
        return 0;
    }

    function elmYPosition(eID) {
        var elm = document.getElementById(eID);
        var y = elm.offsetTop;
        var node = elm;
        while (node.offsetParent && node.offsetParent != document.body) {
            node = node.offsetParent;
            y += node.offsetTop;
        } return y;
    }
}

/**
 * Throws an error if this isn't a good http-response
 * @param response
 * @returns {*}
 */
export function checkHttpStatus(response) {
    if (response.status >= 200 && response.status < 300) {
        return response
    } else {
        var error = new Error(response.statusText)
        error.response = response
        throw error
    }
}

/**
 *
 * e.g.
 * ```
 * withDefaults({a: 1}, {a: 4, b: 8}) // {a: 1, b: 8}
 * withDefaults({a: 1, b: 2, c: 3}, {a: 4, b: 8}) // {a: 1, b: 2}
 * withDefaults(undefined, {a: 4, b: 8}) // {a: 4, b: 8}
 * ```
 * @param defaults
 * @param obj
 * @returns {object} an object with the fields from defaults
 *                   overwritten by the ones in obj where they exist.
 *                   always returns an object as long as `defaults` is one
 */
export function withDefaults(obj, defaults) {
    const ret = {};
    for(var k in defaults) {
        ret[k] = obj && obj[k]? obj[k] : defaults[k];
    }
    return ret;
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
    for(let [key, value] of entries(obj)) {
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
            innerList? flattendList.concat(innerList) : [], //not concatenating `undefined`s
        [] //concat onto empty list as start
    )
}

/**
 * @param objOfObj e.g. { a: { x: 1, y: 2}, b: {z: 3}, c: {} }
 * @return {*} e.g. {x: 1, y: 2, z: 3}
 */
export function flattenObj(objOfObj) {
    let flattened = {};
    for(const [outerKeys, innerObjects] of entries(objOfObj)) {
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
 * @return {*}
 */
export function urisToLookupMap(uris, asyncLookupFunction) {
    //make sure we have an array and not a single uri.
    const urisAsArray = is('Array', uris) ? uris : [uris];
    const asyncLookups = urisAsArray.map(uri =>
        asyncLookupFunction(uri)
        .catch(error => {
            console.error({msg: `failed lookup for ${uri} in utils.js:urisToLookupMap`, error, urisAsArray, uris})
            return undefined;
        })
    );
    return Promise.all(asyncLookups).then( dataObjects => {
        const lookupMap = {};
        //make sure there's the same
        for (let i = 0; i < uris.length; i++) {
            if(dataObjects[i]) {
                lookupMap[uris[i]] = dataObjects[i];
            }
        }
        return lookupMap;
    });
}

/**
 * Takes a single uri or an array of uris, performs the lookup function on each
 * of them seperately, collects the results and builds an map/object
 * with the uris as keys and the results as values.
 * Will throw an error if any of the asyncLookupFunction fails. If it
 * doesn't fail, the result is guaranteed to have the same number of
 * uri-value-pairs as uris where passed.
 * @param uris
 * @param asyncLookupFunction
 * @return {*}
 */
export function urisToLookupMapStrict(uris, asyncLookupFunction) {
    //make sure we have an array and not a single uri.
    const urisAsArray = is('Array', uris) ? uris : [uris];
    const asyncLookups = urisAsArray.map(uri =>
        asyncLookupFunction(uri)
        .catch(error => {
            throw({msg: `failed lookup for ${uri} in utils.js:urisToLookupMap`, error, urisAsArray, uris})
        })
    );
    return Promise.all(asyncLookups).then( dataObjects => {
        const lookupMap = {};
        //make sure there's the same
        for (let i = 0; i < uris.length; i++) {
            lookupMap[uris[i]] = dataObjects[i];
        }
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
    if(is('Array', object)) {
        const promises = object.map(el => asyncFunction(el));
        return Promise.all(promises);
    } else if(is('Object', object)){
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
 * @param type
 * @param obj
 * @return {boolean}
 */
export function is(type, obj) {
    var clas = Object.prototype.toString.call(obj).slice(8, -1);
    return obj !== undefined && obj !== null && clas === type;
}

export function decodeUriComponentProperly(encodedUri) {
    if(!encodedUri)
        return undefined; //for some reason decodeUri(undefined) yields "undefined"
    else
        return decodeURIComponent(encodedUri);
}

export function msStringToDate(ts) {
    if(is('String', ts)) {
        ts = Number.parseInt(ts);
    }
    return new Date(ts);
}

/**
 * Searches the nominatim address-lookup service and
 * returns a list with the search results.
 */
export function searchNominatim(searchStr) {
    var url = "https://nominatim.openstreetmap.org/search" +
        "?q=" + encodeURIComponent(searchStr) +
        "&format=json";
    console.log("About to query nominatim: " + url);
    return fetchJSON(url);
}


export function reverseSearchNominatim(lat, lon, zoom) {
    let url = "https://nominatim.openstreetmap.org/reverse" +
        "?lat=" + lat +
        "&lon=" + lon +
        "&format=json";

    if(!isNaN(zoom)) {
       url += "&zoom=" + Math.max(0, Math.min(zoom, 18));
    }
    console.log("About to do reverse lookup on nominatim: " + url);
    return fetchJSON(url);
}

/**
 * drop info not stored in rdf, thus info that we
 * couldn't restore for previously used locations
 */
export function nominatim2draftLocation(searchResult) {
    const b = searchResult.boundingbox;
    return {
        name: searchResult.display_name,
        lon: Number.parseFloat(searchResult.lon),
        lat: Number.parseFloat(searchResult.lat),
        //importance: searchResult.importance,
        bounds: [
            [ Number.parseFloat(b[0]), Number.parseFloat(b[2]) ], //north-western point
            [ Number.parseFloat(b[1]), Number.parseFloat(b[3]) ] //south-eastern point
        ],
    }
}

function fetchJSON(url) {
    return fetch(url, {
        method: 'get',
        //credentials: "same-origin",
        headers: { 'Accept': 'application/json' }
    })
        .then(resp => {
            /*
             * handle errors and read json-data
             */
            const errorMsg =
                "GET to " + url + " failed with ("
                + resp.status + "): " + resp.statusText +
                "\n" + resp;
            if(resp.status !== 200) {
                throw new Error(errorMsg);
            } else {
                try {
                    return resp.json();
                } catch (jsonParseError) { // nominatim responded with an HTTP-200 with an error html-page m(
                    const e = new Error(errorMsg)
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
    var copy;

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
        for (var i = 0, len = obj.length; i < len; i++) {
            copy[i] = clone(obj[i]);
        }
        return copy;
    }

    // Handle Object
    if (obj instanceof Object) {
        copy = {};
        for (var attr in obj) {
            if (obj.hasOwnProperty(attr)) copy[attr] = clone(obj[attr]);
        }
        return copy;
    }

    throw new Error("Unable to copy obj! Its type isn't supported.");
}

/**
 * Tries to look up a property-path on a nested object-structure.
 * Where `obj.x.y` would throw an exception if `x` wasn't defined
 * `get(obj, ['x','y'])` would return undefined.
 * @param obj
 * @param path
 * @return {*}
 */
export function getIn(obj, path) {
    if(!path || !obj || path.length === 0) {
        return undefined;
    } else {
        let child;
        if(obj.toJS && obj.get) {
            /* obj is an immutabljs-object
             * NOTE: the canonical check atm would be `Immutable.Iterable.isIterable(obj)`
             * but that would require including immutable as dependency her and it'd be better
             * to keep this library independent of anything.
             */
            child = obj.get(path[0])
        } else {
            /* obj is a vanilla object */
            child = obj[path[0]]
        }
        if(path.length === 1) {
            /* end of the path */
            return child;
        } else {
            /* recurse */
            return getIn(child, path.slice(1));
        }
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
    const zs = new Array(Math.min(xs.length, ys.length));
    for(let i = 0; i < xs.length && i < ys.length; i++) {
        zs[i] = f(xs[i], ys[i]);
    }
    return zs;
}

export function all(boolArr) {
    return boolArr.reduce((b1, b2) => b1 && b2, true);
}

/**
 * compares two arrays and checks if their contents are equal
 */
export function arrEq(xs, ys) {
    return xs.length === ys.length &&
        all(
            //elementwise comparison
            zipWith((x, y) => x === y, xs, ys)
        );

}

/**
 * Converts from proper json-ld to the format
 * that is returned by `rdfstore.node`. Note that
 * the process isn't reversible, as rdf-prefixes
 * are stripped as part of it.
 * @param jsonldObj
 * @returns {{}}
 */
export function jsonld2simpleFormat(jsonldObj, context = undefined) {
    if (!jsonldObj) {
        return;
    }
    if(!context && jsonldObj['@context']) {
        context = jsonldObj['@context']; // needed for expanding values
    }
    if (is('String', jsonldObj)) {

        // try to expand any existing prefixes
        if(context) {
            for (let prefix of Object.keys(context)) {
                if (jsonldObj.startsWith(prefix + ':')) {
                    // found a compacted prefix that we can expand using the context
                    return jsonldObj.replace(prefix + ':', context[prefix])
                }
            }
        }

        // no prefix found -- it's either a plain value or the prefix isn't in the context
        return jsonldObj;

    } else if (is('Array', jsonldObj)) {
        return jsonldObj.map(x => jsonld2simpleFormat(x, context));
    } else if (is('Object', jsonldObj)) {

        if (Object.keys(jsonldObj).length === 1 && jsonldObj['@id']) {
            // current node is an URL. return it as string.
            return jsonld2simpleFormat(jsonldObj['@id'], context);
        } else if (Object.keys(jsonldObj).length === 2 && jsonldObj['@value']) {
            // encountered value with datatype. we're interested in the @value
            return jsonld2simpleFormat(jsonldObj['@value'], context);
        } else {
            // full fledged jsonld-node. iterate over the keys and recurse into the values.

            var newObj = {};
            for (let k of Object.keys(jsonldObj)) {
                var newKey;
                switch (k) {
                    case '@context':
                        // drop it. we can't use the json-ld context in
                        // the simplified object-format anyway, as there's no prefixes
                        continue;

                    case '@id':
                        newKey = 'uri'
                        //newObj['uri'] = jsonldObj['@id'];
                        break;

                    case '@type':
                        newKey = 'type'
                        // newObj['type'] = jsonldObj['@type'];
                        break;

                    default:
                        var split = k.split(':')
                        if (split.length !== 2) {
                            throw new Exception(
                                'encountered unexpected predicate when parsing json-ld. it doesn\'t follow the "<prefix>:<postfix> structure: "'
                                + k
                            );
                        }
                        newKey = split[1];
                        break;
                }
                newObj[newKey] = jsonld2simpleFormat(jsonldObj[k], context);
            }
            return newObj;
        }
    } else {
        throw new Exception('Encountered unexpected value while parsing json-ld: ', jsonldObj);
    }
}

/**
 * Similar to Promise.all, takes an array of promises and returns a promise.
 * That promise will resolve if at least one of the promises succeeds.
 * The value with which it resolves it is an array of equal length as the input
 * containing either the resolve value of the promise or null if rejected.
 * If an errorHandler is specified, it is called with ([array key], [reject value]) of
 * each rejected promise.
 *
 * Consider using `Promise.race` if you just need once Promise to resolve/reject.
 * `somePromises` waits for all promises to either resolve or reject, then resolves
 * if at least one of them was successful.
 *
 * @param promises
 */
export function somePromises(promises, errorHandler) {
    if(!promises || promises.length === 0) {
        Promise.resolve();
    }

    let numPromises = promises.length,
        successes = 0,
        failures = 0,
        results = Array.isArray(promises) ? [] : {},
        handler = typeof errorHandler === 'function' ? errorHandler : function(x,y){};

    const resultPromise = new Promise((resolve, reject) =>
        promises.forEach((promise, key) => {
            promise.then(
                value => {
                    successes++;
                    if (results.hasOwnProperty(key)) return; //TODO: not sure if we need this
                    results[key] = value;
                    if (failures + successes >= numPromises) resolve(results);
                },
                reason => {
                    failures ++;
                    //console.log("linkeddata-service-won.js: warning: promise failed. Reason " + JSON.stringify(reason));
                    if (results.hasOwnProperty(key)) return; //TODO: not sure if we need this
                    results[key] = null;
                    handler(key, reason);
                    if (failures >= numPromises) {
                        reject(results);
                    } else if (failures + successes >= numPromises) {
                        resolve(results);
                    }
                }
            )


        })
    )
}

/**
 *
 * Adapted from https://stackoverflow.com/questions/901115/how-can-i-get-query-string-values-in-javascript
 *
 * Usage:
 * ```
 * // query string: ?foo=lorem&bar=&baz
 * var foo = getParameterByName('foo'); // "lorem"
 * var bar = getParameterByName('bar'); // "" (present with empty value)
 * var baz = getParameterByName('baz'); // "" (present with no value)
 * var qux = getParameterByName('qux'); // null (absent)
 * ```
 * @param name
 * @param url
 * @returns {*}
 */
export function getParameterByName(name, url) {
    if (!url) url = window.location.href;
    name = name.replace(/[\[\]]/g, "\\$&");
    const regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)");
    const results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

/**
 * Retrieves parameters from the url-bar or parses them from a passed url.
 * @param url
 * @returns {{}}
 */
export function getParameters(url) {
    const url_ = url? url : window.location.href; // e.g. url_ = "http://example.org/?privateId=5kpskm09-ocri63&foo=bar&asdf"
    const [, paramsString] = url_.split('?') // e.g. paramsString = "privateId=5kpskm09-ocri63&foo=bar&asdf"

    if(!paramsString) {
        // no parameters present
        return {};
    }

    const paramsKVArray = paramsString
        .split('&')  // e.g. ["privateId=5kpskm09-ocri63", "foo=bar", "asdf"]
        .map(p => p.split('=')) // e.g. [["privateId", "5kpskm09-ocri63"], ["foo", "bar"], ["asdf"]]
        .filter(p => p.length === 2); // filter out parameter that's not a proper key-value pair, e.g. "asdf"

    // create object from kv-pairs
    var params = {};
    paramsKVArray.forEach(kv => params[kv[0]] = kv[1]);

    return params;
}
