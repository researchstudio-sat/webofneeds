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

        [actionTypes.messages.markAsSent]: (messages, {payload:{ eventUri }}) => {
            const msg = messages.getIn(['enqueued', eventUri]);
            return messages
                .removeIn(['enqueued', eventUri])
                .setIn(['sent', eventUri], msg)

        },

        [actionTypes.messages.markAsSuccess]: (messages, {payload:{ eventUri }}) =>
            messages.removeIn(['sent', eventUri]),

    }
);

