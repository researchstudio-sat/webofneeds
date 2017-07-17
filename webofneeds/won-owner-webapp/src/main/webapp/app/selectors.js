/**
 * Created by ksinger on 22.01.2016.
 */

import { createSelector } from 'reselect';
import Immutable from 'immutable';
import won from './won-es6';
import {
    decodeUriComponentProperly,
    is,
} from './utils';

import {
    selectTimestamp,
} from './won-utils';

export const selectConnections = state => state.getIn(['connections']);
export const selectEvents = state => state.getIn(['events', 'events']);
export const selectLastUpdateTime = state => state.get('lastUpdateTime');
export const selectRouterParams = state => state.getIn(['router', 'currentParams']);

export const selectAllNeeds = state => state.get("needs");
export const selectAllOwnNeeds = state => selectAllNeeds(state).filter(need =>
    need.get("ownNeed")
);
export const selectAllTheirNeeds = state => selectAllNeeds(state).filter(need =>
    !need.get("ownNeed")
);

/**
 * Get the need for a given connectionUri
 * @param state to retrieve data from
 * @param connectionUri to find corresponding need for
 */
export function selectNeedByConnectionUri(state, connectionUri){
    let needs = selectAllOwnNeeds(state); //we only check own needs as these are the only ones who have connections stored
    return needs.filter(need => need.getIn(["connections", connectionUri])).first();
}

/**
 * Get all connections stored within your own needs as a map
 * @returns Immutable.Map with all connections
 */
export function selectAllConnections(state) {
    const needs = selectAllOwnNeeds(state); //we only check own needs as these are the only ones who have connections stored
    let connections = Immutable.Map();

    needs.map(function(need){
        connections = connections.merge(need.get("connections"));
    });

    return connections;
}

export function selectAllMessages(state) {
    const connections = selectAllConnections(state);
    let messages = Immutable.Map();

    connections.map(function(conn){
        messages = messages.merge(conn.get("messages"));
    });

    return messages;
}

export function selectAllMessagesByNeedUri(state, needUri) {
    const connections = state.getIn(["needs", needUri, "connections"]);
    let messages = Immutable.Map();

    if(connections){
        connections.map(function(conn){
            messages = messages.merge(conn.get("messages"));
        });
    }

    return messages;
}

export const selectRemoteEvents = createSelector(
    selectEvents,
    events => {
        const remoteUrisAndEvents = events
            .toList()
            .map(e => {
                let remote = e.get('hasCorrespondingRemoteMessage'); // select remote
                if(is('String', remote)) remote = events.get(remote); // for those rare cases where remote is only a uri
                if(!remote) return undefined;
                remote = remote.set('correspondsToOwnMsg', e); //add back-reference to it
                return remote && [remote.get('uri'), remote]
            })
            .filter(uriAndEvent => uriAndEvent); // filter out `undefined`s
        return Immutable.Map(remoteUrisAndEvents)
    }
);

/**
 * selects a map of `connectionUri -> { connection, events, ownNeed, remoteNeed }`
 * - thus: everything a connection has direct references to. Use this selector
 * when you're needing connection-centric data (e.g. for a view with a strong
 * focus on the connection)
 *
 * NOTE: the app-state used to have events and needs stored in this fashion.
 * Thus, this selector is also used to allow older code to use the new
 * state-structure with minimal changes.
 */
export const selectAllByConnections = createSelector(
    state => state, //reselect's createSelector always needs a dependency
    state => state
        .getIn(['connections'])
        .map(connection => allByConnection(connection)(state))
);
const allByConnection = (connection) => (state) => {
    const ownNeedUri = connection.get('belongsToNeed');
    const ownNeed = state.getIn(['needs', ownNeedUri]);

    const remoteNeedUri = connection.get('hasRemoteNeed');
    const remoteNeed = state.getIn(['needs', remoteNeedUri]);

    const events = connection
        .get('hasEvents')
        .map(eventUri => state.getIn(['events', 'events', eventUri]))
        .filter(event => !!event);


    return Immutable.Map({ connection, events, ownNeed, remoteNeed });
};

export const selectOpenConnectionUri = createSelector(
    selectRouterParams,
    selectConnections,
    (routerParams, connections) => {
        //de-escaping is lost in transpiling if not done in two steps :|
        const openConnectionUri = decodeUriComponentProperly(
            routerParams.get('connectionUri') ||
            routerParams.get('openConversation')
        );

        const myUri = decodeUriComponentProperly(routerParams.get('myUri')); //TODO deprecated parameter

        const theirUri = decodeUriComponentProperly(routerParams.get('theirUri')); //TODO deprecated parameter

        if(openConnectionUri) {
            return openConnectionUri;
        } else if (myUri && theirUri) {
            /*
             returns undefined when there's no
             connection like that in the state.
             */
            return connections
                .filter(c =>
                    c.get('belongsToNeed') === myUri  &&
                    c.get('hasRemoteNeed') === theirUri
                ).keySeq().first()
        } else {
            return undefined;
        }
    }
);

export const selectOpenConnection = createSelector(
    selectOpenConnectionUri, selectConnections,
    (uri, connections) =>
        connections.get(uri)
);

export const selectOpenPostUri = createSelector(
    state => state,
    state => {
        const encodedPostUri =
            state.getIn(['router', 'currentParams', 'postUri']) ||
            state.getIn(['router', 'currentParams', 'myUri']); //deprecated parameter
        return decodeUriComponentProperly(encodedPostUri);
    }
);

export const displayingOverview = createSelector(
    selectOpenPostUri,
    postUri => !postUri //if there's a postUri, this is almost certainly a detail view
);

/**
 * @deprecated doesn't use daisy-chaining yet.
 */
export const selectLastUpdatedPerConnection = createSelector(
    selectAllByConnections,
    allByConnections => allByConnections.map(connectionAndRelated =>
        connectionAndRelated.get('events')
        .map( event =>
            //selectTimestamp(event, connectionAndRelated.getIn(['connection','uri']) )
            selectTimestamp(event)
        )
        /*
         * don't use events without timestamp
         * NOTE if there's no events with timestamps
         * for the connection:
         * `Immutable.List([]).max() === undefined`
         */
        .filter(timestamp => timestamp)
        .map(timestamp => Number.parseInt(timestamp))
        .max()
    )
);