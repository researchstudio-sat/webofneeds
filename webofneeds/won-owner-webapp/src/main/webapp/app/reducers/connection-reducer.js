/**
 * Created by syim on 20.01.2016.
 */
import { actionTypes } from '../actions/actions';
import Immutable from 'immutable';
import { createReducer } from 'redux-immutablejs'
import won from '../won-es6';

const initialState = Immutable.fromJS({});

export default function(connections = initialState, action = {}) {
    switch(action.type) {
        case actionTypes.messages.closeNeed.failed:
        case actionTypes.initialPageLoad:
        case actionTypes.login:
            return storeConnections(connections, action.payload.get('connections'));

        case actionTypes.messages.open.successOwn:
            var connectionUri = action.payload.events['msg:FromSystem'].hasReceiver;
            return connections.setIn([connectionUri, 'messageDraft'], ""); // successful open -- can reset draft

        case actionTypes.connections.connect: // user has sent a request
            var { connectionUri, eventUri } = action.payload
            return storeEventUris(connections, connectionUri, [eventUri])
                .setIn([connectionUri, 'hasConnectionState'], won.WON.RequestSent);

        case actionTypes.connections.open:
            var eventUri = action.payload.eventUri;
            var connectionUri = action.payload.optimisticEvent.hasSender;
            return connections.updateIn(
                [connectionUri, 'hasEvents'],
                    events => events.add(eventUri)
            )
            .setIn(
                [connectionUri, 'hasConnectionState'],
                won.WON.Connected
            );

        case actionTypes.messages.open.failure:
            var acceptConnectionUri = action.payload.events['msg:FromSystem'].hasReceiver;
            return connections.setIn([acceptConnectionUri, 'hasConnectionState'], won.WON.RequestReceived);

        case actionTypes.messages.connect.success:
            var connectionUri = action.payload.hasReceiver;
            return storeEventUris(connections, connectionUri, [action.payload.uri])
                .setIn([connectionUri, 'hasConnectionState'], won.WON.RequestSent)

        case actionTypes.messages.close.success:
            var connectionUri = action.payload.hasReceiver;
            return storeEventUris(connections, connectionUri, [action.payload.uri])
                .setIn([connectionUri, 'hasConnectionState'], won.WON.Closed);

        case actionTypes.connections.close:
            return connections.setIn([action.payload.connectionUri, 'hasConnectionState'], won.WON.Closed);


        case actionTypes.connections.typedAtChatMessage:
            var connectionUri = action.payload.connectionUri;
            var chatMessage = action.payload.message;
            return connections.setIn([connectionUri, 'messageDraft'], chatMessage);

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
            return storeEventUris(connections, connectionUri, [msgFromOwner.uri])
                .setIn([connectionUri, 'messageDraft'], chatMessage); // successful post -- no need for draft anymore

        case actionTypes.messages.chatMessage.successRemote:
            var eventOnOwnNode = action.payload.events['msg:FromExternal'];
            var msgFromOwner = action.payload.events['msg:FromSystem'];
            var connectionUri = msgFromOwner.hasReceiver;
            return storeEventUris(
                connections,
                connectionUri,
                [msgFromOwner.uri, eventOnOwnNode.uri]
            );

        case actionTypes.connections.showLatestMessages:
        case actionTypes.connections.showMoreMessages:
            if(action.payload.get('pending')) {
                return connections.update(
                    action.payload.get('connectionUri'),
                    cnct => {
                        const existingCnct = cnct? cnct : Immutable.Map();
                        return existingCnct
                            .set('loadingEvents', true)
                            .delete('failedLoadingEvents')
                    }
                );
            } else if (action.payload.get('error')) {
                return connections.update(
                    action.payload.get('connectionUri'),
                    cnct => cnct
                        .delete('loadingEvents')
                        .set('failedLoadingEvents', action.payload.get('error'))
                );
            } else /* success */ {
                var loadedEvents = action.payload.get('events');
                var connectionUri = action.payload.get('connectionUri');
                var connectionsWithUpdatedFlags = connections.update(
                    action.payload.get('connectionUri'),
                    cnct => cnct
                        .delete('loadingEvents')
                        .delete('failedLoadingEvents')
                );
                return loadedEvents.reduce((cncts, event) =>
                    storeEventUri(cncts, connectionUri, event.get('uri')),
                    connectionsWithUpdatedFlags
                );

            }

        case actionTypes.messages.connectionMessageReceived:
            var eventOnOwn = action.payload.events['msg:FromExternal'];
            return storeEventUri(connections, eventOnOwn.hasReceiver, eventOnOwn.uri);

        case actionTypes.messages.connectMessageReceived:
        case actionTypes.messages.openMessageReceived:
        case actionTypes.messages.hintMessageReceived:
            return storeConnection(connections, action.payload.connection);

        case actionTypes.logout:

            return initialState;

        default:
            return connections;
    }
}

function storeConnection(connections, connectionToStore) {
    let immutableCnct = sanitizeConnection(connectionToStore);
    return connections.mergeIn([immutableCnct.get('uri')], immutableCnct);
}

function storeConnections(connections, connectionsToStore) {
    if(connectionsToStore && connectionsToStore.size > 0) {
        const connectionsWithEventSets = connectionsToStore.map(sanitizeConnection);
        return connections.merge(connectionsWithEventSets);
    } else {
        console.error("invalid or empty connectionsToStore: ", connectionsToStore);
        return connections;
    }
}

function sanitizeConnection(connection) {
    let immutableCnct = connection;
    if(!Immutable.Map.isMap(connection)) {
        immutableCnct = Immutable.fromJS(connection)
    }
    if(!Immutable.Set.isSet(immutableCnct.get('hasEvents'))) {
        //make sure events are stored as set (i.e. every uri only once)
        immutableCnct = immutableCnct.update('hasEvents', events => Immutable.Set(events));
    }
    return immutableCnct;
}


function storeEventUri(connections, connectionUri, newEventUri) {
    if(!newEventUri) {
        console.error("Tried to store event with undefined or empty uri: ", newEventUri);
        return connections;
    }
    return connections.updateIn(
        [connectionUri, 'hasEvents'],
        eventUris => eventUris && Immutable.Set.isSet(eventUris)?
                eventUris.add(newEventUri):
                Immutable.Set([newEventUri])
    );
}

function storeEventUris(connections, connectionUri, newEventUris) {
    if(!newEventUris) {
        console.error("Tried to store events from undefined or empty list: ", newEventUris);
        return connections;
    }
    return connections.updateIn(
        [connectionUri, 'hasEvents'],
        eventUris => eventUris && Immutable.Set.isSet(eventUris)?
            eventUris.merge(newEventUris) :
            Immutable.Set(newEventUris)
    );
}
