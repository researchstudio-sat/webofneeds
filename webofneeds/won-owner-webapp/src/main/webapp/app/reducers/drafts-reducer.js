/**
 * Created by ksinger on 26.11.2015.
 */

import { actionTypes } from '../actions/actions';
import { repeatVar } from '../utils';
import Immutable from 'immutable';
import { createReducer } from 'redux-immutablejs'
import { combineReducersStable } from '../redux-utils';

export default createReducer(
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
