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
    unreadEventUris:{},
})

export default function(state = initialState, action = {}) {
    switch(action.type) {

        case actionTypes.load:
            const allPreviousEvents = action.payload.get('events');
            return state.mergeIn(['events'], allPreviousEvents);

        case actionTypes.events.read:
            return state.deleteIn(['unreadEventUris', action.payload]);

        /**
         * @deprecated this is a legacy action
         */
        case actionTypes.connections.load:
            return action.payload.reduce(
                (updatedState, connectionWithRelatedData) =>
                    storeConnectionRelatedData(updatedState, connectionWithRelatedData),
                state);

        case actionTypes.messages.connectMessageReceived:
        case actionTypes.messages.hintMessageReceived:
            const event = action.payload.receivedEvent;
            const updatedState = state.setIn(
                ['unreadEventUris', event.unreadUri],
                Immutable.fromJS(event)
            );
            return storeConnectionRelatedData(updatedState, action.payload);

        default:
            return state;
    }
}
function storeConnectionRelatedData(state, connectionWithRelatedData) {
    console.log("EVENT-REDUCER STORING CONNECTION AND RELATED DATA");
    console.log(connectionWithRelatedData);
    return connectionWithRelatedData.events.reduce(

        (updatedState, event) =>
            updatedState.getIn(['events', event.uri]) ?
                updatedState : // we already know this one. no need to trigger re-rendering
                updatedState.setIn(['events', event.uri], Immutable.fromJS(event)) // add the event

        , state // start with the original state
    );
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