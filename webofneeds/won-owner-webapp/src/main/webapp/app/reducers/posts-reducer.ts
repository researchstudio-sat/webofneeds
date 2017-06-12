/**
 * Created by syim on 11.12.2015.
 */
import { actionTypes } from '../actions/actions';
import { repeatVar } from '../utils';
import Immutable from 'immutable';
import { createReducer } from 'redux-immutablejs'
import { combineReducersStable } from '../redux-utils';
import { buildCreateMessage } from '../won-message-utils';
const initialState = Immutable.fromJS({
        activePostsView: true,
        closedPostsView: true,
})
export default function(state = initialState, action = {}) {
    switch(action.type) {
        case actionTypes.posts_overview.openPostsView:
            if (state === 'undefined') {
                return initialState
            }else{
                return !Immutable.fromJS(initialState.openPostsView)
            }

        case actionTypes.logout:
            return Immutable.fromJS(initialState);

        default:
            return state;
    }
}
