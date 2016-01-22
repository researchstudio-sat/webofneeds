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
        /*
    unreadEventsByNeedByType: {},
    unreadEventsByTypeByNeed:{
        'hint': {count: 0, timestamp: new Date().getTime() },
        'connect': {count: 0, timestamp: new Date().getTime()},
        'message': {count: 0, timestamp: new Date().getTime()},
        'close': {count: 0, timestamp: new Date().getTime()},
        'created': {count: 0, timestamp: new Date().getTime()}
    },
    */
    unreadEventUris:{}
})
export default createReducer(
    initialState,
    {

        [actionTypes.events.addUnreadEventUri]:(state,action)=>{
            return state.setIn(['unreadEventUris',action.payload.unreadUri],Immutable.fromJS(action.payload))
        },
        [actionTypes.events.read]:(state,action)=>{
            let events = state.getIn(['unreadEventUris'])
            events =  events.delete(action.payload)
            return state.setIn(['unreadEventUris'],events)
        }
    }
)
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