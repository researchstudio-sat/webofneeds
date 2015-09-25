/**
 * Created by ksinger on 24.09.2015.
 */

import { actionTypes } from '../actions';
import { repeatVar } from '../utils';
import Immutable from 'immutable';

export function wubs(state = Immutable.List(), action = {}) {
        switch(action.type) {
            case actionTypes.moreWub:
                console.log('reducer ', action);
                //let howMuch = action.howMuch;
                const howMuch = action.payload;
                const additionalWubs = Immutable.fromJS(repeatVar('wub', howMuch));
                return state.concat(additionalWubs);

            default:
                return state;

        }
    }
