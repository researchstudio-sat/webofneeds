/**
 * Created by ksinger on 19.02.2016.
 */

import  won from '../won-es6';
import { actionTypes, actionCreators, getConnectionRelatedData, messageTypeToEventType  } from './actions';
import { getEventData,setCommStateFromResponseForLocalNeedMessage } from '../won-message-utils';

import { selectAllByConnections } from '../selectors';

import {
    checkHttpStatus,
} from '../utils';

import {
    buildCreateMessage,
    buildOpenMessage,
    buildCloseMessage,
    buildRateMessage,
    buildConnectMessage
} from '../won-message-utils';

export function connectionsFetch(data) {
    return dispatch=> {
        const allConnectionsPromise = won.executeCrawlableQuery(won.queries["getAllConnectionUrisOfNeed"], data.needUri);
        allConnectionsPromise.then(function (connections) {
            console.log("fetching connections")
            dispatch(actionCreators.needs__connectionsReceived({needUri: data.needUri, connections: connections}))
            dispatch(actionCreators.events__fetch({connectionUris: connections}))
        })
    }
}

export function connectionsLoad(needUris) {
    return dispatch => {
        needUris.forEach(needUri =>
                won.executeCrawlableQuery(won.queries["getAllConnectionUrisOfNeed"], needUri)
                    .then(function (connectionsOfNeed) {
                        console.log("fetching connections");
                        Promise.all(connectionsOfNeed.map(connection => getConnectionRelatedData(
                            connection.need.value,
                            connection.remoteNeed.value,
                            connection.connection.value
                        )))
                            .then(connectionsWithRelatedData =>
                                dispatch({
                                    type: actionTypes.connections.load,
                                    payload: connectionsWithRelatedData
                                })
                        );
                    })
        );
    }
}

export function connectionsOpen(connectionData,message) {
    return (dispatch, getState) => {
        const state = getState();
        const eventData = selectAllByConnections(state).get(connectionData.connection.uri).toJS(); // TODO avoid toJS;
        //let eventData = state.getIn(['connections', 'connectionsDeprecated', connectionData.connection.uri])
        let messageData = null;
        let deferred = Q.defer()
        won.getConnection(eventData.connection.uri).then(connection=> {
            let msgToOpenFor = {event: eventData, connection: connection}
            buildOpenMessage(msgToOpenFor, message).then(messageData=> {
                console.log("built open message");
                deferred.resolve(messageData);
            })
        })
        deferred.promise.then((action)=> {
            console.log("dispatching messages__send action"+ action);
            dispatch(actionCreators.messages__send({eventUri: action.eventUri, message: action.message}));
        })
    }
}

export function connectionsConnect(connectionData,message) {
    return (dispatch, getState) => {
        const state = getState();
        const eventData = selectAllByConnections(state).get(connectionData.connection.uri).toJS(); // TODO avoid toJS;
        //let eventData = state.getIn(['connections', 'connectionsDeprecated', connectionData.connection.uri])
        let messageData = null;
        let deferred = Q.defer()
        won.getConnection(eventData.connection.uri).then(connection=> {
            let msgToOpenFor = {event: eventData, connection: connection}
            buildConnectMessage(msgToOpenFor, message).then(messageData=> {
                deferred.resolve(messageData);
            })
        })
        deferred.promise.then((action)=> {
            dispatch(actionCreators.messages__send({eventUri: action.eventUri, message: action.message}));
        })
    }
}

export function connectionsClose(connectionData) {
    return (dispatch, getState) => {
        const state = getState();
        const eventData = selectAllByConnections(state).get(connectionData.connection.uri).toJS();// TODO avoid toJS
        //let eventData = state.getIn(['connections', 'connectionsDeprecated', connectionData.connection.uri])
        let messageData = null;
        let deferred = Q.defer()
        won.getConnection(eventData.connection.uri).then(connection=> {
            let msgToOpenFor = {event: eventData, connection: connection}
            buildCloseMessage(msgToOpenFor).then(messageData=> {
                deferred.resolve(messageData);
            })
        })
        deferred.promise.then((action)=> {
            dispatch(actionCreators.messages__send({eventUri: action.eventUri, message: action.message}));
        })
    }
}

export function connectionsRate(connectionData,rating) {
    return (dispatch, getState) => {
        console.log(connectionData);
        console.log(rating);

        const state = getState();
        const eventData = selectAllByConnections(state).get(connectionData.connection.uri);// TODO avoid toJS
        //let eventData = state.getIn(['connections', 'connectionsDeprecated', connectionData.connection.uri])
        let messageData = null;
        let deferred = Q.defer()
        won.getConnection(eventData.connection.uri).then(connection=> {
            let msgToOpenFor = {event: eventData, connection: connection}
            buildRateMessage(msgToOpenFor, rating).then(messageData=> {
                deferred.resolve(messageData);
            })
        })
        deferred.promise.then((action)=> {
            dispatch(actionCreators.messages__send({eventUri: action.eventUri, message: action.message}));
        })
    }
}
