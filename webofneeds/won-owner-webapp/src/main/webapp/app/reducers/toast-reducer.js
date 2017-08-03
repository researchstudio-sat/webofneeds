import { actionTypes } from '../actions/actions';
import Immutable from 'immutable';
import { createReducer } from 'redux-immutablejs'
import won from '../won-es6';
import {
    getIn,
    generateIdString,
} from '../utils';
import config from '../config';

const initialState = Immutable.fromJS({
});

export default function(allToasts = initialState, action = {}) {
    switch(action.type) {
        case actionTypes.toasts.test:
            allToasts = pushNewToast(allToasts, "Error Toast", won.WON.errorToast);
            allToasts = pushNewToast(allToasts, "Warning Toast", won.WON.warnToast);
            allToasts = pushNewToast(allToasts, "Info Toast", won.WON.infoToast);
            return allToasts;


        case actionTypes.logout:
            return initialState;

        case actionTypes.loginFailed:
            if(getIn(action, ['payload', 'loginError']) === 'invalid privateId') {
                return pushNewToast(
                    allToasts,
                    'Sorry, we couldn\'t find the private ID ' + getIn(action, ['payload', 'privateId']) +
                    ' (the one in your url-bar). Try reloading or removing it.',
                    won.WON.errorToast
                )
            } else {
                return allToasts;
            }

        case actionTypes.lostConnection:
            return pushNewToast(allToasts, "Lost connection - progress " +
                "can't be saved any more. Make sure your " +
                "internet-connection is working, then click \"Reconnect\"", won.WON.warnToast);
        //INFO TOASTS: won.WON.infoToast

        //WARN TOASTS: won.WON.warnToast
        //ERROR TOASTS: won.WON.errorToast
        case actionTypes.messages.closeNeed.failed:
            return pushNewToast(allToasts, "Closing Need failed", won.WON.errorToast);

        case actionTypes.messages.chatMessage.failure:
            return pushNewToast(allToasts, "Sending Chat Message failed", won.WON.errorToast);

        //SPECIFIC TOAST ACTIONS
        case actionTypes.toasts.delete:
            return allToasts.deleteIn([action.payload.get("id")]);

        default:
            return allToasts;
    }
}

function pushNewToast(allToasts, msg, type) {
    var toastType = type;
    if(!toastType){
        toastType = won.WON.infoToast;
    }
    const id = generateIdString(6);
    return allToasts.setIn([id], Immutable.fromJS({id: id, type: toastType, msg: msg}));
}

