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

export function readAsDataURL(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = function() {
            resolve(reader.result);
        };
        reader.onerror = function() {
            reject(f);
        };
        reader.readAsDataURL(file);
    });
};

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
