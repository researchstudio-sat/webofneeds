/**
 * Created by syim on 11.12.2015.
 */
import { actionTypes } from '../actions/actions';
import { repeatVar } from '../utils';
import Immutable from 'immutable';
import { createReducer } from 'redux-immutablejs'
import { combineReducersStable } from '../redux-utils';
import won from '../won-es6';

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

        case actionTypes.connections.sendChatMessage:
            var eventUri = action.payload.eventUri;
            return storeOptimisticEvent(state, action.payload.optimisticEvent);

        case actionTypes.messages.chatMessage.failure:
            //var eventOnRemoteNode = action.payload.events['msg:FromOwner'];
            //var eventOnOwnNode = action.payload.events['msg:FromExternal'];
            //var connectionUri = msgFromOwner.hasReceiver;
            var msgFromOwner = action.payload.events['msg:FromSystem'];
            var eventUri = msgFromOwner.isRemoteResponseTo || msgFromOwner.isResponseTo;
            return state.removeIn(['events', eventUri]);

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
        case actionTypes.messages.connectMessageReceived:
        case actionTypes.messages.openMessageReceived:
        case actionTypes.messages.hintMessageReceived:
            //TODO events should be an object too
            var event = action.payload.events.filter(e => e.uri === action.payload.receivedEvent)[0];
            event.unreadUri = action.payload.updatedConnection;

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