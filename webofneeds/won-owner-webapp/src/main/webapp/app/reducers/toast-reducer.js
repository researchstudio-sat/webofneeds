import { actionTypes } from '../actions/actions.js';
import Immutable from 'immutable';
import { createReducer } from 'redux-immutablejs'
import won from '../won-es6.js';
import {
    getIn,
    generateIdString,
} from '../utils.js';
import config from '../config.js';

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
                    'Sorry, we couldn\'t find the private ID (the one in your url-bar). If ' +
                    'you copied this address make sure you <strong>copied everything</strong> and try ' +
                    '<strong>reloading the page</strong>. ' +
                    'If this doesn\'t work you can try ' +
                    '<a href="#">' +
                      'removing it' +
                    '</a> to start fresh.',
                    won.WON.errorToast,
                    {htmlEnabled: true}
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

/**
 *
 * @param allToasts
 * @param msg
 * @param type
 * @param options
 *   * htmlEnabled: set this to true only if it's really necessary and never
 *   with non-static text (e.g. never use this with toasts that show need-contents
 *   as this would open the possibility for XSS-attacks)
 * @returns {*}
 */
function pushNewToast(allToasts, msg, type, options) {
    const options_ = Object.assign({
        htmlEnabled: false,
    }, options);

    let toastType = type;
    if(!toastType){
        toastType = won.WON.infoToast;
    }

    const id = generateIdString(6);
    return allToasts.setIn([id], Immutable.fromJS({
        id: id,
        type: toastType,
        msg: msg,
        htmlEnabled: options_.htmlEnabled,
    }));
}

