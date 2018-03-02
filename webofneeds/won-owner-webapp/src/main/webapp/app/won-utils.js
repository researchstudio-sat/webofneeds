/**
 * Created by ksinger on 11.08.2016.
 */

import Immutable from 'immutable';
import L from './leaflet-bundleable.js';
import {
    arrEq,
    checkHttpStatus,
    generateIdString,
    getIn,
    is,
} from './utils.js';

import N3 from '../scripts/N3/n3-browserify.js';
window.N34dbg = N3;

import jsonld from 'jsonld';
window.jsonld4dbg = jsonld;

export function initLeaflet(mapMount) {
    if(!L) {
        throw new Exception("Tried to initialize a leaflet widget while leaflet wasn't loaded.");
    }

    const baseMaps = initLeafletBaseMaps();

    const map = L.map(mapMount,{
        center: [37.44, -42.89], //centered on north-west africa
        zoom: 1, //world-map
        layers: [baseMaps['Detailed default map']], //initially visible layers

    }); //.setView([51.505, -0.09], 13);

    //map.fitWorld() // shows every continent twice :|
    map.fitBounds([[-80, -190],[80, 190]]); // fitWorld without repetition

    L.control.layers(baseMaps).addTo(map);

    // Force it to adapt to actual size
    // for some reason this doesn't happen by default
    // when the map is within a tag.
    // this.map.invalidateSize();
    // ^ doesn't work (needs to be done manually atm);

    return map;
}

export function initLeafletBaseMaps() {
    if(!L) {
        throw new Exception("Tried to initialize leaflet map-sources while leaflet wasn't loaded.");
    }
    const secureOsmSource = 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png' // secure osm.org
    const secureOsm = L.tileLayer(secureOsmSource, {
        attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
    });

    const transportSource = 'http://{s}.tile2.opencyclemap.org/transport/{z}/{x}/{y}.png';
    const transport = L.tileLayer(transportSource, {
        attribution: 'Maps &copy; <a href="http://www.thunderforest.com">Thunderforest</a>, Data &copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>',
    });

    const baseMaps = {
        "Detailed default map": secureOsm,
        "Transport (Insecurely loaded!)": transport,
    };

    return baseMaps;
}

export function selectTimestamp(event, ownNeedUri) {
    /*
     * the "outer" event is from our own event
     * container. The receivedTimestamp there
     * should have been placed by our own node.
     *
     * The exception are events that haven't
     * been confirmed yet. They don't have a
     * received timestamp, as these are optimistic
     * assumptions with only sent timestamps.
     */
    return event.get('hasReceivedTimestamp') || event.get('hasSentTimestamp');
};


/**
 * Makes sure the select-statement is reevaluated, should
 * one of the watched fields change.
 *
 * example usage:
 * ```
 * reduxSelectDependsOnProperties(['self.needUri', 'self.timestamp'], selectFromState, this)
 * ```
 *
 * @param properties a list of watch expressions
 * @param selectFromState same as $ngRedux.connect
 * @param ctrl the controller to bind the results to. needs to have `$ngRedux` and `$scope` attached.
 * @returns {*}
* @returns a function to unregister the watch
 */
export function reduxSelectDependsOnProperties(properties, selectFromState, ctrl) {
    const firstVals = properties.map(p => getIn(
        ctrl.$scope,
        p.split('.'))
    );
    let firstTime = true;
    return ctrl.$scope.$watchGroup(properties, (newVals, oldVals) => {

        if(
            (firstTime && !arrEq(newVals, firstVals)) ||
            !arrEq(newVals, oldVals)
        ) {
            const state = ctrl.$ngRedux.getState();
            const stateSlice = selectFromState(state);
            Object.assign(ctrl, stateSlice);
        }
        if(firstTime) {
            firstTime = false;
        }
    });
}

/**
 * Connects a component to ng-redux, sets up watches for the
 * properties that `selectFromState` depends on and handles
 * cleanup when the component is destroyed.
 * @param selectFromState
 * @param actionCreators
 * @param properties
 * @param ctrl a controller/component with `$scope` and `$ngRedux` attached
 */
export function connect2Redux(selectFromState, actionCreators, properties, ctrl) {
    const disconnectRdx = ctrl.$ngRedux.connect(selectFromState, actionCreators)(ctrl);
    const disconnectProps = reduxSelectDependsOnProperties(properties, selectFromState, ctrl );
    ctrl.$scope.$on('$destroy', () => {
        disconnectRdx();
        disconnectProps();
    });
}

/**
 * Checks whether the user has a logged-in session.
 * Returns a promise with the user-object if successful
 * or a failing promise if an error has occured.
 *
 * @returns {*}
 */
export function checkLoginStatus() {
    return fetch('rest/users/isSignedIn', {credentials: 'include'})
        .then(checkHttpStatus) // will reject if not logged in
        .then(resp => resp.json());
}

/**
 * Registers the account with the server.
 * The returned promise fails if something went
 * wrong during creation.
 *
 * @param credentials either {email, password} or {privateId}
 * @returns {*}
 */
export function registerAccount(credentials) {
    const {email, password} = parseCredentials(credentials);
    return fetch('/owner/rest/users/', {
        method: 'post',
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify({username: email, password: password})
    })
    .then(
        checkHttpStatus
    );
}


