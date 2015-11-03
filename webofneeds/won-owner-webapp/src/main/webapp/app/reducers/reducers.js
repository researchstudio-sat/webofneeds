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
        }


        //TODO delete draft once it's completely empty
        //TODO init drafts from server
        //TODO isValidNeed function to check compliance to won-ontology (and throw errors early)
    }
);

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

/* note that `combineReducers` is opinionated as a root reducer for the
 * sake of convenience and ease of first use. It takes an object
 * with seperate reducers and applies each to it's seperate part of the
 * store/model. e.g.: an reducers object `{ drafts: function(state = [], action){...} }`
 * would result in a store like `{ drafts: [...] }`
 */
export default combineReducersStable(Immutable.Map({router, drafts, wubs}));

window.ImmutableFoo = Immutable;