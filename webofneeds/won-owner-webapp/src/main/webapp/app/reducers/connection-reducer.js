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

const initialState = Immutable.fromJS({})
export default function(state = initialState, action = {}) {
    switch(action.type) {
        case actionTypes.load:
            const allPreviousConnections = action.payload.get('connections');
            return state.merge(allPreviousConnections);

        case actionTypes.connections.accepted:
            const acceptEvent = action.payload;
            const acceptConnectionUri = acceptEvent.hasReceiver;
            return state.setIn([acceptConnectionUri, 'hasConnectionState'], won.WON.Connected);

        case actionTypes.connections.denied:
            const deniedEvent = action.payload;
            const deniedConnectionUri = deniedEvent.hasReceiver;
            return state.setIn([deniedConnectionUri, 'hasConnectionState'], won.WON.Closed);

        case actionTypes.connections.load:
            return action.payload.reduce(
                (updatedState, connectionWithRelatedData) =>
                    storeConnectionAndRelatedData(updatedState, connectionWithRelatedData),
                state);

        case actionTypes.messages.connectMessageReceived:
        case actionTypes.messages.hintMessageReceived:
            return storeConnectionAndRelatedData(state, action.payload);

        case actionTypes.connections.reset:
            return initialState;

        default:
            return state;
    }
}
function storeConnectionAndRelatedData(state, connectionWithRelatedData) {
    console.log("STORING CONNECTION AND RELATED DATA");
    console.log(connectionWithRelatedData);
    const connection = Immutable.fromJS(connectionWithRelatedData.connection);

    return state
        .setIn([connection.get('uri')], connection)
}
