/**
 * Created by ksinger on 26.11.2015.
 */

import { actionTypes } from '../actions/actions';
import { repeatVar } from '../utils';
import Immutable from 'immutable';
import { createReducer } from 'redux-immutablejs'
import { combineReducersStable } from '../redux-utils';

const initialState = Immutable.fromJS({});


export default function(drafts = initialState, action = {}) {
    switch (action.type) {
        case actionTypes.logout:
            return initialState;

        /**
         * @param {*} draftId : the draft that's type has changed
         * @param {string} type : the short-hand won-type (e.g. `'won:Demand'`)
         */
        case actionTypes.drafts.change.type:
            var { draftId, type } = action.payload;
            return update(drafts, draftId, 'type', type);

        /**
         * @param {*} draftId : the draft that's title has changed
         * @param {string} title : any user-entered text, e.g. `'I am moving and need a new couch.'`
         */
        case actionTypes.drafts.change.title:
            var {draftId, title} = action.payload;
            return update(drafts, draftId, 'title', title);

        case actionTypes.drafts.change.location:
            var {draftId, location} = action.payload;
            return update(drafts, draftId, 'location', location);

        case actionTypes.drafts.change.description: 
            var {draftId, description} = action.payload
            return update(drafts, draftId, 'description', description);
    
        case actionTypes.drafts.change.tags: 
            var {draftId, tags} = action.payload;
            return update(drafts, draftId, 'tags', tags);

        /**
         * @param {*} draftId : the draft that's thumbnail has changed
         * @param {object} image : e.g. `{ name: 'somepic.png', type: 'image/png', data: 'iVBORw0...gAAI1=' }`
         */
        case actionTypes.drafts.change.thumbnail:
            var {draftId, image} = action.payload;
            //TODO use json-ld for state
            var stateWithDraft = guaranteeDraftExistence(drafts, draftId);
            console.log('changed thumbnail ', image);
            return stateWithDraft.setIn([draftId, 'thumbnail'], Immutable.fromJS(image));

        case actionTypes.drafts.publish:
            var {draftId, needUri} = action.payload;
            return guaranteeDraftExistence(drafts, draftId)
                .setIn([draftId, 'pendingPublishingAs'], needUri);

        case actionTypes.drafts.publishSuccessful:
            var { needUri } = action.payload;
            var draftId = drafts
                .filter(draft => draft.get('pendingPublishingAs') === needUri)
                .map(draft => draft.get('draftId'))
                .first();
            return drafts.remove(draftId);

        default:
            return drafts;

        //TODO delete draft once it's completely empty
        //TODO init drafts from server
        //TODO isValidNeed function to check compliance to won-ontology (and throw errors early)
    }
}

function update(drafts, draftId, field, value) {
    const stateWithDraft = guaranteeDraftExistence(drafts, draftId);
    return value ?
        stateWithDraft.setIn([draftId, field], Immutable.fromJS(value)) :
        stateWithDraft.deleteIn([draftId, field])
}

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
