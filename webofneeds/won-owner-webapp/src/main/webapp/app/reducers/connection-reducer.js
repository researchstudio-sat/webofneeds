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
export default createReducer(
    initialState,
    {
        [actionTypes.connections.hintsOfNeedRetrieved]:(state,action)=>{
            return state.setIn(
                ['connections',action.payload.connection.uri],
                //TODO Immutable.fromJS(action.payload))
                action.payload)
        },
        [actionTypes.connections.add]:(state,action)=>{
            return state.setIn(
                ['connections',action.payload.connection.uri],
                //TODO Immutable.fromJS(action.payload))
                action.payload)
        },
        [actionTypes.connections.reset]:(state,action)=>{
            return initialState;
        }

    }

)