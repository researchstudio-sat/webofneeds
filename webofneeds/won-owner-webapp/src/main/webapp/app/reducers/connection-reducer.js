/**
 * Created by syim on 20.01.2016.
 */
import { actionTypes } from '../actions/actions';
import { repeatVar } from '../utils';
import Immutable from 'immutable';
import { createReducer } from 'redux-immutablejs'
import { combineReducersStable } from '../redux-utils';
import { buildCreateMessage } from '../won-message-utils';
import won from '../won-es6';

const initialState = Immutable.fromJS({
    isFetching: false,
    didInvalidate: false,
    connections: Immutable.Map(),

})
export default function(state = initialState, action = {}) {
    switch(action.type) {
        case actionTypes.connections.hintsOfNeedRetrieved:
        case actionTypes.connections.add:
            return state.setIn(
                ['connections',action.payload.connection.uri],
                //TODO Immutable.fromJS(action.payload))
                action.payload)

        case actionTypes.connections.reset:
            return initialState;


        default:
            return state;
    }
}
