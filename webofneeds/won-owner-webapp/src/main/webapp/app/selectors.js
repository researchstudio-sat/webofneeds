/**
 * Created by ksinger on 22.01.2016.
 */

import { createSelector } from 'reselect';
import Immutable from 'immutable';
import won from './won-es6.js';
import {
    decodeUriComponentProperly,
    is,
    getIn,
} from './utils.js';

export const selectLastUpdateTime = state => state.get('lastUpdateTime');
export const selectRouterParams = state => getIn(state, ['router', 'currentParams']);

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
 * Get the connection for a given connectionUri
 * @param state to retrieve data from
 * @param connectionUri to find corresponding connection for
 */
export function selectConnection(state, connectionUri){
    let need = selectNeedByConnectionUri(state, connectionUri);
    return need.getIn(['connections', connectionUri]);
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

export function selectAllMessagesByNeedUriAndConnected(state, needUri) {
    const connections = state.getIn(["needs", needUri, "connections"]);
    const connectionsWithoutClosed = connections && connections.filter(conn => conn.get("state") === won.WON.Connected);
    let messages = Immutable.Map();

    if(connectionsWithoutClosed){
        connectionsWithoutClosed.map(function(conn){
            messages = messages.merge(conn.get("messages"));
        });
    }

    return messages;
}

export const selectOpenConnectionUri = createSelector(
    selectRouterParams,
    (routerParams) => {
        //de-escaping is lost in transpiling if not done in two steps :|
        const openConnectionUri = decodeUriComponentProperly(
            routerParams['connectionUri'] ||
            routerParams['openConversation']
        );

        if(openConnectionUri) {
            return openConnectionUri;
        } else {
            return undefined;
        }
    }
);

export const selectOpenPostUri = createSelector(
    state => state,
    state => {
        const encodedPostUri = getIn(state, ['router', 'currentParams', 'postUri']);
        return decodeUriComponentProperly(encodedPostUri);
    }
);