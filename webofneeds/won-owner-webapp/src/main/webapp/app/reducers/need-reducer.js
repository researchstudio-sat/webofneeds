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
export default createReducer(
    initialState,
    {
        [actionTypes.needs.failed]: (state, {payload: {error}}) => {
            console.log('reducers.js: failed receive needlist action');
            return Immutable.fromJS({error: error});
        },
        [actionTypes.needs.received]:(state,action)=>{
            return state.setIn(['needs',action.payload.uri],Immutable.fromJS(action.payload))
        },
        [actionTypes.needs.connectionsReceived]:(state,action)=>{
            return state.setIn(['needs',action.payload.needUri,'connectionUris'],action.payload.connections)
        },
        [actionTypes.needs.clean]:(state,{})=>{
            return initialState
        }
    }

)