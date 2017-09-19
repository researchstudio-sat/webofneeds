/**
 * Created by syim on 11.12.2015.
 */
import { actionTypes } from '../actions/actions.js';
import {
    repeatVar,
    getIn,
    contains,
} from '../utils.js';
import Immutable from 'immutable';
import { createReducer } from 'redux-immutablejs'
import { combineReducersStable } from '../redux-utils.js';
import won from '../won-es6.js';

const initialState = Immutable.fromJS({
    events: {},
}).set('unreadEventUris', Immutable.Set());

export default function(state = initialState, action = {}) {
    switch(action.type) {
        case actionTypes.logout:
            return initialState;
        
        case actionTypes.initialPageLoad:
        case actionTypes.login:
            const allPreviousEvents = action.payload.get('events');
            return state.mergeIn(['events'], allPreviousEvents);

        case '@@reduxUiRouter/onSuccess':
            var uiRouterState = getIn(action, ['payload', 'currentState', 'name']);
            var connectionUri = getIn(action, ['payload', 'currentParams', 'connectionUri']);

            if(contains(['post','overviewIncomingRequests','overviewMatches'], uiRouterState)) {
                    if(connectionUri ) {
                        const seenUris = state
                            .get('unreadEventUris')
                            .map(uri => state.getIn(['events', uri]))
                            .filter(e =>
                                e.get('hasReceiver') === connectionUri ||
                                e.get('hasSender') === connectionUri
                            )
                            .map(e => e.get('uri'))
                            .toSet();
                        return state.update('unreadEventUris',
                            unread => unread.subtract(seenUris));
                    }
            }

        case actionTypes.events.read:
            return state.update('unreadEventUris',
                    unread => unread.remove(action.payload));

        /**
         * @deprecated this is a legacy action
         */
        case actionTypes.connections.load:
            return action.payload.reduce(
                (updatedState, connectionWithRelatedData) =>
                    storeConnectionRelatedData(updatedState, connectionWithRelatedData),
                state);

        case actionTypes.messages.close.success:
            var event = action.payload;
            return state.setIn(['events', event.uri], Immutable.fromJS(event));

        case actionTypes.connections.open:
        case actionTypes.connections.sendChatMessage:
        case actionTypes.connections.connect:
            var eventUri = action.payload.eventUri;
            return storeOptimisticEvent(state, action.payload.optimisticEvent);

        case actionTypes.messages.connect.failure:
        case actionTypes.messages.open.failure:
        case actionTypes.messages.chatMessage.failure:
            //var eventOnRemoteNode = action.payload.events['msg:FromOwner'];
            //var eventOnOwnNode = action.payload.events['msg:FromExternal'];
            //var connectionUri = msgFromOwner.hasReceiver;
            var msgFromOwner = action.payload.events['msg:FromSystem'];
            var eventUri = msgFromOwner.isRemoteResponseTo || msgFromOwner.isResponseTo;
            return state.removeIn(['events', eventUri]);

        case actionTypes.messages.connect.successOwn:
        case actionTypes.messages.open.successOwn:
        case actionTypes.messages.chatMessage.successOwn:
            var msgFromOwner = Immutable.fromJS(action.payload.events['msg:FromSystem']);
            var eventUri = msgFromOwner.get('isResponseTo');
            return state
                .setIn(['events', msgFromOwner.get('uri')], msgFromOwner)
                .updateIn(['events', eventUri], e =>
                    // This is a good-enough solution. We assume the republishing done
                    // by the owner happens at the same time that it sends us
                    // the success-response -- so we just use the latters timestamp
                    // with the optimistic event created previously.
                    e.set('hasSentTimestamp', msgFromOwner.get('hasReceivedTimestamp'))
                     .set('hasReceivedTimestamp', msgFromOwner.get('hasReceivedTimestamp'))
                )

        case actionTypes.messages.connect.successRemote:
        case actionTypes.messages.open.successRemote:
        case actionTypes.messages.chatMessage.successRemote:
            //var eventOnRemoteNode = Immutable.fromJS(action.payload.events['msg:FromOwner']);
            var eventOnOwnNode = Immutable.fromJS(action.payload.events['msg:FromExternal']);
            var msgFromOwner = Immutable.fromJS(action.payload.events['msg:FromSystem']);
            var eventUri = msgFromOwner.get('isRemoteResponseTo');
            return state
                .setIn(['events', msgFromOwner.get('uri')], msgFromOwner)
                .setIn(['events', eventOnOwnNode.get('uri')], eventOnOwnNode)
                .setIn(['events', eventUri, 'unconfirmed'], false);


        case actionTypes.messages.connectionMessageReceived:
            var eventOnRemote = Immutable.fromJS(action.payload.events['msg:FromOwner']);
            eventOnRemote = eventOnRemote.set(
                    'eventType',
                    won.messageType2EventType[eventOnRemote.get('hasMessageType')]
            );
            var event = Immutable.fromJS(action.payload.events['msg:FromExternal'])
                .set( 'hasCorrespondingRemoteMessage', eventOnRemote );
            var eventUri = event.get('uri');
            return state
                .update('unreadEventUris', unread => unread.add(eventUri))
                .update('events', events => events.set(eventUri, event));

        case actionTypes.connections.showLatestMessages:
        case actionTypes.connections.showMoreMessages:
            //ALREADY IN NEW STATE
            var loadedEvents = action.payload.get('events');
            return state.update('events', events => events.merge(loadedEvents));


        case actionTypes.messages.connectMessageReceived:
        case actionTypes.messages.openMessageReceived:
        case actionTypes.messages.hintMessageReceived:

            //TODO flatten "hasCorrespondingRemoteMessage" to an uri. we want to store these in a
            // normalized. ensure this in the event-reducer(!)

            var event = action.payload.events.filter(e => e.uri === action.payload.receivedEvent)[0];
            var updatedState = state.update('unreadEventUris', unread => unread.add(event.uri));
            return storeConnectionRelatedData(updatedState, action.payload);

        default:
            return state;
    }
}

