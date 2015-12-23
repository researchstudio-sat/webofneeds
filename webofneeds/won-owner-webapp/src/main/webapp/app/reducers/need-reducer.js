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
    needs: []

})
export default createReducer(
    initialState,
    {
        [actionTypes.needs.received]:(state,action)=>{

            return state.get('needs').push(Immutable.fromJS(action.payload))
        },
        [actionTypes.needs.clean]:(state,{})=>{
            return Immutable.fromJS(initialState);
        }
    }

)