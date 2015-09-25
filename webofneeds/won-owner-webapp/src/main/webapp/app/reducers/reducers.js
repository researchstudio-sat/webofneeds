/**
 * Created by ksinger on 24.09.2015.
 */

import { actionTypes } from '../actions';
import { repeatVar } from '../utils';
import Immutable from 'immutable';
import { createReducer } from 'redux-immutablejs'

export const wubs = createReducer(Immutable.List(), {
    [actionTypes.moreWub]: (state, action) => {
        console.log('in moreWub-reducer: ', action);
        const howMuch = action.payload;
        const additionalWubs = Immutable.fromJS(repeatVar('wub', howMuch));
        return state.concat(additionalWubs);
    }
});
