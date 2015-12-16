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
export const enqueuedMessagesReducer =  createReducer(
    //initial state
    Immutable.List(),

    //handlers
    {
        [actionTypes.drafts.publish]: (state, { payload: { need, nodeUri } }) => {
            console.log('about to publish ', need, ' on ', nodeUri);
            const ret = buildCreateMessage(need, nodeUri);
            const message = ret[0];
            const eventUri = ret[1];

            console.log('enqueued publish-msg: ', message);
            return state.push(message);
        },
        [actionTypes.messages.enqueue]: (state, {payload:{msg}}) => {
            console.log('enqueued ', msg);
            return state.push(msg);
        },
        [actionTypes.messages.markAsSent]: (state, {payload:{msg}}) => {
            /*
             * TODO this should use Ids, so multiple calls to markAsSent
             * don't remove more than just the one message that has been
             * sent (assuming there's multiple identical messages in the queue)
             */
            if(state.first() === msg)
                return state.slice(1);
            else
                return state;
        }
    }
);

/* TODO this fragment is part of an attempt to sketch a different
 * approach to asynchronity (Remove it or the thunk-based
 * solution afterwards)
 */
export const sentMessagesReducer = createReducer(
    //initial state
    Immutable.List(),

    //handlers
    {
        [actionTypes.messages.markAsSent]: (state, {payload:{msg}}) => state.push(msg),
        [actionTypes.messages.receive]: (state, {payload:{msg}}) =>  {
            if(state.first() === msg/*use msgIds instead*/) {
                return state.slice(1)
            } else {
                return state;
            }
        }
    }
);

/* TODO this fragment is part of an attempt to sketch a different
 * approach to asynchronity (Remove it or the thunk-based
 * solution afterwards)
 */
export const receivedMessagesReducer = createReducer(
    //initial state
    Immutable.List(),

    //handlers
    {
        [actionTypes.messages.receive]: (state, {payload:{msg}}) =>  state.push(msg)
    }
);
