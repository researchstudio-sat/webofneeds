/**
 * Created by ksinger on 26.11.2015.
 */

import { actionTypes } from '../actions/actions.js';
import { repeatVar } from '../utils.js';
import Immutable from 'immutable';
import { createReducer } from 'redux-immutablejs'
import { combineReducersStable } from '../redux-utils.js';
import { buildCreateMessage } from '../won-message-utils.js';

/* TODO this fragment is part of an attempt to sketch a different
 * approach to asynchronity (Remove it or the thunk-based
 * solution afterwards)
 */
const initialState = Immutable.fromJS({
    enqueued: {},
    waitingForAnswer: {},
});
export function messagesReducer(messages = initialState, action = {}) {
    switch(action.type) {
        case actionTypes.logout:
            return initialState;

        case actionTypes.needs.connect:            
        case actionTypes.needs.create:
            return messages.setIn(
                ['enqueued', action.payload.eventUri],
                action.payload.message
            );

        case actionTypes.needs.createSuccessful:
            return messages.removeIn(['waitingForAnswer', action.payload.publishEventUri]);

        case actionTypes.messages.chatMessage.failure:
        case actionTypes.messages.chatMessage.success:
            return messages.removeIn(['waitingForAnswer', action.payload.eventUri]);


        case actionTypes.messages.waitingForAnswer:
            const pendingEventUri = action.payload.eventUri;
            const msg = messages.getIn(['enqueued', pendingEventUri]);
            return messages
                .removeIn(['enqueued', pendingEventUri])
                .setIn(['waitingForAnswer', pendingEventUri], msg);

        case actionTypes.connections.open:
        case actionTypes.connections.sendChatMessage:
        case actionTypes.messages.send:
            return messages.setIn(
                ['enqueued', action.payload.eventUri],
                action.payload.message
            );


        case actionTypes.lostConnection:
            return messages
                .set('lostConnection', true)
                .set('reconnecting', false);

        case actionTypes.reconnect:
            return messages.set('reconnecting', true);

        case actionTypes.reconnectSuccess:
            return messages
                .set('lostConnection', false)
                .set('reconnecting', false);

        case actionTypes.messages.dispatchActionOn.registerSuccessOwn: 
        	console.log("registering for SuccessOwn");
        	const path = ['dispatchOnSuccessOwn', action.payload.eventUri];
        	const toDispatchList = messages.getIn(path);
        	if (!toDispatchList){
        		return messages.setIn(path, [action.payload.actionToDispatch]);
        	} 
        	return messages.updateIn(path, list => list.push(action.payload.actionToDispatch));

        case actionTypes.messages.dispatchActionOn.failureOwn:
        case actionTypes.messages.dispatchActionOn.successOwn:
        	console.log("cleaning up after successOwn");
        	//all the dispatching was done by the action creator. remove the queued actions now:
            return messages.removeIn(['dispatchOnSuccessOwn', action.payload.eventUri])
            			   .removeIn(['dispatchOnFailureOwn', action.payload.eventUri]);	

        case actionTypes.messages.dispatchActionOn.failureRemote:
        case actionTypes.messages.dispatchActionOn.successRemote:
        	console.log("cleaning up after successOwn");
        	//all the dispatching was done by the action creator. remove the queued actions now:
            return messages.removeIn(['dispatchOnSuccessRemote', action.payload.eventUri])
            			   .removeIn(['dispatchOnFailureRemote', action.payload.eventUri]);     
            
            
            
        default:
            return messages;
    }
}

