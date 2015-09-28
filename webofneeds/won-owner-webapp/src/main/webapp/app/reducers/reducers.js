/**
 * Created by ksinger on 24.09.2015.
 */

import { actionTypes } from '../actions';
import { repeatVar } from '../utils';
import Immutable from 'immutable';
import { createReducer } from 'redux-immutablejs'

export const wubs = createReducer(Immutable.List(), {
    [actionTypes.moreWub]: (state, action) => {
        const howMuch = action.payload;
        const additionalWubs = Immutable.fromJS(repeatVar('wub', howMuch));
        return state.concat(additionalWubs);
    }
});

export const drafts = createReducer(
    //initial state
    Immutable.fromJS({
        //list of draft objects
        draftList: [],
        // the index of the currently active draft (or undefined
        // if the draft-list is empty)
        activeDraftIdx: undefined
    }),

    //handlers
    {
        //TODO init drafts from server
        //TODO isValidNeed function to check compliance to won-ontology (and throw errors early)
        [actionTypes.drafts.new]: (state, {payload:draft}) => {
            return state
                    .set('draftList', state.get('draftList').push(draft))
                    .set('activeDraftIdx', state.get('draftList').size);
        },

        [actionTypes.drafts.select]: ( state, {payload:selectedIdx} ) => {
            if(0 <= selectedIdx && selectedIdx < state.get('draftList').size) {
                return state.set('activeDraftIdx', selectedIdx);
            } else {
                return state; //TODO throw error instead?
            }
        },

        [actionTypes.drafts.change.type]: ( state, {payload:{idx, type}} ) => {

            if(idx < 0 || idx > state.get('draftList').size)
                return state; //TODO throw error instead?

            const draft = state.get('draftList').get(idx);

            //TODO set draft-type
            type;

            return state
                .set('draftList', state.get('draftList').set(idx, draft))
                .set('activeDraftIdx', idx);
        }
    }
);
