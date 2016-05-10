/**
 * Created by ksinger on 10.05.2016.
 */
import { actionTypes } from '../actions/actions';
import { repeatVar } from '../utils';
import Immutable from 'immutable';
import { createReducer } from 'redux-immutablejs'
import { combineReducersStable } from '../redux-utils';
import { buildCreateMessage } from '../won-message-utils';
import won from '../won-es6';

const initialState = Immutable.fromJS({});

export default function(userData = initialState, action = {}) {
    switch(action.type) {
        case actionTypes.initialPageLoad:
        case actionTypes.login:

            //because we get payload as immutablejs-map sometimes but not always
            var immutablePayload = Immutable.fromJS(action.payload);

            var loggedIn = immutablePayload.get('loggedIn');
            var email = immutablePayload.get('email');

            if(loggedIn == true){
                console.log('reducers.js: received successful-login action from app-server');
                return Immutable.fromJS({loggedIn: true, email: email});
            }else{
                console.log('reducers.js: received notlogged in action from app-server');
                return Immutable.fromJS({loggedIn: false});
            }
        case actionTypes.logout:
            return Immutable.fromJS({loggedIn: false});

        case actionTypes.loginFailed:
            console.log('reducers.js: received UNsuccessful-login action from app-server');
            return Immutable.fromJS({loginError: action.payload.loginError});

        case actionTypes.registerFailed:
            console.log('reducers.js: received UNsuccessful-login action from app-server');
            return Immutable.fromJS({registerError: action.payload.registerError});

        default:
            return userData;
    }
}
