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
} from './utils.js';

export function initLeaflet(mapMount) {
    if(!L) {
        throw new Exception("Tried to initialize a leaflet widget while leaflet wasn't loaded.");
    }

    const baseMaps = initLeafletBaseMaps();

    console.log("Mounting map into: ", mapMount);
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

export function selectEventsOfConnection(state, connectionUri) {
    const eventUris = state.getIn(['connections', connectionUri, 'hasEvents']);
    const eventUrisAndEvents = eventUris &&
        eventUris.map(eventUri => [
            eventUri,
            state.getIn(['events', 'events', eventUri])
        ]);
    return Immutable.Map(eventUrisAndEvents);
}


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
    const {email, password} = parseCredentials(credentials);
    return fetch('/owner/rest/users/signin', {
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
