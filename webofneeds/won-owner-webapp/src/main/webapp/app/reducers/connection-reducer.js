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
        case actionTypes.load:
            const allPreviousConnections = action.payload.get('connections');
            return connections.merge(allPreviousConnections);

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
            return connections
                .updateIn([connectionUri, 'hasEvents'], events => events.add(eventUri));

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

        case actionTypes.messages.connectionMessageReceived:
        case actionTypes.messages.connectMessageReceived:
        case actionTypes.messages.hintMessageReceived:
            return storeConnectionAndRelatedData(connections, action.payload);

        case actionTypes.connections.reset:
            return initialState;

        default:
            return connections;
    }
}

function storeEventUris(connections, connectionUri, eventUris) {
    return connections.updateIn(
        [connectionUri, 'hasEvents'],
        events => events.merge(eventUris)
    );
}


function storeConnectionAndRelatedData(state, connectionWithRelatedData) {
    console.log("STORING CONNECTION AND RELATED DATA");
    console.log(connectionWithRelatedData);

    //make sure we have a set of events (as opposed to a list with redundancies)
    const events = Immutable.Set(connectionWithRelatedData.connection.hasEvents)
    const connection = Immutable
        .fromJS(connectionWithRelatedData.connection)
        .set('hasEvents', events);

    return state
        .mergeDeepIn([connection.get('uri')], connection);
}
