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
    unreadEventsByNeedByType: {},
    unreadEventsByTypeByNeed:{
        'hint': {count: 0, timestamp: new Date().getTime() },
        'connect': {count: 0, timestamp: new Date().getTime()},
        'message': {count: 0, timestamp: new Date().getTime()},
        'close': {count: 0, timestamp: new Date().getTime()},
        'created': {count: 0, timestamp: new Date().getTime()}
    },
    unreadEventUris:{}
})
export default createReducer(
    initialState,
    {

        [actionTypes.events.addUnreadEventUri]:(state,action)=>{
            return state.setIn(['unreadEventUris',action.payload.unreadUri],Immutable.fromJS(action.payload))
        },

        [actionTypes.events.addNeedToUnreadEventsByNeedByType]: (state, action) => {
            let needUri = action.payload.data.hasReceiverNeed;
            let need = state.getIn(['needs','needs',needUri]);
            let byNeedbyType = {}
            let ts = new Date().getTime();
            if(!state.get(['events','unreadEventsByNeedByType',needUri]) ){
                byNeedByType ={
                    'hint': {count:0, events: [], timestamp: ts, need: action.payload.need},
                    'connect': {count:0, events: [], timestamp: ts, need: action.payload.need},
                    'message': {count:0, events: [], timestamp: ts, need: action.payload.need},
                    'close': {count:0, events: [], timestamp: ts, need: action.payload.need},
                    'created': {count:0, events: [], timestamp: ts, need: action.payload.need},
                    'need':action.payload.need
                }
            }
            if(action.payload.data.eventType!=undefined && action.payload.data.eventType!=undefined){
                let now = new Date().getTime();

                let unreadEntry = state.getIn(['unreadEventsByNeedByType',action.payload.data.needUri,getUnreadEventType(action.payload.data.eventType)]).toJS();
                if(unreadEntry == null || typeof unreadEntry === 'undefined'){
                    unreadEntry = {"events" : []};
                    //unreadEntry.events = [];
                    unreadEntry.count = 0;
                }
                unreadEntry.events.push(action.payload.data);
                unreadEntry.timestamp=action.payload.data.timestamp;
                unreadEntry.need = action.payload.need
                unreadEntry.count ++;
                byNeedbyType[getUnreadEventType(action.payload.data.eventType)]
                state.setIn(['unreadEventsByNeedByType',action.payload.data.needUri,getUnreadEventType(action.payload.data.eventType)],Immutable.fromJS(unreadEntry))
            }
            return state.setIn(['unreadEventsByNeedByType',action.payload.data.needUri] ,Immutable.fromJS({
                'hint': {count:0, events: [], timestamp: ts, need: action.payload.need},
                'connect': {count:0, events: [], timestamp: ts, need: action.payload.need},
                'message': {count:0, events: [], timestamp: ts, need: action.payload.need},
                'close': {count:0, events: [], timestamp: ts, need: action.payload.need},
                'created': {count:0, events: [], timestamp: ts, need: action.payload.need},
                'need':action.payload.need
            }))
        },
        [actionTypes.events.addNeedToUnreadEventsByNeedByType]: (state, action) => {
            let ts = new Date().getTime();

            return state.setIn(['unreadEventsByNeedByType',action.payload.data.needUri] ,Immutable.fromJS({
                'hint': {count:0, events: [], timestamp: ts, need: action.payload.need},
                'connect': {count:0, events: [], timestamp: ts, need: action.payload.need},
                'message': {count:0, events: [], timestamp: ts, need: action.payload.need},
                'close': {count:0, events: [], timestamp: ts, need: action.payload.need},
                'created': {count:0, events: [], timestamp: ts, need: action.payload.need},
                'need':action.payload.need
            }))
        },
        [actionTypes.events.addEventToUnreadEventsByNeedByType]:(state,action)=>{
            let now = new Date().getTime();

            let unreadEntry = state.getIn(['unreadEventsByNeedByType',action.payload.data.needUri,getUnreadEventType(action.payload.data.eventType)]).toJS();
            if(unreadEntry == null || typeof unreadEntry === 'undefined'){
                unreadEntry = {"events" : []};
                //unreadEntry.events = [];
                unreadEntry.count = 0;
            }
            unreadEntry.events.push(action.payload.data);
            unreadEntry.timestamp=action.payload.data.timestamp;
            unreadEntry.need = action.payload.need
            unreadEntry.count ++;

            return state.setIn(['unreadEventsByNeedByType',action.payload.data.needUri,getUnreadEventType(action.payload.data.eventType)],Immutable.fromJS(unreadEntry))
/*            privateData.unreadEventsByNeedByType[needURI][getUnreadEventType(eventType)].timestamp = now;
            privateData.unreadEventsByNeedByType[needURI].timestamp = now;
            privateData.unreadEventsByNeedByType[needURI].count++;*/
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