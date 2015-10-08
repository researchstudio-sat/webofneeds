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

const wubs = createReducer(Immutable.List(), {
    [actionTypes.moreWub]: (state, action) => {
        const howMuch = action.payload;
        const additionalWubs = Immutable.fromJS(repeatVar('wub', howMuch));
        return state.concat(additionalWubs);
    }
});

const drafts = createReducer(
    //initial state
    Immutable.Map(),

    //handlers
    {
        '@@reduxUiRouter/$stateChangeSuccess': (state, {payload}) => {
            console.log('statechange in draftreducer: ', payload);
            const draftId = payload.currentParams.draftId;
            //if draft doesn't exist yet, create it. or redirect to new random draft?

            if (state.get(draftId)) {
                // draft exists, we can display it
                return state;
            } else {
                /*
                 * TODO as long as it's just an empty object, we can simply create it as soon as
                 * data is added, to avoid creating empty drafts
                 */
                const defaultDraft = { draftId };
                return state.set(draftId, Immutable.fromJS(defaultDraft));
            }
        },
        [actionTypes.drafts.change.type]: (state, {payload:{draftId, type}}) =>
            type ? state.setIn([draftId, 'type'], type) : state.deleteIn([draftId, 'type'])

        //TODO init drafts from server
        //TODO isValidNeed function to check compliance to won-ontology (and throw errors early)
        //TODO go to new draft (change routing-state)
    }
);

/* note that `combineReducers` is opinionated as a root reducer for the
 * sake of convenience and ease of first use. It takes an object
 * with seperate reducers and applies each to it's seperate part of the
 * store/model. e.g.: an reducers object `{ drafts: function(state = [], action){...} }`
 * would result in a store like `{ drafts: [...] }`
 */
export default combineReducersStable(Immutable.Map({router, drafts, wubs}));

window.ImmutableFoo = Immutable;