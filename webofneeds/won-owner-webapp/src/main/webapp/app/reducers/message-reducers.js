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
export const messagesReducer =  createReducer(
    //initial state
    Immutable.fromJS({
        enqueued: {},
        sent: {}
    }),

    //handlers
    {

        [actionTypes.drafts.publish]: (messages, {payload:{eventUri, message}}) =>
            messages.setIn(['enqueued', eventUri], message),
        [actionTypes.messages.markAsSent]: (state, {payload:{ eventUri }}) => {
            const msg = state.getIn(['enqueued', eventUri]);
            return state
                .removeIn(['enqueued', eventUri])
                .setIn(['sent', eventUri], msg)

        },
        [actionTypes.messages.markAsSuccess]: (state, {payload:{ eventUri }}) =>
            state.removeIn(['sent', eventUri])
    }
);