function storeOptimisticEvent(state, event) {
    var optimisticEvent = Immutable
        .fromJS(event)
        .set('unconfirmed', true);
    return state.setIn(['events', event.uri], optimisticEvent);
}

function storeConnectionRelatedData(state, connectionWithRelatedData) {
    const keyValuePairs = connectionWithRelatedData.events.map(e => [e.uri, Immutable.fromJS(e)]);
    const updatedEvents = Immutable.Map(keyValuePairs);
    return state.mergeDeepIn(['events'], updatedEvents);
}

var createOrUpdateUnreadEntry = function(needURI, eventData, unreadEntry){

    if(unreadEntry == null || typeof unreadEntry === 'undefined'){
        unreadEntry = {"events" : []};
        //unreadEntry.events = [];
        unreadEntry.count = 0;
    }
    unreadEntry.events.push(eventData);
    unreadEntry.timestamp=eventData.timestamp;
    unreadEntry.need = privateData.allNeeds[needURI];
    unreadEntry.count ++;
    return unreadEntry;
};
var getUnreadEventType = function(eventType){
    var unreadEventType = null;
    switch (eventType){
        case won.EVENT.HINT_RECEIVED:unreadEventType = won.UNREAD.TYPE.HINT;
            break;
        case won.EVENT.CONNECT_RECEIVED: unreadEventType = won.UNREAD.TYPE.CONNECT;
            break;
        case won.EVENT.CONNECT_SENT: unreadEventType = won.UNREAD.TYPE.CONNECT;
            break;
        case won.EVENT.OPEN_RECEIVED:unreadEventType = won.UNREAD.TYPE.MESSAGE;
            break;
        case won.EVENT.OPEN_SENT:unreadEventType = won.UNREAD.TYPE.MESSAGE;
            break;
        //  case won.Event.Message_Rece_RECEIVED: privateData.unreadEventsByNeedByType[needURI].hint.push(eventData);
        case won.EVENT.CLOSE_RECEIVED: unreadEventType = won.UNREAD.TYPE.CLOSE;
            break;
        case won.EVENT.NEED_CREATED: unreadEventType = won.UNREAD.TYPE.CREATED;
            break;
        case won.EVENT.CONNECTION_MESSAGE_RECEIVED: unreadEventType = won.UNREAD.TYPE.MESSAGE;
            break;
        case won.EVENT.CONNECTION_MESSAGE_SENT: unreadEventType = won.UNREAD.TYPE.MESSAGE;
            break;
        // case won.Event.HINT_RECEIVED: privateData.unreadEventsByNeedByType[needURI].hint.push(eventData);
    }
    return unreadEventType;
}