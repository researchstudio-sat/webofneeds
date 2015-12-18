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
        [actionTypes.drafts.publish]: (state, { payload: { need, nodeUri } }) => {
            console.log('about to publish ', need, ' on ', nodeUri);
            const { message, eventUri } = buildCreateMessage(need, nodeUri);

            console.log('enqueued publish-msg: ', message);
            /*
             * NOTE: messages aren't made immutable, as they are
             * intended to be send as-is and switching to
             * ImmutableJS-structures and back would incur
             * a runtime-overhead.
             */
            return state.setIn(['enqueued', eventUri], message);
        },

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
