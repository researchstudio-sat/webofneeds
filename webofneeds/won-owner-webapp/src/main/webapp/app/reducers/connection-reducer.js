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
        case actionTypes.connections.load:
            return action.payload.reduce(
                (updatedState, connectionWithRelatedData) =>
                    storeConnectionAndRelatedData(updatedState, connectionWithRelatedData),
                state);

        case actionTypes.messages.connectMessageReceived:
        case actionTypes.messages.hintMessageReceived:
	case actionTypes.messages.openResponseReceived;
            return storeConnectionAndRelatedData(state, action.payload);

        case actionTypes.connections.reset:
            return initialState;

        default:
            return state;
    }
}
function storeConnectionAndRelatedData(state, connectionWithRelatedData) {
    return state.setIn(
        ['connections',connectionWithRelatedData.connection.uri],
        connectionWithRelatedData);
}
