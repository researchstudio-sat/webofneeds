/**
 * Created by syim on 20.01.2016.
 */
import { actionTypes } from '../actions/actions';
import { repeatVar } from '../utils';
import Immutable from 'immutable';
import { createReducer } from 'redux-immutablejs'
import { combineReducersStable } from '../redux-utils';
import { buildCreateMessage } from '../won-message-utils';
import won from '../won-es6';

const initialState = Immutable.fromJS({});

export default function(connections = initialState, action = {}) {
    switch(action.type) {
        case actionTypes.messages.closeNeed.failed:
        case actionTypes.initialPageLoad:
        case actionTypes.login:
            return storeConnections(connections, action.payload.get('connections'));

        case actionTypes.connections.accepted:
            const acceptEvent = action.payload;
            const acceptConnectionUri = acceptEvent.hasReceiver;
            return connections.setIn([acceptConnectionUri, 'hasConnectionState'], won.WON.Connected);

        case actionTypes.needs.received:
            const connectionUris = action.payload.affectedConnections
            if (!connectionUris){
                return connections;
            } else {
                return connectionUris.reduce((updatedConnections, connectionUri) =>
                    updatedConnections.setIn([connectionUri, 'hasConnectionState'], won.WON.Closed),
                    connections
                )
            }

        case actionTypes.messages.connect.success:
            var connectionUri = action.payload.hasReceiver;
            return storeEventUris(connections, connectionUri, [action.payload.uri])
                .setIn([connectionUri, 'hasConnectionState'], won.WON.RequestSent)

        case actionTypes.messages.close.success:
            var connectionUri = action.payload.hasReceiver;
            return storeEventUris(connections, connectionUri, [action.payload.uri])
                .setIn([connectionUri, 'hasConnectionState'], won.WON.Closed);

        case actionTypes.connections.sendChatMessage:
            var eventUri = action.payload.eventUri;
            var connectionUri = action.payload.optimisticEvent.hasSender;
            return connections.updateIn(
                [connectionUri, 'hasEvents'],
                events => events.add(eventUri)
            );

        case actionTypes.connections.load:
            return action.payload.reduce(
                (updatedState, connectionWithRelatedData) =>
                    storeConnectionAndRelatedData(updatedState, connectionWithRelatedData),
                connections);

        case actionTypes.messages.chatMessage.failure:
            return connections.updateIn(
                [action.payload.connectionUri, 'hasEvents'],
                eventUris => eventUris.remove(action.payload.eventUri)
            );

        case actionTypes.messages.chatMessage.successOwn:
            var msgFromOwner = action.payload.events['msg:FromSystem'];
            var connectionUri = msgFromOwner.hasReceiver;
            return storeEventUris(connections, connectionUri, [msgFromOwner.uri]);

        case actionTypes.messages.chatMessage.successRemote:
            var eventOnOwnNode = action.payload.events['msg:FromExternal'];
            var msgFromOwner = action.payload.events['msg:FromSystem'];
            var connectionUri = msgFromOwner.hasReceiver;
            return storeEventUris(
                connections,
                connectionUri,
                [msgFromOwner.uri, eventOnOwnNode.uri]
            );

        case 'requiredData':
            var loadedEvents = Immutable.fromJS(action.payload.events);
            return loadedEvents.reduce((updatedConnections, event) => {
                const cnctUri = event.get('hasReceiver') || event.getIn(['hasCorrespondingRemoteMessage', 'hasReceiver']);
                return storeEventUri(updatedConnections, cnctUri, event.get('uri'));
            }, connections);

        case actionTypes.messages.connectionMessageReceived:
            var eventOnOwn = action.payload.events['msg:FromExternal'];
            return storeEventUri(connections, eventOnOwn.hasReceiver, eventOnOwn.uri);

        case actionTypes.messages.connectMessageReceived:
        case actionTypes.messages.openMessageReceived:
        case actionTypes.messages.hintMessageReceived:
            return storeConnectionAndRelatedData(connections, action.payload);

        case actionTypes.logout:

            return initialState;

        default:
            return connections;
    }
}

function storeConnections(connections, connectionsToStore) {
    if(connectionsToStore && connectionsToStore.size > 0) {
        const connectionsWithEventSets = connectionsToStore.map(connection =>
                //make sure hasEvents are sets
                connection.update('hasEvents', events => Immutable.Set(events))
        );
        return connections.merge(connectionsWithEventSets);
    } else {
        return connections;
    }
}

function storeEventUri(connections, connectionUri, eventUri) {
    return connections.updateIn(
        [connectionUri, 'hasEvents'],
        eventUris => eventUris?
                eventUris.add(eventUri):
                Immutable.Set([eventUri])
    );
}

function storeEventUris(connections, connectionUri, eventUris) {
    return connections.updateIn(
        [connectionUri, 'hasEvents'],
        events => events ?
            events.merge(eventUris) :
            Immutable.Set(eventUris)
    );
}


function storeConnectionAndRelatedData(state, connectionWithRelatedData) {
    console.log("STORING CONNECTION AND RELATED DATA");
    console.log(connectionWithRelatedData);

    //make sure we have a set of events (as opposed to a list with redundancies)
    const events = Immutable.Set(connectionWithRelatedData.connection.hasEvents);
    const connection = Immutable
        .fromJS(connectionWithRelatedData.connection)
        .set('hasEvents', events);

    return state
        .mergeDeepIn([connection.get('uri')], connection);
}
