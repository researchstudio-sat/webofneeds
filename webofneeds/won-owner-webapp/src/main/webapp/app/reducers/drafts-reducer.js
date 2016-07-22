/**
 * Created by ksinger on 26.11.2015.
 */

import { actionTypes } from '../actions/actions';
import { repeatVar } from '../utils';
import Immutable from 'immutable';
import { createReducer } from 'redux-immutablejs'
import { combineReducersStable } from '../redux-utils';

const initialState = Immutable.fromJS({});


export const draftsReducer = createReducer(
    //initial state
    Immutable.Map(),

    //handlers
    {
        [actionTypes.logout]: () => {
            return initialState;
        },
        /**
         * @param {*} draftId : the draft that's type has changed
         * @param {string} type : the short-hand won-type (e.g. `'won:Demand'`)
         */
        [actionTypes.drafts.change.type]: (drafts, {payload:{draftId, type}}) => {
            //TODO use json-ld for state
            const stateWithDraft = guaranteeDraftExistence(drafts, draftId);
            return type ?
                stateWithDraft.setIn([draftId, 'type'], type) :
                stateWithDraft.deleteIn([draftId, 'type'])

        },
        /**
         * @param {*} draftId : the draft that's title has changed
         * @param {string} title : any user-entered text, e.g. `'I am moving and need a new couch.'`
         */
        [actionTypes.drafts.change.title]: (drafts, {payload:{draftId, title}}) => {
            //TODO use json-ld for state
            const stateWithDraft = guaranteeDraftExistence(drafts, draftId);
            return title ?
                stateWithDraft.setIn([draftId, 'title'], title) :
                stateWithDraft.deleteIn([draftId, 'title'])
        },

        [actionTypes.drafts.change.description]: (drafts, {payload:{draftId, description}}) => {
            const stateWithDraft = guaranteeDraftExistence(drafts, draftId);
            return description ?
                stateWithDraft.setIn([draftId, 'description'], description) :
                stateWithDraft.deleteIn([draftId, 'description']);
        },

        [actionTypes.drafts.change.tags]: (drafts, {payload:{draftId, tags}}) => {
            const stateWithDraft = guaranteeDraftExistence(drafts, draftId);
            return tags ?
                stateWithDraft.setIn([draftId, 'tags'], tags) :
                stateWithDraft.deleteIn([draftId, 'tags']);
        },

        /**
         * @param {*} draftId : the draft that's thumbnail has changed
         * @param {object} image : e.g. `{ name: 'somepic.png', type: 'image/png', data: 'iVBORw0...gAAI1=' }`
         */
        [actionTypes.drafts.change.thumbnail]: (drafts, {payload:{draftId, image}}) => {
            //TODO use json-ld for state
            const stateWithDraft = guaranteeDraftExistence(drafts, draftId);
            console.log('changed thumbnail ', image);
            return stateWithDraft.setIn([draftId, 'thumbnail'], Immutable.fromJS(image));
        },

        [actionTypes.drafts.publish]: (drafts, {payload:{draftId, needUri}}) =>
            guaranteeDraftExistence(drafts, draftId)
                .setIn([draftId, 'pendingPublishingAs'], needUri),

        [actionTypes.drafts.publishSuccessful]: (drafts, {payload:{ needUri }}) => {
            const draftId = drafts
                .filter(draft => draft.get('pendingPublishingAs') === needUri)
                .map(draft => draft.get('draftId'))
                .first();
            return drafts.remove(draftId);
        }

        //TODO delete draft once it's completely empty
        //TODO init drafts from server
        //TODO isValidNeed function to check compliance to won-ontology (and throw errors early)
    }
)

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
