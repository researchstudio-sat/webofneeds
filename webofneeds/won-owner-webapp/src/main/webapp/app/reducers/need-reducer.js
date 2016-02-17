/**
 * Created by syim on 11.12.2015.
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
    needs: {}

})

export default function(state = initialState, action = {}) {
    switch(action.type) {
        case actionTypes.needs.failed:
            console.log('reducers.js: failed receive needlist action');
            return Immutable.fromJS({error: error});

        case actionTypes.needs.received:
            return state.setIn(['needs',action.payload.uri],Immutable.fromJS(action.payload))

        case actionTypes.needs.connectionsReceived:
            return state.setIn(['needs',action.payload.needUri,'connectionUris'],action.payload.connections)

        case actionTypes.needs.clean:
            return initialState

        default:
            return state;
    }
}

