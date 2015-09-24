/**
 * Created by ksinger on 24.09.2015.
 */

import { actionTypes } from '../actions';

export function wubs(state = [], action = {}) {
        switch(action.type) {
            case actionTypes.moreWub:
                console.log('reducer ', action);
                let updatedWubs = state;
                for(let i = 0; i < action.howMuch; i++) {
                    updatedWubs = updatedWubs.concat('wub');
                }
                return updatedWubs;

            default:
                return state;

        }
    }
