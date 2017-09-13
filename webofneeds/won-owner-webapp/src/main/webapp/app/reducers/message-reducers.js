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

        default:
            return messages;
    }
}

