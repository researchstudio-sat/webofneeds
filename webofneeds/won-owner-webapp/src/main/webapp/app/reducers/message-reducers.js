/**
 * Created by ksinger on 26.11.2015.
 */

import { actionTypes } from '../actions/actions';
import { repeatVar } from '../utils';
import Immutable from 'immutable';
import { createReducer } from 'redux-immutablejs'
import { combineReducersStable } from '../redux-utils';
import { buildCreateMessage } from '../won-message-utils';

/* TODO this fragment is part of an attempt to sketch a different
 * approach to asynchronity (Remove it or the thunk-based
 * solution afterwards)
 */
const initialState = Immutable.fromJS({
    enqueued: {},
    waitingForAnswer: {},
    /**
     * TODO this field is part of the session-upgrade hack documented in:
     * https://github.com/researchstudio-sat/webofneeds/issues/381#issuecomment-172569377
     */
    resetWsRequested_Hack: false,
});
export function messagesReducerAlt(messages = initialState, action = {}) {
    switch(action.type) {
        case actionTypes.messages.openResponseReceived:
        case actionTypes.drafts.publishSuccessful:
            return messages.removeIn(['waitingForAnswer', action.payload.eventUri]);
        default:
            return messages;
    }
}
export const messagesReducer =  createReducer(
    //initial state
    Immutable.fromJS({
        enqueued: {},
        waitingForAnswer: {},
        /**
         * TODO this field is part of the session-upgrade hack documented in:
         * https://github.com/researchstudio-sat/webofneeds/issues/381#issuecomment-172569377
         */
        resetWsRequested_Hack: false,
    }),

    //handlers
    {

        [actionTypes.drafts.publish]: (messages, {payload:{eventUri, message}}) =>
            messages.setIn(['enqueued', eventUri], message),

        [actionTypes.messages.waitingForAnswer]: (messages, {payload:{ eventUri }}) => {
            const msg = messages.getIn(['enqueued', eventUri]);
            return messages
                .removeIn(['enqueued', eventUri])
                .setIn(['waitingForAnswer', eventUri], msg)

        },
        [actionTypes.messages.remoteResponseReceived]:(messages,action)=>{
            let data = messages.getIn(['waitingForAnswer',action.payload]);
            data.remoteResponse = true
            return messages.setIn(['waitingForAnswer',action.payload],data)
        },
        [actionTypes.messages.ownResponseReceived]:(messages,action)=>{
            let data = messages.getIn(['waitingForAnswer',action.payload]);
            data.ownResponse = true
            return messages.setIn(['waitingForAnswer',action.payload],data)
        },
        [actionTypes.messages.openResponseReceived]:(messages,{payload:{eventUri}})=>
            messages.removeIn(['waitingForAnswer', eventUri]),
        [actionTypes.messages.send]:(messages,action)=>
            messages.setIn(['enqueued',action.payload.eventUri],action.payload.message)
        ,
        [actionTypes.drafts.publishSuccessful]: (messages, {payload:{ publishEventUri }}) =>
            messages.removeIn(['waitingForAnswer', publishEventUri]),

        /**
         * TODO this sub-reducer is part of the session-upgrade hack documented in:
         * https://github.com/researchstudio-sat/webofneeds/issues/381#issuecomment-172569377
         */
        [actionTypes.messages.requestWsReset_Hack]: (messages, { payload = true}) =>
            messages.set('resetWsRequested_Hack', payload),

    }
);