/**
 * @param credentials either {email, password} or {privateId}
 * @returns {*}
 */
export function login(credentials) {
    const {email, password, rememberMe} = parseCredentials(credentials);
    const loginUrl = '/owner/rest/users/signin'
    const params = 'username=' + encodeURIComponent(email) + '&password=' + encodeURIComponent(password) + (rememberMe ? '&remember-me=true':'');

    return fetch(loginUrl, {
        method: 'post',
        headers: {
            'Accept': 'application/json',
            "Content-Type": "application/x-www-form-urlencoded",
        },
        body: params,
        credentials: 'include',
    })
    .then(
        checkHttpStatus
    );
}

export function logout() {
    return fetch('/owner/rest/users/signout', {
        method: 'post',
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify({})
    })
        .then(
        checkHttpStatus
    )
}

/**
 * Generates a privateId of `[usernameFragment]-[password]`
 * @returns {string}
 */
export function generatePrivateId() {
    return generateIdString(8) + '-' + generateIdString(8); //<usernameFragment>-<password>
}

/**
 * Parses a given privateId into a fake email address and a password.
 * @param privateId
 * @returns {{email: string, password: *}}
 */
export function privateId2Credentials(privateId) {
    const [usernameFragment, password] = privateId.split('-');
    const email = usernameFragment + '@matchat.org';
    return {
        email,
        password,
    }
}

/**
 * @param credentials either {email, password} or {privateId}
 * @returns {email, password}
 */
export function parseCredentials(credentials) {
    return credentials.privateId ?
        privateId2Credentials(credentials.privateId) :
        credentials;
}

export async function jsonLdToTrig(jsonldData) {
    if(
        !jsonldData || !(
            (is('Array', jsonldData) && jsonldData.length > 0) ||
            (is('Object', jsonldData) && jsonldData['@graph'])
        )
    ) {
        const msg = "Couldn't parse the following json-ld to trig: " + JSON.stringify(jsonldData);
        return Promise.reject(msg);
    }
    const quadString = await jsonld.promises.toRDF(jsonldData, {format: 'application/nquads'})
    const quads = await n3Parse(quadString, {format: 'application/n-quads'});
    const trig = await n3Write(quads, { format: 'application/trig' });
    return trig;
}
window.jsonLdToTrig4dbg = jsonLdToTrig;

/**
 * An wrapper for N3's writer that returns a promise
 * @param {*} triples list of triples, each with "subject", "predicate", 
 *   "object" and optionally "graph"
 * @param {*} writerArgs the arguments for intializing the writer. 
 *   e.g. `{format: 'application/trig'}`. See the writer-documentation 
 *   (https://github.com/RubenVerborgh/N3.js#writing) for more details.
 */
export async function n3Write(triples, writerArgs) {
    const writer = N3.Writer(writerArgs);
    return new Promise((resolve, reject) => {
        triples.forEach(t => writer.addTriple(t))
        writer.end((error, result) => {
            if(error) reject(error);
            else resolve(result)
        })
    });
}

/**
 * A wrapper for N3's parse that returns a promise
 * @param {*} rdf a rdf-string to be parsed
 * @param {*} parserArgs arguments for initializing the parser, 
 *   e.g. `{format: 'application/n-quads'}` if you want to make
 *   parser stricter about what it accepts. See the parser-documentation
 *   (https://github.com/RubenVerborgh/N3.js#parsing) for more details.
 */
export async function n3Parse(rdf, parserArgs) {
    const parser = parserArgs? 
        N3.Parser(parserArgs) : 
        N3.Parser();
    return new Promise((resolve, reject) => {
        let triples = [];
        parser.parse( rdf, (error, triple, prefixes) => {
            if(error) {
                reject(error);
            } else if (triple) {
                triples.push(triple);
            } else {
                // all triples collected
                resolve(triples, prefixes);
            }
        })
    });
}

export async function ttlToJsonLd(ttl) {
    return n3Parse(ttl)
    .then((triples, prefixes) => {
        const graphUri = 'ignoredgraphuri:placeholder';

        /* 
         * the parsing doesn't give us information if a
         * thing was an uri, a blind-node-id or a literal
         * so we need to find that by ourselves before 
         * generating the quads.
         */
        const wrap = frag => {
            if( frag.startsWith("_:") || // id of blind node 
                frag.match(/^".*"$/)  // string-literal
            ) {
                return frag; 
            } else { // uri
                return `<${frag}>`;
            }
        }
        const nquads = triples.map(t => 
            wrap(t.subject) + " " +
            wrap(t.predicate) + " " +
            wrap(t.object) + " " +
            wrap(graphUri) + "." // even if it's ignored it's necessary as jsonld can only parse quads, not tripples
        ).join('\n');

        return jsonld.promises.fromRDF(nquads, {format: 'application/nquads'});
    })
    .then(jsonld => {
        console.log('jsonld parsed from input turtle: ', jsonld);
        return jsonld;
    })
    .catch(e => {
        e.message = "error while parsing the following turtle:\n\n" + ttl + "\n\n----\n\n" + e.message;
        throw e;
    })
}

window.ttlToJsonLd4dbg = ttlToJsonLd;