/**
 * Created by syim on 19.01.2016.
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
    incomingRequestsByNeed: {}

})
export default createReducer(
    initialState,
    {
        [actionTypes.requests.incomingReceived]:(state,action)=>{
            let byNeed = state.getIn(['incomingRequestsByNeed',action.payload.ownNeed['@id']])
            if(!byNeed){
                state.setIn(['incomingRequestsByNeed',action.payload.ownNeed['@id']],[action.payload])
            }else{

            }


            let match = {"ownNeedData":action.payload.ownNeed,"connections":action.payload.connections}
            return state.setIn(['matches',action.payload.connection.uri],action.payload)
        },
        [actionTypes.matches.add]:(state,action)=>{
            return state.setIn(['matches',action.payload.connection.uri],action.payload)
        }

    }

)