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
    //connectionsDeprecated: {},//don't use data from this map
    connections: {},
})
export default function(state = initialState, action = {}) {
    switch(action.type) {
        case actionTypes.load:
            const allPreviousConnections = action.payload.get('connections');
            return state.mergeIn(['connections'], allPreviousConnections);

        case actionTypes.connections.load:
            return action.payload.reduce(
                (updatedState, connectionWithRelatedData) =>
                    storeConnectionAndRelatedData(updatedState, connectionWithRelatedData),
                state);

        case actionTypes.messages.connectMessageReceived:
        case actionTypes.messages.hintMessageReceived:
        case actionTypes.messages.openResponseReceived:
            return storeConnectionAndRelatedData(state, action.payload);

        case actionTypes.connections.reset:
            return initialState;

        default:
            return state;
    }
}
function storeConnectionAndRelatedData(state, connectionWithRelatedData) {
    const connection = Immutable.fromJS(connectionWithRelatedData.connection);

    return state
        .setIn(['connections', connection.get('uri')], connection)
        /*
        .setIn( //TODO deletme, deprecated state-structure
            ['connectionsDeprecated',connectionWithRelatedData.connection.uri],
            connectionWithRelatedData);
            */
}
