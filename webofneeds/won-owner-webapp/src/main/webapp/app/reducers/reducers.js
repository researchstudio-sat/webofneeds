/**
 * Created by ksinger on 24.09.2015.
 */

import { actionTypes } from '../actions';
import { repeatVar } from '../utils';
import Immutable from 'immutable';
import { createReducer } from 'redux-immutablejs'
import { combineReducersStable } from '../redux-utils';


/*
 * this reducer attaches a 'router' object to our state that keeps the routing state.
 */
import { router } from 'redux-ui-router';

const reducers = {

    wubs: createReducer(Immutable.List(), {
        [actionTypes.moreWub]: (state, action) => {
            const howMuch = action.payload;
            const additionalWubs = Immutable.fromJS(repeatVar('wub', howMuch));
            return state.concat(additionalWubs);
        }
    }),

    drafts: createReducer(
        //initial state
        Immutable.Map(),

        //handlers
        {
            /**
             * @param {*} draftId : the draft that's type has changed
             * @param {string} type : the short-hand won-type (e.g. `'won:Demand'`)
             */
            [actionTypes.drafts.change.type]: (state, {payload:{draftId, type}}) => {
                //TODO use json-ld for state
                const stateWithDraft = guaranteeDraftExistence(state, draftId);
                return type ?
                    stateWithDraft.setIn([draftId, 'type'], type) :
                    stateWithDraft.deleteIn([draftId, 'type'])

            },
            /**
             * @param {*} draftId : the draft that's title has changed
             * @param {string} title : any user-entered text, e.g. `'I am moving and need a new couch.'`
             */
            [actionTypes.drafts.change.title]: (state, {payload:{draftId, title}}) => {
                //TODO use json-ld for state
                const stateWithDraft = guaranteeDraftExistence(state, draftId);
                return title ?
                    stateWithDraft.setIn([draftId, 'title'], title) :
                    stateWithDraft.deleteIn([draftId, 'title'])
            },
            /**
             * @param {*} draftId : the draft that's thumbnail has changed
             * @param {object} image : e.g. `{ name: 'somepic.png', type: 'image/png', data: 'iVBORw0...gAAI1=' }`
             */
            [actionTypes.drafts.change.thumbnail]: (state, {payload:{draftId, image}}) => {
                //TODO use json-ld for state
                const stateWithDraft = guaranteeDraftExistence(state, draftId);
                console.log('changed thumbnail ', image);
                return stateWithDraft.setIn([draftId, 'thumbnail'], Immutable.fromJS(image));
            },

            /**
             * @param {*} draftId : the draft to be published
             */
            [actionTypes.drafts.change.publish]: (state, {payload:{draftId}}) => {
                //TODO use json-ld for state
                let d = state.get(draftId);
                if(!d) {
                    return state;
                } else {
                    return state.setIn([draftId, 'publishState'], 'awaitingSending'); //TODO codify message-states
                }
            },
            /**
             * @param {*} draftId : the draft that has been published
             */
            [actionTypes.drafts.notifyOfSuccessfulPublish]: (state, {payload:{draftId}}) => {
                console.log('reducers.js: received successful-publish action from app-server');
                return state.remove(draftId);
            }

            //TODO delete draft once it's completely empty
            //TODO init drafts from server
            //TODO isValidNeed function to check compliance to won-ontology (and throw errors early)
        }
    ),
    user: createReducer(
        //initial state
        Immutable.Map(),

        //handlers
        {
            [actionTypes.user.receive]: (state, {payload: {username, password}}) => {
                console.log('reducers.js: received successful-login action from app-server');
                return Immutable.fromJS(payload);
            }
        }
    ),
/*
const ownposts = createReducer(
    //initial state
    Immutable.Map(),

    //handlers
    {
        [actionTypes.drafts.notifyOfSuccessfulPublish]: (state, {payload:{draftId}}) => {
            return state;
        }
    }
);
*/

/**
 * This is a convenience reducer/part of the state that holds a
 * list [( msgRendererFunction, pathToArgs )]
 */
    /*
const pendingMessages = createReducer(
    //initial state
    Immutable.List(),

    //handlers
    {
        [actionTypes.ownposts.new]: (state, payload) => {
            const {type, title, thumbnail} = payload;


        }
    }
);
*/
    /* TODO this fragment is part of an attempt to sketch a different
     * approach to asynchronity (Remove it or the thunk-based
     * solution afterwards)
     */
    enqueuedMessages: createReducer(
        //initial state
        Immutable.List(),

        //handlers
        {
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
    ),
    /* TODO this fragment is part of an attempt to sketch a different
     * approach to asynchronity (Remove it or the thunk-based
     * solution afterwards)
     */
    sentMessages: createReducer(
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
    ),
    /* TODO this fragment is part of an attempt to sketch a different
     * approach to asynchronity (Remove it or the thunk-based
     * solution afterwards)
     */
    receivedMessages: createReducer(
        //initial state
        Immutable.List(),

        //handlers
        {
            [actionTypes.messages.receive]: (state, {payload:{msg}}) =>  state.push(msg)
        }
    )
}


/* note that `combineReducers` is opinionated as a root reducer for the
 * sake of convenience and ease of first use. It takes an object
 * with seperate reducers and applies each to it's seperate part of the
 * store/model. e.g.: an reducers object `{ drafts: function(state = [], action){...} }`
 * would result in a store like `{ drafts: [...] }`
 */
export default combineReducersStable(Immutable.Map(reducers));

window.ImmutableFoo = Immutable;



/**
 * Adds an empty draft to the state if it doesn't exist yet
 * @param drafts
 * @param draftId
 * @returns {*}
 */
function guaranteeDraftExistence(drafts, draftId) {
    if(drafts.get(draftId)) {
        return drafts
    } else {
        const defaultDraft = Immutable.fromJS({ draftId });
        return drafts.set(draftId, defaultDraft);
    }
}
