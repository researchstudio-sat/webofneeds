/**
 * Created by ksinger on 24.09.2015.
 */

import { actionTypes } from '../actions/actions';
import Immutable from 'immutable';
import { combineReducersStable } from '../redux-utils';
import { messagesReducer } from './message-reducers';
import reduceReducers from 'reduce-reducers';
import needReducer from './need-reducer';
import eventReducer from './event-reducer';
import userReducer from './user-reducer';
import matchReducer from './match-reducer';
import toastReducer from './toast-reducer';
import connectionReducer from './connection-reducer';

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
    events: eventReducer,
    user: userReducer,
    needs:needReducer,
    matches: matchReducer,
    messages: messagesReducer,
    toasts: toastReducer,

    // contains the Date.now() of the last action
    lastUpdateTime: (state = Date.now(), action = {}) => Date.now(),

    initialLoadFinished: (state = false, action = {}) =>
        state || action.type === actionTypes.initialPageLoad,

    loginVisible: (visible = false, action = {}) => {
        switch (action.type) {
            case actionTypes.showLogin:
                return true;

            case actionTypes.hideLogin:
            case actionTypes.login:
            case actionTypes.logout:
                return false;

            default:
                return visible;
        }
    },


    //config: createReducer(
    config: (config = Immutable.Map(), action = {}) => {
        switch(action.type) {

            case actionTypes.config.update:
                /*
                 * `.merge` assumes a flat config-object. should the config
                 * become nested, use `mergeDeep` instead or use a
                 * custom merging-function (or more fine-grained actions)
                 */
                return config.merge(action.payload);

            default:
                return config;
        }
    },
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

             /**
              * Add all actions that load connections
              * and their events. The reducer here makes
              * sure that no connections between two needs
              * that both are owned by the user, remain
              * in the state.
              */
             case actionTypes.initialPageLoad:
             case actionTypes.login:
             case actionTypes.messages.connectMessageReceived:
             case actionTypes.messages.hintMessageReceived:
                 return deleteCnctBetweenOwned(state);

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

function deleteCnctBetweenOwned(state) {
    var cnctBetweenOwned = selectCnctUrisBetweenOwnedNeeds(state);
    return cnctBetweenOwned.reduce(purgeConnection, state); //remove all those connections
}

function selectCnctUrisBetweenOwnedNeeds(state) {

    const ownedNeedsUris = state
        .getIn(['needs', 'ownNeeds'])
        .keySeq().toSet();

    const cnctBetweenOwned = state
        .get('connections')
        .filter(connection =>
            ownedNeedsUris.contains(
                connection.get('hasRemoteNeed')
            )
    );

    return cnctBetweenOwned.keySeq().toSet();
}

function purgeConnection(state, cnctUri) {
    const cnct = state.getIn(['connections', cnctUri]);
    state = state.deleteIn(['connections', cnctUri]);
    cnct.get('hasEvents').forEach(eventUri => {
        state = state.deleteIn(['events', 'events', eventUri]);
    })
    const needUri = cnct.get('belongsToNeed');

    const path = [
        'needs', 'ownNeeds', needUri,
        'hasConnections'
    ];

    state = state.updateIn(path, connections =>
        connections && connections.delete(cnctUri)
    );
    return state;
}
