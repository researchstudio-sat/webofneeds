/**
 * Created by ksinger on 24.09.2015.
 */

import { actionTypes } from '../actions/actions';
import { repeatVar } from '../utils';
import Immutable from 'immutable';
import { createReducer } from 'redux-immutablejs'
import { combineReducersStable } from '../redux-utils';
import { draftsReducer } from './drafts-reducer';
import { messagesReducer } from './message-reducers'
import reduceReducers from 'reduce-reducers';
import postsReducer from './posts-reducer'
import needReducer from './need-reducer'
import eventReducer from './event-reducer'
import matchReducer from './match-reducer'
import connectionReducer from './connection-reducer'

/*
 * this reducer attaches a 'router' object to our state that keeps the routing state.
 */
import { router } from 'redux-ui-router';

const reducers = {
    router,

    /**
    simplereducer: (state = initialState, action) => {
        switch(action.type) {
            case actionTypes.moreWub:
                return state.setIn(...);
            default:
                return state;
        }
    },
    */


    connections:connectionReducer,
    drafts: draftsReducer,
    events: eventReducer,
    user: createReducer(
        //initial state
        Immutable.Map(),

        //handlers
        {
            [actionTypes.user.loggedIn]: (state, {payload: {loggedIn, email}}) => {
                if(loggedIn == true){
                    console.log('reducers.js: received successful-login action from app-server');
                    return Immutable.fromJS({loggedIn: true, email: email});
                }else{
                    console.log('reducers.js: received notlogged in action from app-server');
                    return Immutable.fromJS({loggedIn: false});
                }
            },
            [actionTypes.user.loginFailed]: (state, {payload: {loginError}}) => {
                console.log('reducers.js: received UNsuccessful-login action from app-server');
                return Immutable.fromJS({loginError: loginError});
            },
            [actionTypes.user.registerFailed]: (state, {payload: {registerError}}) => {
                console.log('reducers.js: received UNsuccessful-login action from app-server');
                return Immutable.fromJS({registerError: registerError});
            }
        }
    ),
    needs:needReducer,
    matches: matchReducer,
    postOverview:postsReducer,
    messages: messagesReducer,

    // contains the Date.now() of the last action
    lastUpdateTime: (state = Date.now(), action = {}) => Date.now(),

    config: createReducer(
        //initial state
        Immutable.Map(),

        //handlers
        {
            [actionTypes.config.update]: (state, { payload }) =>
                /*
                 * `.merge` assumes a flat config-object. should the config
                 * become nested, use `mergeDeep` instead or use a
                 * custom merging-function (or more fine-grained actions)
                 */
                state.merge(payload)
        }

    ),
}


export default reduceReducers( //passes on the state from one reducer to another


    /* note that `combineReducers` is opinionated as a root reducer for the
     * sake of convenience and ease of first use. It takes an object
     * with seperate reducers and applies each to it's seperate part of the
     * store/model. e.g.: an reducers object `{ drafts: function(state = [], action){...} }`
     * would result in a store like `{ drafts: [...] }`
     */
     combineReducersStable(Immutable.Map(reducers)),



    /*--------------------- <cross-cutting-reducer> -------------------
     *
     * combineReducers above parcels out the state to individual
     * reducers that deal with their slice on their own. Sadly not
     * all updates can work like this. Occasionally there's cross-cutting
     * concerns between parts of the state. These can be addressed in
     * this reducer.
     *
     * **How to use me:**
     *
     * * Try not to create new branches of the state here (this is
     *   what combineReducers is for)
     * * Make sure the actual methods updating a respective part
     *   of the state are in the js-file responsible for that part
     *   of the state. This is to ensure that all write-accesses
     *   to the data can be discerned from that one file.
     *
     * @dependent state: https://github.com/rackt/redux/issues/749
     *
     * also, if you need to resolve the data-dependency just for
     * a component, you can use [memoized selectors]
     * (http://rackt.org/redux/docs/recipes/ComputingDerivedData.html)
     */
     (state, action) => {
         switch(action.type) {
             /*
              * TODO try to resolve a lot of the AC-dispatching so only
              * high-level actions are left there. avoid actions that
              * trigger other actions. also, actions shouldn't have a
              * 1:1 mapping to state.
              * see: https://github.com/rackt/redux/issues/857#issuecomment-146021839
              * see: https://github.com/rackt/redux/issues/857#issuecomment-146269384
              */
             default:
                 return state;
         }
     }
    //-------------------- </cross-cutting-reducer> -------------------
)


window.ImmutableFoo = Immutable;
